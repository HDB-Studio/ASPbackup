package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Resumes an interrupted backup from its checkpoint.
 */
public class ResumeCommand implements Subcommand {

    private final ASPBackup plugin;

    public ResumeCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /aspbackup resume <task-id>");
            return true;
        }

        String taskId = args[0];
        sender.sendMessage(ChatColor.YELLOW + "Resuming backup task " + taskId + "...");

        var newTaskId = plugin.getBackupManager().resumeBackup(taskId);
        if (newTaskId != null) {
            sender.sendMessage(ChatColor.GREEN + "Backup resumed! Task ID: " + ChatColor.WHITE + newTaskId);
        } else {
            sender.sendMessage(ChatColor.RED + "Cannot resume task " + taskId + ". Checkpoint may not exist or task is not in PAUSED state.");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getBackupManager().getPausedTaskIds();
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.resume"; }

    @Override
    public String getUsage() { return "/aspbackup resume <task-id>"; }

    @Override
    public String getDescription() { return "Resume an interrupted backup from its checkpoint"; }

    @Override
    public String getName() { return "resume"; }
}