package me.icegames.ignpcutils.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.icegames.ignpcutils.IGNpcUtils;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Storage implements IStorage {
    private final IGNpcUtils plugin;
    private HikariDataSource dataSource;
    private final Map<String, Set<Integer>> cache = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, String>> statesCache = new ConcurrentHashMap<>();

    private static final Set<String> VALID_TABLES = Set.of(
            "npcutils_shown", "npcutils_hidden", "npcutils_states");

    public Storage(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        String type = plugin.getConfig().getString("storage.type", "sqlite");
        plugin.getLogger().info("Initializing database (" + type.toUpperCase() + ")...");
        try {
            if (type.equalsIgnoreCase("mysql")) {
                setupMySQL();
            } else {
                setupSQLite();
            }
            plugin.getLogger().info("Database connection pool created successfully.");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "FATAL: Database initialization failed!", e);
            plugin.getLogger().severe("Plugin cannot function without database. Disabling...");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private void setupMySQL() {
        String host = plugin.getConfig().getString("storage.mysql.host");
        int port = plugin.getConfig().getInt("storage.mysql.port");
        String database = plugin.getConfig().getString("storage.mysql.database");
        String username = plugin.getConfig().getString("storage.mysql.username");
        String password = plugin.getConfig().getString("storage.mysql.password");

        plugin.getLogger().info("Connecting to MySQL: " + host + ":" + port + "/" + database);

        try {
            HikariConfig config = new HikariConfig();

            // Simple JDBC URL - let the driver handle everything
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);

            // Conservative pool settings
            config.setMaximumPoolSize(plugin.getConfig().getInt("storage.mysql.pool_size", 10));
            config.setMinimumIdle(2);
            config.setMaxLifetime(1800000); // 30 minutes
            config.setConnectionTimeout(5000); // 5 seconds
            config.setIdleTimeout(600000); // 10 minutes

            // MySQL optimizations via DataSource properties
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            plugin.getLogger().info("Creating MySQL connection pool...");
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("MySQL connected successfully!");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MySQL database");
            plugin.getLogger().severe("  Host: " + host + ":" + port);
            plugin.getLogger().severe("  Database: " + database);
            plugin.getLogger().severe("  Username: " + username);
            throw new RuntimeException("MySQL connection failed", e);
        }
    }

    private void setupSQLite() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:plugins/IGNpcUtils/npcutils.db");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setConnectionTestQuery(null);
        dataSource = new HikariDataSource(config);
    }

    private void createTables() {
        String createShownTable = "CREATE TABLE IF NOT EXISTS npcutils_shown (uuid TEXT, npc_id INTEGER, server_name TEXT)";
        String createHiddenTable = "CREATE TABLE IF NOT EXISTS npcutils_hidden (uuid TEXT, npc_id INTEGER, server_name TEXT)";
        String createStatesTable = "CREATE TABLE IF NOT EXISTS npcutils_states (uuid TEXT, npc_id INTEGER, state_name TEXT, server_name TEXT)";
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createShownTable);
            stmt.executeUpdate(createHiddenTable);
            stmt.executeUpdate(createStatesTable);

            addColumnIfNotExists(conn, "npcutils_shown", "server_name", "TEXT");
            addColumnIfNotExists(conn, "npcutils_hidden", "server_name", "TEXT");
            addColumnIfNotExists(conn, "npcutils_states", "server_name", "TEXT");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating tables in database", e);
        }
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String type) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, table, column);
            if (!rs.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                    plugin.getLogger().info("Added column " + column + " to table " + table);

                    String serverName = plugin.getConfig().getString("server_name", "survival");
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " IS NULL")) {
                        ps.setString(1, serverName);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error checking/adding column " + column + " to table " + table, e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized.");
        }
        return dataSource.getConnection();
    }

    @Override
    public Set<Integer> getHiddenNPCs(UUID uuid) {
        return getNPCs("npcutils_hidden", uuid);
    }

    @Override
    public Set<Integer> getShownNPCs(UUID uuid) {
        return getNPCs("npcutils_shown", uuid);
    }

    public Set<Integer> getNPCs(String table, UUID uuid) {
        if (!VALID_TABLES.contains(table)) {
            plugin.getLogger().warning("Attempted to access invalid table: " + table);
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        String cacheKey = table + ":" + uuid;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        Set<Integer> result = new HashSet<>();
        String serverName = plugin.getConfig().getString("server_name", "survival");
        String query = "SELECT npc_id FROM " + table + " WHERE uuid = ? AND server_name = ?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serverName);
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

    @Override
    public void saveShown(UUID uuid, Set<Integer> ids) {
        saveNPCs("npcutils_shown", uuid, ids);
    }

    @Override
    public void saveNPCs(String table, UUID uuid, Set<Integer> ids) {
        String cacheKey = table + ":" + uuid;
        cache.remove(cacheKey);

        String serverName = plugin.getConfig().getString("server_name", "survival");
        String deleteQuery = "DELETE FROM " + table + " WHERE uuid = ? AND server_name = ?";
        String insertQuery = "INSERT INTO " + table + " (uuid, npc_id, server_name) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            conn.setAutoCommit(false);

            deleteStmt.setString(1, uuid.toString());
            deleteStmt.setString(2, serverName);
            deleteStmt.executeUpdate();

            for (int id : ids) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.setInt(2, id);
                insertStmt.setString(3, serverName);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();

            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving NPCs for player in " + table, e);
        }
    }

    @Override
    public void clearCacheForPlayer(UUID uuid) {
        cache.remove("npcutils_shown:" + uuid);
        cache.remove("npcutils_hidden:" + uuid);
        cache.remove("npcutils_states:" + uuid);
        statesCache.remove("states:" + uuid);
    }

    @Override
    public void savePlayerState(UUID uuid, int npcId, String stateName) {
        String serverName = plugin.getConfig().getString("server_name", "survival");
        String deleteQuery = "DELETE FROM npcutils_states WHERE uuid = ? AND npc_id = ? AND server_name = ?";
        String insertQuery = "INSERT INTO npcutils_states (uuid, npc_id, state_name, server_name) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            conn.setAutoCommit(false);

            deleteStmt.setString(1, uuid.toString());
            deleteStmt.setInt(2, npcId);
            deleteStmt.setString(3, serverName);
            deleteStmt.executeUpdate();

            insertStmt.setString(1, uuid.toString());
            insertStmt.setInt(2, npcId);
            insertStmt.setString(3, stateName);
            insertStmt.setString(4, serverName);
            insertStmt.executeUpdate();

            conn.commit();
            cache.remove("npcutils_states:" + uuid);
            statesCache.remove("states:" + uuid);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving player state", e);
        }
    }

    @Override
    public String getPlayerState(UUID uuid, int npcId) {
        Map<Integer, String> states = getPlayerStates(uuid);
        return states.get(npcId);
    }

    @Override
    public Map<Integer, String> getPlayerStates(UUID uuid) {
        String cacheKey = "states:" + uuid;
        if (statesCache.containsKey(cacheKey)) {
            return new HashMap<>(statesCache.get(cacheKey));
        }

        Map<Integer, String> result = new HashMap<>();
        String serverName = plugin.getConfig().getString("server_name", "survival");
        String query = "SELECT npc_id, state_name FROM npcutils_states WHERE uuid = ? AND server_name = ?";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serverName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("npc_id"), rs.getString("state_name"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error loading player states", e);
        }

        statesCache.put(cacheKey, result);
        return result;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}