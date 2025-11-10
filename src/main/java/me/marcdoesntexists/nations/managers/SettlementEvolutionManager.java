package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Empire;
import org.bukkit.configuration.file.FileConfiguration;

public class SettlementEvolutionManager {
    private static SettlementEvolutionManager instance;
    private final Nations plugin;
    private final ConfigurationManager configManager;
    private final SocietiesManager societiesManager;

    private SettlementEvolutionManager(Nations plugin) {
        this.plugin = plugin;
        this.configManager = ConfigurationManager.getInstance();
        this.societiesManager = SocietiesManager.getInstance();
    }

    public static SettlementEvolutionManager getInstance(Nations plugin) {
        if (instance == null) {
            instance = new SettlementEvolutionManager(plugin);
        }
        return instance;
    }

    public static SettlementEvolutionManager getInstance() {
        return instance;
    }

    public boolean canEvolveToKingdom(Town town) {
        FileConfiguration config = configManager.getSettlementsConfig();

        int minimumClaims = config.getInt("settlement-evolution.town-to-kingdom.minimum-claims", 100);
        int minimumPopulation = config.getInt("settlement-evolution.town-to-kingdom.minimum-population", 10);
        int minimumTreasury = config.getInt("settlement-evolution.town-to-kingdom.minimum-treasury", 50000);
        int minimumDaysOld = config.getInt("settlement-evolution.town-to-kingdom.minimum-days-old", 7);

        boolean hasClaims = town.getClaims().size() >= minimumClaims;
        boolean hasPopulation = town.getMembers().size() >= minimumPopulation;
        boolean hasFunds = town.getBalance() >= minimumTreasury;

        return hasClaims && hasPopulation && hasFunds;
    }

    public boolean evolveToKingdom(Town town, String kingdomName) {
        if (!canEvolveToKingdom(town)) {
            return false;
        }

        try {
            Kingdom kingdom = new Kingdom(kingdomName, town.getName());

            for (String claim : town.getClaims()) {
                kingdom.addFunctionalArea(claim, null);
            }

            societiesManager.registerKingdom(kingdom);
            town.setKingdom(kingdomName);

            plugin.getLogger().info("Town " + town.getName() + " evolved into Kingdom " + kingdomName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to evolve town to kingdom: " + e.getMessage());
            return false;
        }
    }

    public boolean canEvolveToEmpire(Kingdom kingdom) {
        FileConfiguration config = configManager.getSettlementsConfig();

        int minimumClaims = config.getInt("settlement-evolution.kingdom-to-empire.minimum-claims", 500);
        int minimumPopulation = config.getInt("settlement-evolution.kingdom-to-empire.minimum-population", 50);
        int minimumTreasury = config.getInt("settlement-evolution.kingdom-to-empire.minimum-treasury", 250000);
        int minimumVassals = config.getInt("settlement-evolution.kingdom-to-empire.minimum-vassal-kingdoms", 2);

        int totalClaims = 0;
        for (String townName : kingdom.getTowns()) {
            Town town = societiesManager.getTown(townName);
            if (town != null) {
                totalClaims += town.getClaims().size();
            }
        }

        int totalPopulation = 0;
        for (String townName : kingdom.getTowns()) {
            Town town = societiesManager.getTown(townName);
            if (town != null) {
                totalPopulation += town.getMembers().size();
            }
        }

        boolean hasClaims = totalClaims >= minimumClaims;
        boolean hasPopulation = totalPopulation >= minimumPopulation;
        boolean hasFunds = true;
        boolean hasVassals = kingdom.getVassals().size() >= minimumVassals;

        return hasClaims && hasPopulation && hasFunds && hasVassals;
    }

    public boolean evolveToEmpire(Kingdom kingdom, String empireName) {
        if (!canEvolveToEmpire(kingdom)) {
            return false;
        }

        try {
            Empire empire = new Empire(empireName, kingdom.getName());
            empire.addKingdom(kingdom.getName());

            societiesManager.registerEmpire(empire);
            kingdom.setEmpire(empireName);

            plugin.getLogger().info("Kingdom " + kingdom.getName() + " evolved into Empire " + empireName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to evolve kingdom to empire: " + e.getMessage());
            return false;
        }
    }

    public void addTownToKingdom(Kingdom kingdom, Town town) {
        kingdom.addTown(town.getName());
        town.setKingdom(kingdom.getName());
    }

    public void addKingdomToEmpire(Empire empire, Kingdom kingdom) {
        empire.addKingdom(kingdom.getName());
        kingdom.setEmpire(empire.getName());
    }
}
