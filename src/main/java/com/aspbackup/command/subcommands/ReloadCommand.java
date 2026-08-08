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
        sender.sendMessage(ChatColor.YELLOW + "Reloading ASPbackup configuration...");
        plugin.getConfigManager().reload();
        sender.sendMessage(ChatColor.GREEN + "ASPbackup configuration reloaded successfully!");
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
        return "Reload the plugin configuration from config.yml";
    }

    @Override
    public String getName() {
        return "reload";
    }
}