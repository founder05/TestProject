package me.marcdoesntexists.nations.military;

import java.util.*;

public class War {
    private UUID warId;
    private String attackerKingdom;
    private String defenderKingdom;
    private WarStatus status;
    private long startDate;
    private long endDate;
    private UUID initiatorId;
    private String reason;
    private Set<WarCrime> warCrimes;
    private int attackerCasualties;
    private int defenderCasualties;

    public enum WarStatus {
        DECLARED, ACTIVE, CEASEFIRE, CONCLUDED
    }

    public War(String attackerKingdom, String defenderKingdom, UUID initiatorId, String reason) {
        this.warId = UUID.randomUUID();
        this.attackerKingdom = attackerKingdom;
        this.defenderKingdom = defenderKingdom;
        this.status = WarStatus.DECLARED;
        this.startDate = System.currentTimeMillis();
        this.initiatorId = initiatorId;
        this.reason = reason;
        this.warCrimes = new HashSet<>();
        this.attackerCasualties = 0;
        this.defenderCasualties = 0;
    }

    public void declareWar() {
        this.status = WarStatus.ACTIVE;
    }

    public void endWar() {
        this.status = WarStatus.CONCLUDED;
        this.endDate = System.currentTimeMillis();
    }

    public void recordCasualty(String kingdom) {
        if (kingdom.equals(attackerKingdom)) {
            attackerCasualties++;
        } else if (kingdom.equals(defenderKingdom)) {
            defenderCasualties++;
        }
    }

    public UUID getWarId() { return warId; }
    public String getAttackerKingdom() { return attackerKingdom; }
    public String getDefenderKingdom() { return defenderKingdom; }
    public WarStatus getStatus() { return status; }
    public void setStatus(WarStatus status) { this.status = status; }
    public long getStartDate() { return startDate; }
    public long getEndDate() { return endDate; }
    public UUID getInitiatorId() { return initiatorId; }
    public String getReason() { return reason; }
    public Set<WarCrime> getWarCrimes() { return new HashSet<>(warCrimes); }
    public void addWarCrime(WarCrime crime) { warCrimes.add(crime); }
    public int getAttackerCasualties() { return attackerCasualties; }
    public int getDefenderCasualties() { return defenderCasualties; }
}
