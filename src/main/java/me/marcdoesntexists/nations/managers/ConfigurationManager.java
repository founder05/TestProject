package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationManager {
    private static ConfigurationManager instance;
    private final Nations plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    private ConfigurationManager(Nations plugin) {
        this.plugin = plugin;
        loadAllConfigurations();
    }

    public static ConfigurationManager getInstance(Nations plugin) {
        if (instance == null) {
            instance = new ConfigurationManager(plugin);
        }
        return instance;
    }

    public static ConfigurationManager getInstance() {
        return instance;
    }

    private void loadAllConfigurations() {
        plugin.getLogger().info("Loading configuration files...");

        loadConfig("config.yml");
        loadConfig("settlements.yml");
        loadConfig("economy.yml");
        loadConfig("feudal.yml");
        loadConfig("religion.yml");
        loadConfig("diplomacy.yml");
        loadConfig("war.yml");
        loadConfig("legal.yml");
        loadConfig("military.yml");
        loadConfig("social.yml");

        plugin.getLogger().info("All configuration files loaded successfully!");
    }

    private void loadConfig(String fileName) {
        try {
            File configFile = new File(plugin.getDataFolder(), fileName);

            if (!configFile.exists()) {
                plugin.saveResource(fileName, false);
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            configs.put(fileName, config);
            plugin.getLogger().info("Loaded " + fileName);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig(String configName) {
        return configs.get(configName);
    }

    public FileConfiguration getMainConfig() {
        return configs.get("config.yml");
    }

    public FileConfiguration getSettlementsConfig() {
        return configs.get("settlements.yml");
    }

    public FileConfiguration getEconomyConfig() {
        return configs.get("economy.yml");
    }

    public FileConfiguration getFeudalConfig() {
        return configs.get("feudal.yml");
    }

    public FileConfiguration getReligionConfig() {
        return configs.get("religion.yml");
    }

    public FileConfiguration getDiplomacyConfig() {
        return configs.get("diplomacy.yml");
    }

    public FileConfiguration getWarConfig() {
        return configs.get("war.yml");
    }

    public FileConfiguration getLegalConfig() {
        return configs.get("legal.yml");
    }

    public FileConfiguration getMilitaryConfig() {
        return configs.get("military.yml");
    }

    public FileConfiguration getSocialConfig() {
        return configs.get("social.yml");
    }

    public void reloadAllConfigurations() {
        plugin.getLogger().info("Reloading all configuration files...");
        configs.clear();
        loadAllConfigurations();
    }
}
