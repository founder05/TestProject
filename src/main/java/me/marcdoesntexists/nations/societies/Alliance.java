package me.marcdoesntexists.nations.societies;

import me.marcdoesntexists.nations.enums.AlliancePerk;
import me.marcdoesntexists.nations.enums.AllianceTier;

import java.util.*;

public class Alliance {
    private final UUID allianceId;
    private final String name;
    private final String leader;
    private final UUID founder;
    private final Set<String> members = new HashSet<>();
    private final Set<String> pendingInvites = new HashSet<>();
    private long createdAt;
    private long expiresAt;
    private String description = "No description";
    private int tier = 1;
    private String currentTier = "none";

    // NEW: Enhanced alliance system
    private AllianceTier allianceTier = AllianceTier.DEFENSIVE_PACT;
    private Set<AlliancePerk> activePerks = new HashSet<>();
    private Map<String, Integer> contributions = new HashMap<>();
    private int totalContributions = 0;

    public Alliance(String name, String leader) {
        this.allianceId = UUID.randomUUID();
        this.name = name;
        this.leader = leader;
        this.founder = null;
        this.createdAt = System.currentTimeMillis();
        this.members.add(leader);
    }

    public Alliance(String name, UUID founder) {
        this.allianceId = UUID.randomUUID();
        this.name = name;
        this.founder = founder;
        this.leader = founder.toString();
        this.createdAt = System.currentTimeMillis();
        this.members.add(founder.toString());
    }

    public UUID getAllianceId() {
        return allianceId;
    }

    public String getName() {
        return name;
    }

    public String getLeader() {
        return leader;
    }

    public UUID getFounder() {
        return founder;
    }

    public Set<String> getMembers() {
        return members;
    }

    public Set<String> getPendingInvites() {
        return pendingInvites;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long time) {
        this.createdAt = time;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long time) {
        this.expiresAt = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public String getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(String tier) {
        this.currentTier = tier;
    }

    public boolean addMember(String kingdomName) {
        return members.add(kingdomName);
    }

    public boolean removeMember(String kingdomName) {
        if (kingdomName.equals(leader)) return false;
        return members.remove(kingdomName);
    }

    public boolean isMember(String kingdomName) {
        return members.contains(kingdomName);
    }

    public int getMemberCount() {
        return members.size();
    }

    public void addInvite(String kingdomName) {
        pendingInvites.add(kingdomName);
    }

    public void removePendingInvite(String kingdomName) {
        pendingInvites.remove(kingdomName);
    }

    public boolean hasInvite(String kingdomName) {
        return pendingInvites.contains(kingdomName);
    }

    // Alliance tier methods
    public AllianceTier getAllianceTier() {
        return allianceTier;
    }

    public void setAllianceTier(AllianceTier tier) {
        this.allianceTier = tier;
    }

    // Perks methods
    public Set<AlliancePerk> getActivePerks() {
        return new HashSet<>(activePerks);
    }

    public void activatePerk(AlliancePerk perk) {
        activePerks.add(perk);
    }

    public void deactivatePerk(AlliancePerk perk) {
        activePerks.remove(perk);
    }

    public boolean hasPerk(AlliancePerk perk) {
        return activePerks.contains(perk);
    }

    // Contribution methods
    public Map<String, Integer> getContributions() {
        return new HashMap<>(contributions);
    }

    public int getContribution(String kingdomName) {
        return contributions.getOrDefault(kingdomName, 0);
    }

    public void addContribution(String kingdomName, int amount) {
        contributions.put(kingdomName, contributions.getOrDefault(kingdomName, 0) + amount);
        totalContributions += amount;
    }

    public int getTotalContributions() {
        return totalContributions;
    }
}
