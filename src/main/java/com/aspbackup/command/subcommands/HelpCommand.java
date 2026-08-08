package com.aspbackup.command.subcommands;

import com.aspbackup.command.ASPBackupCommand;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays help information for all subcommands or a specific one.
 */
public class HelpCommand implements Subcommand {

    private final ASPBackupCommand parent;

    public HelpCommand(ASPBackupCommand parent) {
        this.parent = parent;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            // Show help for specific subcommand
            Subcommand sub = parent.getSubcommands().get(args[0].toLowerCase());
            if (sub == null) {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                return true;
            }
            showSubcommandHelp(sender, sub);
            return true;
        }

        // Show all commands
        sender.sendMessage(ChatColor.GOLD + "===== ASPbackup Commands =====");
        sender.sendMessage("");

        Map<String, Subcommand> subs = parent.getSubcommands();
        for (Subcommand sub : subs.values()) {
            if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
                continue;
            }
            sender.sendMessage(ChatColor.YELLOW + sub.getUsage());
            sender.sendMessage(ChatColor.GRAY + "  " + sub.getDescription());
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Use /aspbackup help <command> for detailed usage.");
        return true;
    }

    private void showSubcommandHelp(CommandSender sender, Subcommand sub) {
        sender.sendMessage(ChatColor.GOLD + "===== Help: " + sub.getName() + " =====");
        sender.sendMessage(ChatColor.YELLOW + "Usage: " + sub.getUsage());
        sender.sendMessage(ChatColor.WHITE + "Description: " + sub.getDescription());
        if (sub.getPermission() != null) {
            sender.sendMessage(ChatColor.GRAY + "Permission: " + sub.getPermission());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return parent.getSubcommands().keySet().stream()
                    .filter(name -> name.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "aspbackup.command";
    }

    @Override
    public String getUsage() {
        return "/aspbackup help [subcommand]";
    }

    @Override
    public String getDescription() {
        return "Show help for ASPbackup commands";
    }

    @Override
    public String getName() {
        return "help";
    }
}