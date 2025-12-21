package me.icegames.ignpcutils.commands;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.sub.*;
import me.icegames.ignpcutils.managers.NPCManager;
import me.icegames.ignpcutils.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NPCUtilsCommand implements CommandExecutor {

    private final NPCManager manager;
    private final IGNpcUtils plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public NPCUtilsCommand(IGNpcUtils plugin) {
        this.plugin = plugin;
        this.manager = plugin.getNPCManager();
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("reload", new ReloadCommand(plugin, manager));
        subCommands.put("move", new MoveCommand(plugin));
        subCommands.put("hideall", new HideAllCommand(plugin, plugin.getNPCResolver()));
        subCommands.put("showall", new ShowAllCommand(plugin, plugin.getNPCResolver()));
        subCommands.put("show", new ShowCommand(plugin, manager, plugin.getNPCResolver()));
        subCommands.put("hide", new HideCommand(plugin, manager, plugin.getNPCResolver()));
        subCommands.put("sit", new SitCommand(plugin));
        subCommands.put("stand", new StandCommand(plugin));
        subCommands.put("sleep", new SleepCommand(plugin));
        subCommands.put("status", new StatusCommand(plugin, plugin.getStatusManager()));
        subCommands.put("skin", new SkinCommand(plugin, plugin.getNPCResolver()));
        subCommands.put("alias", new AliasCommand(plugin, plugin.getNPCResolver()));
        subCommands.put("group", new GroupCommand(plugin, plugin.getNPCResolver()));
        subCommands.put("reset", new ResetCommand(plugin, manager));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("npcutils.admin") && args.length == 0) {
            sender.sendMessage("§b• §fRunning §3§lI§b§lG§f§lNpcUtils §bv" + plugin.getDescription().getVersion()
                    + "§f by §bIceGames");
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

        String sub = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(sub);

        if (subCommand != null) {
            if (!sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "no_permission"));
                return true;
            }
            subCommand.execute(sender, args);
        } else {
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
