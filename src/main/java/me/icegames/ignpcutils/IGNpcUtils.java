package me.icegames.ignpcutils;

import me.icegames.ignpcutils.database.Storage;
import me.icegames.ignpcutils.listeners.PlayerJoinListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class IGNpcUtils extends JavaPlugin {

    private static IGNpcUtils instance;
    private NPCManager npcManager;
    private Storage storage;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Iniciando o plugin IGNpcUtils...");
        getLogger().info("Versão: " + getDescription().getVersion());
        getLogger().info("Autor: IceGames");

        saveDefaultConfig();
        getLogger().info("Configuração carregada com sucesso.");

        this.storage = new Storage(this);
        storage.init();
        getLogger().info("Database inicializada com sucesso.");

        npcManager = new NPCManager(this, storage);
        npcManager.loadFromConfig();

        getCommand("npcutils").setExecutor(new NPCUtilsCommand(npcManager, getConfig(), this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(npcManager), this);

        getLogger().info("Plugin IGNpcUtils iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Encerrando o plugin IGNpcUtils...");
        npcManager.saveToConfig(); // Salva os NPCs ocultos globalmente no config.yml
        getLogger().info("Plugin IGNpcUtils encerrado com sucesso.");
    }

    public static IGNpcUtils getInstance() {
        return instance;
    }

    public FileConfiguration getMessages() {
        return getConfig();
    }
}
