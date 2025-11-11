package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.societies.NobleTier;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private static DataManager instance;
    private final Nations plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final File dataFolder;
    private final int startingMoney;

    private DataManager(Nations plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
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
            e.printStackTrace();
        }
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
