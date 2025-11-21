package me.icegames.ignpcutils;

import me.icegames.ignpcutils.commands.NPCUtilsCommand;
import me.icegames.ignpcutils.database.Storage;
import me.icegames.ignpcutils.listeners.PlayerJoinListener;
import me.icegames.ignpcutils.listeners.PlayerQuitListener;
import me.icegames.ignpcutils.managers.StatusManager;
import me.icegames.ignpcutils.managers.NPCManager;
import me.icegames.ignpcutils.util.NPCResolver;
import me.icegames.ignpcutils.util.ConfigMigration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class IGNpcUtils extends JavaPlugin {

    private static IGNpcUtils instance;
    private NPCManager npcManager;
    private StatusManager statusManager;
    private NPCResolver npcResolver;
    private Storage storage;
    private FileConfiguration messagesConfig;

    private final String pluginName = "NpcUtils";
    private final String pluginDescription = "The NPC Utilities for Citizens2";

    private void startingBanner() {
        System.out.println("\u001B[36m  ___ \u001B[0m\u001B[1;36m____   \u001B[0m");
        System.out.println("\u001B[36m |_ _\u001B[0m\u001B[1;36m/ ___|  \u001B[0m ");
        System.out.println(
                "\u001B[36m  | \u001B[0m\u001B[1;36m| |  _   \u001B[0m \u001B[36mI\u001B[0m\u001B[1;36mG\u001B[0m\u001B[1;37m"
                        + pluginName + " \u001B[1;36mv" + getDescription().getVersion()
                        + "\u001B[0m by \u001B[1;36mIceGames");
        System.out.println("\u001B[36m  | \u001B[0m\u001B[1;36m| |_| |  \u001B[0m \u001B[1;30m" + pluginDescription);
        System.out.println("\u001B[36m |___\u001B[0m\u001B[1;36m\\____| \u001B[0m");
        System.out.println("\u001B[36m         \u001B[0m");
    }

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();

        startingBanner();

        getLogger().info("Starting IGNpcUtils...");
        saveDefaultConfig();
        loadMessages();
        getLogger().info("Configuration successfully loaded.");

        // Run config migration if needed
        ConfigMigration migration = new ConfigMigration(this);
        if (migration.needsMigration()) {
            migration.migrate();
            reloadConfig(); // Reload after migration
            getLogger().info("Configuration migrated successfully.");
        }

        getLogger().info("Loading database...");
        storage = new Storage(this);
        storage.init();

        String storageType = getConfig().getString("storage.type", "UNKNOWN").toUpperCase();
        getLogger().info("Database successfully initialized (" + storageType + ")");

        npcManager = new NPCManager(this, storage);
        npcManager.loadFromConfig();
        statusManager = new StatusManager(this, storage);
        npcResolver = new NPCResolver(this);

        getCommand("npcutils").setExecutor(new NPCUtilsCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        long endTime = System.currentTimeMillis();
        getLogger().info("\u001B[1;32mPlugin loaded successfully in " + (endTime - startTime) + "ms\u001B[0m");
    }

    @Override
    public void onDisable() {
        npcManager.saveToConfig();
        if (storage != null) {
            storage.close();
        }
        getLogger().info("Plugin disabled.");
    }

    public static IGNpcUtils getInstance() {
        return instance;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    public StatusManager getStatusManager() {
        return statusManager;
    }

    public NPCResolver getNPCResolver() {
        return npcResolver;
    }

    public Storage getStorage() {
        return storage;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (messagesFile.exists()) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } else {
            saveResource("messages.yml", false);
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }
}
