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
            sender.sendMessage(ChatColor.RED + "用法：/aspbackup resume <任务ID>");
            return true;
        }

        String taskId = args[0];
        sender.sendMessage(ChatColor.YELLOW + "正在恢复备份任务 " + taskId + "...");

        var newTaskId = plugin.getBackupManager().resumeBackup(taskId);
        if (newTaskId != null) {
            sender.sendMessage(ChatColor.GREEN + "备份已恢复！任务ID：" + ChatColor.WHITE + newTaskId);
        } else {
            sender.sendMessage(ChatColor.RED + "无法恢复任务 " + taskId + "。检查点可能不存在或任务不在暂停状态。");
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
    public String getDescription() { return "从检查点恢复已中断的备份"; }

    @Override
    public String getName() { return "resume"; }
}