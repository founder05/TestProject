package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.societies.*;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class DataManager {
    private static DataManager instance;
    private final Nations plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final File dataFolder;
    private final File townsFolder;
    private final File kingdomsFolder;
    private final File empiresFolder;
    private final File religionsFolder;
    private final File alliancesFolder;
    private final File warsFolder;
    private final int startingMoney;

    private DataManager(Nations plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.townsFolder = new File(plugin.getDataFolder(), "towns");
        this.kingdomsFolder = new File(plugin.getDataFolder(), "kingdoms");
        this.empiresFolder = new File(plugin.getDataFolder(), "empires");
        this.religionsFolder = new File(plugin.getDataFolder(), "religions");
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
        dataFolder.mkdirs();
        townsFolder.mkdirs();
        kingdomsFolder.mkdirs();
        empiresFolder.mkdirs();
        religionsFolder.mkdirs();
        alliancesFolder.mkdirs();
        warsFolder.mkdirs();
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
                data.setReligion(config.getString("religion"));
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
            config.set("religion", data.getReligion());
            config.set("job", data.getJob());
            config.set("socialClass", data.getSocialClass());
            config.set("nobleTier", data.getNobleTier().toString());
            config.save(playerFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player data for " + playerId + ": " + e.getMessage());
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

            config.save(townFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save town " + town.getName() + ": " + e.getMessage());
        }
    }

    public Town loadTown(String townName) {
        try {
            File townFile = new File(townsFolder, townName + ".yml");
            if (!townFile.exists()) return null;

            FileConfiguration config = YamlConfiguration.loadConfiguration(townFile);

            UUID mayor = UUID.fromString(Objects.requireNonNull(config.getString("mayor")));
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

            config.save(kingdomFile);
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

            config.save(allianceFile);
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

    // ========== RELIGION DATA ==========
    public void saveReligion(Religion religion) {
        try {
            File religionFile = new File(religionsFolder, religion.getName() + ".yml");
            FileConfiguration config = new YamlConfiguration();

            config.set("name", religion.getName());
            config.set("founder", religion.getFounder().toString());
            config.set("followers", religion.getFollowers().stream().map(UUID::toString).toList());

            List<String> clergyData = new ArrayList<>();
            for (Map.Entry<UUID, String> entry : religion.getClergy().entrySet()) {
                clergyData.add(entry.getKey() + ":" + entry.getValue());
            }
            config.set("clergy", clergyData);

            config.save(religionFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save religion " + religion.getName() + ": " + e.getMessage());
        }
    }

    public Religion loadReligion(String religionName) {
        try {
            File religionFile = new File(religionsFolder, religionName + ".yml");
            if (!religionFile.exists()) return null;

            FileConfiguration config = YamlConfiguration.loadConfiguration(religionFile);

            UUID founder = UUID.fromString(Objects.requireNonNull(config.getString("founder")));
            Religion religion = new Religion(config.getString("name"), founder);

            List<String> followerStrings = config.getStringList("followers");
            for (String followerStr : followerStrings) {
                religion.addFollower(UUID.fromString(followerStr));
            }

            List<String> clergyData = config.getStringList("clergy");
            for (String data : clergyData) {
                String[] parts = data.split(":");
                if (parts.length == 2) {
                    religion.setClergyRank(UUID.fromString(parts[0]), parts[1]);
                }
            }

            return religion;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load religion " + religionName + ": " + e.getMessage());
            return null;
        }
    }

    public void saveAllReligions() {
        for (Religion religion : SocietiesManager.getInstance().getAllReligions()) {
            saveReligion(religion);
        }
    }

    public void loadAllReligions() {
        File[] files = religionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String religionName = file.getName().replace(".yml", "");
            Religion religion = loadReligion(religionName);
            if (religion != null) {
                SocietiesManager.getInstance().registerReligion(religion);
            }
        }
    }

    // ========== SAVE/LOAD ALL ==========
    public void saveAllData() {
        plugin.getLogger().info("Saving all data...");
        saveAllPlayerData();
        saveAllTowns();
        saveAllKingdoms();
        saveAllAlliances();
        saveAllReligions();
        plugin.getLogger().info("All data saved!");
    }

    public void loadAllData() {
        plugin.getLogger().info("Loading all data...");
        loadAllTowns();
        loadAllKingdoms();
        loadAllAlliances();
        loadAllReligions();
        plugin.getLogger().info("All data loaded!");
    }

    public void saveAllPlayerData() {
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId);
        }
    }

    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
    }
}