package me.icegames.ignpcutils.commands;

import org.bukkit.command.CommandSender;

public interface SubCommand {
    void execute(CommandSender sender, String[] args);

    String getName();

    String getPermission();

    /**
     * Check if silent flag is present in args
     */
    static boolean isSilent(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("silent:true") || arg.equalsIgnoreCase("silent")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send message only if not silent
     */
    static void sendMessage(CommandSender sender, String message, boolean silent) {
        if (!silent) {
            sender.sendMessage(message);
        }
    }
}
