package com.aspbackup.core.schedule;

import com.aspbackup.ASPBackup;
import com.aspbackup.core.backup.BackupType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Schedules periodic automatic backups based on configuration.
 */
public class AutoBackupScheduler {

    private final ASPBackup plugin;
    private BukkitTask task;

    // Track last backup time
    private long lastBackupTime = 0;

    public AutoBackupScheduler(ASPBackup plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the auto-backup scheduler.
     */
    public void start() {
        int intervalMinutes = plugin.getConfigManager().getScheduleConfig().getIntervalMinutes();
        long intervalTicks = intervalMinutes * 60L * 20L; // Convert to ticks

        // Check every 60 seconds if a backup is due
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            long intervalMs = intervalMinutes * 60L * 1000L;

            if (lastBackupTime == 0 || (now - lastBackupTime) >= intervalMs) {
                if (isInQuietHours()) {
                    return; // Skip backup during quiet hours
                }

                lastBackupTime = now;

                // Run backup on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    var targets = plugin.getConfigManager().getBackupConfig().getTargets();
                    if (targets.isEmpty()) return;

                    String targetId = plugin.getConfigManager().getScheduleConfig().getTargetId();
                    String typeStr = plugin.getConfigManager().getScheduleConfig().getBackupType();
                    BackupType type = "incremental".equalsIgnoreCase(typeStr)
                            ? BackupType.INCREMENTAL : BackupType.FULL;

                    plugin.getBackupManager().startBackup(type, targetId);
                    plugin.getLogger().info("Scheduled backup executed to target: " + targetId);
                });
            }
        }, 1200L, 1200L); // Initial delay 60s, check every 60s

        plugin.getLogger().info("Auto-backup scheduler started (interval: " + intervalMinutes + " min)");
    }

    /**
     * Stop the auto-backup scheduler.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            plugin.getLogger().info("Auto-backup scheduler stopped.");
        }
    }

    /**
     * Check if the current time falls within configured quiet hours.
     */
    private boolean isInQuietHours() {
        var quietHours = plugin.getConfigManager().getScheduleConfig().getQuietHours();
        if (quietHours.isEmpty()) return false;

        java.time.LocalTime now = java.time.LocalTime.now();
        for (String range : quietHours) {
            try {
                String[] parts = range.split("-");
                if (parts.length != 2) continue;
                java.time.LocalTime start = java.time.LocalTime.parse(parts[0].trim());
                java.time.LocalTime end = java.time.LocalTime.parse(parts[1].trim());
                if (start.isBefore(end)) {
                    // Normal range (e.g., 02:00-04:00)
                    if (!now.isBefore(start) && now.isBefore(end)) return true;
                } else {
                    // Overnight range (e.g., 23:00-02:00)
                    if (!now.isBefore(start) || now.isBefore(end)) return true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid quiet-hours format: " + range);
            }
        }
        return false;
    }
}