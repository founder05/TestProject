package me.marcdoesntexists.realms.societies;

import me.marcdoesntexists.realms.utils.FunctionalArea;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Empire {
    private String name;
    private String capital;
    private Set<String> kingdoms = new HashSet<>();

    private Map<String, FunctionalArea> functionalAreas = new HashMap<>();

    // Progression System
    private int progressionLevel = 1;
    private long progressionExperience = 0;

    // Trusted members at empire level
    private Set<java.util.UUID> trustedMembers = new HashSet<>();

    // Jail support removed in this build

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

    public Empire(String name, String capital) {
        this.name = name;
        this.capital = capital;
        kingdoms.add(capital);
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

    // Getter for capital (added to resolve references to e.getCapital())
    public String getCapital() {
        return capital;
    }

    public Set<String> getKingdoms() {
        return kingdoms;
    }

    public void addKingdom(String kingdomName) {
        kingdoms.add(kingdomName);
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

    // jail methods removed

    // ========== SUBCLAIMS SUPPORT (for Empire members) ==========
    // Members of an empire can buy subclaims within empire territory
    // These are tracked via Claim.ownerUuid in ClaimManager

    /**
     * Check if a player can buy/own subclaims in this empire
     * (e.g., members of kingdoms that belong to this empire)
     */
    public boolean canOwnSubclaim(java.util.UUID playerId) {
        if (playerId == null) return false;
        // Empire trusted members can own subclaims
        return isTrusted(playerId);
    }

    /**
     * Get all members of this empire (from all kingdoms and their towns)
     * Requires access to SocietiesManager to enumerate kingdoms, towns and their members
     */
    public java.util.Set<java.util.UUID> getAllMembers(me.marcdoesntexists.realms.managers.SocietiesManager sm) {
        java.util.Set<java.util.UUID> members = new java.util.HashSet<>();
        if (sm == null) return members;
        for (String kingdomName : kingdoms) {
            me.marcdoesntexists.realms.societies.Kingdom kingdom = sm.getKingdom(kingdomName);
            if (kingdom != null) {
                members.addAll(kingdom.getAllMembers(sm));
            }
        }
        return members;
    }
}