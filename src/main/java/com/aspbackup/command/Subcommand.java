package com.aspbackup.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Interface for subcommands of /aspbackup.
 */
public interface Subcommand {

    /**
     * Execute this subcommand.
     *
     * @param sender the command sender
     * @param args   the command arguments (excluding the subcommand name)
     * @return true if the command executed successfully
     */
    boolean execute(CommandSender sender, String[] args);

    /**
     * Provide tab completions for this subcommand.
     *
     * @param sender the command sender
     * @param args   the current arguments
     * @return list of completions
     */
    List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Get the permission node required for this subcommand.
     */
    String getPermission();

    /**
     * Get usage instructions for this subcommand.
     */
    String getUsage();

    /**
     * Get a short description of this subcommand.
     */
    String getDescription();

    /**
     * Get the name of this subcommand (used in help).
     */
    String getName();
}