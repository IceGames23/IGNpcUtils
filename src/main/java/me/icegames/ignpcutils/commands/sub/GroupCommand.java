package me.icegames.ignpcutils.commands.sub;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.commands.SubCommand;
import me.icegames.ignpcutils.util.MessageUtil;
import me.icegames.ignpcutils.util.NPCResolver;
import org.bukkit.command.CommandSender;

import java.util.List;

public class GroupCommand implements SubCommand {

    private final IGNpcUtils plugin;
    private final NPCResolver resolver;

    public GroupCommand(IGNpcUtils plugin, NPCResolver resolver) {
        this.plugin = plugin;
        this.resolver = resolver;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // /npcutils group <create|delete|add|remove|list> [args...]
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_group"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreate(sender, args);
                break;
            case "delete":
                handleDelete(sender, args);
                break;
            case "add":
                handleAdd(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "list":
                handleList(sender, args);
                break;
            default:
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_group"));
                break;
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        // /npcutils group create <name>
        if (args.length != 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_group_create"));
            return;
        }

        String groupName = args[2].toLowerCase();

        if (plugin.getConfig().contains("groups." + groupName)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_already_exists",
                    "%group%", groupName));
            return;
        }

        // Create empty group
        plugin.getConfig().set("groups." + groupName, List.of());
        plugin.saveConfig();

        // Reload resolver
        resolver.loadMappings();

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_created",
                "%group%", groupName));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        // /npcutils group delete <name>
        if (args.length != 3) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_group_delete"));
            return;
        }

        String groupName = args[2].toLowerCase();

        if (!plugin.getConfig().contains("groups." + groupName)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_not_found",
                    "%group%", groupName));
            return;
        }

        // Delete group
        plugin.getConfig().set("groups." + groupName, null);
        plugin.saveConfig();

        // Reload resolver
        resolver.loadMappings();

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_deleted",
                "%group%", groupName));
    }

    private void handleAdd(CommandSender sender, String[] args) {
        // /npcutils group add <groupName> <npcId>
        if (args.length != 4) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_group_add"));
            return;
        }

        String groupName = args[2].toLowerCase();
        int npcId;

        try {
            npcId = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        if (!plugin.getConfig().contains("groups." + groupName)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_not_found",
                    "%group%", groupName));
            return;
        }

        // Get current group
        List<Integer> group = plugin.getConfig().getIntegerList("groups." + groupName);

        if (group.contains(npcId)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_npc_already_in",
                    "%id%", String.valueOf(npcId), "%group%", groupName));
            return;
        }

        // Add NPC to group
        group.add(npcId);
        plugin.getConfig().set("groups." + groupName, group);
        plugin.saveConfig();

        // Reload resolver
        resolver.loadMappings();

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_npc_added",
                "%id%", String.valueOf(npcId), "%group%", groupName));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        // /npcutils group remove <groupName> <npcId>
        if (args.length != 4) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_group_remove"));
            return;
        }

        String groupName = args[2].toLowerCase();
        int npcId;

        try {
            npcId = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "invalid_id"));
            return;
        }

        if (!plugin.getConfig().contains("groups." + groupName)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_not_found",
                    "%group%", groupName));
            return;
        }

        // Get current group
        List<Integer> group = plugin.getConfig().getIntegerList("groups." + groupName);

        if (!group.contains(npcId)) {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_npc_not_in",
                    "%id%", String.valueOf(npcId), "%group%", groupName));
            return;
        }

        // Remove NPC from group
        group.remove(Integer.valueOf(npcId));
        plugin.getConfig().set("groups." + groupName, group);
        plugin.saveConfig();

        // Reload resolver
        resolver.loadMappings();

        sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_npc_removed",
                "%id%", String.valueOf(npcId), "%group%", groupName));
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // List all groups
            List<String> groups = resolver.getGroups();

            if (groups.isEmpty()) {
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_list_empty"));
                return;
            }

            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_list_header"));
            for (String groupRef : groups) {
                String groupName = groupRef.replace("group:", "");
                List<Integer> npcs = resolver.resolve(groupRef);
                sender.sendMessage("  §b" + groupName + " §7(" + npcs.size() + " NPCs)");
            }
        } else if (args.length == 3) {
            // List specific group
            String groupName = args[2].toLowerCase();
            List<Integer> npcs = resolver.resolve("group:" + groupName);

            if (npcs.isEmpty()) {
                sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_not_found",
                        "%group%", groupName));
                return;
            }

            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "group_detail_header",
                    "%group%", groupName));
            sender.sendMessage("  §7NPCs: §3" + npcs.toString());
        } else {
            sender.sendMessage(MessageUtil.getMessage(plugin.getMessagesConfig(), "usage_group_list"));
        }
    }

    @Override
    public String getName() {
        return "group";
    }

    @Override
    public String getPermission() {
        return "npcutils.admin";
    }
}
