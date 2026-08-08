package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Reloads the plugin configuration from config.yml.
 */
public class ReloadCommand implements Subcommand {

    private final ASPBackup plugin;

    public ReloadCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "正在重新加载 ASPbackup 配置...");
        plugin.getConfigManager().reload();
        sender.sendMessage(ChatColor.GREEN + "ASPbackup 配置已重新加载成功！");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "aspbackup.reload";
    }

    @Override
    public String getUsage() {
        return "/aspbackup reload";
    }

    @Override
    public String getDescription() {
        return "重新加载插件配置文件";
    }

    @Override
    public String getName() {
        return "reload";
    }
}