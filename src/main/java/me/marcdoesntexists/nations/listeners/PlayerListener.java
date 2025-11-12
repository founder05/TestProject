package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.economy.EconomyService;
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

        // If an external economy is available, sync the cached PlayerData balance immediately
        try {
            EconomyService es = EconomyService.getInstance();
            if (es != null) {
                double bal = es.getPlayerBalance(player.getUniqueId());
                data.setMoney((int) Math.floor(bal));
            }
        } catch (Exception ignored) {}

        if (data.getTown() != null) {
            player.sendMessage("§7[§6Nations§7] §eWelcome back to §6" + data.getTown() + "§e!");
        } else {
            player.sendMessage("§7[§6Nations§7] §eType §6/town create <name>§e to start your own town!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Unload will save and remove player from cache. Avoid double-saving.
        dataManager.unloadPlayerData(player.getUniqueId());
        ClaimVisualizer cv = plugin.getClaimVisualizer();
        if (cv != null) {
            cv.stopVisualization(player);
        }
    }
}
