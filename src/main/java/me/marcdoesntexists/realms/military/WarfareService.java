package me.marcdoesntexists.realms.military;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.MilitaryManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Kingdom;

import java.util.UUID;

public class WarfareService {
    private static WarfareService instance;
    private final Realms plugin;
    private final MilitaryManager militaryManager;
    private final SocietiesManager societiesManager;

    private WarfareService(Realms plugin) {
        this.plugin = plugin;
        this.militaryManager = MilitaryManager.getInstance();
        this.societiesManager = SocietiesManager.getInstance();
    }

    public static WarfareService getInstance(Realms plugin) {
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

    // War crimes have been removed from the codebase: provide no-op safe methods
    public boolean recordWarCrime(War war, UUID perpetratorId, Enum<?> crimeType, String description) {
        // Deprecated: war crime recording disabled
        plugin.getLogger().warning("Attempted to record war crime but feature is disabled in this build.");
        return false;
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

    public int getWarCrimeSeverity(Enum<?> crimeType) {
        // War crimes removed - return default severity 0
        return 0;
    }

    public double getWarCrimeFine(Enum<?> crimeType) {
        // War crimes removed - return default fine 0
        return 0.0;
    }
}
