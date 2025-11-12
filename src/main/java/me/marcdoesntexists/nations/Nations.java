package me.marcdoesntexists.nations;

import me.marcdoesntexists.nations.commands.*;
import me.marcdoesntexists.nations.economy.EconomyService;
import me.marcdoesntexists.nations.gui.NationsGUI;
import me.marcdoesntexists.nations.integrations.EconomyHook;
import me.marcdoesntexists.nations.integrations.GriefPreventionIntegration;
import me.marcdoesntexists.nations.law.JusticeService;
import me.marcdoesntexists.nations.listeners.*;
import me.marcdoesntexists.nations.managers.*;
import me.marcdoesntexists.nations.military.WarfareService;
import me.marcdoesntexists.nations.societies.DiplomacyService;
import me.marcdoesntexists.nations.societies.FeudalService;
import me.marcdoesntexists.nations.societies.ReligionService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Method;
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
    private ExemptionManager exemptionManager;

    // Reflection-based economy providers
    private Object vaultEconomyProvider = null; // instance of net.milkbowl.vault.economy.Economy if available

    private Class<?> essentialsEconomyClass = null; // com.earth2me.essentials.Economy if available

    private boolean vaultAvailable = false;
    private boolean essentialsAvailable = false;

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

            // Exemption manager for partial-save exemptions
            exemptionManager = new ExemptionManager(this);

            getLogger().info("All managers initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize managers!");
            getLogger().log(Level.SEVERE, "Error initializing managers", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void initializeServices() {
        try {
            // Setup Vault / EssentialsX economy integration early so services can rely on it
            // new wrapper for economy integrations
            EconomyHook economyHook = new EconomyHook(this);
            economyHook.setup();

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
        pm.registerEvents(new NationsGUI(), this);
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
        safeRegisterCommand("show", showClaimsCommand, showClaimsCommand);

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

        // Nations GUI command
        me.marcdoesntexists.nations.commands.NationsGUICommand nationsGuiCommand = new me.marcdoesntexists.nations.commands.NationsGUICommand(this);
        safeRegisterCommand("nations", nationsGuiCommand, nationsGuiCommand);

        // Save-exempt command
        me.marcdoesntexists.nations.commands.SaveExemptCommand saveExemptCommand = new me.marcdoesntexists.nations.commands.SaveExemptCommand(this);
        safeRegisterCommand("saveexempt", saveExemptCommand, null);

        // Auto-register remaining commands declared in plugin.yml but not registered above
        autoRegisterRemainingCommands();

        getLogger().info("Commands registered");
    }

    // Auto-register commands declared in plugin.yml but not explicitly registered
    private void autoRegisterRemainingCommands() {
        try {
            for (String cmdName : getDescription().getCommands().keySet()) {
                PluginCommand pc = getCommand(cmdName);
                if (pc == null) continue; // not declared
                continue; // already registered

                // Build class name: e.g. 'treaty' -> 'TreatyCommand'
            }
        } catch (Throwable t) {
            getLogger().warning("Error while auto-registering commands: " + t.getMessage());
        }
    }

    private String toCommandClassName(String cmdName) {
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : cmdName.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) { cap = true; continue; }
            if (cap) { sb.append(Character.toUpperCase(c)); cap = false; } else sb.append(c);
        }
        sb.append("Command");
        return sb.toString();
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

    // --- Economy integration helpers (Vault + EssentialsX via reflection) ---
    private void setupEconomy() {
        // Try Vault first via ServicesManager and reflection to avoid compile-time dependency
        try {
            try {
                Class<?> vaultEconomyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                Object registration = getServer().getServicesManager().getRegistration(vaultEconomyClass);
                if (registration != null) {
                    Method getProvider = registration.getClass().getMethod("getProvider");
                    vaultEconomyProvider = getProvider.invoke(registration);
                    if (vaultEconomyProvider != null) {
                        vaultAvailable = true;
                        getLogger().info("Hooked into Vault economy provider: " + vaultEconomyProvider.getClass().getName());
                    }
                } else {
                    getLogger().info("Vault API found on classpath but no provider registered via ServicesManager.");
                }
            } catch (ClassNotFoundException cnfe) {
                // Vault not present
                getLogger().info("Vault API not present on the server (class not found).");
            }
        } catch (Throwable t) {
            getLogger().warning("Unexpected error while detecting Vault: " + t.getMessage());
        }

        // Try Essentials/EseentialsX economy API (best-effort, via reflection)
        try {
            if (Bukkit.getPluginManager().getPlugin("Essentials") != null || Bukkit.getPluginManager().getPlugin("EssentialsX") != null) {
                try {
                    essentialsEconomyClass = Class.forName("com.earth2me.essentials.Economy");
                    essentialsAvailable = true;
                    getLogger().info("Detected Essentials Economy API (will attempt to use it as a fallback).");
                } catch (ClassNotFoundException e) {
                    getLogger().info("Essentials plugin found but Economy API class not available via reflection.");
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Unexpected error while detecting Essentials: " + t.getMessage());
        }

        if (!vaultAvailable && !essentialsAvailable) {
            getLogger().info("No economy integration found (Vault/Essentials). Economy features will be limited.");
        }
    }

    public boolean hasEconomy() {
        return vaultAvailable || essentialsAvailable;
    }

    public double getBalance(OfflinePlayer player) {
        if (vaultAvailable && vaultEconomyProvider != null) {
            try {
                // try OfflinePlayer signature first
                try {
                    Method m = vaultEconomyProvider.getClass().getMethod("getBalance", OfflinePlayer.class);
                    Object res = m.invoke(vaultEconomyProvider, player);
                    if (res instanceof Number) return ((Number) res).doubleValue();
                } catch (NoSuchMethodException ignored) {
                    Method m = vaultEconomyProvider.getClass().getMethod("getBalance", String.class);
                    Object res = m.invoke(vaultEconomyProvider, player.getName());
                    if (res instanceof Number) return ((Number) res).doubleValue();
                }
            } catch (Throwable t) {
                getLogger().warning("Failed to get balance via Vault provider: " + t.getMessage());
            }
        }

        if (essentialsAvailable && essentialsEconomyClass != null) {
            try {
                // try static getMoney(String)
                try {
                    Method m = essentialsEconomyClass.getMethod("getMoney", String.class);
                    Object res = m.invoke(null, player.getName());
                    if (res instanceof Number) return ((Number) res).doubleValue();
                } catch (NoSuchMethodException ignored) {
                    // try getBalance
                    Method m = essentialsEconomyClass.getMethod("getBalance", String.class);
                    Object res = m.invoke(null, player.getName());
                    if (res instanceof Number) return ((Number) res).doubleValue();
                }
            } catch (Throwable t) {
                getLogger().warning("Failed to get balance via Essentials API: " + t.getMessage());
            }
        }

        return 0.0d;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return false;

        if (vaultAvailable && vaultEconomyProvider != null) {
            try {
                // try deposit with OfflinePlayer
                try {
                    Method m = vaultEconomyProvider.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player, amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    // Vault's depositPlayer may return EconomyResponse - check transactionSuccess()
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignored1) { }
                } catch (NoSuchMethodException ignored2) {
                    Method m = vaultEconomyProvider.getClass().getMethod("depositPlayer", String.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player.getName(), amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignored3) { }
                }
            } catch (Throwable t) {
                getLogger().warning("Failed to deposit via Vault provider: " + t.getMessage());
            }
        }

        if (essentialsAvailable && essentialsEconomyClass != null) {
            try {
                // try static method give or add
                try {
                    Method m = essentialsEconomyClass.getMethod("give", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                } catch (NoSuchMethodException ignored4) {
                    Method m = essentialsEconomyClass.getMethod("add", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                }
            } catch (Throwable t) {
                getLogger().warning("Failed to deposit via Essentials API: " + t.getMessage());
            }
        }

        return false;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) return false;

        if (vaultAvailable && vaultEconomyProvider != null) {
            try {
                // try withdraw with OfflinePlayer
                try {
                    Method m = vaultEconomyProvider.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player, amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignored5) { }
                } catch (NoSuchMethodException ignored6) {
                    Method m = vaultEconomyProvider.getClass().getMethod("withdrawPlayer", String.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player.getName(), amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignored7) { }
                }
            } catch (Throwable t) {
                getLogger().warning("Failed to withdraw via Vault provider: " + t.getMessage());
            }
        }

        if (essentialsAvailable && essentialsEconomyClass != null) {
            try {
                // try static method take or remove
                try {
                    Method m = essentialsEconomyClass.getMethod("take", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                } catch (NoSuchMethodException ignored8) {
                    Method m = essentialsEconomyClass.getMethod("remove", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                }
            } catch (Throwable t) {
                getLogger().warning("Failed to withdraw via Essentials API: " + t.getMessage());
            }
        }

        return false;
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
        // Full save every 5 minutes (legacy behavior)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                dataManager.saveAllData();
                getLogger().info("Auto-save completed");
            } catch (Exception e) {
                getLogger().severe("Auto-save failed: " + e.getMessage());
            }
        }, 6000L, 6000L);

        // Partial saves and society saves controlled by economy config
        FileConfiguration econConfig = configurationManager.getEconomyConfig();
        int partialIntervalSec = econConfig != null ? econConfig.getInt("partial-save-interval", 60) : 60;
        int societyIntervalSec = econConfig != null ? econConfig.getInt("society-save-interval", 300) : 300;

        long partialTicks = Math.max(1L, partialIntervalSec) * 20L;
        long societyTicks = Math.max(1L, societyIntervalSec) * 20L;

        // Partial save: persist only player money for online players
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (economyService != null) economyService.persistOnlinePlayersMoney();
            } catch (Exception e) {
                getLogger().warning("Partial-save (money) failed: " + e.getMessage());
            }
        }, partialTicks, partialTicks);

        // Periodic save of societies (towns/kingdoms/alliances/religions)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (economyService != null) economyService.persistSocieties();
            } catch (Exception e) {
                getLogger().warning("Society-save failed: " + e.getMessage());
            }
        }, societyTicks, societyTicks);

        getLogger().info("Auto-save task started (every 5 minutes) and partial/society saves scheduled");
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

    public ExemptionManager getExemptionManager() { return exemptionManager; }

    @SuppressWarnings("deprecation")
    private String getPluginVersion() {
        return getDescription().getVersion();
    }
}
