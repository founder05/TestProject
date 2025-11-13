package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.societies.Alliance;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.NobleTier;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class DataManager {
    private static DataManager instance;
    private final Nations plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final File dataFolder;
    private final File townsFolder;
    private final File kingdomsFolder;
    private final File empiresFolder;
    private final File alliancesFolder;
    private final File warsFolder;
    private final int startingMoney;

    private DataManager(Nations plugin) {
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

    public static DataManager getInstance(Nations plugin) {
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

            return kingdom;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load kingdom " + kingdomName + ": " + e.getMessage());
            return null;
        }
    }

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
            Kingdom kingdom = loadKingdom(kingdomName);
            if (kingdom != null) {
                SocietiesManager.getInstance().registerKingdom(kingdom);
            }
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
                var main = ConfigurationManager.getInstance().getMainConfig();
                if (main != null) {
                    tmpDirCfg = main.getString("save.tmp-dir", "");
                    maxAttemptsCfg = main.getInt("save.max-attempts", maxAttemptsCfg);
                    initialBackoffMsCfg = main.getLong("save.initial-backoff-ms", initialBackoffMsCfg);
                    maxBackoffMsCfg = main.getLong("save.max-backoff-ms", maxBackoffMsCfg);
                }
            }
        } catch (Exception ignored) {
        }

        // create temp file in configured directory or system tmp to reduce interference from OneDrive watchers
        File sysTmpDir = !tmpDirCfg.isEmpty() ? new File(tmpDirCfg) : new File(System.getProperty("java.io.tmpdir"));
        File tmp = null;
        try {
            tmp = File.createTempFile(target.getName() + "-", ".tmp", sysTmpDir);
        } catch (Exception e) {
            // fallback to same directory if creation in sys tmp fails
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

        // write to the temp file (overwrite)
        config.save(tmp);

        // Now try to move/copy into place with more attempts and longer backoff
        final int maxAttempts = Math.max(1, maxAttemptsCfg);
        long waitMillis = Math.max(1L, initialBackoffMsCfg);
        Exception lastException = null;
        boolean moved = false;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Try to obtain exclusive lock on target before replacing (helps on Windows/OneDrive)
                try {
                    java.nio.file.Path targetPath = target.toPath();
                    try (FileChannel channel = FileChannel.open(targetPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                        FileLock lock = null;
                        try {
                            // try a few times quickly to get lock
                            for (int la = 0; la < 3; la++) {
                                try {
                                    lock = channel.tryLock();
                                    if (lock != null) break;
                                } catch (Exception lockEx) {
                                    // ignore and retry
                                }
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }

                            if (lock != null) {
                                try {
                                    // perform atomic move when possible
                                    try {
                                        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                                    } catch (UnsupportedOperationException | AtomicMoveNotSupportedException atomicEx) {
                                        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    }
                                    moved = true;
                                    return; // success, exit method early
                                } finally {
                                    try {
                                        lock.release();
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        } finally {
                            // channel closed automatically
                        }
                    }
                } catch (Exception lockAndMoveEx) {
                    // lock-oriented replace failed; fall back to regular move below
                }

                // If lock path didn't succeed, fallback to move without explicit lock
                try {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (UnsupportedOperationException | AtomicMoveNotSupportedException atomicEx) {
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                moved = true;
                break;
            } catch (Exception ex) {
                lastException = ex;
                plugin.getLogger().warning("Attempt " + attempt + " to replace " + target.getAbsolutePath() + " failed: " + ex.getMessage());
                // Try fallback copy
                try {
                    Files.copy(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    moved = true;
                    break;
                } catch (Exception copyEx) {
                    lastException = copyEx;
                    plugin.getLogger().warning("Attempt " + attempt + " to copy tmp to target failed: " + copyEx.getMessage());
                }
                // Wait before retrying
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                waitMillis = Math.min(maxBackoffMsCfg, waitMillis * 2);
            }
        }

        // If the move failed and tmp is in system tmp, try a last-resort: write tmp directly next to target and try move once
        if (!moved && !tmp.getParentFile().equals(target.getParentFile())) {
            File localTmp = new File(target.getAbsolutePath() + ".tmp2");
            try {
                Files.copy(tmp.toPath(), localTmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(localTmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    moved = true;
                } catch (Exception ex) {
                    lastException = ex;
                    plugin.getLogger().warning("Final attempt to move local tmp to target failed: " + ex.getMessage());
                }
            } catch (Exception e) {
                lastException = e;
                plugin.getLogger().warning("Failed to copy system tmp to local tmp: " + e.getMessage());
            } finally {
                if (localTmp.exists()) try {
                    localTmp.delete();
                } catch (Exception ignored) {
                }
            }
        }

        if (!moved) {
            // Final fallback: try to copy bytes from tmp to target using FileChannel + FileLock (retry a few times).
            Exception channelException = null;
            try {
                java.nio.file.Path tmpPath = tmp.toPath();
                java.nio.file.Path targetPath = target.toPath();

                for (int attempt2 = 1; attempt2 <= 3 && !moved; attempt2++) {
                    try (FileChannel in = FileChannel.open(tmpPath, StandardOpenOption.READ);
                         FileChannel out = FileChannel.open(targetPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                        FileLock lock = null;
                        try {
                            lock = out.tryLock();
                            if (lock != null) {
                                out.truncate(0);
                                long transferred = 0;
                                long size = in.size();
                                while (transferred < size) {
                                    transferred += in.transferTo(transferred, Math.min(1024 * 1024, size - transferred), out);
                                }
                                moved = true;
                                break;
                            }
                        } finally {
                            try {
                                if (lock != null && lock.isValid()) lock.release();
                            } catch (Exception ignored) {
                            }
                        }
                    } catch (Exception ce) {
                        channelException = ce;
                        try {
                            Thread.sleep(150 * attempt2);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                channelException = e;
            }

            if (!moved) {
                if (tmp.exists()) {
                    try {
                        tmp.delete();
                    } catch (Exception ignored) {
                    }
                }
                Exception toThrow = lastException != null ? lastException : channelException;
                throw new java.io.IOException("Failed to move temp file to target '" + target.getAbsolutePath() + "' after " + maxAttempts + " attempts", toThrow);
            }
        }
        if (tmp.exists()) {
            try {
                tmp.delete();
            } catch (Exception ignored) {
            }
        }
    }

    // ========== SAVE/LOAD ALL ==========
    public void saveAllData() {
        plugin.getLogger().info("Saving all data...");
        // On full save (e.g. shutdown) persist all player data including online players
        saveAllPlayerDataForce();
        saveAllTowns();
        saveAllKingdoms();
        saveAllAlliances();
        plugin.getLogger().info("All data saved!");
    }

    /**
     * Save all cached player data including players currently online. Used for shutdown/full saves.
     */
    public void saveAllPlayerDataForce() {
        for (UUID playerId : new ArrayList<>(playerDataCache.keySet())) {
            savePlayerData(playerId);
        }
    }

    public void loadAllData() {
        plugin.getLogger().info("Loading all data...");
        loadAllTowns();
        loadAllKingdoms();
        loadAllAlliances();
        loadAllPlayerData();
        plugin.getLogger().info("All data loaded!");
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
