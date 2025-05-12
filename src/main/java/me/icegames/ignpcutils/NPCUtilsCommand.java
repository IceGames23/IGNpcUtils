package me.icegames.ignpcutils;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NPCUtilsCommand implements CommandExecutor {

    private final NPCManager manager;
    private final FileConfiguration config;
    private final IGNpcUtils plugin;

    public NPCUtilsCommand(NPCManager manager, FileConfiguration config, IGNpcUtils plugin) {
        this.manager = manager;
        this.config = config;
        this.plugin = plugin;
    }

    private String getMessage(String path, String... placeholders) {
        String message = config.getString("messages." + path, "&cMensagem não configurada.");
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return message.replace("&", "§");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("npcutils.admin")) {
            sender.sendMessage(getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            sendHelpMessage(sender);
            return true;
        }

        String sub = args[0];
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid_id"));
            return true;
        }

        switch (sub.toLowerCase()) {
            case "reload":
                if (args.length != 1) {
                    sender.sendMessage(getMessage("usage_reload"));
                    return true;
                }
                try {
                    plugin.reloadConfig();
                    manager.loadFromConfig();
                    sender.sendMessage(getMessage("reload"));
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Erro ao recarregar a configuração.");
                    e.printStackTrace();
                }
                break;

            case "move":
                if (args.length < 8) {
                    sender.sendMessage(getMessage("usage_move"));
                    return true;
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
                    sender.sendMessage(getMessage("invalid_coordinates"));
                    return true;
                }

                NPC npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(getMessage("npc_not_found", "%id%", String.valueOf(id)));
                    return true;
                }

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    sender.sendMessage(getMessage("invalid_world", "%world%", worldName));
                    return true;
                }

                npc.teleport(new Location(world, x, y, z, yaw, pitch), PlayerTeleportEvent.TeleportCause.PLUGIN);
                sender.sendMessage(getMessage("npc_moved", "%id%", String.valueOf(id), "%world%", worldName, "%x%", String.valueOf(x), "%y%", String.valueOf(y), "%z%", String.valueOf(z)));
                break;

            case "hideall":
                if (args.length < 2) {
                    sender.sendMessage(getMessage("usage_hideall"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }

                if (manager.addDefaultHidden(id)) {
                    int finalId1 = id;
                    Bukkit.getOnlinePlayers().forEach(player -> manager.hideNPCFromPlayer(finalId1, player));
                    manager.saveToConfig();
                    sender.sendMessage(getMessage("npc_added", "%id%", String.valueOf(id)));
                } else {
                    sender.sendMessage(getMessage("npc_already_added"));
                }
                break;

            case "showall":
                if (args.length < 2) {
                    sender.sendMessage(getMessage("usage_showall"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }

                if (manager.removeDefaultHidden(id)) {
                    int finalId = id;
                    Bukkit.getOnlinePlayers().forEach(player -> manager.showNPCToPlayer(finalId, player));
                    manager.saveToConfig();
                    sender.sendMessage(getMessage("npc_removed", "%id%", String.valueOf(id)));
                } else {
                    sender.sendMessage(getMessage("npc_not_hidden"));
                }
                break;

            case "show":
            case "hide":
                if (args.length < 3) {
                    sender.sendMessage(getMessage(sub.equals("show") ? "usage_show" : "usage_hide"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(getMessage("player_not_found"));
                    return true;
                }
                if (sub.equalsIgnoreCase("show")) {
                    manager.showNPCToPlayer(id, target);
                    sender.sendMessage(getMessage("npc_shown", "%id%", String.valueOf(id), "%player%", target.getName()));
                } else {
                    manager.hideNPCFromPlayer(id, target);
                    sender.sendMessage(getMessage("npc_hidden", "%id%", String.valueOf(id), "%player%", target.getName()));
                }
                manager.saveToConfig();
                break;

            default:
                sender.sendMessage(getMessage("unknown_subcommand"));
                sendHelpMessage(sender);
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        for (String line : config.getStringList("messages.help")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }
}