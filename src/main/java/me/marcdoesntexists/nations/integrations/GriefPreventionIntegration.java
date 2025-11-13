package me.marcdoesntexists.nations.integrations;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.societies.Town;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

import me.marcdoesntexists.nations.economy.EconomyService;

public class GriefPreventionIntegration {
    private static GriefPreventionIntegration instance;
    private final Nations plugin;
    private final GriefPrevention griefPrevention;

    private final Map<String, Set<Long>> townClaims = new HashMap<>();

    private final Map<Long, String> claimToTown = new HashMap<>();

    private boolean enabled = false;

    private GriefPreventionIntegration(Nations plugin) {
        this.plugin = plugin;

        Plugin griefPreventionPlugin = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefPreventionPlugin instanceof GriefPrevention gp) {
            this.griefPrevention = gp;
            this.enabled = true;
            plugin.getLogger().info("✓ GriefPrevention integration enabled!");
            loadMappings();
        } else {
            this.griefPrevention = null;
            this.enabled = false;
            plugin.getLogger().warning("⚠ GriefPrevention not found! Using internal claim system.");
        }
    }

    public static GriefPreventionIntegration getInstance(Nations plugin) {
        if (instance == null) {
            instance = new GriefPreventionIntegration(plugin);
        }
        return instance;
    }

    public static GriefPreventionIntegration getInstance() {
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ClaimResult createTownClaim(Player player, Town town, Chunk chunk) {
        if (!enabled) {
            return new ClaimResult(false, "GriefPrevention not available!");
        }

        Location corner1 = chunk.getBlock(0, 0, 0).getLocation();
        Location corner2 = chunk.getBlock(15, 255, 15).getLocation();

        Claim existingClaim = griefPrevention.dataStore.getClaimAt(corner1, false, null);
        if (existingClaim != null) {
            String owner = existingClaim.getOwnerName();
            return new ClaimResult(false, "This area is already claimed by " + owner + "!");
        }

        int cost = calculateClaimCost(town);
        if (town.getBalance() < cost) {
            return new ClaimResult(false, "Not enough money! Need " + cost + " coins.");
        }

        try {
            CreateClaimResult createResult = griefPrevention.dataStore.createClaim(
                    corner1.getWorld(),
                    corner1.getBlockX(),
                    corner2.getBlockX(),
                    corner1.getBlockY(),
                    corner2.getBlockY(),
                    corner1.getBlockZ(),
                    corner2.getBlockZ(),
                    player.getUniqueId(),
                    null,
                    null,
                    player
            );

            if (!createResult.succeeded || createResult.claim == null) {
                return new ClaimResult(false, "Failed to create claim!");
            }

            Claim newClaim = createResult.claim;

            // Try to charge the player first via external economy
            EconomyService econ = EconomyService.getInstance();
            boolean chargedFromPlayer = false;
            if (econ != null) {
                chargedFromPlayer = econ.withdrawFromPlayer(player.getUniqueId(), cost);
            }

            if (!chargedFromPlayer) {
                // fallback to town treasury
                if (town.getBalance() >= cost) {
                    town.removeMoney(cost);
                } else {
                    // rollback GP claim creation
                    griefPrevention.dataStore.deleteClaim(newClaim, true);
                    return new ClaimResult(false, "Not enough money to pay for the claim (player/town).");
                }
            }

            // record mapping and also update town internal claim list so GUI and other systems see it
            townClaims.computeIfAbsent(town.getName(), k -> new HashSet<>())
                    .add(newClaim.getID());
            claimToTown.put(newClaim.getID(), town.getName());

            // add the internal chunk key to the town's claim set so GUI counts match
            String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
            town.getClaims().add(chunkKey);

            for (UUID memberId : town.getMembers()) {
                newClaim.setPermission(memberId.toString(), ClaimPermission.Build);
                newClaim.setPermission(memberId.toString(), ClaimPermission.Access);
            }

            griefPrevention.dataStore.saveClaim(newClaim);

            // Refresh GUI so players viewing towns see updated counts
            try { me.marcdoesntexists.nations.gui.NationsGUI.refreshGUIsForCategory("TOWNS"); } catch (Throwable ignored) {}

            plugin.getLogger().info("Town " + town.getName() + " claimed chunk at " +
                    chunk.getX() + "," + chunk.getZ() + " (GP Claim ID: " + newClaim.getID() + ")");

            // Persist town immediately
            try { plugin.getDataManager().saveTown(town); } catch (Throwable ignored) {}

            return new ClaimResult(true, "Chunk claimed successfully! (GP Claim #" + newClaim.getID() + ")");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create GP claim: " + e.getMessage());
            e.printStackTrace();
            return new ClaimResult(false, "Error creating claim: " + e.getMessage());
        }
    }

    public ClaimResult removeTownClaim(Town town, Chunk chunk) {
        if (!enabled) {
            return new ClaimResult(false, "GriefPrevention not available!");
        }

        Location loc = chunk.getBlock(8, 64, 8).getLocation();
        Claim claim = griefPrevention.dataStore.getClaimAt(loc, false, null);

        if (claim == null) {
            return new ClaimResult(false, "This chunk is not claimed!");
        }

        String townName = claimToTown.get(claim.getID());
        if (!town.getName().equals(townName)) {
            return new ClaimResult(false, "This claim doesn't belong to your town!");
        }

        Set<Long> claims = townClaims.get(town.getName());
        if (claims.size() <= 1) {
            return new ClaimResult(false, "Cannot remove your last claim!");
        }

        try {
            griefPrevention.dataStore.deleteClaim(claim, true);

            claims.remove(claim.getID());
            claimToTown.remove(claim.getID());

            // remove internal chunk key from town's claim set
            String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
            town.getClaims().remove(chunkKey);

            int refund = calculateClaimCost(town) / 2;
            town.addMoney(refund);

            // Refresh GUI so town claim counts are updated
            try { me.marcdoesntexists.nations.gui.NationsGUI.refreshGUIsForCategory("TOWNS"); } catch (Throwable ignored) {}

            plugin.getLogger().info("Town " + town.getName() + " unclaimed GP Claim #" + claim.getID());

            // Persist town immediately
            try { plugin.getDataManager().saveTown(town); } catch (Throwable ignored) {}

            return new ClaimResult(true, "Claim removed! Refund: " + refund + " coins");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove GP claim: " + e.getMessage());
            e.printStackTrace();
            return new ClaimResult(false, "Error removing claim: " + e.getMessage());
        }
    }

    public void addMemberToClaims(Town town, UUID memberId) {
        if (!enabled) return;

        Set<Long> claimIds = townClaims.get(town.getName());
        if (claimIds == null) return;

        for (Long claimId : claimIds) {
            Claim claim = griefPrevention.dataStore.getClaim(claimId);
            if (claim != null) {
                claim.setPermission(memberId.toString(), ClaimPermission.Build);
                claim.setPermission(memberId.toString(), ClaimPermission.Access);
                griefPrevention.dataStore.saveClaim(claim);
            }
        }
    }

    public void removeMemberFromClaims(Town town, UUID memberId) {
        if (!enabled) return;

        Set<Long> claimIds = townClaims.get(town.getName());
        if (claimIds == null) return;

        for (Long claimId : claimIds) {
            Claim claim = griefPrevention.dataStore.getClaim(claimId);
            if (claim != null) {
                claim.dropPermission(memberId.toString());
                griefPrevention.dataStore.saveClaim(claim);
            }
        }
    }

    public int getTownClaimCount(String townName) {
        if (!enabled) return 0;

        Set<Long> claims = townClaims.get(townName);
        return claims != null ? claims.size() : 0;
    }

    public String getTownAtLocation(Location location) {
        if (!enabled) return null;

        Claim claim = griefPrevention.dataStore.getClaimAt(location, false, null);
        if (claim == null) return null;

        return claimToTown.get(claim.getID());
    }

    public boolean isChunkClaimed(Chunk chunk) {
        if (!enabled) return false;

        Location loc = chunk.getBlock(8, 64, 8).getLocation();
        Claim claim = griefPrevention.dataStore.getClaimAt(loc, false, null);

        return claim != null && claimToTown.containsKey(claim.getID());
    }

    public void deleteTownClaims(String townName) {
        if (!enabled) return;

        Set<Long> claimIds = townClaims.remove(townName);
        if (claimIds == null) return;

        for (Long claimId : claimIds) {
            Claim claim = griefPrevention.dataStore.getClaim(claimId);
            if (claim != null) {
                griefPrevention.dataStore.deleteClaim(claim, true);
            }
            claimToTown.remove(claimId);
        }

        plugin.getLogger().info("Deleted all claims for town: " + townName);
    }

    private int calculateClaimCost(Town town) {
        int baseCost = plugin.getConfigurationManager()
                .getSettlementsConfig()
                .getInt("settlements.towns.claim-cost", 1000);

        int numClaims = getTownClaimCount(town.getName());
        return baseCost + (int) (baseCost * numClaims * 0.1);
    }

    public void saveMappings() {
        if (!enabled) return;
        try {
            File file = new File(plugin.getDataFolder(), "gp-mappings.yml");
            FileConfiguration cfg = new YamlConfiguration();

            // save townClaims
            for (Map.Entry<String, Set<Long>> entry : townClaims.entrySet()) {
                cfg.set("townClaims." + entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            // save claimToTown
            for (Map.Entry<Long, String> entry : claimToTown.entrySet()) {
                cfg.set("claimToTown." + entry.getKey(), entry.getValue());
            }

            cfg.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save GP mappings: " + e.getMessage());
        }
    }

    public void loadMappings() {
        if (!enabled) return;
        try {
            File file = new File(plugin.getDataFolder(), "gp-mappings.yml");
            if (!file.exists()) return;

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            if (cfg.isConfigurationSection("townClaims")) {
                for (String townName : cfg.getConfigurationSection("townClaims").getKeys(false)) {
                    List<Long> ids = cfg.getLongList("townClaims." + townName);
                    Set<Long> set = new HashSet<>(ids);
                    townClaims.put(townName, set);
                }
            }

            if (cfg.isConfigurationSection("claimToTown")) {
                for (String key : cfg.getConfigurationSection("claimToTown").getKeys(false)) {
                    long id = Long.parseLong(key);
                    String town = cfg.getString("claimToTown." + key);
                    if (town != null) claimToTown.put(id, town);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load GP mappings: " + e.getMessage());
        }
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
