package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * 显示插件信息、版本和作者。
 */
public class AboutCommand implements Subcommand {

    private final ASPBackup plugin;

    public AboutCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String version = plugin.getDescription().getVersion();
        String apiVersion = plugin.getDescription().getAPIVersion();

        sender.sendMessage(ChatColor.GOLD + "===== ASPbackup 插件信息 =====");
        sender.sendMessage(ChatColor.YELLOW + "名称：" + ChatColor.WHITE + "ASPbackup");
        sender.sendMessage(ChatColor.YELLOW + "版本：" + ChatColor.WHITE + "v" + version);
        sender.sendMessage(ChatColor.YELLOW + "API 版本：" + ChatColor.WHITE + apiVersion);
        sender.sendMessage(ChatColor.YELLOW + "作者：" + ChatColor.WHITE + "HDB-Studio");
        sender.sendMessage(ChatColor.YELLOW + "网站：" + ChatColor.AQUA + "https://github.com/HDB-Studio/ASPbackup");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "高级备份系统，支持分布式传输、断点续传和完整性校验。");
        sender.sendMessage(ChatColor.GRAY + "使用 /aspbackup help 查看所有命令。");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.command"; }

    @Override
    public String getUsage() { return "/aspbackup about"; }

    @Override
    public String getDescription() { return "显示插件信息"; }

    @Override
    public String getName() { return "about"; }
}