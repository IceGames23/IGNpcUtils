package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

public class SitCommand implements SubCommand {

    private final IGNpcUtils plugin;

    public SitCommand(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_sit"));
            return;
        }
        boolean silent = SubCommand.isSilent(args);
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
        npc.data().set("Sit", true);
        CitizensAPI.getTraitFactory().getTraitClass("Sit");
        npc.getOrAddTrait(net.citizensnpcs.trait.SitTrait.class).setSitting(npc.getEntity().getLocation());
        SubCommand.sendMessage(sender,
                MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_sitting", "%id%", String.valueOf(id)),
                silent);
    }

    @Override
    public String getName() {
        return "sit";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
