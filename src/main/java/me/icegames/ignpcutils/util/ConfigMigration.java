package me.icegames.ignpcutils.util;

import me.icegames.ignpcutils.IGNpcUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ConfigMigration {

    private final IGNpcUtils plugin;

    public ConfigMigration(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes config migration from v1.x to v2.0
     * Converts defaultHidden list to new states format
     */
    public void migrate() {
        FileConfiguration config = plugin.getConfig();

        // Check if old format exists
        if (!config.contains("defaultHidden")) {
            plugin.getLogger().info("No migration needed - config already in new format.");
            return;
        }

        plugin.getLogger().info("=========================================");
        plugin.getLogger().info("Config migration started (v1.x -> v2.0)");
        plugin.getLogger().info("=========================================");

        // Backup old config
        backupConfig();

        // Get old default hidden NPCs
        List<Integer> defaultHidden = config.getIntegerList("defaultHidden");
        plugin.getLogger().info("Found " + defaultHidden.size() + " NPCs in defaultHidden");

        // Migrate to new format
        ConfigurationSection npcsSection = config.getConfigurationSection("npcs");
        if (npcsSection == null) {
            config.createSection("npcs");
            npcsSection = config.getConfigurationSection("npcs");
        }

        int migratedCount = 0;
        for (int npcId : defaultHidden) {
            String npcPath = "npcs." + npcId;

            // Check if already migrated
            if (config.contains(npcPath + ".states")) {
                plugin.getLogger().info("  - NPC " + npcId + " already has states, skipping");
                continue;
            }

            // Create default state with visible: false
            config.set(npcPath + ".states.default.visible", false);
            config.set(npcPath + ".states.default.actions", List.of());

            plugin.getLogger().info("  - Migrated NPC " + npcId + " (default hidden)");
            migratedCount++;
        }

        // Remove old format
        config.set("defaultHidden", null);

        // Save config
        plugin.saveConfig();

        plugin.getLogger().info("=========================================");
        plugin.getLogger().info("Migration complete!");
        plugin.getLogger().info("  - " + migratedCount + " NPCs migrated");
        plugin.getLogger().info("  - Backup saved: config.yml.backup");
        plugin.getLogger().info("=========================================");
    }

    /**
     * Creates a backup of the current config
     */
    private void backupConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");

            if (configFile.exists()) {
                Files.copy(
                        configFile.toPath(),
                        backupFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Config backup created: config.yml.backup");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create config backup: " + e.getMessage());
        }
    }

    /**
     * Check if migration is needed
     */
    public boolean needsMigration() {
        return plugin.getConfig().contains("defaultHidden");
    }
}
