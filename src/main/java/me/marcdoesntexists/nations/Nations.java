package me.marcdoesntexists.nations;

import me.marcdoesntexists.nations.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Nations extends JavaPlugin {
    private static Nations instance;
    private ConfigurationManager configurationManager;
    private DataManager dataManager;
    private SocietiesManager societiesManager;
    private EconomyManager economyManager;
    private LawManager lawManager;
    private MilitaryManager militaryManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        initializeConfigurations();
        initializeManagers();
        registerEventListeners();
        registerCommands();

        getLogger().info("Nations plugin has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
    }

    private void initializeConfigurations() {
        try {
            configurationManager = ConfigurationManager.getInstance(this);
            getLogger().info("Configuration system initialized");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize configuration system!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void initializeManagers() {
        try {
            dataManager = DataManager.getInstance(this);
            societiesManager = SocietiesManager.getInstance();
            economyManager = EconomyManager.getInstance();
            lawManager = LawManager.getInstance();
            militaryManager = MilitaryManager.getInstance();

            getLogger().info("All managers initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize managers!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void registerEventListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        getLogger().info("Event listeners registered");
    }

    private void registerCommands() {
        getLogger().info("Commands registered");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllPlayerData();
        }
        getLogger().info("Nations plugin has been disabled!");
    }

    public static Nations getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public SocietiesManager getSocietiesManager() {
        return societiesManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public LawManager getLawManager() {
        return lawManager;
    }

    public MilitaryManager getMilitaryManager() {
        return militaryManager;
    }
}
