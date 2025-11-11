package me.marcdoesntexists.nations.law;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.CrimeType;
import me.marcdoesntexists.nations.enums.PunishmentType;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import me.marcdoesntexists.nations.managers.LawManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class JusticeService {
    private static JusticeService instance;
    private final Nations plugin;
    private final LawManager lawManager;
    private final ConfigurationManager configManager;

    private JusticeService(Nations plugin) {
        this.plugin = plugin;
        this.lawManager = LawManager.getInstance();
        this.configManager = ConfigurationManager.getInstance();
    }

    public static JusticeService getInstance(Nations plugin) {
        if (instance == null) {
            instance = new JusticeService(plugin);
        }
        return instance;
    }

    public static JusticeService getInstance() {
        return instance;
    }

    public boolean recordCrime(UUID criminalId, CrimeType crimeType, String townId, String location) {
        try {
            Crime crime = new Crime(criminalId, crimeType, townId, location);
            lawManager.registerCrime(crime);

            plugin.getLogger().info("Crime recorded: " + crimeType + " by " + criminalId + " in " + townId);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to record crime: " + e.getMessage());
            return false;
        }
    }

    public boolean startTrial(Crime crime, UUID judgeId, UUID defendantId) {
        try {
            Trial trial = new Trial(crime, judgeId, defendantId);
            lawManager.registerTrial(trial);

            plugin.getLogger().info("Trial started for " + defendantId + " regarding crime " + crime.getCrimeType());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start trial: " + e.getMessage());
            return false;
        }
    }

    public boolean concludeTrial(Trial trial, PunishmentType verdict, double punishment, String reason) {
        try {
            trial.conclude(verdict, punishment, reason);

            if (verdict == PunishmentType.GUILTY) {
                Criminal criminal = lawManager.getCriminal(trial.getDefendantId());
                if (criminal != null) {
                    criminal.arrest();
                }
            }

            plugin.getLogger().info("Trial concluded with verdict: " + verdict);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to conclude trial: " + e.getMessage());
            return false;
        }
    }

    public double getCrimeBaseFine(CrimeType crimeType) {
        FileConfiguration legalConfig = configManager.getLegalConfig();
        return legalConfig.getDouble("legal-system.default-crimes." + crimeType.name().toLowerCase() + ".base-fine", 1000);
    }

    public int getCrimeSeverity(CrimeType crimeType) {
        FileConfiguration legalConfig = configManager.getLegalConfig();
        return legalConfig.getInt("legal-system.default-crimes." + crimeType.name().toLowerCase() + ".severity", 1);
    }

    public boolean isCriminal(UUID playerId) {
        Criminal criminal = lawManager.getCriminal(playerId);
        return criminal != null && criminal.getWantedLevel() > 0;
    }

    public int getWantedLevel(UUID playerId) {
        Criminal criminal = lawManager.getCriminal(playerId);
        if (criminal != null) {
            return criminal.getWantedLevel();
        }
        return 0;
    }

    public void arrestCriminal(UUID playerId) {
        Criminal criminal = lawManager.getCriminal(playerId);
        if (criminal != null) {
            criminal.arrest();
            plugin.getLogger().info("Criminal " + playerId + " has been arrested");
        }
    }

    public void releaseCriminal(UUID playerId) {
        Criminal criminal = lawManager.getCriminal(playerId);
        if (criminal != null) {
            criminal.release();
            plugin.getLogger().info("Criminal " + playerId + " has been released");
        }
    }

    public double getTotalFines(UUID playerId) {
        Criminal criminal = lawManager.getCriminal(playerId);
        if (criminal != null) {
            return criminal.getTotalFines();
        }
        return 0;
    }
}
