package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Lists backup history records.
 */
public class ListCommand implements Subcommand {

    private final ASPBackup plugin;

    public ListCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "===== Backup History =====");
        sender.sendMessage(ChatColor.GRAY + "Backup history will be available in Phase 2.");
        sender.sendMessage(ChatColor.GRAY + "Use /aspbackup status for active tasks.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return List.of("--active", "--completed", "--failed", "--all").stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.list"; }

    @Override
    public String getUsage() { return "/aspbackup list [--active|--completed|--failed|--all] [--page <n>]"; }

    @Override
    public String getDescription() { return "List backup history"; }

    @Override
    public String getName() { return "list"; }
}