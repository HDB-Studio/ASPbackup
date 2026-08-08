package com.aspbackup.core.schedule;

import com.aspbackup.ASPBackup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Triggers automatic backup when the server finishes loading.
 */
public class StartupBackupListener implements Listener {

    private final ASPBackup plugin;

    public StartupBackupListener(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (!plugin.getConfigManager().getBackupConfig().isAutoOnStart()) {
            return;
        }

        int delaySeconds = plugin.getConfigManager().getBackupConfig().getStartDelaySeconds();
        plugin.getLogger().info("Startup backup scheduled in " + delaySeconds + " seconds...");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            var targets = plugin.getConfigManager().getBackupConfig().getTargets();
            if (targets.isEmpty()) {
                plugin.getLogger().warning("No backup targets configured, skipping startup backup.");
                return;
            }

            String targetId = targets.get(0).getId();
            plugin.getBackupManager().startBackup(
                    com.aspbackup.core.backup.BackupType.FULL, targetId);
            plugin.getLogger().info("Startup backup initiated to target: " + targetId);
        }, delaySeconds * 20L); // Convert seconds to ticks
    }
}