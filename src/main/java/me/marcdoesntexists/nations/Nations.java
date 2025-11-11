package me.marcdoesntexists.nations;

import me.marcdoesntexists.nations.commands.*;
import me.marcdoesntexists.nations.listeners.*;
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
    private ClaimManager claimManager;
    private HybridClaimManager hybridClaimManager;

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
        
        loadData();

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
            claimManager = ClaimManager.getInstance(this);
            hybridClaimManager = HybridClaimManager.getInstance(this);

            getLogger().info("All managers initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize managers!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void registerEventListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new MoveListener(this), this);
        pm.registerEvents(new EntityListener(this), this);
        pm.registerEvents(new InteractListener(this), this);
        
        getLogger().info("Event listeners registered");
    }

    private void registerCommands() {
        TownCommand townCommand = new TownCommand(this);
        getCommand("town").setExecutor(townCommand);
        getCommand("town").setTabCompleter(townCommand);
        
        KingdomCommand kingdomCommand = new KingdomCommand(this);
        getCommand("kingdom").setExecutor(kingdomCommand);
        getCommand("kingdom").setTabCompleter(kingdomCommand);
        
        WarCommand warCommand = new WarCommand(this);
        getCommand("war").setExecutor(warCommand);
        getCommand("war").setTabCompleter(warCommand);
        
        JobCommand jobCommand = new JobCommand(this);
        getCommand("job").setExecutor(jobCommand);
        getCommand("job").setTabCompleter(jobCommand);
        
        AllianceCommand allianceCommand = new AllianceCommand(this);
        getCommand("alliance").setExecutor(allianceCommand);
        getCommand("alliance").setTabCompleter(allianceCommand);
        
        ReligionCommand religionCommand = new ReligionCommand(this);
        getCommand("religion").setExecutor(religionCommand);
        getCommand("religion").setTabCompleter(religionCommand);
        
        LawCommand lawCommand = new LawCommand(this);
        getCommand("law").setExecutor(lawCommand);
        getCommand("law").setTabCompleter(lawCommand);
        
        getLogger().info("Commands registered");
    }
    
    private void loadData() {
        try {
            
            getLogger().info("Data loaded successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to load data!");
            e.printStackTrace();
        }
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

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public HybridClaimManager getHybridClaimManager() {
        return hybridClaimManager;
    }
}
