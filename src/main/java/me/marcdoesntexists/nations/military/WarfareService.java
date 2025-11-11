package me.marcdoesntexists.nations.military;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.WarCrimeType;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import me.marcdoesntexists.nations.managers.MilitaryManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Kingdom;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class WarfareService {
    private static WarfareService instance;
    private final Nations plugin;
    private final MilitaryManager militaryManager;
    private final SocietiesManager societiesManager;
    private final ConfigurationManager configManager;

    private WarfareService(Nations plugin) {
        this.plugin = plugin;
        this.militaryManager = MilitaryManager.getInstance();
        this.societiesManager = SocietiesManager.getInstance();
        this.configManager = ConfigurationManager.getInstance();
    }

    public static WarfareService getInstance(Nations plugin) {
        if (instance == null) {
            instance = new WarfareService(plugin);
        }
        return instance;
    }

    public static WarfareService getInstance() {
        return instance;
    }

    public boolean declareWar(String attackerKingdom, String defenderKingdom, UUID initiatorId, String reason) {
        try {
            Kingdom attacker = societiesManager.getKingdom(attackerKingdom);
            Kingdom defender = societiesManager.getKingdom(defenderKingdom);

            if (attacker == null || defender == null) {
                return false;
            }

            War war = new War(attackerKingdom, defenderKingdom, initiatorId, reason);
            militaryManager.declareWar(war);

            attacker.declareWar(defenderKingdom);
            defender.declareWar(attackerKingdom);

            plugin.getLogger().info("War declared between " + attackerKingdom + " and " + defenderKingdom);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to declare war: " + e.getMessage());
            return false;
        }
    }

    public boolean recordWarCrime(War war, UUID perpetratorId, WarCrimeType crimeType, String description) {
        try {
            WarCrime crime = new WarCrime(perpetratorId, crimeType, war.getAttackerKingdom(), war.getDefenderKingdom(), description);
            militaryManager.recordWarCrime(crime);
            war.addWarCrime(crime);

            FileConfiguration warConfig = configManager.getWarConfig();
            int reputationLoss = warConfig.getInt("war-crimes.crimes." + crimeType.name().toLowerCase() + ".reputation-loss", 50);
            double fineAmount = warConfig.getDouble("war-crimes.crimes." + crimeType.name().toLowerCase() + ".fine-amount", 10000);

            plugin.getLogger().info("War crime recorded: " + crimeType.name() + " by " + perpetratorId);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to record war crime: " + e.getMessage());
            return false;
        }
    }

    public void recordCasualty(War war, String kingdom) {
        war.recordCasualty(kingdom);
    }

    public void endWar(War war) {
        war.endWar();
        Kingdom attacker = societiesManager.getKingdom(war.getAttackerKingdom());
        Kingdom defender = societiesManager.getKingdom(war.getDefenderKingdom());

        if (attacker != null) {
            attacker.removeWar(war.getDefenderKingdom());
        }
        if (defender != null) {
            defender.removeWar(war.getAttackerKingdom());
        }

        plugin.getLogger().info("War between " + war.getAttackerKingdom() + " and " + war.getDefenderKingdom() + " has ended");
    }

    public boolean isAtWar(String kingdom1, String kingdom2) {
        Kingdom k1 = societiesManager.getKingdom(kingdom1);
        Kingdom k2 = societiesManager.getKingdom(kingdom2);

        if (k1 == null || k2 == null) {
            return false;
        }

        return k1.getWars().contains(kingdom2) || k2.getWars().contains(kingdom1);
    }

    public int getWarCrimeSeverity(WarCrimeType crimeType) {
        FileConfiguration warConfig = configManager.getWarConfig();
        return warConfig.getInt("war-crimes.crimes." + crimeType.name().toLowerCase() + ".severity", 5);
    }

    public double getWarCrimeFine(WarCrimeType crimeType) {
        FileConfiguration warConfig = configManager.getWarConfig();
        return warConfig.getDouble("war-crimes.crimes." + crimeType.name().toLowerCase() + ".fine-amount", 10000);
    }
}
