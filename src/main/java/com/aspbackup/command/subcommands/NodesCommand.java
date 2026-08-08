package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Manages distributed transfer nodes.
 */
public class NodesCommand implements Subcommand {

    private final ASPBackup plugin;

    public NodesCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /aspbackup nodes <list|add|remove|status|enable|disable>");
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "list" -> nodesList(sender);
            case "status" -> nodesStatus(sender, args.length > 1 ? args[1] : null);
            default -> sender.sendMessage(ChatColor.YELLOW + "Node management will be available in Phase 6 (distributed transfer).");
        }
        return true;
    }

    private void nodesList(CommandSender sender) {
        var nodes = plugin.getConfigManager().getNodeConfigs();
        if (nodes.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No transfer nodes configured.");
            sender.sendMessage(ChatColor.GRAY + "Add nodes in config.yml under transfer.nodes section.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "===== Transfer Nodes =====");
        for (var node : nodes) {
            String status = node.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
            sender.sendMessage(String.format("  %s - %s:%d [%s%s]",
                    node.getId(), node.getHost(), node.getPort(), status, ChatColor.RESET));
        }
    }

    private void nodesStatus(CommandSender sender, String nodeId) {
        sender.sendMessage(ChatColor.GRAY + "Node status monitoring will be available in Phase 6.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return List.of("list", "add", "remove", "status", "enable", "disable").stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            return plugin.getConfigManager().getNodeConfigs().stream()
                    .map(n -> n.getId())
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.nodes"; }

    @Override
    public String getUsage() { return "/aspbackup nodes <list|add|remove|status|enable|disable>"; }

    @Override
    public String getDescription() { return "Manage distributed transfer nodes"; }

    @Override
    public String getName() { return "nodes"; }
}