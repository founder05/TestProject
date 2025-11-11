package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ClaimVisualizer;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Nations plugin;
    private final DataManager dataManager;

    public PlayerListener(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() != null) {
            player.sendMessage("§7[§6Nations§7] §eWelcome back to §6" + data.getTown() + "§e!");
        } else {
            player.sendMessage("§7[§6Nations§7] §eType §6/town create <name>§e to start your own town!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        dataManager.savePlayerData(player.getUniqueId());
        dataManager.unloadPlayerData(player.getUniqueId());

        // claimVisualizer may be initialized after this listener; obtain dynamically and null-check
        ClaimVisualizer cv = plugin.getClaimVisualizer();
        if (cv != null) {
            cv.stopVisualization(player);
        }
    }
}
