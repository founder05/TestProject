package me.marcdoesntexists.nations;

import me.marcdoesntexists.nations.commands.*;
import me.marcdoesntexists.nations.economy.EconomyService;
import me.marcdoesntexists.nations.integrations.GriefPreventionIntegration;
import me.marcdoesntexists.nations.law.JusticeService;
import me.marcdoesntexists.nations.listeners.*;
import me.marcdoesntexists.nations.managers.*;
import me.marcdoesntexists.nations.military.WarfareService;
import me.marcdoesntexists.nations.societies.DiplomacyService;
import me.marcdoesntexists.nations.societies.FeudalService;
import me.marcdoesntexists.nations.societies.ReligionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Nations extends JavaPlugin {
    private static Nations instance;
    private ConfigurationManager configurationManager;
    private DataManager dataManager;
    private SocietiesManager societiesManager;
    private EconomyManager economyManager;
    private LawManager lawManager;
    private MilitaryManager militaryManager;
    private ClaimVisualizer claimVisualizer;
    private ClaimManager claimManager;
    private HybridClaimManager hybridClaimManager;
    private EconomyService economyService;
    private JusticeService justiceService;
    private WarfareService warfareService;
    private DiplomacyService diplomacyService;
    private FeudalService feudalService;
    private ReligionService religionService;
    private SettlementEvolutionManager evolutionManager;

    public static Nations getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdirs();
            if (!created && !getDataFolder().exists()) {
                getLogger().severe("Unable to create data folder: " + getDataFolder().getAbsolutePath());
                // continue without disabling; warn only
            }
        }

        initializeConfigurations();
        initializeManagers();

        initializeServices();
        registerEventListeners();
        registerCommands();

        loadData();

        startAutoSaveTasks();

        getLogger().info("Nations plugin has been enabled!");
        getLogger().info("Version: " + getPluginVersion());
        getLogger().info("All services initialized and running!");
    }

    private void initializeConfigurations() {
        try {
            configurationManager = ConfigurationManager.getInstance(this);
            getLogger().info("Configuration system initialized");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize configuration system!");
            getLogger().log(Level.SEVERE, "Error initializing configuration system", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void initializeManagers() {
        try {
            dataManager = DataManager.getInstance(this);
            societiesManager = SocietiesManager.getInstance();
            economyManager = EconomyManager.getInstance();
            claimVisualizer = new ClaimVisualizer(this);
            lawManager = LawManager.getInstance();
            militaryManager = MilitaryManager.getInstance();
            claimManager = ClaimManager.getInstance(this);
            hybridClaimManager = HybridClaimManager.getInstance(this);

            getLogger().info("All managers initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize managers!");
            getLogger().log(Level.SEVERE, "Error initializing managers", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void initializeServices() {
        try {

            economyService = EconomyService.getInstance(this);
            justiceService = JusticeService.getInstance(this);
            warfareService = WarfareService.getInstance(this);
            diplomacyService = DiplomacyService.getInstance(this);
            feudalService = FeudalService.getInstance(this);
            religionService = ReligionService.getInstance(this);
            evolutionManager = SettlementEvolutionManager.getInstance(this);

            getLogger().info("All services initialized successfully");
            getLogger().info("✓ Economy Service - Active");
            getLogger().info("✓ Justice Service - Active");
            getLogger().info("✓ Warfare Service - Active");
            getLogger().info("✓ Diplomacy Service - Active");
            getLogger().info("✓ Feudal Service - Active");
            getLogger().info("✓ Religion Service - Active");
            getLogger().info("✓ Settlement Evolution Manager - Active");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize services!");
            getLogger().log(Level.SEVERE, "Error initializing services", e);
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
        // Create a single instance per command and register it as both executor and tab completer
        TownCommand townCommand = new TownCommand(this);
        safeRegisterCommand("town", townCommand, townCommand);

        KingdomCommand kingdomCommand = new KingdomCommand(this);
        safeRegisterCommand("kingdom", kingdomCommand, kingdomCommand);

        WarCommand warCommand = new WarCommand(this);
        safeRegisterCommand("war", warCommand, warCommand);

        ShowClaimsCommand showClaimsCommand = new ShowClaimsCommand(this);
        safeRegisterCommand("showclaims", showClaimsCommand, showClaimsCommand);

        JobCommand jobCommand = new JobCommand(this);
        safeRegisterCommand("job", jobCommand, jobCommand);

        AllianceCommand allianceCommand = new AllianceCommand(this);
        safeRegisterCommand("alliance", allianceCommand, allianceCommand);

        ReligionCommand religionCommand = new ReligionCommand(this);
        safeRegisterCommand("religion", religionCommand, religionCommand);

        LawCommand lawCommand = new LawCommand(this);
        safeRegisterCommand("law", lawCommand, lawCommand);

        TreatyCommand treatyCommand = new TreatyCommand(this);
        safeRegisterCommand("treaty", treatyCommand, treatyCommand);

        FeudalCommand feudalCommand = new FeudalCommand(this);
        safeRegisterCommand("feudal", feudalCommand, feudalCommand);

        EvolveCommand evolveCommand = new EvolveCommand(this);
        safeRegisterCommand("evolve", evolveCommand, evolveCommand);

        // Explicitly register additional commands present in the commands package
        MilitaryCommand militaryCommand = new MilitaryCommand(this);
        safeRegisterCommand("military", militaryCommand, militaryCommand);

        TrialCommand trialCommand = new TrialCommand(this);
        safeRegisterCommand("trial", trialCommand, trialCommand);

        NobleCommand nobleCommand = new NobleCommand(this);
        safeRegisterCommand("noble", nobleCommand, nobleCommand);

        GodCommand godCommand = new GodCommand(this);
        safeRegisterCommand("god", godCommand, godCommand);

        getLogger().info("Commands registered");
    }

    // Safely register a command; if the command is not declared in plugin.yml, log an error but don't disable the plugin
    private void safeRegisterCommand(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command '" + name + "' not found in plugin.yml. Skipping registration.");
            return;
        }
        if (executor != null) cmd.setExecutor(executor);
        if (completer != null) cmd.setTabCompleter(completer);
    }

    private void loadData() {
        try {
            // Load all data from files
            dataManager.loadAllData();
            getLogger().info("Data loaded successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to load data!");
            getLogger().log(Level.SEVERE, "Error loading data", e);
        }
    }

    private void startAutoSaveTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                dataManager.saveAllData();
                getLogger().info("Auto-save completed");
            } catch (Exception e) {
                getLogger().severe("Auto-save failed: " + e.getMessage());
            }
        }, 6000L, 6000L);

        getLogger().info("Auto-save task started (every 5 minutes)");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            getLogger().info("Saving all data before shutdown...");
            dataManager.saveAllData();
            getLogger().info("All data saved successfully!");
        }

        // stop any active claim visualizations
        if (claimVisualizer != null) {
            claimVisualizer.stopAll();
        }

        // Save griefprevention mappings if integration exists
        try {
            GriefPreventionIntegration gp = GriefPreventionIntegration.getInstance(this);
            if (gp != null) gp.saveMappings();
        } catch (Exception ignored) {
        }

        getLogger().info("Nations plugin has been disabled!");
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

    // Service getters
    public EconomyService getEconomyService() {
        return economyService;
    }

    public JusticeService getJusticeService() {
        return justiceService;
    }

    public WarfareService getWarfareService() {
        return warfareService;
    }

    public DiplomacyService getDiplomacyService() {
        return diplomacyService;
    }

    public FeudalService getFeudalService() {
        return feudalService;
    }

    public ReligionService getReligionService() {
        return religionService;
    }

    public SettlementEvolutionManager getEvolutionManager() {
        return evolutionManager;
    }

    public ClaimVisualizer getClaimVisualizer() { return claimVisualizer; }

    @SuppressWarnings("deprecation")
    private String getPluginVersion() {
        return getDescription().getVersion();
    }
}
