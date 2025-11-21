package me.icegames.ignpcutils.listeners;

import me.icegames.ignpcutils.IGNpcUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final IGNpcUtils plugin;

    public PlayerJoinListener(IGNpcUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getNPCManager().handleJoin(event.getPlayer());
        plugin.getStatusManager().loadPlayerStates(event.getPlayer().getUniqueId());
    }
}
