package me.marcdoesntexists.nations.integrations;

import me.marcdoesntexists.nations.Nations;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class EconomyHook {
    private final Nations plugin;

    private Object vaultEconomyProvider = null;
    private Class<?> vaultEconomyClass = null;

    private Class<?> essentialsEconomyClass = null;

    private boolean vaultAvailable = false;
    private boolean essentialsAvailable = false;

    public EconomyHook(Nations plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        // Try Vault first via ServicesManager and reflection
        try {
            try {
                vaultEconomyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                Object registration = plugin.getServer().getServicesManager().getRegistration(vaultEconomyClass);
                if (registration != null) {
                    Method getProvider = registration.getClass().getMethod("getProvider");
                    vaultEconomyProvider = getProvider.invoke(registration);
                    if (vaultEconomyProvider != null) {
                        vaultAvailable = true;
                        plugin.getLogger().info("Hooked into Vault economy provider: " + vaultEconomyProvider.getClass().getName());
                    }
                } else {
                    plugin.getLogger().info("Vault API found on classpath but no provider registered via ServicesManager.");
                }
            } catch (ClassNotFoundException cnfe) {
                plugin.getLogger().info("Vault API not present on the server (class not found).");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Unexpected error while detecting Vault: " + t.getMessage());
        }

        // Try Essentials/EseentialsX economy API (best-effort, via reflection)
        try {
            if (Bukkit.getPluginManager().getPlugin("Essentials") != null || Bukkit.getPluginManager().getPlugin("EssentialsX") != null) {
                try {
                    essentialsEconomyClass = Class.forName("com.earth2me.essentials.Economy");
                    essentialsAvailable = true;
                    plugin.getLogger().info("Detected Essentials Economy API (will attempt to use it as a fallback).");
                } catch (ClassNotFoundException e) {
                    plugin.getLogger().info("Essentials plugin found but Economy API class not available via reflection.");
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Unexpected error while detecting Essentials: " + t.getMessage());
        }

        if (!vaultAvailable && !essentialsAvailable) {
            plugin.getLogger().info("No economy integration found (Vault/Essentials). Economy features will be limited.");
        }
    }

    public boolean hasEconomy() {
        return vaultAvailable || essentialsAvailable;
    }

    public double getBalance(OfflinePlayer player) {
        if (vaultAvailable && vaultEconomyProvider != null) {
            try {
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
                plugin.getLogger().warning("Failed to get balance via Vault provider: " + t.getMessage());
            }
        }

        if (essentialsAvailable && essentialsEconomyClass != null) {
            try {
                try {
                    Method m = essentialsEconomyClass.getMethod("getMoney", String.class);
                    Object res = m.invoke(null, player.getName());
                    if (res instanceof Number) return ((Number) res).doubleValue();
                } catch (NoSuchMethodException ignored) {
                    Method m = essentialsEconomyClass.getMethod("getBalance", String.class);
                    Object res = m.invoke(null, player.getName());
                    if (res instanceof Number) return ((Number) res).doubleValue();
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to get balance via Essentials API: " + t.getMessage());
            }
        }

        return 0.0d;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return false;

        if (vaultAvailable && vaultEconomyProvider != null) {
            try {
                try {
                    Method m = vaultEconomyProvider.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player, amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignoredA) { }
                } catch (NoSuchMethodException ignoredB) {
                    Method m = vaultEconomyProvider.getClass().getMethod("depositPlayer", String.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player.getName(), amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignoredC) { }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to deposit via Vault provider: " + t.getMessage());
            }
        }

        if (essentialsAvailable && essentialsEconomyClass != null) {
            try {
                try {
                    Method m = essentialsEconomyClass.getMethod("give", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                } catch (NoSuchMethodException ignoredD) {
                    Method m = essentialsEconomyClass.getMethod("add", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to deposit via Essentials API: " + t.getMessage());
            }
        }

        return false;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) return false;

        if (vaultAvailable && vaultEconomyProvider != null) {
            try {
                try {
                    Method m = vaultEconomyProvider.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player, amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignoredE) { }
                } catch (NoSuchMethodException ignoredF) {
                    Method m = vaultEconomyProvider.getClass().getMethod("withdrawPlayer", String.class, double.class);
                    Object res = m.invoke(vaultEconomyProvider, player.getName(), amount);
                    if (res instanceof Boolean) return (Boolean) res;
                    try {
                        Method success = res.getClass().getMethod("transactionSuccess");
                        Object ok = success.invoke(res);
                        if (ok instanceof Boolean) return (Boolean) ok;
                    } catch (NoSuchMethodException ignoredG) { }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to withdraw via Vault provider: " + t.getMessage());
            }
        }

        if (essentialsAvailable && essentialsEconomyClass != null) {
            try {
                try {
                    Method m = essentialsEconomyClass.getMethod("take", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                } catch (NoSuchMethodException ignoredH) {
                    Method m = essentialsEconomyClass.getMethod("remove", String.class, double.class);
                    m.invoke(null, player.getName(), amount);
                    return true;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to withdraw via Essentials API: " + t.getMessage());
            }
        }

        return false;
    }
}
