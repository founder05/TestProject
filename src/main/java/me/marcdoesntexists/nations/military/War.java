package me.marcdoesntexists.nations.military;

import me.marcdoesntexists.nations.enums.CasusBelli;
import me.marcdoesntexists.nations.enums.WarGoal;
import me.marcdoesntexists.nations.enums.WarType;
import me.marcdoesntexists.nations.societies.WarGoalProgress;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class War {
    private UUID warId;
    private String attackerKingdom;
    private String defenderKingdom;
    private WarStatus status;
    private WarType warType;
    private long startDate;
    private long endDate;
    private UUID initiatorId;
    private String reason;
    private Set<WarCrime> warCrimes;
    private int attackerCasualties;
    private int defenderCasualties;

    // NEW: Enhanced war system
    private int warScore; // -100 to +100, positive favors attacker
    private CasusBelli casusBelli;
    private WarGoalProgress warGoalProgress;
    private long lastScoreUpdate;
    private Set<String> participants; // Allied kingdoms involved

    public War(String attackerKingdom, String defenderKingdom, UUID initiatorId, String reason) {
        this.warId = UUID.randomUUID();
        this.attackerKingdom = attackerKingdom;
        this.defenderKingdom = defenderKingdom;
        this.status = WarStatus.DECLARED;
        this.warType = WarType.STANDARD;
        this.startDate = System.currentTimeMillis();
        this.initiatorId = initiatorId;
        this.reason = reason;
        this.warCrimes = new HashSet<>();
        this.attackerCasualties = 0;
        this.defenderCasualties = 0;
        this.warScore = 0;
        this.casusBelli = CasusBelli.UNJUSTIFIED;
        this.warGoalProgress = new WarGoalProgress(WarGoal.CONQUEST, 100);
        this.lastScoreUpdate = System.currentTimeMillis();
        this.participants = new HashSet<>();
        participants.add(attackerKingdom);
        participants.add(defenderKingdom);
    }

    // New constructor with CasusBelli and WarGoal
    public War(String attackerKingdom, String defenderKingdom, UUID initiatorId, String reason, CasusBelli casusBelli, WarGoal warGoal) {
        this(attackerKingdom, defenderKingdom, initiatorId, reason);
        this.casusBelli = casusBelli;
        this.warGoalProgress = new WarGoalProgress(warGoal, 100);
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

    // War score methods
    public void addWarScore(int amount) {
        this.warScore = Math.max(-100, Math.min(100, this.warScore + amount));
        this.lastScoreUpdate = System.currentTimeMillis();
    }

    public int getWarScore() {
        return warScore;
    }

    public void setWarScore(int score) {
        this.warScore = Math.max(-100, Math.min(100, score));
    }

    public String getWinningKingdom() {
        if (warScore > 0) return attackerKingdom;
        if (warScore < 0) return defenderKingdom;
        return null; // Stalemate
    }

    public UUID getWarId() {
        return warId;
    }

    public String getAttackerKingdom() {
        return attackerKingdom;
    }

    public String getDefenderKingdom() {
        return defenderKingdom;
    }

    public WarStatus getStatus() {
        return status;
    }

    public void setStatus(WarStatus status) {
        this.status = status;
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public UUID getInitiatorId() {
        return initiatorId;
    }

    public String getReason() {
        return reason;
    }

    public Set<WarCrime> getWarCrimes() {
        return new HashSet<>(warCrimes);
    }

    public void addWarCrime(WarCrime crime) {
        warCrimes.add(crime);
    }

    public int getAttackerCasualties() {
        return attackerCasualties;
    }

    public int getDefenderCasualties() {
        return defenderCasualties;
    }

    public CasusBelli getCasusBelli() {
        return casusBelli;
    }

    public WarGoalProgress getWarGoalProgress() {
        return warGoalProgress;
    }

    public WarType getWarType() {
        return warType;
    }

    public void setWarType(WarType type) {
        this.warType = type;
    }

    public Set<String> getParticipants() {
        return new HashSet<>(participants);
    }

    public void addParticipant(String kingdomName) {
        participants.add(kingdomName);
    }

    public long getLastScoreUpdate() {
        return lastScoreUpdate;
    }

    public enum WarStatus {
        DECLARED, ACTIVE, CEASEFIRE, CONCLUDED
    }
}
