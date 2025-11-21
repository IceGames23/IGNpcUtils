package me.icegames.ignpcutils.listeners;

import me.icegames.ignpcutils.IGNpcUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final IGNpcUtils plugin;

    public PlayerQuitListener(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getNPCManager().handleQuit(event.getPlayer());
        plugin.getStatusManager().clearPlayerStates(event.getPlayer().getUniqueId());
    }
}