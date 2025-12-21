package me.icegames.ignpcutils.database;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interface for storage implementations.
 * Allows the plugin to work with different storage backends (YAML, SQLite,
 * MySQL).
 */
public interface IStorage {

    /**
     * Initialize the storage backend.
     */
    void init();

    /**
     * Close the storage backend and release resources.
     */
    void close();

    /**
     * Get the set of NPC IDs hidden for a specific player.
     *
     * @param uuid the player's UUID
     * @return set of hidden NPC IDs
     */
    Set<Integer> getHiddenNPCs(UUID uuid);

    /**
     * Get the set of NPC IDs shown for a specific player.
     *
     * @param uuid the player's UUID
     * @return set of shown NPC IDs
     */
    Set<Integer> getShownNPCs(UUID uuid);

    /**
     * Save shown NPCs for a player.
     *
     * @param uuid the player's UUID
     * @param ids  set of NPC IDs to save as shown
     */
    void saveShown(UUID uuid, Set<Integer> ids);

    /**
     * Save NPCs to a specific table/category for a player.
     *
     * @param table the table name (npcutils_shown or npcutils_hidden)
     * @param uuid  the player's UUID
     * @param ids   set of NPC IDs to save
     */
    void saveNPCs(String table, UUID uuid, Set<Integer> ids);

    /**
     * Clear all cached data for a player.
     *
     * @param uuid the player's UUID
     */
    void clearCacheForPlayer(UUID uuid);

    /**
     * Save a player's state for a specific NPC.
     *
     * @param uuid      the player's UUID
     * @param npcId     the NPC ID
     * @param stateName the state name to save
     */
    void savePlayerState(UUID uuid, int npcId, String stateName);

    /**
     * Get a player's state for a specific NPC.
     *
     * @param uuid  the player's UUID
     * @param npcId the NPC ID
     * @return the state name, or null if not set
     */
    String getPlayerState(UUID uuid, int npcId);

    /**
     * Get all states for a player.
     *
     * @param uuid the player's UUID
     * @return map of NPC ID to state name
     */
    Map<Integer, String> getPlayerStates(UUID uuid);
}
