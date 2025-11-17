package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class PlayerNotificationListener implements Listener {
    private final Realms plugin;
    private final DataManager dataManager;

    public PlayerNotificationListener(Realms plugin) {
        this.plugin = plugin;
        this.dataManager = DataManager.getInstance(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        PlayerData data = dataManager.getPlayerData(id);
        if (data == null) return;

        List<String> notes = data.getNotifications();
        if (notes == null || notes.isEmpty()) return;

        // deliver notifications
        for (String n : notes) {
            try {
                player.sendMessage(n);
            } catch (Throwable ignored) {
            }
        }

        // clear and persist
        data.clearNotifications();
        dataManager.savePlayerData(id);
    }
}

