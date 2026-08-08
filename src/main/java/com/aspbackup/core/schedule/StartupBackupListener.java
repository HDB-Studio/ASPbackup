package com.aspbackup.core.schedule;

import com.aspbackup.ASPBackup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * 伺服器启动时自动触发备份。
 * 使用阻塞式备份确保备份完成后再继续加载流程。
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
        plugin.getLogger().info("启动备份将在 " + delaySeconds + " 秒后开始...");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            var targets = plugin.getConfigManager().getBackupConfig().getTargets();
            if (targets.isEmpty()) {
                plugin.getLogger().warning("未配置备份目标，跳过启动备份。");
                return;
            }

            String targetId = targets.get(0).getId();
            plugin.getLogger().info("正在执行启动备份，目标：" + targetId + "，请稍候...");

            // 使用阻塞式备份，确保备份完成后再继续
            plugin.getBackupManager().startBackupBlocking(
                    com.aspbackup.core.backup.BackupType.FULL, targetId);

            plugin.getLogger().info("启动备份已完成，伺服器继续加载...");
        }, delaySeconds * 20L); // 秒转 tick
    }
}