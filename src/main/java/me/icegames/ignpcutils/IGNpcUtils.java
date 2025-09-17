package me.icegames.ignpcutils;

import me.icegames.ignpcutils.commands.NPCUtilsCommand;
import me.icegames.ignpcutils.database.Storage;
import me.icegames.ignpcutils.listeners.PlayerJoinListener;
import me.icegames.ignpcutils.listeners.PlayerQuitListener;
import me.icegames.ignpcutils.managers.NPCManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import static java.lang.Character.toUpperCase;

public class IGNpcUtils extends JavaPlugin {

    private static IGNpcUtils instance;
    private NPCManager npcManager;
    private Storage storage;
    private FileConfiguration messagesConfig;
    private FileConfiguration config;

    private final String pluginName = "NpcUtils";
    private final String pluginDescription = "The NPC Utilities for Citizens2";
    private final String consolePrefix = "\u001B[1;30m[\u001B[0m\u001B[36mI\u001B[1;36mG\u001B[0m\u001B[1;37m" + pluginName + "\u001B[1;30m]\u001B[0m ";

    private void startingBanner() {
        System.out.println("\u001B[36m  ___ \u001B[0m\u001B[1;36m____   \u001B[0m");
        System.out.println("\u001B[36m |_ _\u001B[0m\u001B[1;36m/ ___|  \u001B[0m ");
        System.out.println("\u001B[36m  | \u001B[0m\u001B[1;36m| |  _   \u001B[0m \u001B[36mI\u001B[0m\u001B[1;36mG\u001B[0m\u001B[1;37m" + pluginName + " \u001B[1;36mv" + getDescription().getVersion() + "\u001B[0m by \u001B[1;36mIceGames");
        System.out.println("\u001B[36m  | \u001B[0m\u001B[1;36m| |_| |  \u001B[0m \u001B[1;30m" + pluginDescription);
        System.out.println("\u001B[36m |___\u001B[0m\u001B[1;36m\\____| \u001B[0m");
        System.out.println("\u001B[36m         \u001B[0m");
    }

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();

        startingBanner();

        System.out.println(consolePrefix + "Starting IGNpcUtils...");
        saveDefaultConfig();
        saveDefaultMessagesConfig();
        System.out.println(consolePrefix + "Configuration successfully loaded.");
        System.out.println(consolePrefix + "Loading database...");

        this.storage = new Storage(this);
        storage.init();

        String storageType = getConfig().getString("storage.type", "UNKNOWN").toUpperCase();
        System.out.println(consolePrefix + "Database successfully initialized (" + storageType + ")");

        npcManager = new NPCManager(this, storage);
        npcManager.loadFromConfig();

        getCommand("npcutils").setExecutor(new NPCUtilsCommand(npcManager, getMessagesConfig(), this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(npcManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(npcManager), this);

        long endTime = System.currentTimeMillis();
        System.out.println(consolePrefix + "\u001B[1;32mPlugin loaded successfully in " + (endTime - startTime) + "ms\u001B[0m");
    }

    @Override
    public void onDisable() {
        npcManager.saveToConfig();
        storage.close();
        getLogger().info("Plugin disabled.");
    }

    public static IGNpcUtils getInstance() {
        return instance;
    }

    private void saveDefaultMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            File messagesFile = new File(getDataFolder(), "messages.yml");
            messagesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(messagesFile);
        }
        return messagesConfig;
    }

    public void reloadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (messagesFile.exists()) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } else {
            saveDefaultMessagesConfig();
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }
}
