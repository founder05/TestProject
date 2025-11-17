package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Syncs PlayerData.town with live Town membership on player join.
 * - If PlayerData.town is set but player is not listed in that Town -> clears PlayerData.town
 * - If PlayerData.town is null but player UUID is present in a Town members -> sets PlayerData.town
 * Saves the PlayerData if changed.
 */
public class PlayerDataSyncListener implements Listener {
    private final Realms plugin;
    private final SocietiesManager societiesManager;

    public PlayerDataSyncListener(Realms plugin) {
        this.plugin = plugin;
        this.societiesManager = plugin.getSocietiesManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getDataManager() == null || societiesManager == null) return;

        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        boolean changed = false;

        // If PlayerData has a town, ensure the Town actually contains the player
        String pdTown = data.getTown();
        if (pdTown != null && !pdTown.isEmpty()) {
            Town town = societiesManager.getTown(pdTown);
            if (town == null || !town.getMembers().contains(uuid)) {
                data.setTown(null);
                changed = true;
                plugin.getLogger().info("PlayerDataSync: Removed stale town for player " + event.getPlayer().getName() + " (was: " + pdTown + ")");
            }
        }

        // If PlayerData has no town, try to find a Town that lists this player
        if (data.getTown() == null) {
            for (Town t : societiesManager.getAllTowns()) {
                if (t.getMembers().contains(uuid)) {
                    data.setTown(t.getName());
                    changed = true;
                    plugin.getLogger().info("PlayerDataSync: Restored town " + t.getName() + " for player " + event.getPlayer().getName());
                    break;
                }
            }
        }

        if (changed) {
            try {
                plugin.getDataManager().savePlayerData(uuid);
            } catch (Throwable t) {
                plugin.getLogger().warning("PlayerDataSync: Failed to save PlayerData for " + event.getPlayer().getName() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Ensure we persist any changes to data on quit as well
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getDataManager() == null) return;
        try {
            plugin.getDataManager().savePlayerData(uuid);
        } catch (Throwable t) {
            plugin.getLogger().warning("PlayerDataSync: Failed to save PlayerData on quit for " + event.getPlayer().getName() + ": " + t.getMessage());
        }
    }
}

