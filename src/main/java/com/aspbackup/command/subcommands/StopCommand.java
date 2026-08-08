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
            sender.sendMessage(ChatColor.RED + "用法：/aspbackup stop <任务ID>");
            return true;
        }

        String taskId = args[0];
        boolean stopped = plugin.getBackupManager().stopBackup(taskId);

        if (stopped) {
            sender.sendMessage(ChatColor.GREEN + "备份任务 " + taskId + " 已停止。");
            sender.sendMessage(ChatColor.GRAY + "已保存检查点。使用 /aspbackup resume " + taskId + " 继续执行。");
        } else {
            sender.sendMessage(ChatColor.RED + "未找到活动备份任务，ID：" + taskId);
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
    public String getDescription() { return "停止活动备份并保存检查点"; }

    @Override
    public String getName() { return "stop"; }
}