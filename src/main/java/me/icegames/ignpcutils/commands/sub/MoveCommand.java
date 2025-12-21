package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MoveCommand implements SubCommand {

    private final IGNpcUtils plugin;

    public MoveCommand(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean silent = SubCommand.isSilent(args);

        if ((args.length == 3 || args.length == 4) && args[2].equalsIgnoreCase("here")) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "player_only"));
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
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%",
                        String.valueOf(id)));
                return;
            }

            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
            Location loc = player.getLocation();
            npc.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            SubCommand.sendMessage(sender,
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_moved", "%id%", String.valueOf(id),
                            "%world%", loc.getWorld().getName(), "%x%", String.valueOf(loc.getX()), "%y%",
                            String.valueOf(loc.getY()), "%z%", String.valueOf(loc.getZ()),
                            "%pitch%", String.valueOf(loc.getPitch()), "%yaw%", String.valueOf(loc.getYaw())),
                    silent);
            return;
        }

        if (args.length < 8) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_move"));
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }
        if (id < 0) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        String worldName = args[2];
        double x, y, z;
        float pitch, yaw;

        try {
            x = Double.parseDouble(args[3]);
            y = Double.parseDouble(args[4]);
            z = Double.parseDouble(args[5]);
            pitch = Float.parseFloat(args[6]);
            yaw = Float.parseFloat(args[7]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_coordinates"));
            return;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc == null) {
            sender.sendMessage(
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%", String.valueOf(id)));
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_world", "%world%", worldName));
            return;
        }

        npc.teleport(new Location(world, x, y, z, yaw, pitch), PlayerTeleportEvent.TeleportCause.PLUGIN);
        SubCommand.sendMessage(sender,
                MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_moved", "%id%", String.valueOf(id),
                        "%world%", worldName, "%x%", String.valueOf(x), "%y%", String.valueOf(y), "%z%",
                        String.valueOf(z),
                        "%pitch%", String.valueOf(pitch), "%yaw%", String.valueOf(yaw)),
                silent);
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
