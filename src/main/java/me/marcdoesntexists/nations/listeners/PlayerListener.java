package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.economy.EconomyService;
import me.marcdoesntexists.nations.managers.ClaimVisualizer;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

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
            player.sendMessage(MessageUtils.format("town.welcome_back", Map.of("town", data.getTown())));
        } else {
            player.sendMessage(MessageUtils.get("commands.town_create_hint"));
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
