package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.managers.StatusManager;
import me.icegames.ignpcutils.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StatusCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final StatusManager statusManager;

    public StatusCommand(IGNpcUtils plugin, StatusManager statusManager) {
        this.plugin = plugin;
        this.statusManager = statusManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Usage: /npcutils status <set|get|list|reset> ...
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_status"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "set":
                handleSet(sender, args);
                break;
            case "get":
                handleGet(sender, args);
                break;
            case "list":
                handleList(sender, args);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            default:
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_status"));
                break;
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        // Usage: /npcutils status set <npcId> <state> <player|*>
        if (args.length != 5) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_status_set"));
            return;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        String stateName = args[3];
        String targetPlayer = args[4];

        // Check if state exists
        if (!statusManager.stateExists(npcId, stateName)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "state_not_found",
                    "%state%", stateName, "%id%", String.valueOf(npcId)));
            return;
        }

        CompletableFuture.runAsync(() -> {
            if (targetPlayer.equals("*")) {
                // Apply to all online players
                Bukkit.getScheduler().runTask(plugin, () -> {
                    int count = 0;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        statusManager.applyState(player, npcId, stateName);
                        count++;
                    }
                    final int finalCount = count;
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "state_set_all",
                            "%state%", stateName, "%id%", String.valueOf(npcId), "%count%",
                            String.valueOf(finalCount)));
                });
            } else {
                // Apply to specific player
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(targetPlayer);
                    if (player == null) {
                        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "player_not_found"));
                        return;
                    }

                    statusManager.applyState(player, npcId, stateName);
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "state_set",
                            "%state%", stateName, "%id%", String.valueOf(npcId), "%player%", player.getName()));
                });
            }
        });
    }

    private void handleGet(CommandSender sender, String[] args) {
        // Usage: /npcutils status get <npcId> <player>
        if (args.length != 4) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_status_get"));
            return;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        String targetPlayer = args[3];
        Player player = Bukkit.getPlayer(targetPlayer);
        if (player == null) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "player_not_found"));
            return;
        }

        String currentState = statusManager.getPlayerState(player.getUniqueId(), npcId);
        if (currentState == null) {
            currentState = "none";
        }

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "current_state",
                "%id%", String.valueOf(npcId), "%player%", player.getName(), "%state%", currentState));
    }

    private void handleList(CommandSender sender, String[] args) {
        // Usage: /npcutils status list <npcId>
        if (args.length != 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_status_list"));
            return;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        List<String> states = statusManager.getAvailableStates(npcId);
        if (states.isEmpty()) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found",
                    "%id%", String.valueOf(npcId)));
            return;
        }

        String statesList = String.join(", ", states);
        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "available_states",
                "%id%", String.valueOf(npcId), "%states%", statesList));
    }

    private void handleReset(CommandSender sender, String[] args) {
        // Usage: /npcutils status reset <npcId> <player|*>
        if (args.length != 4) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_status_reset"));
            return;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        String targetPlayer = args[3];

        CompletableFuture.runAsync(() -> {
            if (targetPlayer.equals("*")) {
                // Reset all online players to default state
                List<String> states = statusManager.getAvailableStates(npcId);
                String defaultState = states.contains("default") ? "default"
                        : (states.isEmpty() ? null : states.get(0));

                if (defaultState == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found",
                                "%id%", String.valueOf(npcId)));
                    });
                    return;
                }

                final String finalDefaultState = defaultState;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        statusManager.applyState(player, npcId, finalDefaultState);
                    }
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "state_reset",
                            "%id%", String.valueOf(npcId), "%player%", "*"));
                });
            } else {
                // Reset specific player
                Player player = Bukkit.getPlayer(targetPlayer);
                if (player == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "player_not_found"));
                    });
                    return;
                }

                List<String> states = statusManager.getAvailableStates(npcId);
                String defaultState = states.contains("default") ? "default"
                        : (states.isEmpty() ? null : states.get(0));

                if (defaultState == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found",
                                "%id%", String.valueOf(npcId)));
                    });
                    return;
                }

                final String finalDefaultState = defaultState;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    statusManager.applyState(player, npcId, finalDefaultState);
                    sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "state_reset",
                            "%id%", String.valueOf(npcId), "%player%", player.getName()));
                });
            }
        });
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
