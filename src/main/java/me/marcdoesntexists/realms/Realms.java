package me.marcdoesntexists.realms;

import me.marcdoesntexists.realms.commands.*;
import me.marcdoesntexists.realms.economy.EconomyService;
import me.marcdoesntexists.realms.gui.RealmsGUI;
import me.marcdoesntexists.realms.integrations.EconomyHook;
import me.marcdoesntexists.realms.listeners.*;
import me.marcdoesntexists.realms.managers.*;
import me.marcdoesntexists.realms.military.WarfareService;
import me.marcdoesntexists.realms.societies.DiplomacyService;
import me.marcdoesntexists.realms.societies.FeudalService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

public final class Realms extends JavaPlugin {
    private static Realms instance;
    private ConfigurationManager configurationManager;
    private DataManager dataManager;
    private SocietiesManager societiesManager;
    private EconomyManager economyManager;
    private MilitaryManager militaryManager;
    private ClaimVisualizer claimVisualizer;
    private ClaimManager claimManager;
    private HybridClaimManager hybridClaimManager;
    private EconomyService economyService;
    private WarfareService warfareService;
    private DiplomacyService diplomacyService;
    private FeudalService feudalService;
    // religion service removed
    private SettlementEvolutionManager evolutionManager;
    private ExemptionManager exemptionManager;

    // Reflection-based economy providers
    private Object vaultEconomyProvider = null; // instance of net.milkbowl.vault.economy.Economy if available

    private Class<?> essentialsEconomyClass = null; // com.earth2me.essentials.Economy if available

    private boolean vaultAvailable = false;
    private boolean essentialsAvailable = false;

    public static Realms getInstance() {
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

        // Esegui migrazione automatica dei config PRIMA di inizializzare i manager
        try {
            ConfigMigrationManager migrationManager = new ConfigMigrationManager(this);
            migrationManager.migrateAllConfigs();
        } catch (Exception e) {
            getLogger().warning("Errore durante la migrazione automatica dei config: " + e.getMessage());
        }

        initializeConfigurations();
        initializeManagers();

        initializeServices();
        registerEventListeners();
        registerCommands();

        // Register PlaceholderAPI expansion if PlaceholderAPI is present
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                Plugin placeholderPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
                if (placeholderPlugin != null) {
                    // Register the expansion for Realms (expansion will access Realms.getInstance())
                    new me.marcdoesntexists.realms.placeholder.RealmsPlaceholderExpansion().register();
                    getLogger().info("Registered PlaceholderAPI expansion for Realms.");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
            }
        }

        loadData();

        startAutoSaveTasks();

        // Cleanup any residual visualizer entities from previous crashes or plugin runs
        try {
            boolean doCleanup = getConfig().getBoolean("visualizer.cleanup-on-enable", true);
            if (doCleanup) {
                NamespacedKey cleanupKey = new NamespacedKey(this, "realms_visual_marker");
                Bukkit.getWorlds().forEach(w -> w.getEntities().forEach(e -> {
                    try {
                        // ArmorStand markers created by visualizers
                        if (e instanceof org.bukkit.entity.ArmorStand) {
                            Byte val = e.getPersistentDataContainer().get(cleanupKey, org.bukkit.persistence.PersistentDataType.BYTE);
                            if (val != null && val == (byte) 1) e.remove();
                        }
                        // BlockDisplay / custom displays: try by class name to keep compatibility
                        else if (e.getClass().getSimpleName().equals("BlockDisplay")) {
                            // best-effort: remove any BlockDisplay that contains our key in PDC
                            try {
                                Byte val = e.getPersistentDataContainer().get(cleanupKey, org.bukkit.persistence.PersistentDataType.BYTE);
                                if (val != null && val == (byte) 1) e.remove();
                            } catch (Throwable ignored) {
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }));

                // If claimVisualizer exists, ask it to stop all visualizations and remove any tracked entities
                try {
                    if (claimVisualizer != null) claimVisualizer.stopAll();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to run visualizer cleanup on enable: " + t.getMessage());
        }

        getLogger().info("Realms plugin has been enabled!");
        getLogger().info("Version: " + getPluginVersion());
        getLogger().info("All services initialized and running!");
    }

    private void initializeConfigurations() {
        try {
            configurationManager = ConfigurationManager.getInstance(this);
            getLogger().info("Configuration system initialized");

            // Initialize MessageUtils with the plugin instance
            me.marcdoesntexists.realms.utils.MessageUtils.init(this);
            getLogger().info("Message system initialized");
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
            // Initialize claim visualizer depending on whether ProtocolLib is installed
            if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
                claimVisualizer = new ProtocolLibClaimVisualizer(this);
                getLogger().info("ProtocolLib detected: using ProtocolLibClaimVisualizer for showclaims");
            } else {
                claimVisualizer = new ParticleClaimVisualizer(this);
                getLogger().info("ProtocolLib not found: using ParticleClaimVisualizer as fallback for showclaims");
            }
            militaryManager = MilitaryManager.getInstance();
            claimManager = ClaimManager.getInstance(this);
            hybridClaimManager = HybridClaimManager.getInstance(this);

            // initialize PvP manager monitor
            me.marcdoesntexists.realms.managers.PvPManager.getInstance().init(this);

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
            warfareService = WarfareService.getInstance(this);
            diplomacyService = DiplomacyService.getInstance(this);
            feudalService = FeudalService.getInstance(this);
            // religion service removed
            evolutionManager = SettlementEvolutionManager.getInstance(this);

            getLogger().info("All services initialized successfully");
            getLogger().info("✓ Economy Service - Active");
            getLogger().info("✓ Warfare Service - Active");
            getLogger().info("✓ Diplomacy Service - Active");
            getLogger().info("✓ Feudal Service - Active");
            // religion service removed
            getLogger().info("✓ Settlement Evolution Manager - Active");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize services!");
            getLogger().log(Level.SEVERE, "Error initializing services", e);
        }
    }

    private void registerEventListeners() {
        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new PlayerDataSyncListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new MoveListener(this), this);
        pm.registerEvents(new PlayerNotificationListener(this), this);
        pm.registerEvents(new RealmsGUI(), this);
        pm.registerEvents(new LeaderboardClickListener(this), this);
        pm.registerEvents(new EntityListener(this), this);
        pm.registerEvents(new InteractListener(this), this);
        // Crime system listeners disabled
        pm.registerEvents(new DuelProtectionListener(this), this);
        // Liquid placement/flow protections (bucket, dispenser, flow) - chunk-based with admin bypass
        pm.registerEvents(new LiquidPlaceListener(claimManager), this);
        // Prevent endermen from stealing/placing blocks inside claims
        pm.registerEvents(new EndermanPickupListener(this), this);

        getLogger().info("Event listeners registered");
    }

    private void registerCommands() {
        // Create a single instance per command and register it as both executor and tab completer
        TownCommand townCommand = new TownCommand(this);
        safeRegisterCommand("town", townCommand, townCommand);

        KingdomCommand kingdomCommand = new KingdomCommand(this);
        safeRegisterCommand("kingdom", kingdomCommand, kingdomCommand);

        // Empire command
        me.marcdoesntexists.realms.commands.EmpireCommand empireCommand = new me.marcdoesntexists.realms.commands.EmpireCommand(this);
        safeRegisterCommand("empire", empireCommand, empireCommand);

        WarCommand warCommand = new WarCommand(this);
        safeRegisterCommand("war", warCommand, warCommand);

        ShowClaimsCommand showClaimsCommand = new ShowClaimsCommand(this);
        safeRegisterCommand("show", showClaimsCommand, showClaimsCommand);

        JobCommand jobCommand = new JobCommand(this);
        safeRegisterCommand("job", jobCommand, jobCommand);

        AllianceCommand allianceCommand = new AllianceCommand(this);
        safeRegisterCommand("alliance", allianceCommand, allianceCommand);

        // God/religion commands removed

        TreatyCommand treatyCommand = new TreatyCommand(this);
        safeRegisterCommand("treaty", treatyCommand, treatyCommand);

        FeudalCommand feudalCommand = new FeudalCommand(this);
        safeRegisterCommand("feudal", feudalCommand, feudalCommand);

        EvolveCommand evolveCommand = new EvolveCommand(this);
        safeRegisterCommand("evolve", evolveCommand, evolveCommand);

        // Explicitly register additional commands present in the commands package
        MilitaryCommand militaryCommand = new MilitaryCommand(this);
        safeRegisterCommand("military", militaryCommand, militaryCommand);

        NobleCommand nobleCommand = new NobleCommand(this);
        safeRegisterCommand("noble", nobleCommand, nobleCommand);

        DuelCommand duelCommand = new DuelCommand(this);
        safeRegisterCommand("duel", duelCommand, duelCommand);

        // Justice admin command (criminal info)
        // justice command disabled

        // Leaderboard command
        me.marcdoesntexists.realms.commands.LeaderboardCommand leaderboardCommand = new me.marcdoesntexists.realms.commands.LeaderboardCommand(this);
        safeRegisterCommand("leaderboard", leaderboardCommand, leaderboardCommand);

        // Save check admin command
        SaveCheckCommand saveCheckCommand = new SaveCheckCommand(this);
        safeRegisterCommand("savecheck", saveCheckCommand, null);

        // Diagnostic save test command
        me.marcdoesntexists.realms.commands.TestSaveCommand testSaveCommand = new me.marcdoesntexists.realms.commands.TestSaveCommand(this);
        safeRegisterCommand("testsave", testSaveCommand, null);

        // Realms GUI command
        me.marcdoesntexists.realms.commands.RealmsGUICommand RealmsGuiCommand = new me.marcdoesntexists.realms.commands.RealmsGUICommand(this);
        safeRegisterCommand("realms", RealmsGuiCommand, RealmsGuiCommand);

        // Claim transfer (GriefPrevention -> Realms) command
        me.marcdoesntexists.realms.commands.ClaimTransferCommand claimTransferCommand = new me.marcdoesntexists.realms.commands.ClaimTransferCommand(this);
        safeRegisterCommand("claimtransfer", claimTransferCommand, claimTransferCommand);

        // Save-exempt command
        me.marcdoesntexists.realms.commands.SaveExemptCommand saveExemptCommand = new me.marcdoesntexists.realms.commands.SaveExemptCommand(this);
        safeRegisterCommand("saveexempt", saveExemptCommand, null);

        // Chat channel command
        me.marcdoesntexists.realms.commands.ChannelCommand channelCommand = new me.marcdoesntexists.realms.commands.ChannelCommand(this);
        safeRegisterCommand("channel", channelCommand, channelCommand);

        // Teleport to your town/kingdom/empire
        me.marcdoesntexists.realms.commands.TpMyCommand tpMyCommand = new me.marcdoesntexists.realms.commands.TpMyCommand(this);
        safeRegisterCommand("tpmy", tpMyCommand, tpMyCommand);

        // Config migration manager command
        me.marcdoesntexists.realms.commands.ConfigMigrateCommand configMigrateCommand = new me.marcdoesntexists.realms.commands.ConfigMigrateCommand(this);
        safeRegisterCommand("configmigrate", configMigrateCommand, configMigrateCommand);

        // register commands
        // register allowgrief only if declared in plugin.yml
        {
            org.bukkit.command.PluginCommand allowGriefCmd = getCommand("allowgrief");
            if (allowGriefCmd != null) {
                me.marcdoesntexists.realms.commands.AllowGriefCommand agc = new me.marcdoesntexists.realms.commands.AllowGriefCommand(this);
                allowGriefCmd.setExecutor(agc);
                allowGriefCmd.setTabCompleter(agc);
            }
        }

        // Auto-register remaining commands declared in plugin.yml but not registered above
        autoRegisterRemainingCommands();

        getLogger().info("Commands registered");
    }

    // Auto-register commands declared in plugin.yml but not explicitly registered
    private void autoRegisterRemainingCommands() {
        try {
            for (String cmdName : getDescription().getCommands().keySet()) {
                PluginCommand pc = getCommand(cmdName);
            }
        } catch (Throwable t) {
            getLogger().warning("Error while auto-registering commands: " + t.getMessage());
        }
    }

    private String toCommandClassName(String cmdName) {
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : cmdName.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                cap = true;
                continue;
            }
            if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else sb.append(c);
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
                    } catch (NoSuchMethodException ignored1) {
                    }
                } catch (NoSuchMethodException ignored2) {
                    Method m = vaultEconomyProvider.getClass().getMethod("depositPlayer", String.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player.getName(), amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignored3) {
                    }
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
                    } catch (NoSuchMethodException ignored5) {
                    }
                } catch (NoSuchMethodException ignored6) {
                    Method m = vaultEconomyProvider.getClass().getMethod("withdrawPlayer", String.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player.getName(), amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignored7) {
                    }
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
        // Ensure claim visualizer stops and cleans up entities/tasks
        try {
            if (claimVisualizer != null) claimVisualizer.stopAll();
        } catch (Throwable ignored) {
        }

        if (dataManager != null) {
            getLogger().info("Saving all data before shutdown...");
            dataManager.saveAllData();
            getLogger().info("All data saved successfully!");
        }

        // stop any active claim visualizations
        if (claimVisualizer != null) {
            claimVisualizer.stopAll();
        }


        getLogger().info("Realms plugin has been disabled!");
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public ClaimManager getClaimManager() {
        return this.claimManager;
    }

    public HybridClaimManager getHybridClaimManager() {
        return this.hybridClaimManager;
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    public SocietiesManager getSocietiesManager() {
        return this.societiesManager;
    }

    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    public me.marcdoesntexists.realms.managers.LawManager getLawManager() {
        throw new UnsupportedOperationException("LawManager has been removed from this build");
    }

    public MilitaryManager getMilitaryManager() {
        return this.militaryManager;
    }

    public ClaimVisualizer getClaimVisualizer() {
        return this.claimVisualizer;
    }

    public EconomyService getEconomyService() {
        return this.economyService;
    }


    public WarfareService getWarfareService() {
        return this.warfareService;
    }

    public DiplomacyService getDiplomacyService() {
        return this.diplomacyService;
    }

    public FeudalService getFeudalService() {
        return this.feudalService;
    }

    public SettlementEvolutionManager getEvolutionManager() {
        return this.evolutionManager;
    }

    public ExemptionManager getExemptionManager() {
        return this.exemptionManager;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        configurationManager.reloadAllConfigurations();
    }

    public String getPluginVersion() {
        return getDescription().getVersion();
    }

    public String getPluginName() {
        return getDescription().getName();
    }

    public void log(Level level, String message) {
        getLogger().log(level, message);
    }

    public void log(String message) {
        getLogger().info(message);
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
