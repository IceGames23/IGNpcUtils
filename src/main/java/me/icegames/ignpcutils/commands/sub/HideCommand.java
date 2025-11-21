package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.managers.NPCManager;
import me.icegames.ignpcutils.util.MessageUtil;
import me.icegames.ignpcutils.util.NPCResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HideCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final NPCManager manager;
    private final NPCResolver resolver;

    public HideCommand(IGNpcUtils plugin, NPCManager manager, NPCResolver resolver) {
        this.plugin = plugin;
        this.manager = manager;
        this.resolver = resolver;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_hide"));
            return;
        }

        // Resolve NPC reference (ID or alias)
        List<Integer> npcIds = resolver.resolve(args[1]);
        if (npcIds.isEmpty() || npcIds.size() > 1) {
            sender.sendMessage(
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_npc_reference", "%ref%", args[1]));
            return;
        }
        int id = npcIds.get(0);

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "player_not_found"));
            return;
        }

        manager.hideNPCFromPlayer(id, target);
        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_hidden", "%id%", String.valueOf(id),
                "%player%", target.getName()));
    }

    @Override
    public String getName() {
        return "hide";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
