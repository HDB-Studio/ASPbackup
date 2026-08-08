package com.aspbackup.core.schedule;

import com.aspbackup.ASPBackup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * Triggers automatic backup when the server receives a /stop command.
 */
public class ShutdownBackupListener implements Listener {

    private final ASPBackup plugin;

    public ShutdownBackupListener(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase().trim();

        // Check if this is a stop/restart command
        if (!command.equals("stop") && !command.equals("restart")) {
            return;
        }

        if (!plugin.getConfigManager().getBackupConfig().isAutoOnShutdown()) {
            return;
        }

        plugin.getLogger().info("Shutdown backup initiated...");

        var targets = plugin.getConfigManager().getBackupConfig().getTargets();
        if (targets.isEmpty()) {
            plugin.getLogger().warning("No backup targets configured, skipping shutdown backup.");
            return;
        }

        String targetId = targets.get(0).getId();
        // Run backup synchronously (blocking) before shutdown proceeds
        // In Phase 2, this will be a proper async task with timeout
        var taskId = plugin.getBackupManager().startBackup(
                com.aspbackup.core.backup.BackupType.FULL, targetId);
        plugin.getLogger().info("Shutdown backup completed: " + taskId);
    }
}