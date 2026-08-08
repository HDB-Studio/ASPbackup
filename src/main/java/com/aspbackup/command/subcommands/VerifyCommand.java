package com.aspbackup.command.subcommands;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Verifies the integrity of a completed backup.
 */
public class VerifyCommand implements Subcommand {

    private final ASPBackup plugin;

    public VerifyCommand(ASPBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "用法：/aspbackup verify <任务ID>");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "校验功能将在阶段3中实现。");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getBackupManager().getAllTaskIds();
        }
        return Collections.emptyList();
    }

    @Override
    public String getPermission() { return "aspbackup.verify"; }

    @Override
    public String getUsage() { return "/aspbackup verify <task-id>"; }

    @Override
    public String getDescription() { return "校验已完成备份的完整性"; }

    @Override
    public String getName() { return "verify"; }
}