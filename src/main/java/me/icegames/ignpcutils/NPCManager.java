package me.icegames.ignpcutils;

import me.icegames.ignpcutils.database.Storage;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.PlayerFilter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class NPCManager {

    private final Set<Integer> defaultHiddenNPCs = new HashSet<>();
    private final Map<UUID, Set<Integer>> shownNPCsPerPlayer = new HashMap<>();
    private final IGNpcUtils plugin;
    private final Storage storage;

    public NPCManager(IGNpcUtils plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
        loadFromConfig();
    }

    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        defaultHiddenNPCs.clear();
        defaultHiddenNPCs.addAll(config.getIntegerList("defaultHidden"));

        shownNPCsPerPlayer.clear();
        if (config.isConfigurationSection("shown")) {
            config.getConfigurationSection("shown").getKeys(false).forEach(uuidStr -> {
                UUID uuid = UUID.fromString(uuidStr);
                List<Integer> ids = config.getIntegerList("shown." + uuidStr);
                shownNPCsPerPlayer.put(uuid, new HashSet<>(ids));
            });
        }
    }

    public void saveToConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("defaultHidden", new ArrayList<>(defaultHiddenNPCs));

        plugin.saveConfig();
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("npcutils.bypass")) {
            for (int id : defaultHiddenNPCs) {
                NPC npc = CitizensAPI.getNPCRegistry().getById(id);
                if (npc != null) {
                    PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
                    filter.removePlayer(uuid);
                }
            }
            return;
        }

        Set<Integer> shown = storage.getShownNPCs(uuid);
        Set<Integer> hidden = storage.getHiddenNPCs(uuid);

        for (int id : defaultHiddenNPCs) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(id);
            if (npc != null && npc.isSpawned()) {
                PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
                filter.setDenylist();

                if (!shown.contains(id) && hidden.contains(id)) {
                    filter.addPlayer(uuid);
                } else {
                    filter.removePlayer(uuid);
                }
            }
        }

        shownNPCsPerPlayer.put(uuid, shown);
    }

    public boolean addDefaultHidden(int id) {
        return defaultHiddenNPCs.add(id);
    }

    public boolean removeDefaultHidden(int id) {
        return defaultHiddenNPCs.remove(id);
    }

    public void showNPCToPlayer(int id, Player player) {
        UUID uuid = player.getUniqueId();
        shownNPCsPerPlayer.computeIfAbsent(uuid, k -> new HashSet<>()).add(id);

        // Atualizar banco de dados
        storage.saveShown(uuid, shownNPCsPerPlayer.get(uuid));

        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc != null) {
            PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
            filter.removePlayer(uuid);
        }
    }

    public void hideNPCFromPlayer(int id, Player player) {
        UUID uuid = player.getUniqueId();
        shownNPCsPerPlayer.computeIfAbsent(uuid, k -> new HashSet<>()).remove(id);

        // Atualizar banco de dados
        storage.saveShown(uuid, shownNPCsPerPlayer.get(uuid));

        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc != null) {
            PlayerFilter filter = npc.getOrAddTrait(PlayerFilter.class);
            filter.addPlayer(uuid);
        }
    }
}