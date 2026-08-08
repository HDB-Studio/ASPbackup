package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import com.aspbackup.core.backup.BackupState;
import com.aspbackup.core.backup.BackupTask;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Shows the status of backup operations.
 */
public class StatusCommand implements Subcommand {

    private final ASPBackup plugin;

    public StatusCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            // Show detailed status for specific task
            String taskId = args[0];
            var task = plugin.getBackupManager().getTask(taskId);
            if (task == null) {
                sender.sendMessage(ChatColor.RED + "No task found with ID: " + taskId);
                return true;
            }
            showDetailedStatus(sender, task);
            return true;
        }

        // Show summary of all tasks
        var tasks = plugin.getBackupManager().getAllTasks();
        if (tasks.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No backup tasks found.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "===== Backup Tasks =====");
        for (var task : tasks) {
            showTaskSummary(sender, task);
        }
        return true;
    }

    private void showTaskSummary(CommandSender sender, BackupTask task) {
        ChatColor color = switch (task.getState()) {
            case COMPLETED -> ChatColor.GREEN;
            case FAILED, CANCELLED -> ChatColor.RED;
            case PAUSED -> ChatColor.YELLOW;
            default -> ChatColor.AQUA;
        };

        sender.sendMessage(String.format("%s[%s] %s%s - %s%.1f%% - %s%s",
                color, task.getState().name(),
                ChatColor.WHITE, task.getTaskId(),
                ChatColor.GRAY, task.getProgress(),
                ChatColor.GRAY, task.getType().name()));
    }

    private void showDetailedStatus(CommandSender sender, BackupTask task) {
        sender.sendMessage(ChatColor.GOLD + "===== Task: " + task.getTaskId() + " =====");
        sender.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + task.getType());
        sender.sendMessage(ChatColor.YELLOW + "State: " + ChatColor.WHITE + task.getState());
        sender.sendMessage(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE + String.format("%.1f%%", task.getProgress()));
        sender.sendMessage(ChatColor.YELLOW + "Files: " + ChatColor.WHITE +
                task.getFilesProcessed() + "/" + task.getTotalFiles());
        sender.sendMessage(ChatColor.YELLOW + "Bytes: " + ChatColor.WHITE +
                formatBytes(task.getBytesProcessed()) + "/" + formatBytes(task.getTotalBytes()));
        sender.sendMessage(ChatColor.YELLOW + "Target: " + ChatColor.WHITE + task.getTargetId());
        sender.sendMessage(ChatColor.YELLOW + "Started: " + ChatColor.WHITE + task.getStartTime());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getBackupManager().getAllTaskIds();
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.status"; }

    @Override
    public String getUsage() { return "/aspbackup status [task-id]"; }

    @Override
    public String getDescription() { return "View backup operation status"; }

    @Override
    public String getName() { return "status"; }
}