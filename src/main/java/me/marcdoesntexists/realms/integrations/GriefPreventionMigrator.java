package me.marcdoesntexists.realms.integrations;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.managers.ConfigurationManager;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Empire;
import me.marcdoesntexists.realms.societies.Kingdom;
import me.marcdoesntexists.realms.societies.Town;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility to migrate claims from GriefPrevention to Realms internal ClaimManager.
 * Supports three modes:
 * - If GriefPrevention plugin is present, use reflection to resolve claim objects.
 * - If gp-mappings.yml exists, use it as mapping from claimId -> townName.
 * - If ClaimData files exist (plugins/GriefPreventionData/ClaimData), parse them for corners.
 *
 * Also supports automatic creation of Town/Kingdom/Empire depending on claim size using
 * thresholds from settlements.yml.
 */
public class GriefPreventionMigrator {
    private final Realms plugin;

    public GriefPreventionMigrator(Realms plugin) {
        this.plugin = plugin;
    }

    private String createBackup() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                boolean created = dataFolder.mkdirs();
                if (!created) plugin.getLogger().warning("Could not create data folder for GP migrator: " + dataFolder.getAbsolutePath());
            }

            List<File> toZip = new ArrayList<>();
            File mappings = new File(dataFolder, "gp-mappings.yml");
            if (mappings.exists()) toZip.add(mappings);

            File townsDir = new File(dataFolder, "towns");
            if (townsDir.exists() && townsDir.isDirectory()) {
                File[] townFiles = townsDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
                if (townFiles != null) {
                    toZip.addAll(java.util.Arrays.asList(townFiles));
                }
            }

            if (toZip.isEmpty()) return null;

            String name = "gp-migration-backup-" + System.currentTimeMillis() + ".zip";
            File dest = new File(dataFolder, name);
            try (FileOutputStream fos = new FileOutputStream(dest); ZipOutputStream zos = new ZipOutputStream(fos)) {
                byte[] buffer = new byte[4096];
                for (File f : toZip) {
                    ZipEntry entry = new ZipEntry(f.getName());
                    zos.putNextEntry(entry);
                    try (FileInputStream fis = new FileInputStream(f)) {
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
            return dest.getAbsolutePath();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create GP migration backup: " + e.getMessage());
            return null;
        }
    }

    public MigrationResult migrate() {
        Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");

        File mappingsFile = new File(plugin.getDataFolder(), "gp-mappings.yml");
        FileConfiguration cfg = mappingsFile.exists() ? YamlConfiguration.loadConfiguration(mappingsFile) : null;

        // ClaimData folder used by some GP setups
        File gpClaimDataDir = new File(plugin.getServer().getWorldContainer(), "plugins/GriefPreventionData/ClaimData");
        boolean hasClaimFiles = gpClaimDataDir.exists() && gpClaimDataDir.isDirectory();

        if (gp == null && cfg == null && !hasClaimFiles) {
            return new MigrationResult(false, "No GriefPrevention plugin, no gp-mappings.yml and no ClaimData folder found.");
        }

        String backupPath = createBackup();
        if (backupPath != null) plugin.getLogger().info("Created GP migration backup: " + backupPath);
        else plugin.getLogger().info("No GP files found to backup (or backup creation skipped).");

        try {
            ClaimManager claimManager = plugin.getClaimManager();
            SocietiesManager societiesManager = plugin.getSocietiesManager();
            DataManager dataManager = plugin.getDataManager();
            ConfigurationManager configManager = ConfigurationManager.getInstance();
            FileConfiguration settlementsCfg = configManager != null ? configManager.getSettlementsConfig() : null;

            int townToKingdomMin = settlementsCfg != null ? settlementsCfg.getInt("settlement-evolution.town-to-kingdom.minimum-claims", 100) : 100;
            int kingdomToEmpireMin = settlementsCfg != null ? settlementsCfg.getInt("settlement-evolution.kingdom-to-empire.minimum-claims", 500) : 500;

            AtomicInteger migratedTowns = new AtomicInteger(0);
            AtomicInteger migratedChunks = new AtomicInteger(0);

            // Helper that processes one claim given lesser/greater corners and the target town name
            java.util.function.BiFunction<Location[], String, Boolean> processClaim = (corners, townName) -> {
                if (corners == null || corners.length < 2) return false;
                Location lesserLoc = corners[0];
                Location greaterLoc = corners[1];
                if (lesserLoc == null || greaterLoc == null) return false;

                World world = lesserLoc.getWorld();
                if (world == null) return false;

                int minX = Math.min(lesserLoc.getBlockX(), greaterLoc.getBlockX());
                int maxX = Math.max(lesserLoc.getBlockX(), greaterLoc.getBlockX());
                int minZ = Math.min(lesserLoc.getBlockZ(), greaterLoc.getBlockZ());
                int maxZ = Math.max(lesserLoc.getBlockZ(), greaterLoc.getBlockZ());

                int chunkMinX = (int) Math.floor(minX / 16.0);
                int chunkMaxX = (int) Math.floor(maxX / 16.0);
                int chunkMinZ = (int) Math.floor(minZ / 16.0);
                int chunkMaxZ = (int) Math.floor(maxZ / 16.0);

                Town town = societiesManager.getTown(townName);
                boolean townChanged = false;
                int chunksAdded = 0;
                if (town != null) {
                    for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                        for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                            String chunkKey = claimManager.getChunkKey(world.getName(), cx, cz);
                            if (!town.getClaims().contains(chunkKey)) {
                                town.getClaims().add(chunkKey);
                                chunksAdded++;
                                townChanged = true;
                            }
                        }
                    }
                } else {
                    // Auto-create town/kingdom/empire depending on size
                    int numChunks = (chunkMaxX - chunkMinX + 1) * (chunkMaxZ - chunkMinZ + 1);
                    UUID mayor = UUID.randomUUID(); // fallback
                    // townName might be null; use base
                    String baseTownName = townName != null && !townName.isBlank() ? townName : "gp_town_" + System.currentTimeMillis();
                    String createdTownName = makeUniqueTownName(societiesManager, baseTownName);
                    Town newTown = new Town(createdTownName, mayor);
                    societiesManager.registerTown(newTown);
                    town = newTown;
                    townChanged = true;
                    // add chunks
                    for (int cx = chunkMinX; cx <= chunkMaxX; cx++) for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                        String chunkKey = claimManager.getChunkKey(world.getName(), cx, cz);
                        if (!town.getClaims().contains(chunkKey)) { town.getClaims().add(chunkKey); chunksAdded++; }
                    }

                    // promote
                    if (numChunks >= kingdomToEmpireMin) {
                        String kName = makeUniqueKingdomName(societiesManager, "gp_kingdom_" + createdTownName);
                        Kingdom kingdom = new Kingdom(kName, createdTownName);
                        societiesManager.registerKingdom(kingdom);
                        try { dataManager.saveKingdom(kingdom); } catch (Throwable ignored) {}

                        String eName = makeUniqueEmpireName(societiesManager, "gp_empire_" + createdTownName);
                        Empire empire = new Empire(eName, kName);
                        societiesManager.registerEmpire(empire);
                        kingdom.setEmpire(eName);
                        try { dataManager.saveEmpire(empire); } catch (Throwable ignored) {}
                        try { dataManager.saveKingdom(kingdom); } catch (Throwable ignored) {}
                    } else if (numChunks >= townToKingdomMin) {
                        String kName = makeUniqueKingdomName(societiesManager, "gp_kingdom_" + createdTownName);
                        Kingdom kingdom = new Kingdom(kName, createdTownName);
                        societiesManager.registerKingdom(kingdom);
                        try { dataManager.saveKingdom(kingdom); } catch (Throwable ignored) {}
                    }
                }

                if (townChanged) {
                    claimManager.registerTownClaims(town);
                    try { dataManager.saveTown(town); } catch (Throwable ignored) {}
                }

                if (chunksAdded > 0) migratedChunks.addAndGet(chunksAdded);
                if (townChanged) migratedTowns.incrementAndGet();
                return townChanged;
            };

            // Note: direct integration with GriefPrevention via reflection has been removed.
            // Migration now supports two file-based modes:
            // - gp-mappings.yml in the plugin data folder (claimId -> townName)
            // - ClaimData files located in plugins/GriefPreventionData/ClaimData
            // If server still runs GriefPrevention, admins should export claim files or provide gp-mappings.yml.
            if (gp != null) {
                plugin.getLogger().info("GriefPrevention plugin detected, but automatic reflection-based import is disabled; using files/mappings only.");
            }

            // If mappings + ClaimData files are present (or GP absent), use files
            if (cfg != null && cfg.isConfigurationSection("claimToTown") && hasClaimFiles) {
                for (String key : Objects.requireNonNull(cfg.getConfigurationSection("claimToTown")).getKeys(false)) {
                    long id;
                    try { id = Long.parseLong(key); } catch (NumberFormatException nfe) { continue; }
                    String townName = cfg.getString("claimToTown." + key);
                    File claimFile = findClaimFileById(gpClaimDataDir, id);
                    if (claimFile == null) { plugin.getLogger().warning("Claim file for id " + id + " not found in ClaimData; skipping."); continue; }
                    Location[] corners = parseClaimCornersFromFile(claimFile);
                    processClaim.apply(corners, townName);
                }
            } else if (cfg != null && cfg.isConfigurationSection("townClaims") && hasClaimFiles) {
                // fallback mapping format: town -> list of claim ids
                for (String townName : Objects.requireNonNull(cfg.getConfigurationSection("townClaims")).getKeys(false)) {
                    List<Long> ids = cfg.getLongList("townClaims." + townName);
                    if (ids.isEmpty()) continue;
                    for (Long id : ids) {
                        File claimFile = findClaimFileById(gpClaimDataDir, id);
                        if (claimFile == null) continue;
                        Location[] corners = parseClaimCornersFromFile(claimFile);
                        processClaim.apply(corners, townName);
                    }
                }
            }

            return new MigrationResult(true, "Migrated towns: " + migratedTowns.get() + ", chunks: " + migratedChunks.get());
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Error during GP migration: " + t.getMessage(), t);
            return new MigrationResult(false, "Exception during migration: " + t.getMessage());
        }
    }

    // find file named <id>.* in ClaimData directory
    private File findClaimFileById(File claimDataDir, long id) {
        File[] files = claimDataDir.listFiles();
        if (files == null) return null;
        String idStr = String.valueOf(id);
        for (File f : files) {
            String name = f.getName();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            if (base.equals(idStr)) return f;
        }
        return null;
    }

    // parse corners from claim file lines like: Lesser Boundary Corner: world;-10;-38;0
    private Location[] parseClaimCornersFromFile(File f) {
        try {
            List<String> lines = Files.readAllLines(f.toPath());
            String lesserLine = null, greaterLine = null;
            for (String l : lines) {
                if (l.startsWith("Lesser Boundary Corner:")) lesserLine = l.substring(l.indexOf(':') + 1).trim();
                if (l.startsWith("Greater Boundary Corner:")) greaterLine = l.substring(l.indexOf(':') + 1).trim();
            }
            if (lesserLine == null || greaterLine == null) return null;
            Location lesser = parseCornerString(lesserLine);
            Location greater = parseCornerString(greaterLine);
            if (lesser == null || greater == null) return null;
            return new Location[]{lesser, greater};
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read claim file " + f.getName() + ": " + e.getMessage());
            return null;
        }
    }

    // "world;x;z;y" -> Location
    private Location parseCornerString(String s) {
        String[] parts = s.split(";");
        if (parts.length < 4) return null;
        String worldName = parts[0];
        try {
            int x = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            int y = Integer.parseInt(parts[3].trim());
            World w = plugin.getServer().getWorld(worldName);
            if (w == null) {
                for (World ww : plugin.getServer().getWorlds()) if (ww.getName().equalsIgnoreCase(worldName)) { w = ww; break; }
            }
            if (w == null) return null;
            return new Location(w, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String makeUniqueTownName(SocietiesManager sm, String base) {
        String candidate = base;
        int i = 1;
        while (sm.getTown(candidate) != null) candidate = base + "_" + i++;
        return candidate;
    }

    private String makeUniqueKingdomName(SocietiesManager sm, String base) {
        String candidate = base;
        int i = 1;
        while (sm.getKingdom(candidate) != null) candidate = base + "_" + i++;
        return candidate;
    }

    private String makeUniqueEmpireName(SocietiesManager sm, String base) {
        String candidate = base;
        int i = 1;
        while (sm.getEmpire(candidate) != null) candidate = base + "_" + i++;
        return candidate;
    }

    public record MigrationResult(boolean success, String message) {}
}
