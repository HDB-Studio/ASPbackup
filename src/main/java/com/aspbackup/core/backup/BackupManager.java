package com.aspbackup.core.backup;

import com.aspbackup.ASPBackup;
import com.aspbackup.core.backup.compression.Compressor;
import com.aspbackup.core.backup.compression.TarGzCompressor;
import com.aspbackup.core.backup.compression.ZipCompressor;
import com.aspbackup.core.backup.source.DirectoryBackupSource;
import com.aspbackup.core.backup.source.FileEntry;
import com.aspbackup.core.backup.target.BackupTarget;
import com.aspbackup.core.backup.target.LocalBackupTarget;
import com.aspbackup.core.config.model.BackupConfig;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Manages all backup tasks - creation, monitoring, stopping, and resuming.
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
            Thread t = new Thread(r, "ASPbackup-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start a new backup operation asynchronously.
     */
    public String startBackup(BackupType type, String targetId) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        BackupTask task = new BackupTask(taskId, type, targetId);
        activeTasks.put(taskId, task);

        plugin.getBackupLogger().logBackupStart(taskId, type.name(), "N/A", targetId);

        backupExecutor.submit(() -> executeBackup(task));

        return taskId;
    }

    /**
     * Stop an active backup operation.
     */
    public boolean stopBackup(String taskId) {
        BackupTask task = activeTasks.get(taskId);
        if (task == null) return false;

        task.requestInterrupt();
        plugin.getBackupLogger().info(taskId, "Backup interrupt requested by administrator");
        return true;
    }

    /**
     * Resume a paused backup operation.
     */
    public String resumeBackup(String taskId) {
        BackupTask task = pausedTasks.remove(taskId);
        if (task == null) return null;

        String newTaskId = taskId + "-r";
        BackupTask resumedTask = new BackupTask(newTaskId, task.getType(), task.getTargetId());
        activeTasks.put(newTaskId, resumedTask);

        plugin.getBackupLogger().info(newTaskId, "Backup resumed from checkpoint");
        // Phase 4 will add checkpoint loading here
        backupExecutor.submit(() -> executeBackup(resumedTask));

        return newTaskId;
    }

    /**
     * Core backup execution logic.
     */
    private void executeBackup(BackupTask task) {
        long startTime = System.currentTimeMillis();
        Path tempFile = null;

        try {
            BackupConfig config = plugin.getConfigManager().getBackupConfig();

            // Phase 1: Find target
            BackupTarget target = findTarget(task.getTargetId());
            if (target == null) {
                failTask(task, "Target not found: " + task.getTargetId());
                return;
            }

            // Phase 2: Check disk space
            long requiredSpace = estimateRequiredSpace();
            if (target.getFreeSpace() < requiredSpace) {
                failTask(task, "Insufficient disk space on target. Required: " +
                        formatBytes(requiredSpace) + ", Available: " + formatBytes(target.getFreeSpace()));
                return;
            }

            // Phase 3: Collect files from all sources
            task.setState(BackupState.COLLECTING);
            List<FileEntry> allFiles = new ArrayList<>();
            long totalBytes = 0;

            for (BackupConfig.SourceDef srcDef : config.getSources()) {
                if (task.isInterrupted()) {
                    pauseTask(task);
                    return;
                }

                Path sourcePath = plugin.getDataFolder().getParentFile().getParentFile().toPath().resolve(srcDef.getPath());
                if (!Files.exists(sourcePath)) {
                    plugin.getLogger().warning("Source path does not exist: " + sourcePath);
                    continue;
                }

                DirectoryBackupSource source = new DirectoryBackupSource(
                        sourcePath, srcDef.getName(),
                        config.getFileFilter(), srcDef.getExclude(),
                        srcDef.getMaxDepth(), plugin.getLogger()
                );

                List<FileEntry> files = source.collectFiles();
                allFiles.addAll(files);
                totalBytes += files.stream().mapToLong(FileEntry::getSize).sum();
            }

            task.setTotalFiles(allFiles.size());
            task.setTotalBytes(totalBytes);
            plugin.getBackupLogger().info(task.getTaskId(),
                    String.format("Collected %d files (%s)", allFiles.size(), formatBytes(totalBytes)));

            if (allFiles.isEmpty()) {
                failTask(task, "No files collected for backup");
                return;
            }

            // Phase 4: Compress files
            task.setState(BackupState.COMPRESSING);
            Files.createDirectories(Path.of(config.getTempDirectory()));
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
            String ext = getCompressor(config).getExtension();
            tempFile = Path.of(config.getTempDirectory(), "aspbackup-" + task.getTaskId() + "-" + timestamp + ext);

            Compressor.CompressionResult result;
            try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                result = getCompressor(config).compress(allFiles, fos,
                        plugin.getDataFolder().getParentFile().getParentFile().toPath(), config.getCompressionLevel());
            }

            if (task.isInterrupted()) {
                pauseTask(task);
                return;
            }

            long compressedSize = Files.size(tempFile);
            task.setFilesProcessed(allFiles.size());
            task.setBytesProcessed(compressedSize);
            plugin.getBackupLogger().info(task.getTaskId(),
                    String.format("Compressed to %s (ratio: %.1f%%)",
                            formatBytes(compressedSize), result.getCompressionRatio() * 100));

            // Phase 5: Transfer to target
            task.setState(BackupState.TRANSFERRING);
            String backupFileName = "aspbackup-" + task.getTaskId() + "-" + timestamp + ext;
            try (InputStream fis = Files.newInputStream(tempFile)) {
                target.write(fis, backupFileName);
            }

            // Phase 6: Verify (Phase 3 will add actual verification)
            task.setState(BackupState.VERIFYING);
            task.setChecksum("pending-verification");
            task.setFinalSize(compressedSize);

            // Phase 7: Enforce retention
            target.enforceRetention();

            // Complete
            task.setState(BackupState.COMPLETED);
            task.setDurationMs(System.currentTimeMillis() - startTime);
            activeTasks.remove(task.getTaskId());
            completedTasks.put(task.getTaskId(), task);

            plugin.getBackupLogger().logBackupComplete(task.getTaskId(),
                    compressedSize, task.getDurationMs(), task.getChecksum());
            plugin.getLogger().info("Backup " + task.getTaskId() + " completed successfully in " +
                    formatDuration(task.getDurationMs()));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Backup " + task.getTaskId() + " failed", e);
            failTask(task, e.getMessage());
        } finally {
            // Clean up temp file
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }

    private BackupTarget findTarget(String targetId) {
        for (var tgt : plugin.getConfigManager().getBackupConfig().getTargets()) {
            if (tgt.getId().equals(targetId)) {
                return switch (tgt.getType().toUpperCase()) {
                    case "LOCAL" -> {
                        Path targetPath = plugin.getDataFolder().getParentFile().getParentFile().toPath().resolve(tgt.getPath());
                        yield new LocalBackupTarget(tgt.getId(), targetPath, tgt.getRetentionCount(), plugin.getLogger());
                    }
                    case "NAS" -> {
                        Path nasPath = Path.of(tgt.getPath());
                        yield new LocalBackupTarget(tgt.getId(), nasPath, tgt.getRetentionCount(), plugin.getLogger());
                    }
                    case "REMOTE" -> {
                        plugin.getLogger().warning("Remote target not yet implemented (Phase 6)");
                        yield null;
                    }
                    default -> {
                        plugin.getLogger().warning("Unknown target type: " + tgt.getType());
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
            Path sourcePath = plugin.getDataFolder().getParentFile().getParentFile().toPath().resolve(src.getPath());
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
        return total; // Conservative estimate (pre-compression)
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
        plugin.getBackupLogger().info(task.getTaskId(), "Backup paused (checkpoint will be saved in Phase 4)");
    }

    // --- Query methods ---

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
            plugin.getBackupLogger().warn(task.getTaskId(), "Backup cancelled due to plugin shutdown");
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
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        long min = ms / 60000;
        long sec = (ms % 60000) / 1000;
        return min + "m " + sec + "s";
    }
}