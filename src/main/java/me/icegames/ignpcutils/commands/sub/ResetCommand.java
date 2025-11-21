package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.managers.NPCManager;
import me.icegames.ignpcutils.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final NPCManager manager;

    public ResetCommand(IGNpcUtils plugin, NPCManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_reset"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "player_not_found"));
            return;
        }

        manager.resetPlayerData(target);
        sender.sendMessage(
                MessageUtil.getMessage(plugin.getMessagesConfig(), "data_reset", "%player%", target.getName()));
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
