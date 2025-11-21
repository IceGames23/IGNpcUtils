package me.icegames.ignpcutils.util;

import me.icegames.ignpcutils.IGNpcUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class NPCResolver {

    private final IGNpcUtils plugin;
    private final Map<String, Integer> aliases = new HashMap<>();
    private final Map<String, List<Integer>> groups = new HashMap<>();

    public NPCResolver(IGNpcUtils plugin) {
        this.plugin = plugin;
        loadMappings();
    }

    public void loadMappings() {
        aliases.clear();
        groups.clear();

        // Load aliases
        ConfigurationSection aliasesSection = plugin.getConfig().getConfigurationSection("aliases");
        if (aliasesSection != null) {
            for (String alias : aliasesSection.getKeys(false)) {
                int npcId = aliasesSection.getInt(alias);
                aliases.put(alias.toLowerCase(), npcId);
            }
        }

        // Load groups
        ConfigurationSection groupsSection = plugin.getConfig().getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String groupName : groupsSection.getKeys(false)) {
                List<Integer> npcIds = groupsSection.getIntegerList(groupName);
                groups.put(groupName.toLowerCase(), npcIds);
            }
        }
    }

    /**
     * Resolve NPC reference to list of IDs
     * Supports: direct ID, alias, or group:name
     */
    public List<Integer> resolve(String reference) {
        if (reference == null || reference.isEmpty()) {
            return Collections.emptyList();
        }

        // Check if it's a group reference
        if (reference.toLowerCase().startsWith("group:")) {
            String groupName = reference.substring(6).toLowerCase();
            List<Integer> groupNpcs = groups.get(groupName);
            return groupNpcs != null ? new ArrayList<>(groupNpcs) : Collections.emptyList();
        }

        // Try to parse as direct ID
        try {
            int id = Integer.parseInt(reference);
            return Collections.singletonList(id);
        } catch (NumberFormatException e) {
            // Not a number, try as alias
        }

        // Try to resolve as alias
        Integer aliasId = aliases.get(reference.toLowerCase());
        if (aliasId != null) {
            return Collections.singletonList(aliasId);
        }

        // Invalid reference
        return Collections.emptyList();
    }

    /**
     * Check if reference is a group
     */
    public boolean isGroup(String reference) {
        return reference != null && reference.toLowerCase().startsWith("group:");
    }

    /**
     * Get alias for NPC ID (reverse lookup)
     */
    public String getAlias(int npcId) {
        for (Map.Entry<String, Integer> entry : aliases.entrySet()) {
            if (entry.getValue() == npcId) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get all aliases for tab completion
     */
    public List<String> getAliases() {
        return new ArrayList<>(aliases.keySet());
    }

    /**
     * Get all group names for tab completion
     */
    public List<String> getGroups() {
        List<String> groupNames = new ArrayList<>();
        for (String group : groups.keySet()) {
            groupNames.add("group:" + group);
        }
        return groupNames;
    }

    /**
     * Check if reference is valid
     */
    public boolean isValid(String reference) {
        return !resolve(reference).isEmpty();
    }

    /**
     * Get friendly name for reference (for messages)
     */
    public String getFriendlyName(String reference) {
        if (isGroup(reference)) {
            return reference;
        }

        // Try to get alias
        List<Integer> ids = resolve(reference);
        if (ids.size() == 1) {
            String alias = getAlias(ids.get(0));
            if (alias != null) {
                return alias;
            }
        }

        return reference;
    }
}
