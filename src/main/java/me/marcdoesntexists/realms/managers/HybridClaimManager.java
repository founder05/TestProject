package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.economy.EconomyService;
import me.marcdoesntexists.realms.societies.Town;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HybridClaimManager {
    private static HybridClaimManager instance;
    private final Realms plugin;
    private final ClaimManager internalManager;

    private HybridClaimManager(Realms plugin) {
        this.plugin = plugin;
        this.internalManager = ClaimManager.getInstance(plugin);
    }

    public static HybridClaimManager getInstance(Realms plugin) {
        if (instance == null) {
            instance = new HybridClaimManager(plugin);
        }
        return instance;
    }

    public static HybridClaimManager getInstance() {
        return instance;
    }

    public ClaimResult claimChunk(Player player, Town town, Chunk chunk) {
        // Try to charge the player first via external economy, fallback to town treasury
        EconomyService econ = EconomyService.getInstance();
        // compute cost locally to avoid access issues
        int baseCost = plugin.getConfigurationManager().getSettlementsConfig().getInt("settlements.towns.claim-cost", 1000);
        int numClaims = this.getTownClaimCount(town.getName());
        int cost = baseCost + (int) (baseCost * numClaims * 0.1);

        boolean charged = false;
        if (econ != null) {
            charged = econ.withdrawFromPlayer(player.getUniqueId(), cost);
        }
        if (!charged) {
            if (town.getBalance() < cost) {
                return new ClaimResult(false, "Not enough money! Need " + cost + " coins. Town has " + town.getBalance() + " coins.");
            }
            town.removeMoney(cost);
            // Persist town immediately when using town funds
            try {
                plugin.getDataManager().saveTown(town);
            } catch (Throwable ignored) {
            }
        }

        ClaimManager.ClaimResult result =
                internalManager.claimChunk(chunk, town);

        return new ClaimResult(result.isSuccess(), result.getMessage());
    }

    public ClaimResult unclaimChunk(Town town, Chunk chunk) {
        ClaimManager.ClaimResult result =
                internalManager.unclaimChunk(chunk, town);

        return new ClaimResult(result.isSuccess(), result.getMessage());
    }

    public String getTownAtLocation(Location location) {
        me.marcdoesntexists.realms.utils.Claim claim = internalManager.getClaimAt(location);
        return claim != null ? claim.getTownName() : null;
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return internalManager.isClaimed(chunk);
    }

    public int getTownClaimCount(String townName) {
        return internalManager.getTownClaims(townName).size();
    }

    public void addMemberToClaims(Town town, UUID memberId) {
        // GriefPrevention-specific permissions removed. No-op for internal claim system.
    }

    public void removeMemberFromClaims(Town town, UUID memberId) {
        // GriefPrevention-specific permissions removed. No-op for internal claim system.
    }

    public void deleteTownClaims(String townName) {
        internalManager.removeAllTownClaims(townName);
    }

    public boolean isUsingGriefPrevention() {
        return false;
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
