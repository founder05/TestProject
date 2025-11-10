package me.marcdoesntexists.nations.societies;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class God {
    private final String name;
    private final String description;
    private final String domain;
    private final UUID creator;
    private final Set<UUID> followers = new HashSet<>();
    private final Set<String> altars = new HashSet<>();
    private final Religion religion;
    private int power = 0;

    public God(String name, String description, String domain, UUID creator, Religion religion) {
        this.name = name;
        this.description = description;
        this.domain = domain;
        this.creator = creator;
        this.religion = religion;
        this.followers.add(creator);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDomain() {
        return domain;
    }

    public UUID getCreator() {
        return creator;
    }

    public Religion getRelatedReligion() {
        return religion;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int amount) {
        if (amount >= 0) power = amount;
    }

    public void addPower(int amount) {
        if (amount > 0) power += amount;
    }

    public Set<UUID> getFollowers() {
        return followers;
    }

    public void addFollower(UUID uuid) {
        followers.add(uuid);
    }

    public void removeFollower(UUID uuid) {
        followers.remove(uuid);
    }

    public int getFollowerCount() {
        return followers.size();
    }

    public Set<String> getAltars() {
        return altars;
    }

    public void addAltar(String location) {
        altars.add(location);
    }

    public void removeAltar(String location) {
        altars.remove(location);
    }

    public boolean hasAltar(String location) {
        return altars.contains(location);
    }
}
