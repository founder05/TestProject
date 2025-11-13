package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

public class ExplosionProtectionListener implements Listener {
    private final Nations plugin;

    public ExplosionProtectionListener(Nations plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        try {
            Entity e = event.getEntity();
            Location center = event.getLocation();
            String centerTown = plugin.getHybridClaimManager().getTownAtLocation(center);

            // If explosion is in wilderness or plugin not ready, do nothing
            if (centerTown == null) return;

            // Iterate over block list and remove any block that belongs to a different town than the explosion center.
            Iterator<Block> it = event.blockList().iterator();
            while (it.hasNext()) {
                Block b = it.next();
                String bTown = plugin.getHybridClaimManager().getTownAtLocation(b.getLocation());
                if (bTown == null) {
                    // if block is wilderness, we allow destruction
                    continue;
                }
                if (!centerTown.equals(bTown)) {
                    // block is inside a different town - protect it
                    it.remove();
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ExplosionProtectionListener error: " + t.getMessage());
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        try {
            Location center = event.getBlock().getLocation();
            String centerTown = plugin.getHybridClaimManager().getTownAtLocation(center);
            if (centerTown == null) return;
            Iterator<Block> it = event.blockList().iterator();
            while (it.hasNext()) {
                Block b = it.next();
                String bTown = plugin.getHybridClaimManager().getTownAtLocation(b.getLocation());
                if (bTown == null) continue;
                if (!centerTown.equals(bTown)) it.remove();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ExplosionProtectionListener error: " + t.getMessage());
        }
    }
}

