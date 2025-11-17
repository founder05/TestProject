package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * JailManager - manages jail functionality for towns/kingdoms/empires.
 */
public class JailManager {
    private static JailManager instance;
    private final Realms plugin;

    private JailManager(Realms plugin) {
        this.plugin = plugin;
    }

    public static JailManager getInstance(Realms plugin) {
        if (instance == null) {
            instance = new JailManager(plugin);
        }
        return instance;
    }

    public static JailManager getInstance() {
        return instance;
    }

    public boolean sendToJail(Player player, String societyName, Location jailLocation) {
        if (jailLocation == null) return false;
        try {
            player.teleport(jailLocation);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

