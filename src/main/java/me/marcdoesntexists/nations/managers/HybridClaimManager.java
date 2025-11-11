package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.integrations.GriefPreventionIntegration;
import me.marcdoesntexists.nations.societies.Town;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HybridClaimManager {
    private static HybridClaimManager instance;
    private final Nations plugin;
    private final GriefPreventionIntegration gpIntegration;
    private final ClaimManager internalManager;
    
    private HybridClaimManager(Nations plugin) {
        this.plugin = plugin;
        this.gpIntegration = GriefPreventionIntegration.getInstance(plugin);
        this.internalManager = ClaimManager.getInstance(plugin);
    }
    
    public static HybridClaimManager getInstance(Nations plugin) {
        if (instance == null) {
            instance = new HybridClaimManager(plugin);
        }
        return instance;
    }
    
    public static HybridClaimManager getInstance() {
        return instance;
    }
    
    public ClaimResult claimChunk(Player player, Town town, Chunk chunk) {
        if (gpIntegration.isEnabled()) {
            GriefPreventionIntegration.ClaimResult result = 
                gpIntegration.createTownClaim(player, town, chunk);
            
            return new ClaimResult(result.isSuccess(), result.getMessage());
        } else {
            ClaimManager.ClaimResult result = 
                internalManager.claimChunk(chunk, town);
            
            return new ClaimResult(result.isSuccess(), result.getMessage());
        }
    }
    
    public ClaimResult unclaimChunk(Town town, Chunk chunk) {
        if (gpIntegration.isEnabled()) {
            GriefPreventionIntegration.ClaimResult result = 
                gpIntegration.removeTownClaim(town, chunk);
            
            return new ClaimResult(result.isSuccess(), result.getMessage());
        } else {
            ClaimManager.ClaimResult result = 
                internalManager.unclaimChunk(chunk, town);
            
            return new ClaimResult(result.isSuccess(), result.getMessage());
        }
    }
    
    public String getTownAtLocation(Location location) {
        if (gpIntegration.isEnabled()) {
            return gpIntegration.getTownAtLocation(location);
        } else {
            me.marcdoesntexists.nations.utils.Claim claim = internalManager.getClaimAt(location);
            return claim != null ? claim.getTownName() : null;
        }
    }
    
    public boolean isChunkClaimed(Chunk chunk) {
        if (gpIntegration.isEnabled()) {
            return gpIntegration.isChunkClaimed(chunk);
        } else {
            return internalManager.isClaimed(chunk);
        }
    }
    
    public int getTownClaimCount(String townName) {
        if (gpIntegration.isEnabled()) {
            return gpIntegration.getTownClaimCount(townName);
        } else {
            return internalManager.getTownClaims(townName).size();
        }
    }
    
    public void addMemberToClaims(Town town, UUID memberId) {
        if (gpIntegration.isEnabled()) {
            gpIntegration.addMemberToClaims(town, memberId);
        }
    }
    
    public void removeMemberFromClaims(Town town, UUID memberId) {
        if (gpIntegration.isEnabled()) {
            gpIntegration.removeMemberFromClaims(town, memberId);
        }
    }
    
    public void deleteTownClaims(String townName) {
        if (gpIntegration.isEnabled()) {
            gpIntegration.deleteTownClaims(townName);
        } else {
            internalManager.removeAllTownClaims(townName);
        }
    }
    
    public boolean isUsingGriefPrevention() {
        return gpIntegration.isEnabled();
    }
    
    public ClaimManager getInternalManager() {
        return internalManager;
    }
    
    public static class ClaimResult {
        private final boolean success;
        private final String message;
        
        public ClaimResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
