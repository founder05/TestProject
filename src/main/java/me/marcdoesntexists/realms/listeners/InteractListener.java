package me.marcdoesntexists.realms.listeners;


import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.managers.HybridClaimManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.Claim;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InteractListener implements Listener {


    private static final List<Material> CONTAINERS = Arrays.asList(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARREL,
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.HOPPER,
            Material.DROPPER,
            Material.DISPENSER,
            Material.BREWING_STAND,
            Material.ENCHANTING_TABLE,
            Material.ANVIL,
            Material.CRAFTING_TABLE
    );
    private static final List<Material> INTERACTABLES = Arrays.asList(
            Material.LEVER,
            Material.STONE_BUTTON,
            Material.OAK_BUTTON,
            Material.SPRUCE_BUTTON,
            Material.BIRCH_BUTTON,
            Material.JUNGLE_BUTTON,
            Material.ACACIA_BUTTON,
            Material.DARK_OAK_BUTTON,
            Material.CRIMSON_BUTTON,
            Material.WARPED_BUTTON,
            Material.OAK_DOOR,
            Material.SPRUCE_DOOR,
            Material.BIRCH_DOOR,
            Material.JUNGLE_DOOR,
            Material.ACACIA_DOOR,
            Material.DARK_OAK_DOOR,
            Material.CRIMSON_DOOR,
            Material.WARPED_DOOR,
            Material.OAK_TRAPDOOR,
            Material.SPRUCE_TRAPDOOR,
            Material.BIRCH_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR,
            Material.ACACIA_TRAPDOOR,
            Material.DARK_OAK_TRAPDOOR,
            Material.CRIMSON_TRAPDOOR,
            Material.WARPED_TRAPDOOR,
            Material.OAK_FENCE_GATE,
            Material.SPRUCE_FENCE_GATE,
            Material.BIRCH_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE,
            Material.ACACIA_FENCE_GATE,
            Material.DARK_OAK_FENCE_GATE,
            Material.CRIMSON_FENCE_GATE,
            Material.WARPED_FENCE_GATE
    );
    private final HybridClaimManager hybridClaimManager;
    private final ClaimManager claimManager;
    private final SocietiesManager societiesManager;

    public InteractListener(me.marcdoesntexists.realms.Realms plugin) {
        this.claimManager = ClaimManager.getInstance(plugin);
        this.hybridClaimManager = HybridClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();

    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("realms.admin.bypass")) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        String townName = hybridClaimManager.getTownAtLocation(block.getLocation());

        if (townName != null) {
            Material type = block.getType();

            if (CONTAINERS.contains(type)) {
                Town town = societiesManager.getTown(townName);

                // use isTrusted to allow mayor and trusted members
                if (town == null || !town.isTrusted(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage(MessageUtils.format("errors.generic", Map.of("error", MessageUtils.format("interact.cannot_access", Map.of("town", townName)))));
                    return;
                }
            }
        }

        Chunk chunk = block.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim != null) {
            Material type = block.getType();

            if (CONTAINERS.contains(type)) {
                Town town = societiesManager.getTown(claim.getTownName());

                if (town == null || !town.isTrusted(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage(MessageUtils.format("errors.generic", Map.of("error", MessageUtils.format("interact.cannot_access", Map.of("town", claim.getTownName())))));
                    return;
                }
            }

            if (INTERACTABLES.contains(type)) {
                Town town = societiesManager.getTown(claim.getTownName());

                if (town == null || !town.isTrusted(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage(MessageUtils.format("errors.generic", Map.of("error", MessageUtils.format("interact.cannot_interact", Map.of("town", claim.getTownName())))));

                }
            }
        }
    }
}
