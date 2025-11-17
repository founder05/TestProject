package me.marcdoesntexists.realms.societies;


import me.marcdoesntexists.realms.utils.FunctionalArea;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Kingdom {
    private final String name;
    private final String capital;
    private final Set<String> wars = new HashSet<>();
    private String empire = null;

    private Set<String> towns = new HashSet<>();

    private Map<String, FunctionalArea> functionalAreas = new HashMap<>();

    private int progressionLevel = 1;
    private long progressionExperience = 0;
    private int balance = 0;

    // NEW: Reputation system
    private int prestige = 0;
    private int infamy = 0;
    private long lastReputationUpdate = System.currentTimeMillis();

    // NEW: Vassal loyalty
    private int loyalty = 100; // 0-100, used when this kingdom is a vassal
    private long lastLoyaltyUpdate = System.currentTimeMillis();

    // Diplomacy fields
    private Set<String> allies = new HashSet<>();
    private Set<String> enemies = new HashSet<>();
    private Set<String> neutrals = new HashSet<>();
    private Set<String> alliances = new HashSet<>();
    private Set<String> treaties = new HashSet<>();

    // Feudalism fields
    private String suzerain = null;
    private Set<String> vassals = new HashSet<>();
    private int tributeAmount = 0;

    // Trusted members at kingdom level (players across towns)
    private Set<java.util.UUID> trustedMembers = new HashSet<>();

    // Jail support removed in this build

    public Kingdom(String name, String capital) {
        this.name = name;
        this.capital = capital;
        this.towns.add(capital); // Add the capital town upon creation
    }

    public Map<String, FunctionalArea> getFunctionalAreas() {
        return functionalAreas;
    }

    public boolean addFunctionalArea(String chunkKey, FunctionalArea.AreaType type) {
        if (functionalAreas.containsKey(chunkKey)) return false;
        functionalAreas.put(chunkKey, new FunctionalArea(chunkKey, type));
        return true;
    }

    public String getName() {
        return name;
    }

    public String getCapital() {
        return capital;
    }

    public boolean isKing(String townName) {
        return capital.equals(townName);
    }

    public void declareWar(String kingdom) {
        wars.add(kingdom);
    }

    public Set<String> getWars() {
        return wars;
    }

    public void removeWar(String kingdom) {
        wars.remove(kingdom);
    }

    // NEW: Getters for towns
    public Set<String> getTowns() {
        return towns;
    }

    public void addTown(String townName) {
        towns.add(townName);
    }

    public String getEmpire() {
        return empire;
    }

    public void setEmpire(String e) {
        empire = e;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public void addAlly(String kingdom) {
        allies.add(kingdom);
    }

    public void removeAlly(String kingdom) {
        allies.remove(kingdom);
    }

    public Set<String> getEnemies() {
        return enemies;
    }

    public void addEnemy(String kingdom) {
        enemies.add(kingdom);
    }

    public void removeEnemy(String kingdom) {
        enemies.remove(kingdom);
    }

    public Set<String> getNeutrals() {
        return neutrals;
    }

    public void addNeutral(String kingdom) {
        neutrals.add(kingdom);
    }

    public void removeNeutral(String kingdom) {
        neutrals.remove(kingdom);
    }

    public Set<String> getAlliances() {
        return alliances;
    }

    public void joinAlliance(String alliance) {
        alliances.add(alliance);
    }

    public void leaveAlliance(String alliance) {
        alliances.remove(alliance);
    }

    public Set<String> getTreaties() {
        return treaties;
    }

    public void addTreaty(String treaty) {
        treaties.add(treaty);
    }

    public void removeTreaty(String treaty) {
        treaties.remove(treaty);
    }

    public String getSuzerain() {
        return suzerain;
    }

    public void setSuzerain(String s) {
        this.suzerain = s;
    }

    public Set<String> getVassals() {
        return vassals;
    }

    public void addVassal(String kingdom) {
        vassals.add(kingdom);
    }

    public void removeVassal(String kingdom) {
        vassals.remove(kingdom);
    }

    public int getTributeAmount() {
        return tributeAmount;
    }

    public void setTributeAmount(int amount) {
        this.tributeAmount = amount;
    }

    public int getProgressionLevel() {
        return progressionLevel;
    }

    public void setProgressionLevel(int level) {
        this.progressionLevel = level;
    }

    public long getProgressionExperience() {
        return progressionExperience;
    }

    public void setProgressionExperience(long exp) {
        this.progressionExperience = exp;
    }

    public void addProgressionExperience(long exp) {
        this.progressionExperience += exp;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void addMoney(int amount) {
        this.balance += amount;
    }

    public boolean removeMoney(int amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    // Prestige methods
    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int prestige) {
        this.prestige = Math.max(0, prestige);
    }

    public void addPrestige(int amount) {
        this.prestige = Math.max(0, this.prestige + amount);
        this.lastReputationUpdate = System.currentTimeMillis();
    }

    // Infamy methods
    public int getInfamy() {
        return infamy;
    }

    public void setInfamy(int infamy) {
        this.infamy = Math.max(0, Math.min(100, infamy));
    }

    public void addInfamy(int amount) {
        this.infamy = Math.max(0, Math.min(100, this.infamy + amount));
        this.lastReputationUpdate = System.currentTimeMillis();
    }

    public boolean isHighInfamy() {
        return infamy >= 75;
    }

    // Loyalty methods (for vassals)
    public int getLoyalty() {
        return loyalty;
    }

    public void setLoyalty(int loyalty) {
        this.loyalty = Math.max(0, Math.min(100, loyalty));
    }

    public void modifyLoyalty(int amount) {
        this.loyalty = Math.max(0, Math.min(100, this.loyalty + amount));
        this.lastLoyaltyUpdate = System.currentTimeMillis();
    }

    public double getRebellionChance() {
        return (100 - loyalty) / 100.0;
    }

    public long getLastReputationUpdate() {
        return lastReputationUpdate;
    }

    public long getLastLoyaltyUpdate() {
        return lastLoyaltyUpdate;
    }

    public boolean addTrusted(java.util.UUID uuid) {
        if (uuid == null) return false;
        return trustedMembers.add(uuid);
    }

    public boolean removeTrusted(java.util.UUID uuid) {
        if (uuid == null) return false;
        return trustedMembers.remove(uuid);
    }

    public boolean isTrusted(java.util.UUID uuid) {
        if (uuid == null) return false;
        return trustedMembers.contains(uuid);
    }

    public java.util.Set<java.util.UUID> getTrustedMembers() {
        return trustedMembers;
    }

    // ========== SUBCLAIMS SUPPORT (for Kingdom members) ==========
    // Members of a kingdom can buy subclaims within kingdom territory
    // These are tracked via Claim.ownerUuid in ClaimManager

    /**
     * Check if a player can buy/own subclaims in this kingdom
     * (e.g., members of towns that belong to this kingdom)
     */
    public boolean canOwnSubclaim(java.util.UUID playerId) {
        if (playerId == null) return false;
        // Kingdom owners and trusted members can own subclaims
        return isTrusted(playerId);
    }

    /**
     * Get all members of this kingdom (from all towns)
     * Requires access to SocietiesManager to enumerate towns and their members
     */
    public java.util.Set<java.util.UUID> getAllMembers(me.marcdoesntexists.realms.managers.SocietiesManager sm) {
        java.util.Set<java.util.UUID> members = new java.util.HashSet<>();
        if (sm == null) return members;
        for (String townName : towns) {
            me.marcdoesntexists.realms.societies.Town town = sm.getTown(townName);
            if (town != null) {
                members.addAll(town.getMembers());
            }
        }
        return members;
    }
}
