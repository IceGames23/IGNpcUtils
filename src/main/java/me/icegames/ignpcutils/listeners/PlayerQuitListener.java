package me.icegames.ignpcutils.listeners;

import me.icegames.ignpcutils.database.Storage;
import me.icegames.ignpcutils.managers.NPCManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final NPCManager manager;
    private final Storage storage;

    public PlayerQuitListener(NPCManager manager) {
        this.manager = manager;
        this.storage = manager.getStorage();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
        storage.clearCacheForPlayer(event.getPlayer().getUniqueId());
    }
}