package me.icegames.ignpcutils;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bukkit.Bukkit.getLogger;

public class NPCUtilsCommand implements CommandExecutor {

    private final NPCManager manager;
    private final FileConfiguration config;
    private final IGNpcUtils plugin;

    public NPCUtilsCommand(NPCManager manager, FileConfiguration messagesConfig, IGNpcUtils plugin) {
        this.manager = manager;
        this.config = messagesConfig;
        this.plugin = plugin;
    }

    private String getMessage(String path, String... placeholders) {
        Object messageObj = config.get(path);
        String message;

        if (messageObj instanceof String) {
            message = (String) messageObj;
        } else if (messageObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> messageList = (List<String>) messageObj;
            message = String.join("\n", messageList);
        } else {
            message = "&cMessage '" + messageObj + "' not found in messages.yml.";
        }

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
            sender.sendMessage(getMessage("help"));
            return true;
        }

        String sub = args[0];
        int id;

        switch (sub.toLowerCase()) {
            case "reload":
                try {
                    plugin.reloadConfig();
                    manager.loadFromConfig();
                    sender.sendMessage(getMessage("reload"));
                    getLogger().info("Plugin successfully reloaded.");
                } catch (Exception e) {
                    sender.sendMessage(getMessage("error_reload"));
                    e.printStackTrace();
                }
                break;

            case "move":
                if (args.length != 8) {
                    sender.sendMessage(getMessage("usage_move"));
                    return true;
                }

                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(getMessage("invalid_id"));
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
                if (args.length != 2) {
                    sender.sendMessage(getMessage("usage_hideall"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }

                CompletableFuture.runAsync(() -> {
                    if (manager.addDefaultHidden(id)) {
                        Bukkit.getOnlinePlayers().parallelStream().forEach(player -> manager.hideNPCFromPlayer(id, player));
                        manager.saveToConfig();
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMessage("npc_added", "%id%", String.valueOf(id))));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMessage("npc_already_added")));
                    }
                });
                break;

            case "showall":
                if (args.length != 2) {
                    sender.sendMessage(getMessage("usage_showall"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }

                CompletableFuture.runAsync(() -> {
                    if (manager.removeDefaultHidden(id)) {
                        Bukkit.getOnlinePlayers().parallelStream().forEach(player -> manager.showNPCToPlayer(id, player));
                        manager.saveToConfig();
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMessage("npc_removed", "%id%", String.valueOf(id))));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(getMessage("npc_not_hidden")));
                    }
                });
                break;

            case "show":
            case "hide":
                if (args.length != 3) {
                    sender.sendMessage(getMessage(sub.equals("show") ? "usage_show" : "usage_hide"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(getMessage("invalid_id"));
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
                sender.sendMessage(getMessage("help"));
        }
        return true;
    }
}