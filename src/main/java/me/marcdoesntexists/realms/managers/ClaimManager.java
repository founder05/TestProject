package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.Claim;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {
    private static ClaimManager instance;
    private final Realms plugin;
    private final SocietiesManager societiesManager;

    private final Map<String, Claim> claims = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> townClaims = new ConcurrentHashMap<>();

    private ClaimManager(Realms plugin) {
        this.plugin = plugin;
        this.societiesManager = plugin.getSocietiesManager();
    }

    public static ClaimManager getInstance(Realms plugin) {
        if (instance == null) {
            instance = new ClaimManager(plugin);
        }
        return instance;
    }

    public static ClaimManager getInstance() {
        return instance;
    }

    public ClaimResult claimChunk(Chunk chunk, Town town) {
        return claimChunk(chunk, town, false);
    }

    public ClaimResult claimChunk(Chunk chunk, Town town, boolean paymentHandled) {
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
        if (!paymentHandled && town.getBalance() < claimCost) {
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

        if (!paymentHandled) {
            town.removeMoney(claimCost);
        }

        // Notify GUI refresh so players viewing the towns category see updated claim counts
        try {
            me.marcdoesntexists.realms.gui.RealmsGUI.refreshGUIsForCategory("TOWNS");
        } catch (Throwable ignored) {
        }

        // Persist town immediately
        try {
            plugin.getDataManager().saveTown(town);
        } catch (Throwable ignored) {
        }

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

        // Refresh GUI so town claim counts are updated
        try {
            me.marcdoesntexists.realms.gui.RealmsGUI.refreshGUIsForCategory("TOWNS");
        } catch (Throwable ignored) {
        }

        // Persist town immediately
        try {
            plugin.getDataManager().saveTown(town);
        } catch (Throwable ignored) {
        }

        return new ClaimResult(true, "Chunk unclaimed! Refund: " + refund + " coins");
    }

    public Claim getClaimAt(Location location) {
        return getClaimAt(location.getChunk());
    }

    public Claim getClaimAt(Chunk chunk) {
        String chunkKey = getChunkKey(chunk);
        return claims.get(chunkKey);
    }

    // New helper to get the town name at a specific location (used by MoveListener)
    public String getTownAtLocation(Location location) {
        if (location == null) return null;
        Claim claim = getClaimAt(location);
        return claim != null ? claim.getTownName() : null;
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

    int calculateClaimCost(Town town) {
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

    /**
     * Register claims stored inside a Town object into the ClaimManager internal maps.
     * This is used after loading towns from disk so the runtime claim index is populated.
     */
    public void registerTownClaims(Town town) {
        if (town == null) return;
        Set<String> claimKeys = town.getClaims();
        if (claimKeys == null || claimKeys.isEmpty()) return;

        for (String key : claimKeys) {
            if (key == null || key.isEmpty()) continue;
            if (claims.containsKey(key)) continue;
            try {
                String[] parts = key.split(",");
                if (parts.length < 3) continue;
                String world = parts[0];
                int cx = Integer.parseInt(parts[1]);
                int cz = Integer.parseInt(parts[2]);
                Claim claim = new Claim(key, town.getName(), world, cx, cz);
                // Load persisted metadata (owner + permissions) if exists
                loadClaimMeta(claim);
                claims.put(key, claim);
                townClaims.computeIfAbsent(town.getName(), k -> new HashSet<>()).add(key);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Save claim metadata (owner and permissions) to disk under dataFolder/claims/<chunkKey>.yml
     */
    public void saveClaimMeta(Claim claim) {
        if (claim == null) return;
        try {
            File dir = new File(plugin.getDataFolder(), "claims");
            if (!dir.exists()) dir.mkdirs();
            String safeName = claim.getChunkKey().replace(',', '_').replace(':', '_');
            File f = new File(dir, safeName + ".yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            cfg.set("owner", claim.getOwnerUuid());
            // permissions: map playerUuid -> perm name
            java.util.Map<String, String> perms = new java.util.HashMap<>();
            for (var e : claimPermissionsMap(claim).entrySet()) {
                perms.put(e.getKey(), e.getValue().name());
            }
            cfg.set("permissions", perms);
            // allowed: list of player UUIDs that owner allowed to grief
            java.util.List<String> allowed = new java.util.ArrayList<>(claim.getAllowedToGriefSet());
            cfg.set("allowed", allowed);
            try {
                cfg.save(f);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save claim meta for " + claim.getChunkKey() + ": " + e.getMessage());
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("saveClaimMeta error: " + t.getMessage());
        }
    }

    // helper to extract the permissions map from Claim (reflection-free)
    private java.util.Map<String, Claim.ClaimPermission> claimPermissionsMap(Claim claim) {
        try {
            return new java.util.HashMap<>(claim.getAllPermissions());
        } catch (Throwable t) {
            return new java.util.HashMap<>();
        }
    }

    /**
     * Load claim metadata from disk into the given claim if file exists.
     */
    public void loadClaimMeta(Claim claim) {
        if (claim == null) return;
        try {
            File dir = new File(plugin.getDataFolder(), "claims");
            if (!dir.exists()) return;
            String safeName = claim.getChunkKey().replace(',', '_').replace(':', '_');
            File f = new File(dir, safeName + ".yml");
            if (!f.exists()) return;
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String owner = cfg.getString("owner", null);
            if (owner != null) claim.setOwnerUuid(owner);
            var perms = cfg.getConfigurationSection("permissions");
            if (perms != null) {
                for (String key : perms.getKeys(false)) {
                    String val = perms.getString(key, "NONE");
                    try {
                        Claim.ClaimPermission cp = Claim.ClaimPermission.valueOf(val);
                        try {
                            java.util.UUID uid = java.util.UUID.fromString(key);
                            claim.grantPermission(uid, cp);
                        } catch (IllegalArgumentException ignored) {}
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            // load allowed list
            java.util.List<String> allowed = cfg.getStringList("allowed");
            if (allowed != null) {
                for (String s : allowed) {
                    try {
                        java.util.UUID uid = java.util.UUID.fromString(s);
                        claim.allowGrief(uid);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("loadClaimMeta error for " + claim.getChunkKey() + ": " + t.getMessage());
        }
    }

    public ClaimResult claimChunkByPlayer(Chunk chunk, Town town, java.util.UUID buyerId) {
        String chunkKey = getChunkKey(chunk);

        Claim existing = claims.get(chunkKey);
        int claimCost = calculateClaimCost(town);

        try {
            me.marcdoesntexists.realms.managers.DataManager dm = me.marcdoesntexists.realms.managers.DataManager.getInstance();
            if (dm == null) return new ClaimResult(false, "Internal error: data manager not available");
            var pd = dm.getPlayerData(buyerId);
            if (pd == null) return new ClaimResult(false, "Player data not found");

            // Check social class / noble tier restrictions
            me.marcdoesntexists.realms.societies.NobleTier tier = pd.getNobleTier();
            if (tier == null) tier = me.marcdoesntexists.realms.societies.NobleTier.COMMONER;

            // Commoners cannot own land (personal subclaims)
            if (!tier.canOwnLand()) {
                return new ClaimResult(false, "You must be at least a KNIGHT to own personal land! Current rank: " + tier.name() + ". Use /noble upgrade to advance.");
            }

            // Count how many chunks this player already owns in this town
            long ownedChunks = townClaims.getOrDefault(town.getName(), new HashSet<>())
                    .stream()
                    .map(claims::get)
                    .filter(c -> c != null && c.getOwnerUuid() != null && c.getOwnerUuid().equals(buyerId.toString()))
                    .count();

            int maxPersonalClaims = tier.getLandClaimLimit();
            if (ownedChunks >= maxPersonalClaims) {
                return new ClaimResult(false, "Personal claim limit reached! You own " + ownedChunks + "/" + maxPersonalClaims + " chunks (rank: " + tier.name() + "). Upgrade your noble tier to own more land.");
            }

            me.marcdoesntexists.realms.economy.EconomyService econ = me.marcdoesntexists.realms.economy.EconomyService.getInstance();

            // If the chunk is already claimed
            if (existing != null) {
                // Claimed by same town
                if (existing.getTownName().equals(town.getName())) {
                    // If it's already owned by someone (ownerUuid != null)
                    if (existing.getOwnerUuid() != null) {
                        return new ClaimResult(false, "This chunk is already claimed by your town!");
                    }
                    // It's a town-owned chunk (no specific owner) -> allow player to buy and become owner
                    // Try external economy first
                    boolean paid = false;
                    if (econ != null) {
                        try {
                            paid = econ.withdrawFromPlayer(buyerId, claimCost);
                        } catch (Throwable ignored) { paid = false; }
                    }
                    if (!paid) {
                        if (pd.getMoney() < claimCost) {
                            return new ClaimResult(false, "Not enough money! Need " + claimCost + " coins. You have " + pd.getMoney() + " coins.");
                        }
                        paid = pd.removeMoney(claimCost);
                    }
                    if (!paid) return new ClaimResult(false, "Failed to deduct money from player account.");

                    // Credit town mayor or town treasury (respect external econ)
                    java.util.UUID mayorId = town.getMayor();
                    boolean creditedToMayor = false;
                    if (mayorId != null) {
                        try {
                            if (econ != null) {
                                // deposit to mayor via economy
                                try { econ.depositToPlayer(mayorId, claimCost); creditedToMayor = true; } catch (Throwable ignored) { creditedToMayor = false; }
                            }
                            if (!creditedToMayor) {
                                var mayorPd = dm.getPlayerData(mayorId);
                                if (mayorPd != null) {
                                    mayorPd.addMoney(claimCost);
                                    creditedToMayor = true;
                                    try { dm.savePlayerData(mayorId); } catch (Throwable ignored) {}
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (!creditedToMayor) {
                        town.addMoney(claimCost);
                    }

                    // set owner on existing claim
                    existing.setOwnerUuid(buyerId.toString());
                    try { saveClaimMeta(existing); } catch (Throwable ignored) {}
                    try { dm.savePlayerData(buyerId); } catch (Throwable ignored) {}
                    try { plugin.getDataManager().saveTown(town); } catch (Throwable ignored) {}

                    // Fire event
                    try {
                        org.bukkit.Bukkit.getPluginManager().callEvent(new me.marcdoesntexists.realms.events.ClaimPurchasedEvent(buyerId, existing, claimCost));
                    } catch (Throwable ignored) {}

                    return new ClaimResult(true, "Chunk purchased from town and now owned by player! Cost: " + claimCost + " coins", existing, claimCost);
                } else {
                    return new ClaimResult(false, "This chunk is already claimed by " + existing.getTownName() + "!");
                }
            }

            // Not already claimed -> proceed with new claim creation paid by player
            // Try external economy first
            boolean paid = false;
            if (econ != null) {
                try { paid = econ.withdrawFromPlayer(buyerId, claimCost); } catch (Throwable ignored) { paid = false; }
            }
            if (!paid) {
                if (pd.getMoney() < claimCost) {
                    int have = pd.getMoney();
                    return new ClaimResult(false, "Not enough money! Need " + claimCost + " coins. You have " + have + " coins.");
                }
                paid = pd.removeMoney(claimCost);
            }

            if (!paid) return new ClaimResult(false, "Failed to deduct money from player account.");

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

            // Credit mayor or town (respect external econ)
            java.util.UUID mayorId = town.getMayor();
            boolean creditedToMayor = false;
            if (mayorId != null) {
                try {
                    if (econ != null) {
                        try { econ.depositToPlayer(mayorId, claimCost); creditedToMayor = true; } catch (Throwable ignored) { creditedToMayor = false; }
                    }
                    if (!creditedToMayor) {
                        var mayorPd = dm.getPlayerData(mayorId);
                        if (mayorPd != null) {
                            mayorPd.addMoney(claimCost);
                            creditedToMayor = true;
                            try { dm.savePlayerData(mayorId); } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (!creditedToMayor) {
                town.addMoney(claimCost);
            }

            // Create claim with owner set to the buyer
            Claim claim = new Claim(chunkKey, town.getName(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), buyerId.toString());
            claims.put(chunkKey, claim);

            townClaims.computeIfAbsent(town.getName(), k -> new HashSet<>()).add(chunkKey);
            town.getClaims().add(chunkKey);

            // Persist player data if DataManager supports save
            try {
                dm.savePlayerData(buyerId);
            } catch (Throwable ignored) {}

            // Persist claim metadata (owner)
            saveClaimMeta(claim);

            // Persist town immediately (to save claim list)
            try { plugin.getDataManager().saveTown(town); } catch (Throwable ignored) {}

            // Fire event
            try {
                org.bukkit.Bukkit.getPluginManager().callEvent(new me.marcdoesntexists.realms.events.ClaimPurchasedEvent(buyerId, claim, claimCost));
            } catch (Throwable ignored) {}

            return new ClaimResult(true, "Chunk claimed successfully! Cost: " + claimCost + " coins (paid by player)", claim, claimCost);

        } catch (Throwable t) {
            plugin.getLogger().warning("claimChunkByPlayer failed: " + t.getMessage());
            return new ClaimResult(false, "Failed to claim chunk due to internal error: " + t.getMessage());
        }
    }

    public static class ClaimResult {
        private final boolean success;
        private final String message;
        private final Claim claim; // the created or modified claim (nullable)
        private final int cost; // cost paid by player

        public ClaimResult(boolean success, String message) {
            this(success, message, null, 0);
        }

        public ClaimResult(boolean success, String message, Claim claim, int cost) {
            this.success = success;
            this.message = message;
            this.claim = claim;
            this.cost = cost;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Claim getClaim() { return claim; }

        public int getCost() { return cost; }
    }
}
