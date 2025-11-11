package me.marcdoesntexists.nations.economy;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.JobType;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import me.marcdoesntexists.nations.managers.EconomyManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Empire;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class EconomyService {
    private static EconomyService instance;
    private final Nations plugin;
    private final EconomyManager economyManager;
    private final SocietiesManager societiesManager;
    private final ConfigurationManager configManager;

    private EconomyService(Nations plugin) {
        this.plugin = plugin;
        this.economyManager = EconomyManager.getInstance();
        this.societiesManager = SocietiesManager.getInstance();
        this.configManager = ConfigurationManager.getInstance();
        startAutoTaxCollection();
        startAutoSalaryPayment();
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

    public void addMoneyToPlayer(UUID playerId, double amount) {
        Transaction transaction = new Transaction(playerId, amount, "Manual Addition");
        economyManager.recordTransaction(transaction);
    }

    public void removeMoneyFromPlayer(UUID playerId, double amount) {
        Transaction transaction = new Transaction(playerId, -amount, "Manual Removal");
        economyManager.recordTransaction(transaction);
    }

    public void payJob(UUID playerId, JobType jobType) {
        double salary = jobType.getBaseSalary();
        Transaction transaction = new Transaction(playerId, salary, "Salary: " + jobType.getDisplayName());
        economyManager.recordTransaction(transaction);
    }

    public void collectTownsRecipe(Town town) {
        FileConfiguration config = configManager.getEconomyConfig();
        double taxRate = config.getDouble("taxes.town.base-rate", 0.10);
        double taxAmount = town.getBalance() * taxRate;

        if (taxAmount > 0) {
            town.removeMoney((int) taxAmount);
            Transaction transaction = new Transaction(null, taxAmount, "Town Tax from " + town.getName());
            economyManager.recordTransaction(transaction);
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
        }
    }

    public boolean removeFromTownTreasury(String townName, int amount) {
        Town town = societiesManager.getTown(townName);
        if (town != null) {
            return town.removeMoney(amount);
        }
        return false;
    }
}
