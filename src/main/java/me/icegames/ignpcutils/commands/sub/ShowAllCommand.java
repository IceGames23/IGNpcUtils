package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class ShowAllCommand implements SubCommand {

    private final IGNpcUtils plugin;

    public ShowAllCommand(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_showall"));
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

        CompletableFuture.runAsync(() -> {
            String npcPath = "npcs." + id;
            if (!plugin.getConfig().contains(npcPath)) {
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_not_found", "%id%",
                        String.valueOf(id)));
                return;
            }

            // For simplicity, we'll just modify the config to make default state visible
            plugin.getConfig().set(npcPath + ".states.default.visible", true);
            plugin.saveConfig();

            // Reload states and apply default state to all online players
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getStatusManager().loadStates();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    plugin.getStatusManager().applyState(player, id, "default");
                }
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_removed", "%id%",
                        String.valueOf(id)));
            });
        });
    }

    @Override
    public String getName() {
        return "showall";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
