package me.icegames.ignpcutils.managers;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.database.IStorage;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.PlayerFilter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatusManager {

    private final IGNpcUtils plugin;
    private final IStorage storage;

    // NPC ID -> State Name -> State Config
    private final Map<Integer, Map<String, NPCState>> npcStates = new HashMap<>();

    // Player UUID -> NPC ID -> Current State Name
    private final Map<UUID, Map<Integer, String>> playerStates = new ConcurrentHashMap<>();

    public StatusManager(IGNpcUtils plugin, IStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        loadStates();
    }

    public void loadStates() {
        npcStates.clear();
        ConfigurationSection npcsSection = plugin.getConfig().getConfigurationSection("npcs");
        if (npcsSection == null)
            return;

        for (String npcIdStr : npcsSection.getKeys(false)) {
            try {
                int npcId = Integer.parseInt(npcIdStr);
                ConfigurationSection statesSection = npcsSection.getConfigurationSection(npcIdStr + ".states");
                if (statesSection == null)
                    continue;

                Map<String, NPCState> states = new HashMap<>();
                for (String stateName : statesSection.getKeys(false)) {
                    ConfigurationSection stateConfig = statesSection.getConfigurationSection(stateName);
                    if (stateConfig == null)
                        continue;

                    NPCState state = new NPCState(
                            stateName,
                            stateConfig.getBoolean("visible", true),
                            stateConfig.getStringList("actions"));
                    states.put(stateName, state);
                }
                npcStates.put(npcId, states);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid NPC ID in config: " + npcIdStr);
            }
        }
    }

    public void setPlayerState(UUID playerUuid, int npcId, String stateName) {
        Map<Integer, String> states = playerStates.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        states.put(npcId, stateName);

        // Save to database asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.savePlayerState(playerUuid, npcId, stateName);
        });
    }

    public String getPlayerState(UUID playerUuid, int npcId) {
        return playerStates.getOrDefault(playerUuid, Collections.emptyMap()).get(npcId);
    }

    public void applyState(Player player, int npcId, String stateName) {
        Map<String, NPCState> states = npcStates.get(npcId);
        if (states == null)
            return;

        NPCState state = states.get(stateName);
        if (state == null)
            return;

        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null)
            return;

        // Apply visibility
        PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
        if (state.visible) {
            filter.removePlayer(player.getUniqueId());
        } else {
            filter.addPlayer(player.getUniqueId());
        }

        // Execute actions
        if (state.actions != null && !state.actions.isEmpty()) {
            for (String action : state.actions) {
                String[] parts = action.split(":", 2);
                if (parts.length < 2)
                    continue;

                String type = parts[0].trim();
                String command = parts[1].trim().replace("%player_name%", player.getName());

                if (type.equalsIgnoreCase("console_command")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } else if (type.equalsIgnoreCase("player_command")) {
                    player.performCommand(command);
                }
            }
        }

        // Update player state tracking
        setPlayerState(player.getUniqueId(), npcId, stateName);
    }

    public List<String> getAvailableStates(int npcId) {
        Map<String, NPCState> states = npcStates.get(npcId);
        return states != null ? new ArrayList<>(states.keySet()) : Collections.emptyList();
    }

    public boolean stateExists(int npcId, String stateName) {
        Map<String, NPCState> states = npcStates.get(npcId);
        return states != null && states.containsKey(stateName);
    }

    public void loadPlayerStates(UUID playerUuid) {
        Map<Integer, String> states = storage.getPlayerStates(playerUuid);
        playerStates.put(playerUuid, new ConcurrentHashMap<>(states));
    }

    public void clearPlayerStates(UUID playerUuid) {
        playerStates.remove(playerUuid);
    }

    /**
     * Apply default states to a player for all configured NPCs that don't have a
     * saved state.
     * This ensures NPCs with visible: false in the default state are properly
     * hidden for new players.
     */
    public void applyDefaultStates(Player player) {
        UUID playerUuid = player.getUniqueId();
        Map<Integer, String> currentStates = playerStates.getOrDefault(playerUuid, Collections.emptyMap());

        // Iterate through all configured NPCs
        for (Map.Entry<Integer, Map<String, NPCState>> entry : npcStates.entrySet()) {
            int npcId = entry.getKey();
            Map<String, NPCState> states = entry.getValue();

            // Skip if player already has a state for this NPC
            if (currentStates.containsKey(npcId)) {
                continue;
            }

            // Apply default state if it exists
            NPCState defaultState = states.get("default");
            if (defaultState != null) {
                applyState(player, npcId, "default");
            }
        }
    }

    private static class NPCState {
        boolean visible;
        List<String> actions;

        public NPCState(String name, boolean visible, List<String> actions) {
            // name parameter kept for constructor compatibility but not stored
            this.visible = visible;
            this.actions = actions;
        }
    }
}
