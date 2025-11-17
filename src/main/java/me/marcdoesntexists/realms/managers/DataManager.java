package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.societies.*;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class DataManager {
    private static DataManager instance;
    private final Realms plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final File dataFolder;
    private final File townsFolder;
    private final File kingdomsFolder;
    private final File empiresFolder;
    private final File alliancesFolder;
    private final File warsFolder;
    private final int startingMoney;

    private DataManager(Realms plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.townsFolder = new File(plugin.getDataFolder(), "towns");
        this.kingdomsFolder = new File(plugin.getDataFolder(), "kingdoms");
        this.empiresFolder = new File(plugin.getDataFolder(), "empires");
        this.alliancesFolder = new File(plugin.getDataFolder(), "alliances");
        this.warsFolder = new File(plugin.getDataFolder(), "wars");

        createFolders();

        ConfigurationManager configManager = ConfigurationManager.getInstance();
        FileConfiguration mainConfig = configManager != null ? configManager.getMainConfig() : null;
        this.startingMoney = mainConfig != null ? mainConfig.getInt("player-defaults.starting-money", 1000) : 1000;
    }

    public static DataManager getInstance(Realms plugin) {
        if (instance == null) {
            instance = new DataManager(plugin);
        }
        return instance;
    }

    public static DataManager getInstance() {
        return instance;
    }

    private void createFolders() {
        createFolder(dataFolder);
        createFolder(townsFolder);
        createFolder(kingdomsFolder);
        createFolder(empiresFolder);
        createFolder(alliancesFolder);
        createFolder(warsFolder);
    }

    private void createFolder(File f) {
        if (f.exists()) return;
        try {
            boolean ok = f.mkdirs();
            if (!ok && !f.exists()) {
                plugin.getLogger().warning("Failed to create data folder: " + f.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Exception creating folder " + f.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    // ========== PLAYER DATA ==========
    public PlayerData getPlayerData(UUID playerId) {
        if (playerDataCache.containsKey(playerId)) {
            return playerDataCache.get(playerId);
        }

        File playerFile = new File(dataFolder, playerId + ".yml");
        if (playerFile.exists()) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                PlayerData data = new PlayerData();
                data.setMoney(config.getInt("money", startingMoney));
                data.setTown(config.getString("town"));
                // load notifications if present
                List<String> notes = config.getStringList("notifications");
                if (notes != null && !notes.isEmpty()) {
                    for (String n : notes) data.addNotification(n);
                }
                // religion support removed
                data.setJob(config.getString("job"));
                data.setSocialClass(config.getString("socialClass", "Commoner"));
                String nobleTierName = config.getString("nobleTier");
                if (nobleTierName != null) {
                    try {
                        data.setNobleTier(NobleTier.valueOf(nobleTierName.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                        data.setNobleTier(NobleTier.COMMONER);
                    }
                }
                String chatChannel = config.getString("chatChannel");
                if (chatChannel != null) data.setChatChannel(chatChannel);
                playerDataCache.put(playerId, data);
                return data;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data for " + playerId + ": " + e.getMessage());
            }
        }

        PlayerData newData = new PlayerData(startingMoney);
        playerDataCache.put(playerId, newData);
        return newData;
    }

    public void savePlayerData(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        if (data == null) return;

        try {
            File playerFile = new File(dataFolder, playerId + ".yml");
            FileConfiguration config = new YamlConfiguration();
            config.set("money", data.getMoney());
            config.set("town", data.getTown());
            // religion support removed
            config.set("job", data.getJob());
            config.set("socialClass", data.getSocialClass());
            if (data.getNobleTier() != null) {
                config.set("nobleTier", data.getNobleTier().toString());
            } else {
                config.set("nobleTier", null);
            }
            config.set("chatChannel", data.getChatChannel());
            // persist pending notifications
            if (!data.getNotifications().isEmpty()) config.set("notifications", data.getNotifications());
            else config.set("notifications", null);
            saveConfigAtomic(config, playerFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player data for " + playerId + ": " + e.getMessage());
        }
    }

    public void savePlayerMoney(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        if (data == null) return;

        try {
            File playerFile = new File(dataFolder, playerId + ".yml");
            FileConfiguration config = new YamlConfiguration();
            if (playerFile.exists()) {
                config = YamlConfiguration.loadConfiguration(playerFile);
            }
            config.set("money", data.getMoney());
            // Also persist other critical, frequently-changing numeric fields to avoid loss on crash
            config.set("jobExperience", data.getJobExperience());
            config.set("nobleTierExperience", data.getNobleTierExperience());

            // keep existing values intact if present
            if (config.getString("town") == null) config.set("town", data.getTown());
            if (config.getString("job") == null) config.set("job", data.getJob());
            if (config.getString("socialClass") == null) config.set("socialClass", data.getSocialClass());
            if (config.getString("nobleTier") == null) {
                if (data.getNobleTier() != null) config.set("nobleTier", data.getNobleTier().toString());
            }
            // also persist notifications here
            if (!data.getNotifications().isEmpty()) config.set("notifications", data.getNotifications());
            saveConfigAtomic(config, playerFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player money for " + playerId + ": " + e.getMessage());
        }
    }

    // ========== TOWN DATA ==========
    public void saveTown(Town town) {
        try {
            File townFile = new File(townsFolder, town.getName() + ".yml");
            FileConfiguration config = new YamlConfiguration();

            config.set("name", town.getName());
            config.set("mayor", town.getMayor().toString());
            config.set("members", town.getMembers().stream().map(UUID::toString).toList());
            config.set("claims", new ArrayList<>(town.getClaims()));
            config.set("balance", town.getBalance());
            config.set("kingdom", town.getKingdom());
            config.set("progressionLevel", town.getProgressionLevel());
            config.set("progressionExperience", town.getProgressionExperience());
            // Save trusted members
            config.set("trustedMembers", town.getTrustedMembers().stream().map(UUID::toString).toList());

            saveConfigAtomic(config, townFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save town " + town.getName() + ": " + e.getMessage());
        }
    }

    public Town loadTown(String townName) {
        try {
            File townFile = new File(townsFolder, townName + ".yml");
            if (!townFile.exists()) return null;

            FileConfiguration config = YamlConfiguration.loadConfiguration(townFile);

            String mayorStr = config.getString("mayor");
            if (mayorStr == null) {
                plugin.getLogger().warning("Town file missing mayor: " + townFile.getName());
                return null;
            }
            UUID mayor;
            try {
                mayor = UUID.fromString(mayorStr);
            } catch (Exception ex) {
                plugin.getLogger().severe("Invalid mayor UUID in town file " + townFile.getName() + ": " + mayorStr);
                return null;
            }
            Town town = new Town(config.getString("name"), mayor);

            List<String> memberStrings = config.getStringList("members");
            for (String memberStr : memberStrings) {
                town.addMember(UUID.fromString(memberStr));
            }

            town.getClaims().addAll(config.getStringList("claims"));
            town.addMoney(config.getInt("balance"));
            town.setKingdom(config.getString("kingdom"));
            town.setProgressionLevel(config.getInt("progressionLevel", 1));
            town.setProgressionExperience(config.getLong("progressionExperience", 0));

            // Load trusted members if present
            List<String> trusted = config.getStringList("trustedMembers");
            if (trusted != null) {
                for (String s : trusted) {
                    try {
                        town.addTrusted(UUID.fromString(s));
                    } catch (Exception ignored) {
                    }
                }
            }

            return town;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load town " + townName + ": " + e.getMessage());
            return null;
        }
    }

    public void saveAllTowns() {
        for (Town town : SocietiesManager.getInstance().getAllTowns()) {
            saveTown(town);
        }
    }

    public void loadAllTowns() {
        File[] files = townsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String townName = file.getName().replace(".yml", "");
            Town town = loadTown(townName);
            if (town != null) {
                SocietiesManager.getInstance().registerTown(town);
                // Ensure ClaimManager indexes the claims from the town file so runtime maps are populated
                try {
                    ClaimManager cm = ClaimManager.getInstance(plugin);
                    if (cm != null) cm.registerTownClaims(town);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public void saveAllAlliances() {
        for (Alliance alliance : SocietiesManager.getInstance().getAlliances()) {
            saveAlliance(alliance);
        }
    }

    public void loadAllAlliances() {
        File[] files = alliancesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String allianceIdStr = file.getName().replace(".yml", "");
            try {
                UUID allianceId = UUID.fromString(allianceIdStr);
                Alliance alliance = loadAlliance(allianceId);
                if (alliance != null) {
                    SocietiesManager.getInstance().registerAlliance(alliance);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid alliance file: " + file.getName());
            }
        }
    }

    // ========== KINGDOM DATA ==========
    public void saveKingdom(Kingdom kingdom) {
        try {
            File kingdomFile = new File(kingdomsFolder, kingdom.getName() + ".yml");
            FileConfiguration config = new YamlConfiguration();

            config.set("name", kingdom.getName());
            config.set("capital", kingdom.getCapital());
            config.set("towns", new ArrayList<>(kingdom.getTowns()));
            config.set("wars", new ArrayList<>(kingdom.getWars()));
            config.set("empire", kingdom.getEmpire());
            config.set("allies", new ArrayList<>(kingdom.getAllies()));
            config.set("enemies", new ArrayList<>(kingdom.getEnemies()));
            config.set("alliances", new ArrayList<>(kingdom.getAlliances()));
            config.set("treaties", new ArrayList<>(kingdom.getTreaties()));
            config.set("suzerain", kingdom.getSuzerain());
            config.set("vassals", new ArrayList<>(kingdom.getVassals()));
            config.set("tributeAmount", kingdom.getTributeAmount());
            config.set("progressionLevel", kingdom.getProgressionLevel());
            config.set("progressionExperience", kingdom.getProgressionExperience());
            config.set("balance", kingdom.getBalance());
            // Save trusted members at kingdom level
            config.set("trustedMembers", kingdom.getTrustedMembers().stream().map(java.util.UUID::toString).toList());

            saveConfigAtomic(config, kingdomFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save kingdom " + kingdom.getName() + ": " + e.getMessage());
        }
    }

    public Kingdom loadKingdom(String kingdomName) {
        try {
            File kingdomFile = new File(kingdomsFolder, kingdomName + ".yml");
            if (!kingdomFile.exists()) return null;

            FileConfiguration config = YamlConfiguration.loadConfiguration(kingdomFile);

            Kingdom kingdom = new Kingdom(config.getString("name"), config.getString("capital"));

            kingdom.getTowns().addAll(config.getStringList("towns"));
            kingdom.getWars().addAll(config.getStringList("wars"));
            kingdom.setEmpire(config.getString("empire"));
            kingdom.getAllies().addAll(config.getStringList("allies"));
            kingdom.getEnemies().addAll(config.getStringList("enemies"));
            kingdom.getAlliances().addAll(config.getStringList("alliances"));
            kingdom.getTreaties().addAll(config.getStringList("treaties"));
            kingdom.setSuzerain(config.getString("suzerain"));
            kingdom.getVassals().addAll(config.getStringList("vassals"));
            kingdom.setTributeAmount(config.getInt("tributeAmount"));
            kingdom.setProgressionLevel(config.getInt("progressionLevel", 1));
            kingdom.setProgressionExperience(config.getLong("progressionExperience", 0));
            kingdom.setBalance(config.getInt("balance", 0));

            // Load trusted members for kingdom
            List<String> kTrusted = config.getStringList("trustedMembers");
            if (kTrusted != null) {
                for (String s : kTrusted) {
                    try {
                        kingdom.addTrusted(java.util.UUID.fromString(s));
                    } catch (Exception ignored) {
                    }
                }
            }

            return kingdom;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load kingdom " + kingdomName + ": " + e.getMessage());
            return null;
        }
    }

    // ========== EMPIRE DATA ==========
    public void saveEmpire(Empire empire) {
        try {
            File empireFile = new File(empiresFolder, empire.getName() + ".yml");
            FileConfiguration config = new YamlConfiguration();

            config.set("name", empire.getName());
            config.set("capital", empire.getCapital());
            config.set("kingdoms", new ArrayList<>(empire.getKingdoms()));
            config.set("progressionLevel", empire.getProgressionLevel());
            config.set("progressionExperience", empire.getProgressionExperience());
            // trusted members
            config.set("trustedMembers", empire.getTrustedMembers().stream().map(java.util.UUID::toString).toList());

            saveConfigAtomic(config, empireFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save empire " + empire.getName() + ": " + e.getMessage());
        }
    }

    public Empire loadEmpire(String empireName) {
        try {
            File empireFile = new File(empiresFolder, empireName + ".yml");
            if (!empireFile.exists()) return null;

            FileConfiguration config = YamlConfiguration.loadConfiguration(empireFile);

            Empire empire = new Empire(config.getString("name"), config.getString("capital"));

            empire.getKingdoms().addAll(config.getStringList("kingdoms"));
            empire.setProgressionLevel(config.getInt("progressionLevel", 1));
            empire.setProgressionExperience(config.getLong("progressionExperience", 0));

            List<String> eTrusted = config.getStringList("trustedMembers");
            if (eTrusted != null) {
                for (String s : eTrusted) {
                    try {
                        empire.addTrusted(java.util.UUID.fromString(s));
                    } catch (Exception ignored) {
                    }
                }
            }

            return empire;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load empire " + empireName + ": " + e.getMessage());
            return null;
        }
    }

    // ========== ALLIANCE DATA ==========
    public void saveAlliance(Alliance alliance) {
        try {
            File allianceFile = new File(alliancesFolder, alliance.getAllianceId() + ".yml");
            FileConfiguration config = new YamlConfiguration();

            config.set("id", alliance.getAllianceId().toString());
            config.set("name", alliance.getName());
            config.set("leader", alliance.getLeader());
            config.set("members", new ArrayList<>(alliance.getMembers()));
            config.set("pendingInvites", new ArrayList<>(alliance.getPendingInvites()));
            config.set("createdAt", alliance.getCreatedAt());
            config.set("description", alliance.getDescription());

            saveConfigAtomic(config, allianceFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save alliance " + alliance.getName() + ": " + e.getMessage());
        }
    }

    public Alliance loadAlliance(UUID allianceId) {
        try {
            File allianceFile = new File(alliancesFolder, allianceId + ".yml");
            if (!allianceFile.exists()) return null;

            FileConfiguration config = YamlConfiguration.loadConfiguration(allianceFile);

            Alliance alliance = new Alliance(config.getString("name"), config.getString("leader"));

            alliance.getMembers().addAll(config.getStringList("members"));
            alliance.getPendingInvites().addAll(config.getStringList("pendingInvites"));
            alliance.setCreatedAt(config.getLong("createdAt"));
            alliance.setDescription(config.getString("description", "No description"));

            return alliance;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load alliance " + allianceId + ": " + e.getMessage());
            return null;
        }
    }

    // Writes YAML to a temp file and moves it into place to reduce partial writes / corruption
    private void saveConfigAtomic(FileConfiguration config, File target) throws Exception {
        // Load save config values from ConfigurationManager if available
        int maxAttemptsCfg = 12;
        long initialBackoffMsCfg = 200L;
        long maxBackoffMsCfg = 4000L;
        String tmpDirCfg = "";
        try {
            if (ConfigurationManager.getInstance() != null) {
                ConfigurationManager cm = ConfigurationManager.getInstance();
                tmpDirCfg = cm.getSaveTmpDir();
                maxAttemptsCfg = cm.getSaveMaxAttempts();
                initialBackoffMsCfg = cm.getSaveInitialBackoffMs();
                maxBackoffMsCfg = cm.getSaveMaxBackoffMs();
            }
        } catch (Exception ignored) {
        }

        // prefer plugin tmp directory (safer on OneDrive systems) then configured tmp then system tmp
        File preferredTmpDir = new File(plugin.getDataFolder(), "tmp");
        File sysTmpDir = !tmpDirCfg.isEmpty() ? new File(tmpDirCfg) : new File(System.getProperty("java.io.tmpdir"));
        File tmpDir = preferredTmpDir.exists() || preferredTmpDir.mkdirs() ? preferredTmpDir : (sysTmpDir.exists() || sysTmpDir.mkdirs() ? sysTmpDir : plugin.getDataFolder());

        File tmp = null;
        try {
            tmp = Files.createTempFile(tmpDir.toPath(), target.getName() + "-", ".tmp").toFile();
        } catch (Exception e) {
            // fallback to same directory if creation in tmp fails
            tmp = new File(target.getAbsolutePath() + ".tmp");
        }

        // ensure parent exists for target
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            try {
                parent.mkdirs();
            } catch (Exception ignored) {
            }
        }

        // write YAML content to the temp file using saveToString to avoid writer leaks
        try {
            String yaml = config.saveToString();
            Files.writeString(tmp.toPath(), yaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            // Last resort: try Bukkit's save (may still work)
            try {
                config.save(tmp);
            } catch (Exception ex) {
                // ensure tmp removed if write completely fails
                try {
                    tmp.delete();
                } catch (Exception ignored) {
                }
                throw new Exception("Failed to write temp config file for " + target.getAbsolutePath(), ex);
            }
        }

        // Now try to move/copy into place with more attempts and longer backoff
        final int maxAttempts = Math.max(1, maxAttemptsCfg);
        long waitMillis = Math.max(1L, initialBackoffMsCfg);
        Exception lastException = null;
        boolean success = false;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Prefer atomic move inside same filesystem
                try {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    success = true;
                    return;
                } catch (UnsupportedOperationException | AtomicMoveNotSupportedException atomicEx) {
                    // fallback to non-atomic move
                }

                try {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    success = true;
                    return;
                } catch (Exception moveEx) {
                    // Move failed (often due to Windows file lock). Try copy fallback.
                    try {
                        Files.copy(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        // attempt to delete tmp after copy
                        try {
                            Files.deleteIfExists(tmp.toPath());
                        } catch (Exception ignored) {
                        }
                        success = true;
                        return;
                    } catch (Exception copyEx) {
                        // As final fallback try writing directly to target path (may still fail if locked)
                        try {
                            String yaml = config.saveToString();
                            Files.writeString(target.toPath(), yaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            // delete tmp
                            try {
                                Files.deleteIfExists(tmp.toPath());
                            } catch (Exception ignored) {
                            }
                            success = true;
                            return;
                        } catch (Exception writeEx) {
                            // preserve last exception and retry after backoff
                            lastException = writeEx;
                        }
                    }
                }
            } catch (Exception ex) {
                lastException = ex;
            }

            // wait/backoff before retry
            try {
                Thread.sleep(Math.min(waitMillis, maxBackoffMsCfg));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            waitMillis = Math.min(maxBackoffMsCfg, waitMillis * 2);
        }

        // cleanup tmp as we failed
        try {
            Files.deleteIfExists(tmp.toPath());
        } catch (Exception ignored) {
        }

        if (!success) {
            // write diagnostic dump (stacktrace + YAML) to plugin/tmp for debugging
            try {
                File diagDir = new File(plugin.getDataFolder(), "tmp");
                if (!diagDir.exists()) diagDir.mkdirs();
                String time = String.valueOf(System.currentTimeMillis());
                File diagFile = new File(diagDir, "diagnostic-dump-" + target.getName() + "-" + time + ".log");
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to save target: ").append(target.getAbsolutePath()).append("\n");
                if (lastException != null) {
                    sb.append("Last exception: \n");
                    sb.append(lastException.toString()).append("\n");
                    for (StackTraceElement ste : lastException.getStackTrace()) {
                        sb.append("  at ").append(ste.toString()).append("\n");
                    }
                }
                try {
                    sb.append("\nYAML content:\n").append(config.saveToString()).append("\n");
                } catch (Exception ignored) {
                }
                Files.writeString(diagFile.toPath(), sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                plugin.getLogger().severe("Failed to save config for " + target.getName() + ". Diagnostic dump written to: " + diagFile.getAbsolutePath());
            } catch (Exception ignored) {
            }

            throw new Exception("Failed to move temp file to target '" + target.getAbsolutePath() + "' after " + maxAttempts + " attempts", lastException);
        }
    }

    // Public wrapper for testing the atomic save routine
    public void saveConfigAtomicPublic(FileConfiguration config, File target) throws Exception {
        saveConfigAtomic(config, target);
    }

    // ========== SAVE/LOAD ALL ==========
    public void saveAllData() {
        plugin.getLogger().info("Saving all data...");
        // On full save (e.g. shutdown) persist all player data including online players
        saveAllPlayerDataForce();
        saveAllTowns();
        saveAllKingdoms();
        saveAllEmpires();
        saveAllAlliances();
        plugin.getLogger().info("All data saved!");
    }

    public void loadAllData() {
        plugin.getLogger().info("Loading all data...");
        loadAllTowns();
        loadAllKingdoms();
        loadAllEmpires();
        loadAllAlliances();
        loadAllPlayerData();
        plugin.getLogger().info("All data loaded!");
    }

    // Batch kingdom helpers
    public void saveAllKingdoms() {
        for (Kingdom kingdom : SocietiesManager.getInstance().getAllKingdoms()) {
            saveKingdom(kingdom);
        }
    }

    public void loadAllKingdoms() {
        File[] files = kingdomsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String kingdomName = file.getName().replace(".yml", "");
            Kingdom k = loadKingdom(kingdomName);
            if (k != null) SocietiesManager.getInstance().registerKingdom(k);
        }
    }

    public void saveAllEmpires() {
        for (Empire empire : SocietiesManager.getInstance().getAllEmpires()) {
            saveEmpire(empire);
        }
    }

    public void loadAllEmpires() {
        File[] files = empiresFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String empireName = file.getName().replace(".yml", "");
            Empire e = loadEmpire(empireName);
            if (e != null) SocietiesManager.getInstance().registerEmpire(e);
        }
    }

    /**
     * Save all cached player data including players currently online. Used for shutdown/full saves.
     */
    public void saveAllPlayerDataForce() {
        for (UUID playerId : new ArrayList<>(playerDataCache.keySet())) {
            savePlayerData(playerId);
        }
    }


    public void saveAllPlayerData() {
        for (UUID playerId : new ArrayList<>(playerDataCache.keySet())) {
            if (Bukkit.getPlayer(playerId) != null) {
                continue;
            }
            savePlayerData(playerId);
        }
    }

    public void loadAllPlayerData() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            try {
                UUID uuid = UUID.fromString(id);
                getPlayerData(uuid);
            } catch (Exception e) {
                plugin.getLogger().warning("Skipping invalid player data file: " + file.getName());
            }
        }
    }

    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
    }

    public void saveAllSocieties() {
        saveAllTowns();
        saveAllKingdoms();
        saveAllAlliances();
    }
}
