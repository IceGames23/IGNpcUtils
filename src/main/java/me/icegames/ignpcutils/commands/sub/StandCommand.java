package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;

public class StandCommand implements SubCommand {

    private final IGNpcUtils plugin;

    public StandCommand(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_stand"));
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

        if (npc.getOrAddTrait(net.citizensnpcs.trait.SitTrait.class).isSitting()) {
            npc.data().set("Sit", false);
            Location loc = npc.getEntity().getLocation().clone();
            loc.setY(loc.getBlock().getLocation().getY() + 1.0);
            if (npc.hasTrait(net.citizensnpcs.trait.SitTrait.class)) {
                npc.removeTrait(net.citizensnpcs.trait.SitTrait.class);
            }
            npc.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            npc.data().set("Sleep", false);
            npc.getOrAddTrait(net.citizensnpcs.trait.SleepTrait.class).setSleeping(null);
        }

        sender.sendMessage(
                MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_standing", "%id%", String.valueOf(id)));
    }

    @Override
    public String getName() {
        return "stand";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
