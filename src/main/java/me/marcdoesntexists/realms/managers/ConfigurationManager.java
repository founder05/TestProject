package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationManager {
    private static ConfigurationManager instance;
    private final Realms plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    private ConfigurationManager(Realms plugin) {
        this.plugin = plugin;
        loadAllConfigurations();
    }

    public static ConfigurationManager getInstance(Realms plugin) {
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
        loadConfig("diplomacy.yml");
        loadConfig("gui.yml");
        loadConfig("messages.yml");
        loadConfig("war.yml");
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
            plugin.getLogger().severe("Failed to load " + fileName + ": " + e.toString());
            // also log stacktrace at fine level for debugging
            plugin.getLogger().severe(java.util.Arrays.toString(e.getStackTrace()));
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

    public FileConfiguration getRealmsGuiconfig() {
        return configs.get("gui.yml");
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

    public FileConfiguration getMessagesConfig() {
        return configs.get("messages.yml");
    }

    public void reloadAllConfigurations() {
        plugin.getLogger().info("Reloading all configuration files...");
        configs.clear();
        loadAllConfigurations();
    }

    // ========== Save helpers (con defaults) ==========
    public String getSaveTmpDir() {
        FileConfiguration main = getMainConfig();
        return main != null ? main.getString("save.tmp-dir", "") : "";
    }

    public int getSaveMaxAttempts() {
        FileConfiguration main = getMainConfig();
        return main != null ? main.getInt("save.max-attempts", 12) : 12;
    }

    public long getSaveInitialBackoffMs() {
        FileConfiguration main = getMainConfig();
        return main != null ? main.getLong("save.initial-backoff-ms", 200L) : 200L;
    }

    public long getSaveMaxBackoffMs() {
        FileConfiguration main = getMainConfig();
        return main != null ? main.getLong("save.max-backoff-ms", 4000L) : 4000L;
    }
}
