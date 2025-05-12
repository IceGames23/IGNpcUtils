package me.icegames.ignpcutils.database;

import me.icegames.ignpcutils.IGNpcUtils;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class Storage {
    private final IGNpcUtils plugin;
    private Connection connection;

    public Storage(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    public void init() {
        String type = plugin.getConfig().getString("storage.type", "sqlite");
        try {
            if (type.equalsIgnoreCase("mysql")) {
                String host = plugin.getConfig().getString("storage.mysql.host");
                int port = plugin.getConfig().getInt("storage.mysql.port");
                String db = plugin.getConfig().getString("storage.mysql.database");
                String user = plugin.getConfig().getString("storage.mysql.username");
                String pass = plugin.getConfig().getString("storage.mysql.password");
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + db, user, pass);
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/npcutils.db");
            }
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao conectar no banco de dados", e);
        }
    }

    public void createTables() {
        String createShownTable = "CREATE TABLE IF NOT EXISTS npcutils_shown (uuid TEXT, npc_id INTEGER)";
        String createHiddenTable = "CREATE TABLE IF NOT EXISTS npcutils_hidden (uuid TEXT, npc_id INTEGER)";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createShownTable);
            stmt.executeUpdate(createHiddenTable);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar tabelas no banco de dados", e);
        }
    }

    public Set<Integer> getNPCs(String table, UUID uuid) {
        Set<Integer> result = new HashSet<>();
        String query = "SELECT npc_id FROM " + table + " WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("npc_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao buscar NPCs do jogador em " + table, e);
        }
        return result;
    }

    public void saveNPCs(String table, UUID uuid, Set<Integer> ids) {
        String deleteQuery = "DELETE FROM " + table + " WHERE uuid = ?";
        String insertQuery = "INSERT INTO " + table + " (uuid, npc_id) VALUES (?, ?)";
        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery);
             PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            deleteStmt.setString(1, uuid.toString());
            deleteStmt.executeUpdate();

            for (int id : ids) {
                insertStmt.setString(1, uuid.toString());
                insertStmt.setInt(2, id);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao salvar NPCs para jogador em " + table, e);
        }
    }

    public Set<Integer> getShownNPCs(UUID uuid) {
        return getNPCs("npcutils_shown", uuid);
    }

    public Set<Integer> getHiddenNPCs(UUID uuid) {
        return getNPCs("npcutils_hidden", uuid);
    }

    public void saveShown(UUID uuid, Set<Integer> ids) {
        saveNPCs("npcutils_shown", uuid, ids);
    }

    public void saveHidden(UUID uuid, Set<Integer> ids) {
        saveNPCs("npcutils_hidden", uuid, ids);
    }
}