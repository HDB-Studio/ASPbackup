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
                sender.sendMessage(ChatColor.RED + "No backup targets configured!");
                return true;
            }
            targetId = targets.get(0).getId();
        }

        sender.sendMessage(ChatColor.YELLOW + "Starting " + type + " backup to target '" + targetId + "'...");

        var taskId = plugin.getBackupManager().startBackup(
                type.equalsIgnoreCase("full") ?
                        com.aspbackup.core.backup.BackupType.FULL :
                        com.aspbackup.core.backup.BackupType.INCREMENTAL,
                targetId);

        if (taskId != null) {
            sender.sendMessage(ChatColor.GREEN + "Backup started! Task ID: " + ChatColor.WHITE + taskId);
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to start backup. Check logs for details.");
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
    public String getDescription() { return "Start a manual backup operation"; }

    @Override
    public String getName() { return "start"; }
}