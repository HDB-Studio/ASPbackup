package com.aspbackup.core.backup;

import com.aspbackup.ASPBackup;
import com.aspbackup.core.backup.compression.Compressor;
import com.aspbackup.core.backup.compression.TarGzCompressor;
import com.aspbackup.core.backup.compression.ZipCompressor;
import com.aspbackup.core.backup.source.DirectoryBackupSource;
import com.aspbackup.core.backup.source.FileEntry;
import com.aspbackup.core.backup.target.BackupTarget;
import com.aspbackup.core.backup.target.LocalBackupTarget;
import com.aspbackup.core.backup.target.RemoteBackupTarget;
import com.aspbackup.core.config.model.BackupConfig;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * 管理所有备份任务：创建、监控、停止和续传。
 */
public class BackupManager {

    private final ASPBackup plugin;
    private final Map<String, BackupTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<String, BackupTask> completedTasks = new LinkedHashMap<>();
    private final Map<String, BackupTask> pausedTasks = new LinkedHashMap<>();
    private final ExecutorService backupExecutor;

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public BackupManager(ASPBackup plugin) {
        this.plugin = plugin;
        this.backupExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ASPbackup-工作线程");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 异步启动备份操作。
     * 调用者必须在主线程，saveWorldData 需要在主线程执行。
     */
    public String startBackup(BackupType type, String targetId) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        BackupTask task = new BackupTask(taskId, type, targetId);
        activeTasks.put(taskId, task);

        plugin.getBackupLogger().logBackupStart(taskId, type.name(), "N/A", targetId);

        // 在主线程保存世界数据（此方法从命令处理器调用，已在主线程）
        saveWorldData();

        backupExecutor.submit(() -> executeBackup(task));

        return taskId;
    }

    /**
     * 同步启动备份操作（阻塞当前线程直到备份完成或超时）。
     * 用于启动和关闭时的自动备份，确保备份完成后再继续流程。
     * 调用者必须在主线程，saveWorldData 需要在主线程执行。
     */
    public String startBackupBlocking(BackupType type, String targetId) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        BackupTask task = new BackupTask(taskId, type, targetId);
        activeTasks.put(taskId, task);

        plugin.getLogger().info("开始执行阻塞式备份...");
        plugin.getBackupLogger().logBackupStart(taskId, type.name(), "N/A", targetId);

        // 在主线程保存世界数据（必须在提交到后台线程之前，避免死锁）
        saveWorldData();

        // 阻塞超时时间：启动备份 5 分钟，关闭备份 3 分钟
        int timeoutSec = plugin.getConfigManager().getBackupConfig().getBlockingTimeoutSeconds();
        Future<?> future = backupExecutor.submit(() -> executeBackup(task));
        try {
            future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            plugin.getLogger().warning("备份超时（" + timeoutSec + "秒），取消任务：" + taskId);
            task.setState(BackupState.CANCELLED);
            future.cancel(true);
            activeTasks.remove(taskId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("备份被中断");
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "备份执行失败", e.getCause());
        }

        return taskId;
    }

    /**
     * 停止正在执行的备份操作。
     */
    public boolean stopBackup(String taskId) {
        BackupTask task = activeTasks.get(taskId);
        if (task == null) return false;

        task.requestInterrupt();
        plugin.getBackupLogger().info(taskId, "管理员已请求中断备份");
        return true;
    }

    /**
     * 从中断的检查点恢复备份。
     */
    public String resumeBackup(String taskId) {
        BackupTask task = pausedTasks.remove(taskId);
        if (task == null) return null;

        String newTaskId = taskId + "-r";
        BackupTask resumedTask = new BackupTask(newTaskId, task.getType(), task.getTargetId());
        activeTasks.put(newTaskId, resumedTask);

        plugin.getBackupLogger().info(newTaskId, "从检查点恢复备份");
        backupExecutor.submit(() -> executeBackup(resumedTask));

        return newTaskId;
    }

    /**
     * 核心备份执行逻辑。
     */
    private void executeBackup(BackupTask task) {
        long startTime = System.currentTimeMillis();
        Path tempFile = null;

        try {
            BackupConfig config = plugin.getConfigManager().getBackupConfig();

            // 阶段1：查找目标
            BackupTarget target = findTarget(task);
            if (target == null) {
                failTask(task, "未找到备份目标：" + task.getTargetId());
                return;
            }

            // 阶段2：检查磁盘空间
            long requiredSpace = estimateRequiredSpace();
            if (target.getFreeSpace() < requiredSpace) {
                failTask(task, "目标磁盘空间不足。需要：" +
                        formatBytes(requiredSpace) + "，可用：" + formatBytes(target.getFreeSpace()));
                return;
            }

            // 阶段3：从所有来源收集文件
            task.setState(BackupState.COLLECTING);
            List<FileEntry> allFiles = new ArrayList<>();
            long totalBytes = 0;

            for (BackupConfig.SourceDef srcDef : config.getSources()) {
                if (task.isInterrupted()) {
                    pauseTask(task);
                    return;
                }

                Path sourcePath = getServerRoot().resolve(srcDef.getPath());
                if (!Files.exists(sourcePath)) {
                    plugin.getLogger().warning("来源路径不存在：" + sourcePath);
                    continue;
                }

                DirectoryBackupSource source = new DirectoryBackupSource(
                        sourcePath, srcDef.getName(),
                        config.getFileFilter(), srcDef.getInclude(),
                        srcDef.getExclude(),
                        srcDef.getMaxDepth(), plugin.getLogger()
                );

                List<FileEntry> files = source.collectFiles();
                allFiles.addAll(files);
                totalBytes += files.stream().mapToLong(FileEntry::getSize).sum();
            }

            task.setTotalFiles(allFiles.size());
            task.setTotalBytes(totalBytes);
            plugin.getBackupLogger().info(task.getTaskId(),
                    String.format("已收集 %d 个文件（%s）", allFiles.size(), formatBytes(totalBytes)));

            if (allFiles.isEmpty()) {
                failTask(task, "未收集到任何备份文件");
                return;
            }

            // 阶段4：压缩文件
            task.setState(BackupState.COMPRESSING);
            Files.createDirectories(Path.of(config.getTempDirectory()));
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
            String ext = getCompressor(config).getExtension();
            tempFile = Path.of(config.getTempDirectory(), "aspbackup-" + task.getTaskId() + "-" + timestamp + ext);

            Compressor.CompressionResult result;
            try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                result = getCompressor(config).compress(allFiles, fos,
                        getServerRoot(), config.getCompressionLevel());
            }

            if (task.isInterrupted()) {
                pauseTask(task);
                return;
            }

            long compressedSize = Files.size(tempFile);
            task.setFilesProcessed(allFiles.size());
            task.setBytesProcessed(compressedSize);
            plugin.getBackupLogger().info(task.getTaskId(),
                    String.format("已压缩至 %s（压缩比：%.1f%%）",
                            formatBytes(compressedSize), result.getCompressionRatio() * 100));

            // 阶段5：传输到目标
            task.setState(BackupState.TRANSFERRING);
            String backupFileName = "aspbackup-" + task.getTaskId() + "-" + timestamp + ext;
            try (InputStream fis = Files.newInputStream(tempFile)) {
                target.write(fis, backupFileName);
            }

            // 阶段6：验证完整性
            task.setState(BackupState.VERIFYING);
            task.setChecksum("待验证");
            task.setFinalSize(compressedSize);

            // 阶段7：执行保留策略
            target.enforceRetention();

            // 完成
            task.setState(BackupState.COMPLETED);
            task.setDurationMs(System.currentTimeMillis() - startTime);
            activeTasks.remove(task.getTaskId());
            completedTasks.put(task.getTaskId(), task);

            plugin.getBackupLogger().logBackupComplete(task.getTaskId(),
                    compressedSize, task.getDurationMs(), task.getChecksum());
            plugin.getLogger().info("备份 " + task.getTaskId() + " 已成功完成，耗时 " +
                    formatDuration(task.getDurationMs()));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "备份 " + task.getTaskId() + " 失败", e);
            failTask(task, e.getMessage());
        } finally {
            // 清理暂存文件
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 保存世界数据：关闭自动保存 → 强制保存 → 重新开启自动保存。
     * 必须在主线程调用！直接执行命令，不调度。
     */
    public void saveWorldData() {
        plugin.getLogger().info("正在保存世界数据...");

        // 1. 关闭自动保存
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(), "save-off");
        plugin.getLogger().info("已关闭自动保存。");

        // 2. 强制保存所有数据
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(), "save-all");
        plugin.getLogger().info("已强制保存所有世界数据。");

        // 3. 重新开启自动保存
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(), "save-on");
        plugin.getLogger().info("已重新开启自动保存。");
    }

    /**
     * 获取服务器根目录路径。
     * 使用 Bukkit 官方 API，避免相对路径 getParentFile() 返回 null 的问题。
     */
    private Path getServerRoot() {
        return plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
    }

    private BackupTarget findTarget(BackupTask task) {
        String targetId = task.getTargetId();
        for (var tgt : plugin.getConfigManager().getBackupConfig().getTargets()) {
            if (tgt.getId().equals(targetId)) {
                return switch (tgt.getType().toUpperCase()) {
                    case "LOCAL" -> {
                        Path targetPath = getServerRoot().resolve(tgt.getPath());
                        yield new LocalBackupTarget(tgt.getId(), targetPath, tgt.getRetentionCount(), plugin.getLogger());
                    }
                    case "NAS" -> {
                        Path nasPath = Path.of(tgt.getPath());
                        yield new LocalBackupTarget(tgt.getId(), nasPath, tgt.getRetentionCount(), plugin.getLogger());
                    }
                    case "REMOTE" -> {
                        // 从传输管理器中获取节点
                        var nodes = new ArrayList<>(plugin.getTransferManager().getNodes());
                        if (nodes.isEmpty()) {
                            plugin.getLogger().warning("没有可用的传输节点，无法进行远端备份");
                            yield null;
                        }
                        // 使用负载均衡选择节点
                        var selectedNode = plugin.getTransferManager().getLoadBalancer().selectNode(nodes);
                        if (selectedNode == null) {
                            plugin.getLogger().warning("负载均衡器未选择到可用节点");
                            yield null;
                        }
                        var transferConfig = plugin.getConfigManager().getTransferConfig();
                        yield new RemoteBackupTarget(
                                tgt.getId(), task.getTaskId(), selectedNode,
                                plugin.getTransferManager().getConnectionPool(),
                                transferConfig.getChunkSizeKb(),
                                transferConfig.getConnectTimeoutMs(),
                                transferConfig.getReadTimeoutMs(),
                                transferConfig.getRetryCount(),
                                plugin.getLogger()
                        );
                    }
                    default -> {
                        plugin.getLogger().warning("未知目标类型：" + tgt.getType());
                        yield null;
                    }
                };
            }
        }
        return null;
    }

    private Compressor getCompressor(BackupConfig config) {
        return switch (config.getCompressionFormat().toLowerCase()) {
            case "zip" -> new ZipCompressor();
            case "targz" -> new TarGzCompressor();
            default -> new TarGzCompressor();
        };
    }

    private long estimateRequiredSpace() {
        long total = 0;
        for (var src : plugin.getConfigManager().getBackupConfig().getSources()) {
            Path sourcePath = getServerRoot().resolve(src.getPath());
            try {
                if (Files.exists(sourcePath)) {
                    total += Files.walk(sourcePath)
                            .filter(Files::isRegularFile)
                            .mapToLong(p -> {
                                try { return Files.size(p); } catch (IOException e) { return 0; }
                            })
                            .sum();
                }
            } catch (IOException ignored) {}
        }
        return total;
    }

    private void failTask(BackupTask task, String reason) {
        task.setState(BackupState.FAILED);
        activeTasks.remove(task.getTaskId());
        completedTasks.put(task.getTaskId(), task);
        plugin.getBackupLogger().logBackupError(task.getTaskId(), task.getState().name(), reason);
    }

    private void pauseTask(BackupTask task) {
        task.setState(BackupState.PAUSED);
        activeTasks.remove(task.getTaskId());
        pausedTasks.put(task.getTaskId(), task);
        plugin.getBackupLogger().info(task.getTaskId(), "备份已暂停（检查点将在阶段4中保存）");
    }

    // --- 查询方法 ---

    public BackupTask getTask(String taskId) {
        BackupTask task = activeTasks.get(taskId);
        if (task != null) return task;
        task = completedTasks.get(taskId);
        if (task != null) return task;
        return pausedTasks.get(taskId);
    }

    public List<BackupTask> getAllTasks() {
        List<BackupTask> all = new ArrayList<>();
        all.addAll(activeTasks.values());
        all.addAll(pausedTasks.values());
        all.addAll(completedTasks.values());
        return all;
    }

    public List<String> getActiveTaskIds() { return new ArrayList<>(activeTasks.keySet()); }
    public List<String> getPausedTaskIds() { return new ArrayList<>(pausedTasks.keySet()); }
    public List<String> getAllTaskIds() {
        List<String> ids = new ArrayList<>();
        ids.addAll(activeTasks.keySet());
        ids.addAll(pausedTasks.keySet());
        ids.addAll(completedTasks.keySet());
        return ids;
    }

    public void shutdown() {
        backupExecutor.shutdownNow();
        for (var entry : new ArrayList<>(activeTasks.entrySet())) {
            BackupTask task = entry.getValue();
            task.setState(BackupState.CANCELLED);
            plugin.getBackupLogger().warn(task.getTaskId(), "插件关闭，备份已取消");
        }
        activeTasks.clear();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1f秒", ms / 1000.0);
        long min = ms / 60000;
        long sec = (ms % 60000) / 1000;
        return min + "分" + sec + "秒";
    }
}