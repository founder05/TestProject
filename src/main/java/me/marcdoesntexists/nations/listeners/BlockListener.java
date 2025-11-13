package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ClaimManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.Claim;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Map;

public class BlockListener implements Listener {

    private final Nations plugin;
    private final ClaimManager claimManager;
    private final SocietiesManager societiesManager;

    public BlockListener(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("nations.admin.bypass")) {
            return;
        }

        Chunk chunk = event.getBlock().getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim != null) {
            Town town = societiesManager.getTown(claim.getTownName());

            if (town == null || !town.getMembers().contains(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.format("block.cannot_break", Map.of("town", claim.getTownName())));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("nations.admin.bypass")) {
            return;
        }

        Chunk chunk = event.getBlock().getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim != null) {
            Town town = societiesManager.getTown(claim.getTownName());

            if (town == null || !town.getMembers().contains(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.format("block.cannot_place", Map.of("town", claim.getTownName())));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Chunk chunk = event.getBlock().getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim != null) {
            if (!claim.isFireSpreadEnabled()) {
                event.setCancelled(true);
            }
        }
    }
}
