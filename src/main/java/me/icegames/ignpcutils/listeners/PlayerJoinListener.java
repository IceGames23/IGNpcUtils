package me.icegames.ignpcutils.listeners;

import me.icegames.ignpcutils.IGNpcUtils;
import org.bukkit.entity.Player;
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
        Player player = event.getPlayer();

        // First, handle manual visibility overrides
        plugin.getNPCManager().handleJoin(player);

        // Load saved states from database
        plugin.getStatusManager().loadPlayerStates(player.getUniqueId());

        // Apply default states for NPCs that don't have saved states
        // Schedule this slightly delayed to ensure NPCs are fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getStatusManager().applyDefaultStates(player);
            }
        }, 10L); // 0.5 second delay
    }
}
