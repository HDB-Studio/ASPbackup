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
 * ASPbackup - Advanced Spigot Backup Plugin
 * <p>
 * Provides comprehensive backup management with distributed transfer,
 * checkpoint/resume, integrity verification, and multi-target support.
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

        // Initialize logging first
        backupLogger = new BackupLogger(this);

        getLogger().info("============================================");
        getLogger().info("  ASPbackup v" + getDescription().getVersion() + " starting...");
        getLogger().info("============================================");

        try {
            // Initialize configuration
            configManager = new ConfigManager(this);
            configManager.load();

            // Initialize backup manager
            backupManager = new BackupManager(this);

            // Initialize transfer manager
            transferManager = new TransferManager(this);

            // Register commands
            var command = new ASPBackupCommand(this);
            var cmd = Objects.requireNonNull(getCommand("aspbackup"));
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);

            // Register event listeners
            getServer().getPluginManager().registerEvents(
                    new StartupBackupListener(this), this);
            getServer().getPluginManager().registerEvents(
                    new ShutdownBackupListener(this), this);

            // Start auto-backup scheduler if enabled
            if (configManager.getScheduleConfig().isEnabled()) {
                autoBackupScheduler = new AutoBackupScheduler(this);
                autoBackupScheduler.start();
            }

            getLogger().info("ASPbackup enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable ASPbackup", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ASPbackup shutting down...");

        // Stop auto-backup scheduler
        if (autoBackupScheduler != null) {
            autoBackupScheduler.stop();
        }

        // Shutdown transfer manager (close connections)
        if (transferManager != null) {
            transferManager.shutdown();
        }

        // Cancel any running backup tasks
        if (backupManager != null) {
            backupManager.shutdown();
        }

        // Close logger
        if (backupLogger != null) {
            backupLogger.close();
        }

        getLogger().info("ASPbackup disabled. Goodbye!");
        instance = null;
    }

    /**
     * Get the singleton instance of this plugin.
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