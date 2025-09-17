package me.icegames.ignpcutils.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.icegames.ignpcutils.IGNpcUtils;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Storage {
    private final IGNpcUtils plugin;
    private HikariDataSource dataSource;
    private final Map<String, Set<Integer>> cache = new ConcurrentHashMap<>();

    public Storage(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    public void init() {
        String type = plugin.getConfig().getString("storage.type", "sqlite");
        try {
            if (type.equalsIgnoreCase("mysql")) {
                setupMySQL();
            } else {
                setupSQLite();
            }
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error initializing database", e);
        }
    }

    private void setupMySQL() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" +
                plugin.getConfig().getString("storage.mysql.host") + ":" +
                plugin.getConfig().getInt("storage.mysql.port") + "/" +
                plugin.getConfig().getString("storage.mysql.database") +
                "?autoReconnect=true&useSSL=false");
        config.setUsername(plugin.getConfig().getString("storage.mysql.username"));
        config.setPassword(plugin.getConfig().getString("storage.mysql.password"));
        config.setMaximumPoolSize(plugin.getConfig().getInt("storage.mysql.pool_size", 10));
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(2000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);
    }

    private void setupSQLite() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:plugins/IGNpcUtils/npcutils.db");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(2000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);
    }

    private void createTables() {
        String createShownTable = "CREATE TABLE IF NOT EXISTS npcutils_shown (uuid TEXT, npc_id INTEGER)";
        String createHiddenTable = "CREATE TABLE IF NOT EXISTS npcutils_hidden (uuid TEXT, npc_id INTEGER)";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createShownTable);
            stmt.executeUpdate(createHiddenTable);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating tables in database", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized.");
        }
        return dataSource.getConnection();
    }

    public Set<Integer> getHiddenNPCs(UUID uuid) {
        return getNPCs("npcutils_hidden", uuid);
    }

    public Set<Integer> getShownNPCs(UUID uuid) {
        return getNPCs("npcutils_shown", uuid);
    }

    public Set<Integer> getNPCs(String table, UUID uuid) {
        String cacheKey = table + ":" + uuid;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        Set<Integer> result = new HashSet<>();
        String query = "SELECT npc_id FROM " + table + " WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("npc_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error searching player NPCs in " + table, e);
        }

        cache.put(cacheKey, result);
        return result;
    }

    public void saveShown(UUID uuid, Set<Integer> ids) {
        saveNPCs("npcutils_shown", uuid, ids);
    }

    public void saveNPCs(String table, UUID uuid, Set<Integer> ids) {
        String cacheKey = table + ":" + uuid;
        cache.remove(cacheKey);

        String deleteQuery = "DELETE FROM " + table + " WHERE uuid = ?";
        String insertQuery = "INSERT INTO " + table + " (uuid, npc_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
             PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            conn.setAutoCommit(false);

            deleteStmt.setString(1, uuid.toString());
            deleteStmt.executeUpdate();

            for (int id : ids) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.setInt(2, id);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();

            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving NPCs for player in " + table, e);
        }
    }

    public void clearCacheForPlayer(UUID uuid) {
        cache.remove("npcutils_shown:" + uuid);
        cache.remove("npcutils_hidden:" + uuid);
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}