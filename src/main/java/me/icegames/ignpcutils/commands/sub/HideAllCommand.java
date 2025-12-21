package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HideAllCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final me.icegames.ignpcutils.util.NPCResolver resolver;

    public HideAllCommand(IGNpcUtils plugin, me.icegames.ignpcutils.util.NPCResolver resolver) {
        this.plugin = plugin;
        this.resolver = resolver;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_hideall"));
            return;
        }

        boolean silent = SubCommand.isSilent(args);

        List<Integer> npcIds = resolver.resolve(args[1]);
        if (npcIds.isEmpty()) {
            sender.sendMessage(
                    MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_npc_reference", "%ref%", args[1]));
            return;
        }

        CompletableFuture.runAsync(() -> {
            int count = 0;
            for (int id : npcIds) {
                String npcPath = "npcs." + id;
                if (!plugin.getConfig().contains(npcPath)) {
                    continue;
                }

                // For simplicity, we'll just modify the config to make default state hidden
                plugin.getConfig().set(npcPath + ".states.default.visible", false);
                count++;
            }
            plugin.saveConfig();

            // Reload states and apply default state to all online players
            int finalCount = count;
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getStatusManager().loadStates();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (int id : npcIds) {
                        plugin.getStatusManager().applyState(player, id, "default");
                    }
                }
                if (npcIds.size() == 1) {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_added", "%id%",
                                    String.valueOf(npcIds.get(0))),
                            silent);
                } else {
                    SubCommand.sendMessage(sender,
                            MessageUtil.getMessage(plugin.getMessagesConfig(), "npc_added_multi", "%count%",
                                    String.valueOf(finalCount)),
                            silent);
                }
            });
        });
    }

    @Override
    public String getName() {
        return "hideall";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
