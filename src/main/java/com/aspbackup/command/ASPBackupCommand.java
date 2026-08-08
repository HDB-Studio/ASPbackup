package com.aspbackup.command;

import com.aspbackup.ASPBackup;
import com.aspbackup.command.subcommands.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main command executor for /aspbackup.
 * Dispatches to registered subcommands based on the first argument.
 */
public class ASPBackupCommand implements CommandExecutor, TabCompleter {

    private final ASPBackup plugin;
    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();

    public ASPBackupCommand(ASPBackup plugin) {
        this.plugin = plugin;

        // Register all subcommands
        register(new HelpCommand(this));
        register(new AboutCommand(plugin));
        register(new InfoCommand(plugin));
        register(new ReloadCommand(plugin));
        register(new StartCommand(plugin));
        register(new StopCommand(plugin));
        register(new StatusCommand(plugin));
        register(new ResumeCommand(plugin));
        register(new ListCommand(plugin));
        register(new NodesCommand(plugin));
        register(new VerifyCommand(plugin));
    }

    private void register(Subcommand subcommand) {
        subcommands.put(subcommand.getName().toLowerCase(), subcommand);
    }

    public Map<String, Subcommand> getSubcommands() {
        return Collections.unmodifiableMap(subcommands);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // No subcommand - show help
            subcommands.get("help").execute(sender, new String[0]);
            return true;
        }

        String subName = args[0].toLowerCase();
        Subcommand sub = subcommands.get(subName);

        if (sub == null) {
            sender.sendMessage(ChatColor.RED + "未知子命令：" + subName);
            sender.sendMessage(ChatColor.GRAY + "使用 /aspbackup help 查看命令列表。");
            return true;
        }

        // Check permission
        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(ChatColor.RED + "你无权使用此命令。");
            return true;
        }

        // Execute subcommand with remaining args
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        try {
            return sub.execute(sender, subArgs);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "执行命令时发生错误。");
            plugin.getLogger().warning("执行子命令时出错 '" + subName + "': " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Tab complete subcommand names
            String partial = args[0].toLowerCase();
            return subcommands.entrySet().stream()
                    .filter(e -> sender.hasPermission(e.getValue().getPermission()))
                    .map(Map.Entry::getKey)
                    .filter(name -> name.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Delegate to subcommand's tab completer
        String subName = args[0].toLowerCase();
        Subcommand sub = subcommands.get(subName);
        if (sub != null && sender.hasPermission(sub.getPermission())) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return sub.tabComplete(sender, subArgs);
        }

        return Collections.emptyList();
    }
}