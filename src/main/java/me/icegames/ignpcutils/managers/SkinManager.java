package me.icegames.ignpcutils.managers;

import me.icegames.ignpcutils.IGNpcUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class SkinManager {

    private final IGNpcUtils plugin;
    private FileConfiguration skinsConfig;
    private File skinsFile;

    public SkinManager(IGNpcUtils plugin) {
        this.plugin = plugin;
        loadSkins();
    }

    public void loadSkins() {
        if (skinsFile == null) {
            skinsFile = new File(plugin.getDataFolder(), "skins.yml");
        }
        if (!skinsFile.exists()) {
            plugin.saveResource("skins.yml", false);
        }
        skinsConfig = YamlConfiguration.loadConfiguration(skinsFile);
    }

    public void saveSkin(String name, String value, String signature, String url) {
        skinsConfig.set(name + ".value", value);
        skinsConfig.set(name + ".signature", signature);
        skinsConfig.set(name + ".url", url);
        saveConfig();
    }

    public SkinData getSkin(String name) {
        if (!skinsConfig.contains(name)) {
            return null;
        }
        String value = skinsConfig.getString(name + ".value");
        String signature = skinsConfig.getString(name + ".signature");
        String url = skinsConfig.getString(name + ".url");
        return new SkinData(value, signature, url);
    }

    public boolean hasSkin(String name) {
        return skinsConfig.contains(name);
    }

    public java.util.Set<String> getSkins() {
        return skinsConfig.getKeys(false);
    }

    public void removeSkin(String name) {
        skinsConfig.set(name, null);
        saveConfig();
    }

    public void saveConfig() {
        if (skinsConfig == null || skinsFile == null) {
            return;
        }
        try {
            skinsConfig.save(skinsFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + skinsFile, ex);
        }
    }

    public static class SkinData {
        private final String value;
        private final String signature;
        private final String url;

        public SkinData(String value, String signature, String url) {
            this.value = value;
            this.signature = signature;
            this.url = url;
        }

        public String getValue() {
            return value;
        }

        public String getSignature() {
            return signature;
        }

        public String getUrl() {
            return url;
        }
    }
}
