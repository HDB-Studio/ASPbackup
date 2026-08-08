package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manually starts a backup operation.
 */
public class StartCommand implements Subcommand {

    private final ASPBackup plugin;

    public StartCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Parse arguments
        String type = "full";
        String targetId = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--full" -> type = "full";
                case "--incremental" -> type = "incremental";
                case "--target" -> {
                    if (i + 1 < args.length) targetId = args[++i];
                }
            }
        }

        // Use default target if not specified
        if (targetId == null) {
            var targets = plugin.getConfigManager().getBackupConfig().getTargets();
            if (targets.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "未配置备份目标！");
                return true;
            }
            targetId = targets.get(0).getId();
        }

        sender.sendMessage(ChatColor.YELLOW + "正在启动 " + type + " 备份到目标 '" + targetId + "'...");

        var taskId = plugin.getBackupManager().startBackup(
                type.equalsIgnoreCase("full") ?
                        com.aspbackup.core.backup.BackupType.FULL :
                        com.aspbackup.core.backup.BackupType.INCREMENTAL,
                targetId);

        if (taskId != null) {
            sender.sendMessage(ChatColor.GREEN + "备份已启动！任务ID：" + ChatColor.WHITE + taskId);
        } else {
            sender.sendMessage(ChatColor.RED + "备份启动失败，请查看日志了解详情。");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        String last = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length >= 1 && args[args.length - 2].equalsIgnoreCase("--target")) {
            // Complete target IDs
            for (var t : plugin.getConfigManager().getBackupConfig().getTargets()) {
                if (t.getId().toLowerCase().startsWith(last)) {
                    completions.add(t.getId());
                }
            }
        } else {
            for (String opt : List.of("--full", "--incremental", "--target")) {
                if (opt.startsWith(last)) completions.add(opt);
            }
        }

        return completions;
    }

    @Override
    public String getPermission() { return "aspbackup.start"; }

    @Override
    public String getUsage() { return "/aspbackup start [--full|--incremental] [--target <id>]"; }

    @Override
    public String getDescription() { return "手动启动备份操作"; }

    @Override
    public String getName() { return "start"; }
}