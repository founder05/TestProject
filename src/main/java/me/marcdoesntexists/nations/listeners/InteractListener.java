package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ClaimManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.Claim;
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

public class InteractListener implements Listener {
    
    private final Nations plugin;
    private final ClaimManager claimManager;
    private final SocietiesManager societiesManager;
    
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
    
    public InteractListener(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (player.hasPermission("nations.admin.bypass")) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        
        Chunk chunk = block.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);
        
        if (claim != null) {
            Material type = block.getType();
            
            if (CONTAINERS.contains(type)) {
                Town town = societiesManager.getTown(claim.getTownName());
                
                if (town == null || !town.getMembers().contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("§c✘ You cannot access containers in §6" + claim.getTownName() + "§c!");
                    return;
                }
            }
            
            if (INTERACTABLES.contains(type)) {
                Town town = societiesManager.getTown(claim.getTownName());
                
                if (town == null || !town.getMembers().contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("§c✘ You cannot interact with blocks in §6" + claim.getTownName() + "§c!");
                    return;
                }
            }
        }
    }
}
