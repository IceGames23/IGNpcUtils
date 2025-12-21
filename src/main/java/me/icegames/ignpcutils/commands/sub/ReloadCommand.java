package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.managers.NPCManager;
import me.icegames.ignpcutils.util.MessageUtil;
import org.bukkit.command.CommandSender;

import static org.bukkit.Bukkit.getLogger;

public class ReloadCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final NPCManager manager;

    public ReloadCommand(IGNpcUtils plugin, NPCManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean silent = SubCommand.isSilent(args);
        try {
            plugin.reloadConfig();
            plugin.loadMessages();
            manager.loadFromConfig();
            // Re-initialize storage if needed, though currently not fully supported without
            // restart
            // manager.getStorage().init(); // Optional: consider if we want to support
            // hot-swapping DBs
            SubCommand.sendMessage(sender, MessageUtil.getMessage(plugin.getMessagesConfig(), "reload"), silent);
            getLogger().info("Plugin successfully reloaded.");
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "error_reload"));
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
