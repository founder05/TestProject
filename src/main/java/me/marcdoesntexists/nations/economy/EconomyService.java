package me.marcdoesntexists.nations.economy;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.JobType;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import me.marcdoesntexists.nations.managers.EconomyManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Empire;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class EconomyService {
    private static EconomyService instance;
    private final Nations plugin;
    private final EconomyManager economyManager;
    private final SocietiesManager societiesManager;
    private final ConfigurationManager configManager;

    // External economy hooks
    private Economy vaultEconomy = null;
    private Plugin essentialsPlugin = null;

    private Method essentialsGetUserMethod = null; // reflection entry to get user
    private Method essentialsGetMoneyMethod = null; // user.getMoney()
    private Method essentialsAddMoneyMethod = null; // user.add(amount) or user.deposit
    private Method essentialsTakeMoneyMethod = null; // user.take(amount) or user.withdraw

    private EconomyService(Nations plugin) {
        this.plugin = plugin;
        this.economyManager = EconomyManager.getInstance();
        this.societiesManager = SocietiesManager.getInstance();
        this.configManager = ConfigurationManager.getInstance();

        // Try to hook external economy providers
        setupVaultHook();
        setupEssentialsHook();

        startAutoTaxCollection();
        startAutoSalaryPayment();

        // keep internal cached player balances in sync with external providers
        startBalanceSyncTask();
    }

    public static EconomyService getInstance(Nations plugin) {
        if (instance == null) {
            instance = new EconomyService(plugin);
        }
        return instance;
    }

    public static EconomyService getInstance() {
        return instance;
    }

    // --- Hooks setup ---
    private void setupVaultHook() {
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                vaultEconomy = rsp.getProvider();
                plugin.getLogger().info("Vault economy provider hooked: " + vaultEconomy.getName());
            } else {
                plugin.getLogger().info("Vault economy provider not found");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error while hooking Vault economy: " + e.getMessage());
        }
    }

    private void setupEssentialsHook() {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin("Essentials");
            if (p == null) p = Bukkit.getPluginManager().getPlugin("EssentialsX");
            if (p != null) {
                essentialsPlugin = p;
                plugin.getLogger().info("Found Essentials plugin: " + p.getName());

                // try to find common API methods via reflection
                // getUser(String name) or getUser(org.bukkit.entity.Player)
                Method getUser = null;
                try {
                    getUser = p.getClass().getMethod("getUser", String.class);
                } catch (NoSuchMethodException ignored) {
                }
                if (getUser == null) {
                    try {
                        getUser = p.getClass().getMethod("getUser", org.bukkit.entity.Player.class);
                    } catch (NoSuchMethodException ignored) {
                    }
                }

                essentialsGetUserMethod = getUser;

                // For User object, try to find common money methods
                if (getUser != null) {
                    // create a dummy user by calling with plugin's console name (may return null) - instead try to inspect return type
                    Class<?> userClass = getUser.getReturnType();
                    if (!userClass.equals(Void.TYPE)) {
                        // possible methods: getMoney(), getBalance(), getMoneyInt(), addMoney(double), setMoney(double), takeMoney(double)
                        for (String mn : new String[]{"getMoney", "getBalance", "getWorth"}) {
                            try {
                                essentialsGetMoneyMethod = userClass.getMethod(mn);
                                break;
                            } catch (NoSuchMethodException ignored) {
                            }
                        }

                        for (String mn : new String[]{"add", "addMoney", "deposit", "setMoney", "giveMoney"}) {
                            try {
                                essentialsAddMoneyMethod = userClass.getMethod(mn, double.class);
                                break;
                            } catch (NoSuchMethodException ignored) {
                            }
                        }

                        for (String mn : new String[]{"take", "takeMoney", "withdraw", "removeMoney", "subtractMoney"}) {
                            try {
                                essentialsTakeMoneyMethod = userClass.getMethod(mn, double.class);
                                break;
                            } catch (NoSuchMethodException ignored) {
                            }
                        }
                    }
                }

                plugin.getLogger().info("Essentials reflection hooks prepared (may be partial)");
            } else {
                plugin.getLogger().info("Essentials plugin not found");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error while preparing Essentials reflection: " + e.getMessage());
        }
    }

    // --- General economy API used by the plugin ---
    public double getPlayerBalance(UUID playerId) {
        try {
            if (vaultEconomy != null) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(playerId);
                return vaultEconomy.getBalance(off);
            }

            if (essentialsPlugin != null && essentialsGetUserMethod != null && essentialsGetMoneyMethod != null) {
                Object user = invokeEssentialsGetUser(playerId);
                if (user != null) {
                    Object val = essentialsGetMoneyMethod.invoke(user);
                    if (val instanceof Number) return ((Number) val).doubleValue();
                }
            }

            // fallback to internal stored integer money
            return plugin.getDataManager().getPlayerData(playerId).getMoney();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get balance for " + playerId + " using external providers: " + e.getMessage());
            return plugin.getDataManager().getPlayerData(playerId).getMoney();
        }
    }

    public boolean hasFunds(UUID playerId, double amount) {
        if (amount <= 0) return true;
        try {
            if (vaultEconomy != null) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(playerId);
                return vaultEconomy.has(off, amount);
            }
            if (essentialsPlugin != null && essentialsGetUserMethod != null && essentialsGetMoneyMethod != null) {
                Object user = invokeEssentialsGetUser(playerId);
                if (user != null) {
                    Object val = essentialsGetMoneyMethod.invoke(user);
                    if (val instanceof Number) return ((Number) val).doubleValue() >= amount;
                }
            }
            return plugin.getDataManager().getPlayerData(playerId).getMoney() >= amount;
        } catch (Exception e) {
            plugin.getLogger().warning("hasFunds check failed: " + e.getMessage());
            return plugin.getDataManager().getPlayerData(playerId).getMoney() >= amount;
        }
    }

    public boolean withdrawFromPlayer(UUID playerId, double amount) {
        if (amount <= 0) return true;
        try {
            if (vaultEconomy != null) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(playerId);
                net.milkbowl.vault.economy.EconomyResponse resp = vaultEconomy.withdrawPlayer(off, amount);
                if (resp != null && resp.transactionSuccess()) {
                    Transaction transaction = new Transaction(playerId, -amount, "External Withdraw");
                    economyManager.recordTransaction(transaction);
                    return true;
                }
                return false;
            }
            if (essentialsPlugin != null && essentialsGetUserMethod != null && essentialsTakeMoneyMethod != null) {
                Object user = invokeEssentialsGetUser(playerId);
                if (user != null) {
                    Object res = essentialsTakeMoneyMethod.invoke(user, amount);
                    // interpret return values: null (assume success), Boolean true, Number
                    if (res == null) {
                        Transaction transaction = new Transaction(playerId, -amount, "Essentials Withdraw (unknown result)");
                        economyManager.recordTransaction(transaction);
                        return true; // assume success
                    }
                    if (res instanceof Boolean) {
                        if ((Boolean) res) {
                            Transaction transaction = new Transaction(playerId, -amount, "Essentials Withdraw");
                            economyManager.recordTransaction(transaction);
                            return true;
                        }
                        return false;
                    }
                    if (res instanceof Number) {
                        Transaction transaction = new Transaction(playerId, -amount, "Essentials Withdraw");
                        economyManager.recordTransaction(transaction);
                        return true;
                    }
                }
            }

            // fallback to internal economy
            boolean ok = plugin.getDataManager().getPlayerData(playerId).removeMoney((int) Math.ceil(amount));
            if (ok) {
                Transaction transaction = new Transaction(playerId, -amount, "Internal Withdraw");
                economyManager.recordTransaction(transaction);
                try {
                    plugin.getDataManager().savePlayerMoney(playerId);
                } catch (Throwable ignored) {
                }
                return true;
            }
            return false;
        } catch (InvocationTargetException ite) {
            plugin.getLogger().warning("Error withdrawing via essentials: " + ite.getCause());
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("withdrawFromPlayer failed: " + e.getMessage());
            return false;
        }
    }

    public boolean depositToPlayer(UUID playerId, double amount) {
        if (amount <= 0) return true;
        try {
            if (vaultEconomy != null) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(playerId);
                net.milkbowl.vault.economy.EconomyResponse resp = vaultEconomy.depositPlayer(off, amount);
                if (resp != null && resp.transactionSuccess()) {
                    Transaction transaction = new Transaction(playerId, amount, "External Deposit");
                    economyManager.recordTransaction(transaction);
                    return true;
                }
                return false;
            }
            if (essentialsPlugin != null && essentialsGetUserMethod != null && essentialsAddMoneyMethod != null) {
                Object user = invokeEssentialsGetUser(playerId);
                if (user != null) {
                    Object res = essentialsAddMoneyMethod.invoke(user, amount);
                    if (res == null) {
                        Transaction transaction = new Transaction(playerId, amount, "Essentials Deposit (unknown result)");
                        economyManager.recordTransaction(transaction);
                        return true;
                    }
                    if (res instanceof Boolean) {
                        if ((Boolean) res) {
                            Transaction transaction = new Transaction(playerId, amount, "Essentials Deposit");
                            economyManager.recordTransaction(transaction);
                            return true;
                        }
                        return false;
                    }
                    if (res instanceof Number) {
                        Transaction transaction = new Transaction(playerId, amount, "Essentials Deposit");
                        economyManager.recordTransaction(transaction);
                        return true;
                    }
                }
            }

            // fallback to internal economy
            plugin.getDataManager().getPlayerData(playerId).addMoney((int) Math.floor(amount));
            Transaction transaction = new Transaction(playerId, amount, "Internal Deposit");
            economyManager.recordTransaction(transaction);
            try {
                plugin.getDataManager().savePlayerMoney(playerId);
            } catch (Throwable ignored) {
            }
            return true;
        } catch (InvocationTargetException ite) {
            plugin.getLogger().warning("Error depositing via essentials: " + ite.getCause());
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("depositToPlayer failed: " + e.getMessage());
            return false;
        }
    }

    private Object invokeEssentialsGetUser(UUID playerId) {
        try {
            if (essentialsGetUserMethod == null) return null;
            // try Player first
            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(playerId);
            try {
                // if method accepts Player
                return essentialsGetUserMethod.invoke(essentialsPlugin, off);
            } catch (IllegalArgumentException ignored) {
            }

            // try by name
            if (off.getName() != null) {
                return essentialsGetUserMethod.invoke(essentialsPlugin, off.getName());
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().fine("invokeEssentialsGetUser failed: " + e.getMessage());
            return null;
        }
    }

    // --- Existing API previously implemented (kept but updated to use above helpers) ---
    public void addMoneyToPlayer(UUID playerId, double amount) {
        depositToPlayer(playerId, amount);
    }

    public void removeMoneyFromPlayer(UUID playerId, double amount) {
        withdrawFromPlayer(playerId, amount);
    }

    public void payJob(UUID playerId, JobType jobType) {
        double salary = jobType.getBaseSalary();
        Transaction transaction = new Transaction(playerId, salary, "Salary: " + jobType.getDisplayName());
        economyManager.recordTransaction(transaction);
        // actually deposit
        depositToPlayer(playerId, salary);
    }

    public void collectTownsRecipe(Town town) {
        FileConfiguration config = configManager.getEconomyConfig();
        double taxRate = config.getDouble("taxes.town.base-rate", 0.10);
        double taxAmount = town.getBalance() * taxRate;

        if (taxAmount > 0) {
            town.removeMoney((int) taxAmount);
            Transaction transaction = new Transaction(null, taxAmount, "Town Tax from " + town.getName());
            economyManager.recordTransaction(transaction);
            try {
                plugin.getDataManager().saveTown(town);
            } catch (Throwable ignored) {
            }
        }
    }

    public void collectKingdomTaxes(Kingdom kingdom) {
        FileConfiguration config = configManager.getEconomyConfig();
        double kingdomTaxRate = config.getDouble("taxes.kingdom.base-rate", 0.15);

        double totalTax = 0;
        for (String townName : kingdom.getTowns()) {
            Town town = societiesManager.getTown(townName);
            if (town != null) {
                double townTax = town.getBalance() * kingdomTaxRate;
                totalTax += townTax;
                town.removeMoney((int) townTax);
                try {
                    plugin.getDataManager().saveTown(town);
                } catch (Throwable ignored) {
                }
            }
        }

        if (totalTax > 0) {
            Transaction transaction = new Transaction(null, totalTax, "Kingdom Tax from " + kingdom.getName());
            economyManager.recordTransaction(transaction);
        }
    }

    public void collectEmpireTaxes(Empire empire) {
        FileConfiguration config = configManager.getEconomyConfig();
        double empireTaxRate = config.getDouble("taxes.empire.base-rate", 0.20);

        double totalTax = 0;
        for (String kingdomName : empire.getKingdoms()) {
            Kingdom kingdom = societiesManager.getKingdom(kingdomName);
            if (kingdom != null) {
                for (String townName : kingdom.getTowns()) {
                    Town town = societiesManager.getTown(townName);
                    if (town != null) {
                        double townTax = town.getBalance() * empireTaxRate;
                        totalTax += townTax;
                        town.removeMoney((int) townTax);
                        try {
                            plugin.getDataManager().saveTown(town);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        }

        if (totalTax > 0) {
            Transaction transaction = new Transaction(null, totalTax, "Empire Tax from " + empire.getName());
            economyManager.recordTransaction(transaction);
        }
    }

    private void startAutoTaxCollection() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Town town : societiesManager.getAllTowns()) {
                        collectTownsRecipe(town);
                    }
                    for (Kingdom kingdom : societiesManager.getAllKingdoms()) {
                        collectKingdomTaxes(kingdom);
                    }
                    for (Empire empire : societiesManager.getAllEmpires()) {
                        collectEmpireTaxes(empire);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during auto tax collection: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 20L * 60 * 60, 20L * 60 * 60);
    }

    private void startAutoSalaryPayment() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Job job : economyManager.getAllJobs()) {
                        if (job.isActive()) {
                            payJob(job.getPlayerId(), job.getJobType());
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during auto salary payment: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 20L * 60 * 60, 20L * 60 * 60);
    }

    private void startBalanceSyncTask() {
        FileConfiguration econCfg = configManager.getEconomyConfig();
        int intervalSec = econCfg != null ? econCfg.getInt("balance-sync-interval", 10) : 10;
        long ticks = Math.max(1L, intervalSec) * 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        try {
                            UUID id = p.getUniqueId();
                            double external = getPlayerBalance(id);
                            if (vaultEconomy != null || essentialsPlugin != null) {
                                PlayerData pd = plugin.getDataManager().getPlayerData(id);
                                int newMoney = (int) Math.floor(external);
                                if (pd.getMoney() != newMoney) {
                                    pd.setMoney(newMoney);
                                    plugin.getLogger().fine("Synced money for " + p.getName() + " -> " + newMoney);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().fine("Balance sync iteration failed: " + t.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, ticks);
    }

    // Called externally to persist player money for all online players (partial save)
    public void persistOnlinePlayersMoney() {
        try {
            FileConfiguration econCfg = configManager.getEconomyConfig();
            boolean enabled = econCfg == null || econCfg.getBoolean("partial-save-enabled", true);
            if (!enabled) return; // partial saves disabled

            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    // skip if player has permission or is listed in exemption manager
                    if (p.hasPermission("nations.save.exempt")) continue;
                    if (plugin.getExemptionManager() != null && plugin.getExemptionManager().isExempt(p.getUniqueId()))
                        continue;

                    plugin.getDataManager().savePlayerMoney(p.getUniqueId());
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to persist money for " + p.getName() + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("persistOnlinePlayersMoney failed: " + e.getMessage());
        }
    }

    // Called externally to persist societies (towns/kingdoms/...) periodically
    public void persistSocieties() {
        try {
            plugin.getDataManager().saveAllSocieties();
        } catch (Exception e) {
            plugin.getLogger().warning("persistSocieties failed: " + e.getMessage());
        }
    }

    public double getTownTreasury(String townName) {
        Town town = societiesManager.getTown(townName);
        if (town != null) {
            return town.getBalance();
        }
        return 0;
    }

    public void addToTownTreasury(String townName, int amount) {
        Town town = societiesManager.getTown(townName);
        if (town != null) {
            town.addMoney(amount);
            try {
                plugin.getDataManager().saveTown(town);
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean removeFromTownTreasury(String townName, int amount) {
        Town town = societiesManager.getTown(townName);
        if (town != null) {
            boolean ok = town.removeMoney(amount);
            if (ok) {
                try {
                    plugin.getDataManager().saveTown(town);
                } catch (Throwable ignored) {
                }
            }
            return ok;
        }
        return false;
    }

    // Transfer from player's external account to town treasury atomically.
    // Returns true on success (player charged and town credited), false on failure.
    public boolean transferPlayerToTown(UUID playerId, String townName, int amount) {
        if (amount <= 0) return true;
        try {
            Town town = SocietiesManager.getInstance().getTown(townName);
            if (town == null) return false;

            // Try to withdraw from player's external account first
            boolean withdrawn = withdrawFromPlayer(playerId, amount);
            if (!withdrawn) return false;

            // Credit town
            town.addMoney(amount);
            Transaction tx = new Transaction(playerId, amount, "Player->Town: " + townName);
            EconomyManager.getInstance().recordTransaction(tx);

            // Persist both
            try {
                plugin.getDataManager().saveTown(town);
            } catch (Throwable ignored) {
            }
            try {
                plugin.getDataManager().savePlayerMoney(playerId);
            } catch (Throwable ignored) {
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("transferPlayerToTown failed: " + e.getMessage());
            return false;
        }
    }

    // Transfer from town treasury to player's external account atomically.
    // Returns true on success (town debited and player credited), false on failure.
    public boolean transferTownToPlayer(String townName, UUID playerId, int amount) {
        if (amount <= 0) return true;
        try {
            Town town = SocietiesManager.getInstance().getTown(townName);
            if (town == null) return false;

            if (!town.removeMoney(amount)) return false;

            boolean deposited = depositToPlayer(playerId, amount);
            if (!deposited) {
                // rollback town debit
                town.addMoney(amount);
                return false;
            }

            Transaction tx = new Transaction(playerId, amount, "Town->Player: " + townName);
            EconomyManager.getInstance().recordTransaction(tx);

            // Persist both
            try {
                plugin.getDataManager().saveTown(town);
            } catch (Throwable ignored) {
            }
            try {
                plugin.getDataManager().savePlayerMoney(playerId);
            } catch (Throwable ignored) {
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("transferTownToPlayer failed: " + e.getMessage());
            return false;
        }
    }
}
