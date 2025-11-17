package me.marcdoesntexists.realms.societies;

import me.marcdoesntexists.realms.utils.FunctionalArea;

import java.util.*;

public class Town {
    private String name;
    private UUID mayor;
    private Set<UUID> members = new HashSet<>();
    private Set<String> claims = new HashSet<>();
    private int balance = 0;
    private String kingdom = null;

    // Trusted members who can interact inside claims (mayor always trusted)
    private Set<UUID> trustedMembers = new HashSet<>();

    // Functional Areas
    private Map<String, FunctionalArea> functionalAreas = new HashMap<>();

    // Progression System
    private int progressionLevel = 1;
    private long progressionExperience = 0;

    public Town(String name, UUID mayor) {
        this.name = name;
        this.mayor = mayor;
        this.members.add(mayor);
        this.trustedMembers.add(mayor); // mayor implicitly trusted
    }

    public String getName() {
        return name;
    }

    public UUID getMayor() {
        return mayor;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<String> getClaims() {
        return claims;
    }

    public int getBalance() {
        return balance;
    }

    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String k) {
        kingdom = k;
    }

    public boolean isMayor(UUID uuid) {
        return mayor.equals(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        // also remove trust if present
        trustedMembers.remove(uuid);
    }

    public boolean claimChunk(String chunkKey, int cost) {
        if (balance >= cost) {
            claims.add(chunkKey);
            balance -= cost;
            return true;
        }
        return false;
    }

    public void addMoney(int amount) {
        balance += amount;
    }

    public boolean removeMoney(int amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    // Trusted members management
    public boolean addTrusted(UUID uuid) {
        if (uuid == null) return false;
        if (!members.contains(uuid)) return false;
        return trustedMembers.add(uuid);
    }

    public boolean removeTrusted(UUID uuid) {
        if (uuid == null) return false;
        if (mayor.equals(uuid)) return false; // mayor always trusted
        return trustedMembers.remove(uuid);
    }

    public boolean isTrusted(UUID uuid) {
        if (uuid == null) return false;
        return trustedMembers.contains(uuid) || mayor.equals(uuid);
    }

    public Set<UUID> getTrustedMembers() {
        return trustedMembers;
    }

    // Functional Area Methods
    public Map<String, FunctionalArea> getFunctionalAreas() {
        return functionalAreas;
    }

    public boolean addFunctionalArea(String chunkKey, FunctionalArea.AreaType type) {
        if (functionalAreas.containsKey(chunkKey)) return false;
        functionalAreas.put(chunkKey, new FunctionalArea(chunkKey, type));
        return true;
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

    /**
     * Set a new mayor for the town. Adds the new mayor to members and trusted members.
     */
    public void setMayor(java.util.UUID newMayor) {
        if (newMayor == null) return;
        this.mayor = newMayor;
        this.members.add(newMayor);
        this.trustedMembers.add(newMayor);
    }
}