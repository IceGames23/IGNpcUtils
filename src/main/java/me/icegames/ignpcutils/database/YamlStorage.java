package me.icegames.ignpcutils.database;

import me.icegames.ignpcutils.IGNpcUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * YAML-based storage implementation.
 * Stores player data in individual YAML files without requiring a database.
 */
public class YamlStorage implements IStorage {

    private final IGNpcUtils plugin;
    private final File dataFolder;
    private final String serverName;

    // Cache for player data
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public YamlStorage(IGNpcUtils plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data/players");
        this.serverName = plugin.getConfig().getString("server_name", "survival");
    }

    @Override
    public void init() {
        plugin.getLogger().info("Initializing YAML storage...");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create data folder: " + dataFolder.getAbsolutePath());
        }
        plugin.getLogger().info("YAML storage initialized successfully.");
    }

    @Override
    public void close() {
        // Save all cached data before closing
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            savePlayerData(entry.getKey(), entry.getValue());
        }
        cache.clear();
        plugin.getLogger().info("YAML storage closed.");
    }

    @Override
    public Set<Integer> getHiddenNPCs(UUID uuid) {
        PlayerData data = getOrLoadPlayerData(uuid);
        return new HashSet<>(data.hidden.getOrDefault(serverName, new HashSet<>()));
    }

    @Override
    public Set<Integer> getShownNPCs(UUID uuid) {
        PlayerData data = getOrLoadPlayerData(uuid);
        return new HashSet<>(data.shown.getOrDefault(serverName, new HashSet<>()));
    }

    @Override
    public void saveShown(UUID uuid, Set<Integer> ids) {
        PlayerData data = getOrLoadPlayerData(uuid);
        data.shown.put(serverName, new HashSet<>(ids));
        savePlayerDataAsync(uuid, data);
    }

    @Override
    public void saveNPCs(String table, UUID uuid, Set<Integer> ids) {
        PlayerData data = getOrLoadPlayerData(uuid);
        if ("npcutils_hidden".equals(table)) {
            data.hidden.put(serverName, new HashSet<>(ids));
        } else if ("npcutils_shown".equals(table)) {
            data.shown.put(serverName, new HashSet<>(ids));
        }
        savePlayerDataAsync(uuid, data);
    }

    @Override
    public void clearCacheForPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    @Override
    public void savePlayerState(UUID uuid, int npcId, String stateName) {
        PlayerData data = getOrLoadPlayerData(uuid);
        Map<Integer, String> serverStates = data.states.computeIfAbsent(serverName, k -> new HashMap<>());
        serverStates.put(npcId, stateName);
        savePlayerDataAsync(uuid, data);
    }

    @Override
    public String getPlayerState(UUID uuid, int npcId) {
        PlayerData data = getOrLoadPlayerData(uuid);
        Map<Integer, String> serverStates = data.states.get(serverName);
        return serverStates != null ? serverStates.get(npcId) : null;
    }

    @Override
    public Map<Integer, String> getPlayerStates(UUID uuid) {
        PlayerData data = getOrLoadPlayerData(uuid);
        Map<Integer, String> serverStates = data.states.get(serverName);
        return serverStates != null ? new HashMap<>(serverStates) : new HashMap<>();
    }

    // --- Private helper methods ---

    private PlayerData getOrLoadPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadPlayerData);
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File file = getPlayerFile(uuid);
        PlayerData data = new PlayerData();

        if (!file.exists()) {
            return data;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Load hidden NPCs per server
            if (config.contains("hidden")) {
                for (String server : config.getConfigurationSection("hidden").getKeys(false)) {
                    List<Integer> hiddenList = config.getIntegerList("hidden." + server);
                    data.hidden.put(server, new HashSet<>(hiddenList));
                }
            }

            // Load shown NPCs per server
            if (config.contains("shown")) {
                for (String server : config.getConfigurationSection("shown").getKeys(false)) {
                    List<Integer> shownList = config.getIntegerList("shown." + server);
                    data.shown.put(server, new HashSet<>(shownList));
                }
            }

            // Load states per server
            if (config.contains("states")) {
                for (String server : config.getConfigurationSection("states").getKeys(false)) {
                    Map<Integer, String> serverStates = new HashMap<>();
                    for (String npcIdStr : config.getConfigurationSection("states." + server).getKeys(false)) {
                        try {
                            int npcId = Integer.parseInt(npcIdStr);
                            String stateName = config.getString("states." + server + "." + npcIdStr);
                            serverStates.put(npcId, stateName);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    data.states.put(server, serverStates);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + uuid, e);
        }

        return data;
    }

    private void savePlayerData(UUID uuid, PlayerData data) {
        File file = getPlayerFile(uuid);
        FileConfiguration config = new YamlConfiguration();

        // Save hidden NPCs per server
        for (Map.Entry<String, Set<Integer>> entry : data.hidden.entrySet()) {
            config.set("hidden." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        // Save shown NPCs per server
        for (Map.Entry<String, Set<Integer>> entry : data.shown.entrySet()) {
            config.set("shown." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        // Save states per server
        for (Map.Entry<String, Map<Integer, String>> serverEntry : data.states.entrySet()) {
            for (Map.Entry<Integer, String> stateEntry : serverEntry.getValue().entrySet()) {
                config.set("states." + serverEntry.getKey() + "." + stateEntry.getKey(), stateEntry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player data for " + uuid, e);
        }
    }

    private void savePlayerDataAsync(UUID uuid, PlayerData data) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> savePlayerData(uuid, data));
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    /**
     * Internal class to hold player data in memory.
     */
    private static class PlayerData {
        // Server name -> Set of NPC IDs
        Map<String, Set<Integer>> hidden = new HashMap<>();
        Map<String, Set<Integer>> shown = new HashMap<>();
        // Server name -> (NPC ID -> State name)
        Map<String, Map<Integer, String>> states = new HashMap<>();
    }
}
