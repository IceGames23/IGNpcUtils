package me.icegames.ignpcutils.commands;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.managers.NPCManager;
import me.icegames.ignpcutils.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.util.NMS;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("npcutils.admin") && args.length == 0) {
            sender.sendMessage("§b• §fRunning §3§lI§b§lG§f§lNpcUtils §bv" + plugin.getDescription().getVersion() + "§f by §bIceGames");
            //sender.sendMessage("§b   §7§nhttps://www.spigotmc.org/resources/125318/");
            return true;
        }

        if (!sender.hasPermission("npcutils.admin")) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "no_permission"));
            return true;
        }

        if (args.length < 1) {
            showHelpMessage(sender);
            return true;
        }

        String sub = args[0];
        int id;

        switch (sub.toLowerCase()) {
            case "reload":
                try {
                    plugin.reloadConfig();
                    plugin.reloadMessagesConfig(); // Adicionado para recarregar messages.yml
                    manager.loadFromConfig();
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"reload"));
                    getLogger().info("Plugin successfully reloaded.");
                } catch (Exception e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"error_reload"));
                    e.printStackTrace();
                }
                break;

            case "move":
                if (args.length != 8) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"usage_move"));
                    return true;
                }

                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
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
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_coordinates"));
                    return true;
                }

                NPC npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_not_found", "%id%", String.valueOf(id)));
                    return true;
                }

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_world", "%world%", worldName));
                    return true;
                }

                npc.teleport(new Location(world, x, y, z, yaw, pitch), PlayerTeleportEvent.TeleportCause.PLUGIN);
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_moved", "%id%", String.valueOf(id), "%world%", worldName, "%x%", String.valueOf(x), "%y%", String.valueOf(y), "%z%", String.valueOf(z)));
                break;

            case "hideall":
                if (args.length != 2) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"usage_hideall"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
                    return true;
                }

                CompletableFuture.runAsync(() -> {
                    if (manager.addDefaultHidden(id)) {
                        Bukkit.getOnlinePlayers().parallelStream().forEach(player -> manager.hideNPCFromPlayer(id, player));
                        manager.saveToConfig();
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_added", "%id%", String.valueOf(id))));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_already_added")));
                    }
                });
                break;

            case "showall":
                if (args.length != 2) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"usage_showall"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
                    return true;
                }

                CompletableFuture.runAsync(() -> {
                    if (manager.removeDefaultHidden(id)) {
                        Bukkit.getOnlinePlayers().parallelStream().forEach(player -> manager.showNPCToPlayer(id, player));
                        manager.saveToConfig();
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_removed", "%id%", String.valueOf(id))));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_not_hidden")));
                    }
                });
                break;

            case "show":
            case "hide":
                if (args.length != 3) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),sub.equals("show") ? "usage_show" : "usage_hide"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
                    return true;
                }
                if (id < 0) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"invalid_id"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"player_not_found"));
                    return true;
                }
                if (sub.equalsIgnoreCase("show")) {
                    manager.showNPCToPlayer(id, target);
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_shown", "%id%", String.valueOf(id), "%player%", target.getName()));
                } else {
                    manager.hideNPCFromPlayer(id, target);
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(),"npc_hidden", "%id%", String.valueOf(id), "%player%", target.getName()));
                }
                break;

            case "sit":
                if (args.length != 2) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_sit"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
                    return true;
                }
                npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%", String.valueOf(id)));
                    return true;
                }
                if (!npc.getEntity().getType().equals(EntityType.PLAYER)) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "only_player_type"));
                    return true;
                }
                npc.data().set("Sit", true);
                CitizensAPI.getTraitFactory().getTraitClass("Sit");
                npc.getOrAddTrait(net.citizensnpcs.trait.SitTrait.class).setSitting(npc.getEntity().getLocation());
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_sitting", "%id%", String.valueOf(id)));
                break;

//            case "sneak":
//                if (args.length < 2 || args.length > 3) {
//                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_sneak"));
//                    return true;
//                }
//                try {
//                    id = Integer.parseInt(args[1]);
//                } catch (NumberFormatException e) {
//                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
//                    return true;
//                }
//                npc = CitizensAPI.getNPCRegistry().getById(id);
//                if (npc == null) {
//                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%", String.valueOf(id)));
//                    return true;
//                }
//                if (!npc.getEntity().getType().equals(EntityType.PLAYER)) {
//                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "only_player_type"));
//                    return true;
//                }
//                boolean shouldSneak = true;
//                if (args.length == 3) {
//                    shouldSneak = Boolean.parseBoolean(args[2]);
//                }
//                //npc.data().set("Sneak", shouldSneak);
//                //npc.getOrAddTrait(net.citizensnpcs.trait.SneakTrait.class).setSneaking(shouldSneak);
//                NMS.setSneaking(npc.getEntity(), shouldSneak);
//                if (shouldSneak) {
//                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_sneaking", "%id%", String.valueOf(id)));
//                } else {
//                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_standing", "%id%", String.valueOf(id)));
//                }
//                break;

            case "stand":
                if (args.length != 2) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_stand"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
                    return true;
                }
                npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%", String.valueOf(id)));
                    return true;
                }
                if (!npc.getEntity().getType().equals(EntityType.PLAYER)) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "only_player_type"));
                    return true;
                }
                if (!npc.getEntity().getType().equals(EntityType.PLAYER)) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "only_player_npc_stand"));
                    return true;
                }

                if (npc.getOrAddTrait(net.citizensnpcs.trait.SitTrait.class).isSitting()) {
                    npc.data().set("Sit", false);
                    Location loc = npc.getEntity().getLocation().clone();
                    loc.setY(loc.getBlock().getLocation().getY() + 1.0);
                    if (npc.hasTrait(net.citizensnpcs.trait.SitTrait.class)) {
                        npc.removeTrait(net.citizensnpcs.trait.SitTrait.class);
                    }
                    npc.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
//                } else if (npc.getOrAddTrait(net.citizensnpcs.trait.SneakTrait.class).isSneaking()) {
//                    npc.data().set("Sneak", false);
//                    npc.getOrAddTrait(net.citizensnpcs.trait.SneakTrait.class).setSneaking(false);
                } else {
                    npc.data().set("Sleep", false);
                    npc.getOrAddTrait(net.citizensnpcs.trait.SleepTrait.class).setSleeping(null);
                }

                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_standing", "%id%", String.valueOf(id)));
                break;

            case "sleep":
                if (args.length != 2) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_sleep"));
                    return true;
                }
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
                    return true;
                }
                npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc == null) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%", String.valueOf(id)));
                    return true;
                }
                if (!npc.getEntity().getType().equals(EntityType.PLAYER)) {
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "only_player_type"));
                    return true;
                }
                CitizensAPI.getTraitFactory().getTraitClass("Sleep");
                npc.getOrAddTrait(net.citizensnpcs.trait.SleepTrait.class).setSleeping(npc.getEntity().getLocation());
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_now_sleeping", "%id%", String.valueOf(id)));
                break;

            default:
                showHelpMessage(sender);
        }
        return true;
    }

    private void showHelpMessage(CommandSender sender) {
        Object helpObj = plugin.getMessagesConfig().get("help");
        if (helpObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> helpList = (List<String>) helpObj;
            for (String line : helpList) {
                sender.sendMessage(line.replace("&", "§"));
            }
        } else if (helpObj instanceof String) {
            sender.sendMessage(((String) helpObj).replace("&", "§"));
        }
    }
}
