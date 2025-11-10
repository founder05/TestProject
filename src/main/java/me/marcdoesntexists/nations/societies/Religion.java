package me.marcdoesntexists.nations.societies;


import me.marcdoesntexists.nations.utils.FunctionalArea;

import java.util.*;

public class Religion {
    private String name;
    private UUID founder;
    private Set<UUID> followers = new HashSet<>();
    private Map<UUID, String> clergy = new HashMap<>();

    private Map<String, FunctionalArea> functionalAreas = new HashMap<>();

    public Religion(String name, UUID founder) {
        this.name = name;
        this.founder = founder;
        followers.add(founder);
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

    public Set<UUID> getFollowers() {
        return followers;
    }

    public void addFollower(UUID uuid) {
        followers.add(uuid);
    }

    public UUID getFounder() {
        return founder;
    }

    public Map<UUID, String> getClergy() {
        return clergy;
    }

    public void setClergyRank(UUID uuid, String rank) {
        clergy.put(uuid, rank);
    }
}