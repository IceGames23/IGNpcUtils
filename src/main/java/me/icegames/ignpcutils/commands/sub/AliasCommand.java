package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import me.icegames.ignpcutils.util.NPCResolver;
import org.bukkit.command.CommandSender;

import java.util.List;

public class AliasCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final NPCResolver resolver;

    public AliasCommand(IGNpcUtils plugin, NPCResolver resolver) {
        this.plugin = plugin;
        this.resolver = resolver;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // /npcutils alias <add|remove|list> [args...]
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_alias"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "add":
                handleAdd(sender, args);
                break;
            case "remove":
            case "delete":
                handleRemove(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            default:
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_alias"));
                break;
        }
    }

    private void handleAdd(CommandSender sender, String[] args) {
        // /npcutils alias add <name> <npcId>
        if (args.length != 4) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_alias_add"));
            return;
        }

        String aliasName = args[2].toLowerCase();
        int npcId;

        try {
            npcId = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        // Add to config
        plugin.getConfig().set("aliases." + aliasName, npcId);
        plugin.saveConfig();

        // Reload resolver
        resolver.loadMappings();

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "alias_added",
                "%alias%", aliasName, "%id%", String.valueOf(npcId)));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        // /npcutils alias remove <name>
        if (args.length != 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_alias_remove"));
            return;
        }

        String aliasName = args[2].toLowerCase();

        if (!plugin.getConfig().contains("aliases." + aliasName)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "alias_not_exists",
                    "%alias%", aliasName));
            return;
        }

        // Remove from config
        plugin.getConfig().set("aliases." + aliasName, null);
        plugin.saveConfig();

        // Reload resolver
        resolver.loadMappings();

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "alias_removed",
                "%alias%", aliasName));
    }

    private void handleList(CommandSender sender) {
        List<String> aliases = resolver.getAliases();

        if (aliases.isEmpty()) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "alias_list_empty"));
            return;
        }

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "alias_list_header"));
        for (String alias : aliases) {
            List<Integer> ids = resolver.resolve(alias);
            if (!ids.isEmpty()) {
                sender.sendMessage("  §b" + alias + " §7→ §3" + ids.get(0));
            }
        }
    }

    @Override
    public String getName() {
        return "alias";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
