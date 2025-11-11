package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.Claim;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {
    private static ClaimManager instance;
    private final Nations plugin;
    private final SocietiesManager societiesManager;

    private final Map<String, Claim> claims = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> townClaims = new ConcurrentHashMap<>();

    private ClaimManager(Nations plugin) {
        this.plugin = plugin;
        this.societiesManager = plugin.getSocietiesManager();
    }

    public static ClaimManager getInstance(Nations plugin) {
        if (instance == null) {
            instance = new ClaimManager(plugin);
        }
        return instance;
    }

    public static ClaimManager getInstance() {
        return instance;
    }

    public ClaimResult claimChunk(Chunk chunk, Town town) {
        String chunkKey = getChunkKey(chunk);

        if (claims.containsKey(chunkKey)) {
            Claim existingClaim = claims.get(chunkKey);
            if (existingClaim.getTownName().equals(town.getName())) {
                return new ClaimResult(false, "This chunk is already claimed by your town!");
            } else {
                return new ClaimResult(false, "This chunk is already claimed by " + existingClaim.getTownName() + "!");
            }
        }

        int claimCost = calculateClaimCost(town);
        if (town.getBalance() < claimCost) {
            return new ClaimResult(false, "Not enough money! Need " + claimCost + " coins. Town has " + town.getBalance() + " coins.");
        }

        if (!townClaims.getOrDefault(town.getName(), new HashSet<>()).isEmpty()) {
            if (!isAdjacentToTownClaim(chunk, town.getName())) {
                return new ClaimResult(false, "Claims must be adjacent to existing town territory!");
            }
        }

        int maxClaims = getMaxClaims(town);
        int currentClaims = townClaims.getOrDefault(town.getName(), new HashSet<>()).size();
        if (currentClaims >= maxClaims) {
            return new ClaimResult(false, "Maximum claim limit reached! (" + currentClaims + "/" + maxClaims + ")");
        }

        Claim claim = new Claim(chunkKey, town.getName(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        claims.put(chunkKey, claim);

        townClaims.computeIfAbsent(town.getName(), k -> new HashSet<>()).add(chunkKey);
        town.getClaims().add(chunkKey);

        town.removeMoney(claimCost);

        return new ClaimResult(true, "Chunk claimed successfully! Cost: " + claimCost + " coins");
    }

    public ClaimResult unclaimChunk(Chunk chunk, Town town) {
        String chunkKey = getChunkKey(chunk);

        Claim claim = claims.get(chunkKey);

        if (claim == null) {
            return new ClaimResult(false, "This chunk is not claimed!");
        }

        if (!claim.getTownName().equals(town.getName())) {
            return new ClaimResult(false, "This chunk is not claimed by your town!");
        }

        if (townClaims.get(town.getName()).size() <= 1) {
            return new ClaimResult(false, "Cannot unclaim your last chunk! Towns need at least 1 claim.");
        }

        claims.remove(chunkKey);
        townClaims.get(town.getName()).remove(chunkKey);
        town.getClaims().remove(chunkKey);

        int refund = calculateClaimCost(town) / 2;
        town.addMoney(refund);

        return new ClaimResult(true, "Chunk unclaimed! Refund: " + refund + " coins");
    }

    public Claim getClaimAt(Location location) {
        return getClaimAt(location.getChunk());
    }

    public Claim getClaimAt(Chunk chunk) {
        String chunkKey = getChunkKey(chunk);
        return claims.get(chunkKey);
    }

    public Set<Claim> getTownClaims(String townName) {
        Set<Claim> result = new HashSet<>();
        Set<String> chunkKeys = townClaims.get(townName);

        if (chunkKeys != null) {
            for (String key : chunkKeys) {
                Claim claim = claims.get(key);
                if (claim != null) {
                    result.add(claim);
                }
            }
        }

        return result;
    }

    public boolean isClaimed(Chunk chunk) {
        return claims.containsKey(getChunkKey(chunk));
    }

    public boolean isClaimedBy(Chunk chunk, String townName) {
        Claim claim = getClaimAt(chunk);
        return claim != null && claim.getTownName().equals(townName);
    }

    private int calculateClaimCost(Town town) {
        int baseCost = plugin.getConfigurationManager()
                .getSettlementsConfig()
                .getInt("settlements.towns.claim-cost", 1000);

        int numClaims = townClaims.getOrDefault(town.getName(), new HashSet<>()).size();

        return baseCost + (int) (baseCost * numClaims * 0.1);
    }

    private int getMaxClaims(Town town) {
        int baseMax = plugin.getConfigurationManager()
                .getSettlementsConfig()
                .getInt("settlements.towns.max-claims", 100);

        return baseMax + (town.getProgressionLevel() * 10);
    }

    private boolean isAdjacentToTownClaim(Chunk chunk, String townName) {
        Set<String> existingClaims = townClaims.get(townName);
        if (existingClaims == null || existingClaims.isEmpty()) {
            return true;
        }

        World world = chunk.getWorld();
        int x = chunk.getX();
        int z = chunk.getZ();

        String[] adjacentKeys = {
                getChunkKey(world.getName(), x + 1, z),
                getChunkKey(world.getName(), x - 1, z),
                getChunkKey(world.getName(), x, z + 1),
                getChunkKey(world.getName(), x, z - 1)
        };

        for (String key : adjacentKeys) {
            if (existingClaims.contains(key)) {
                return true;
            }
        }

        return false;
    }

    public ClaimResult autoClaimRadius(Chunk centerChunk, Town town, int radius) {
        if (radius < 1 || radius > 5) {
            return new ClaimResult(false, "Radius must be between 1 and 5!");
        }

        List<Chunk> chunksToCheck = new ArrayList<>();
        World world = centerChunk.getWorld();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                chunksToCheck.add(world.getChunkAt(x, z));
            }
        }

        int successCount = 0;
        int failCount = 0;
        int totalCost = 0;

        for (Chunk chunk : chunksToCheck) {
            ClaimResult result = claimChunk(chunk, town);
            if (result.isSuccess()) {
                successCount++;
                totalCost += calculateClaimCost(town);
            } else {
                failCount++;
            }
        }

        if (successCount == 0) {
            return new ClaimResult(false, "Failed to claim any chunks! All chunks may already be claimed.");
        }

        return new ClaimResult(true,
                "Claimed " + successCount + " chunks! " +
                        (failCount > 0 ? failCount + " chunks failed. " : "") +
                        "Total cost: " + totalCost + " coins");
    }

    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
    }

    public String getChunkKey(String worldName, int x, int z) {
        return worldName + "," + x + "," + z;
    }

    public void removeAllTownClaims(String townName) {
        Set<String> chunkKeys = townClaims.get(townName);
        if (chunkKeys != null) {
            for (String key : chunkKeys) {
                claims.remove(key);
            }
            townClaims.remove(townName);
        }
    }

    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_claims", claims.size());
        stats.put("total_towns_with_claims", townClaims.size());
        return stats;
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
