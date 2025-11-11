package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ClaimManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.Claim;
import org.bukkit.Chunk;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class EntityListener implements Listener {
    
    private final Nations plugin;
    private final ClaimManager claimManager;
    private final SocietiesManager societiesManager;
    
    public EntityListener(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        Chunk chunk = victim.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);
        
        if (claim != null) {
            if (!claim.isPvpEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage("§c✘ PvP is disabled in §6" + claim.getTownName() + "§c!");
                return;
            }
            
            Town town = societiesManager.getTown(claim.getTownName());
            if (town != null) {
                if (town.getMembers().contains(attacker.getUniqueId()) && 
                    town.getMembers().contains(victim.getUniqueId())) {
                    event.setCancelled(true);
                    attacker.sendMessage("§c✘ You cannot attack your own town members!");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Chunk chunk = event.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);
        
        if (claim != null) {
            if (!claim.isExplosionsEnabled()) {
                event.blockList().clear();
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        
        Chunk chunk = event.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);
        
        if (claim != null) {
            if (!claim.isMobSpawningEnabled()) {
                event.setCancelled(true);
            }
        }
    }
}
