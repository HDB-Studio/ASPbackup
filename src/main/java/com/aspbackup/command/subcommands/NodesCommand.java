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
            sender.sendMessage(ChatColor.RED + "用法：/aspbackup nodes <list|add|remove|status|enable|disable>");
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "list" -> nodesList(sender);
            case "status" -> nodesStatus(sender, args.length > 1 ? args[1] : null);
            default -> sender.sendMessage(ChatColor.YELLOW + "节点管理功能将在阶段6（分布式传输）中实现。");
        }
        return true;
    }

    private void nodesList(CommandSender sender) {
        var nodes = plugin.getConfigManager().getNodeConfigs();
        if (nodes.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "未配置传输节点。");
            sender.sendMessage(ChatColor.GRAY + "请在 config.yml 的 transfer.nodes 部分添加节点配置。");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "===== 传输节点 =====");
        for (var node : nodes) {
            String status = node.isEnabled() ? ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用";
            sender.sendMessage(String.format("  %s - %s:%d [%s%s]",
                    node.getId(), node.getHost(), node.getPort(), status, ChatColor.RESET));
        }
    }

    private void nodesStatus(CommandSender sender, String nodeId) {
        sender.sendMessage(ChatColor.GRAY + "节点状态监控功能将在阶段6中实现。");
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
    public String getDescription() { return "管理分布式传输节点"; }

    @Override
    public String getName() { return "nodes"; }
}