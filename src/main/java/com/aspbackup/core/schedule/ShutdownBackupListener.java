package com.aspbackup.core.schedule;

import com.aspbackup.ASPBackup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * 伺服器关闭时自动触发备份。
 * 使用阻塞式备份确保备份完整执行后再关闭。
 */
public class ShutdownBackupListener implements Listener {

    private final ASPBackup plugin;

    public ShutdownBackupListener(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase().trim();

        // 检查是否为 stop/restart 命令
        if (!command.equals("stop") && !command.equals("restart")) {
            return;
        }

        if (!plugin.getConfigManager().getBackupConfig().isAutoOnShutdown()) {
            return;
        }

        plugin.getLogger().info("正在执行关闭备份，请稍候...");

        var targets = plugin.getConfigManager().getBackupConfig().getTargets();
        if (targets.isEmpty()) {
            plugin.getLogger().warning("未配置备份目标，跳过关闭备份。");
            return;
        }

        String targetId = targets.get(0).getId();

        // 使用阻塞式备份，确保备份完整执行后再关闭
        var taskId = plugin.getBackupManager().startBackupBlocking(
                com.aspbackup.core.backup.BackupType.FULL, targetId);

        plugin.getLogger().info("关闭备份已完成：" + taskId + "，伺服器即将关闭。");
    }
}