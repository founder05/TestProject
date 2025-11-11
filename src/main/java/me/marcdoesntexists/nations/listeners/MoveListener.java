package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ClaimManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.Claim;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoveListener implements Listener {
    
    private final Nations plugin;
    private final ClaimManager claimManager;
    private final SocietiesManager societiesManager;
    
    private final Map<UUID, String> playerChunkCache = new HashMap<>();
    
    public MoveListener(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        Player player = event.getPlayer();
        Chunk toChunk = event.getTo().getChunk();
        String toChunkKey = claimManager.getChunkKey(toChunk);
        
        String cachedChunk = playerChunkCache.get(player.getUniqueId());
        if (toChunkKey.equals(cachedChunk)) {
            return;
        }
        
        playerChunkCache.put(player.getUniqueId(), toChunkKey);
        
        Claim claim = claimManager.getClaimAt(toChunk);
        
        if (claim != null) {
            Town town = societiesManager.getTown(claim.getTownName());
            
            if (town != null) {
                PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
                
                if (claim.getTownName().equals(playerData.getTown())) {
                    player.sendActionBar("§a⚑ §6" + town.getName() + " §7- §aYour Territory");
                } else {
                    player.sendActionBar("§e⚑ §6" + town.getName() + " §7- §eForeign Territory");
                }
                
                if (player.hasPermission("nations.claim.verbose")) {
                    player.sendMessage("§7[§6" + town.getName() + "§7] " + 
                        (claim.getTownName().equals(playerData.getTown()) ? "§aYour territory" : "§eForeign territory"));
                }
            }
        } else {
            player.sendActionBar("§8⚐ §7Wilderness §8- §7Unclaimed Land");
        }
    }
}
