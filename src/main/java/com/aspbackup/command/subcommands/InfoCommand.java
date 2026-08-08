package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import com.aspbackup.core.backup.BackupState;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * 显示插件详细状态信息（别名：about + 运行状态）。
 */
public class InfoCommand implements Subcommand {

    private final ASPBackup plugin;

    public InfoCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String version = plugin.getDescription().getVersion();
        var config = plugin.getConfigManager().getBackupConfig();

        sender.sendMessage(ChatColor.GOLD + "===== ASPbackup 运行状态 =====");
        sender.sendMessage(ChatColor.YELLOW + "版本：" + ChatColor.WHITE + "v" + version);
        sender.sendMessage(ChatColor.YELLOW + "启动备份：" + ChatColor.WHITE +
                (config.isAutoOnStart() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "关闭备份：" + ChatColor.WHITE +
                (config.isAutoOnShutdown() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "定时备份：" + ChatColor.WHITE +
                (plugin.getConfigManager().getScheduleConfig().isEnabled() ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.YELLOW + "压缩格式：" + ChatColor.WHITE + config.getCompressionFormat());
        sender.sendMessage(ChatColor.YELLOW + "备份来源：" + ChatColor.WHITE + config.getSources().size() + " 个");
        sender.sendMessage(ChatColor.YELLOW + "备份目标：" + ChatColor.WHITE + config.getTargets().size() + " 个");

        // 统计活动任务
        int active = plugin.getBackupManager().getActiveTaskIds().size();
        int paused = plugin.getBackupManager().getPausedTaskIds().size();
        sender.sendMessage(ChatColor.YELLOW + "活动任务：" + ChatColor.WHITE + active + " / 暂停：" + paused);

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "使用 /aspbackup status 查看任务详情。");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.command"; }

    @Override
    public String getUsage() { return "/aspbackup info"; }

    @Override
    public String getDescription() { return "显示插件运行状态"; }

    @Override
    public String getName() { return "info"; }
}