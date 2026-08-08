package com.aspbackup;

import com.aspbackup.command.ASPBackupCommand;
import com.aspbackup.core.config.ConfigManager;
import com.aspbackup.core.backup.BackupManager;
import com.aspbackup.core.transfer.TransferManager;
import com.aspbackup.logging.BackupLogger;
import com.aspbackup.core.schedule.StartupBackupListener;
import com.aspbackup.core.schedule.ShutdownBackupListener;
import com.aspbackup.core.schedule.AutoBackupScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

/**
 * ASPbackup - Minecraft Spigot 伺服器进阶备份插件
 * <p>
 * 提供全面的备份管理功能，包括分散式传输、断点续传、完整性验证和多目标支持。
 */
public final class ASPBackup extends JavaPlugin {

    private static ASPBackup instance;

    private ConfigManager configManager;
    private BackupLogger backupLogger;
    private BackupManager backupManager;
    private TransferManager transferManager;
    private AutoBackupScheduler autoBackupScheduler;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化日志系统
        backupLogger = new BackupLogger(this);

        getLogger().info("============================================");
        getLogger().info("  ASPbackup v" + getDescription().getVersion() + " 正在启动...");
        getLogger().info("============================================");

        try {
            // 初始化配置
            configManager = new ConfigManager(this);
            configManager.load();

            // 初始化备份管理器
            backupManager = new BackupManager(this);

            // 初始化传输管理器
            transferManager = new TransferManager(this);

            // 注册命令
            var command = new ASPBackupCommand(this);
            var cmd = Objects.requireNonNull(getCommand("aspbackup"));
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);

            // 注册事件监听器
            getServer().getPluginManager().registerEvents(
                    new StartupBackupListener(this), this);
            getServer().getPluginManager().registerEvents(
                    new ShutdownBackupListener(this), this);

            // 如果启用定时备份，启动调度器
            if (configManager.getScheduleConfig().isEnabled()) {
                autoBackupScheduler = new AutoBackupScheduler(this);
                autoBackupScheduler.start();
            }

            getLogger().info("ASPbackup 已成功启用！");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "ASPbackup 启用失败", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ASPbackup 正在关闭...");

        // 停止定时备份调度器
        if (autoBackupScheduler != null) {
            autoBackupScheduler.stop();
        }

        // 关闭传输管理器（关闭所有连接）
        if (transferManager != null) {
            transferManager.shutdown();
        }

        // 取消所有正在执行的备份任务
        if (backupManager != null) {
            backupManager.shutdown();
        }

        // 关闭日志系统
        if (backupLogger != null) {
            backupLogger.close();
        }

        getLogger().info("ASPbackup 已关闭。再见！");
        instance = null;
    }

    /**
     * 获取此插件的单例实例。
     */
    public static ASPBackup getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BackupLogger getBackupLogger() {
        return backupLogger;
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    public TransferManager getTransferManager() {
        return transferManager;
    }
}