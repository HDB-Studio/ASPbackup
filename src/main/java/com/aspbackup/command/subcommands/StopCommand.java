package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Stops a running backup operation.
 */
public class StopCommand implements Subcommand {

    private final ASPBackup plugin;

    public StopCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /aspbackup stop <task-id>");
            return true;
        }

        String taskId = args[0];
        boolean stopped = plugin.getBackupManager().stopBackup(taskId);

        if (stopped) {
            sender.sendMessage(ChatColor.GREEN + "Backup task " + taskId + " has been stopped.");
            sender.sendMessage(ChatColor.GRAY + "A checkpoint has been saved. Use /aspbackup resume " + taskId + " to continue.");
        } else {
            sender.sendMessage(ChatColor.RED + "No active backup task found with ID: " + taskId);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getBackupManager().getActiveTaskIds();
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.stop"; }

    @Override
    public String getUsage() { return "/aspbackup stop <task-id>"; }

    @Override
    public String getDescription() { return "Stop an active backup operation and save a checkpoint"; }

    @Override
    public String getName() { return "stop"; }
}