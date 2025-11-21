package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

public class SleepCommand implements SubCommand {

    private final IGNpcUtils plugin;

    public SleepCommand(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_sleep"));
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc == null) {
            sender.sendMessage(
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%", String.valueOf(id)));
            return;
        }
        if (!npc.getEntity().getType().equals(EntityType.PLAYER)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "only_player_type"));
            return;
        }
        CitizensAPI.getTraitFactory().getTraitClass("Sleep");
        npc.getOrAddTrait(net.citizensnpcs.trait.SleepTrait.class).setSleeping(npc.getEntity().getLocation());
        sender.sendMessage(
                MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_sleeping", "%id%", String.valueOf(id)));
    }

    @Override
    public String getName() {
        return "sleep";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
