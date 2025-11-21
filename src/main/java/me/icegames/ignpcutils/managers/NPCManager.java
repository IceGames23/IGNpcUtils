package me.icegames.ignpcutils.managers;

import me.icegames.ignpcutils.IGNpcUtils;
import me.icegames.ignpcutils.database.Storage;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.PlayerFilter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {

    // Maps to track player visibility states
    private final Map<UUID, Set<Integer>> shownNPCsPerPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> hiddenNPCsPerPlayer = new ConcurrentHashMap<>();

    private final IGNpcUtils plugin;
    private final Storage storage;

    public NPCManager(IGNpcUtils plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void loadFromConfig() {
        // No longer used for defaultHidden, but kept if we need other config loading
        // later
    }

    public void saveToConfig() {
        // No longer used for defaultHidden
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("npcutils.bypass")) {
            return;
        }

        // Load data asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<Integer> shown = storage.getShownNPCs(uuid);
            Set<Integer> hidden = storage.getHiddenNPCs(uuid);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline())
                    return;

                shownNPCsPerPlayer.put(uuid, shown);
                hiddenNPCsPerPlayer.put(uuid, hidden);

                // Apply manual overrides
                for (int id : hidden) {
                    NPC npc = CitizensAPI.getNPCRegistry().getById(id);
                    if (npc != null) {
                        PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
                        filter.addPlayer(uuid);
                    }
                }

                // We don't need to handle 'shown' here because default visibility is visible,
                // and 'shown' was only for overriding defaultHidden.
                // However, if we want to support 'shown' as an override for ConditionManager,
                // ConditionManager would need to check this map.
            });
        });
    }

    public void handleQuit(Player player) {
        shownNPCsPerPlayer.remove(player.getUniqueId());
        hiddenNPCsPerPlayer.remove(player.getUniqueId());
    }

    public void showNPCToPlayer(int id, Player player) {
        UUID uuid = player.getUniqueId();

        // Remove from hidden list
        Set<Integer> hidden = hiddenNPCsPerPlayer.computeIfAbsent(uuid, k -> new HashSet<>());
        if (hidden.remove(id)) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                storage.saveNPCs("npcutils_hidden", uuid, hidden);
            });
        }

        // Apply visibility change
        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc != null) {
            PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
            filter.removePlayer(uuid);
        }
    }

    public void hideNPCFromPlayer(int id, Player player) {
        UUID uuid = player.getUniqueId();

        // Add to hidden list
        Set<Integer> hidden = hiddenNPCsPerPlayer.computeIfAbsent(uuid, k -> new HashSet<>());
        if (hidden.add(id)) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                storage.saveNPCs("npcutils_hidden", uuid, hidden);
            });
        }

        // Apply visibility change
        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc != null) {
            PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
            filter.addPlayer(uuid);
        }
    }

    public void resetPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        shownNPCsPerPlayer.remove(uuid);
        hiddenNPCsPerPlayer.remove(uuid);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.saveShown(uuid, new HashSet<>());
            storage.saveNPCs("npcutils_hidden", uuid, new HashSet<>());
        });

        handleJoin(player);
    }

    public Storage getStorage() {
        return storage;
    }

    public boolean isHidden(UUID uuid, int npcId) {
        Set<Integer> hidden = hiddenNPCsPerPlayer.get(uuid);
        return hidden != null && hidden.contains(npcId);
    }
}