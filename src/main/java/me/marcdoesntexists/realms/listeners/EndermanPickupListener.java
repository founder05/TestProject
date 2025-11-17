package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.utils.Claim;
import org.bukkit.Chunk;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class EndermanPickupListener implements Listener {
    private final Realms plugin;
    private final ClaimManager claimManager;

    public EndermanPickupListener(Realms plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Enderman)) return;

        try {
            Chunk chunk = event.getBlock().getLocation().getChunk();
            Claim claim = claimManager.getClaimAt(chunk);
            if (claim != null) {
                // Prevent Enderman from picking up or placing blocks inside any claim
                event.setCancelled(true);
            }
        } catch (Throwable ignored) {
        }
    }
}

