package me.marcdoesntexists.realms.military;

import java.util.UUID;

public class MilitaryRank {
    private UUID rankId;
    private String rankName;
    private int level;
    private double salary;
    private String permissions;
    private String kingdomName; // changed from UUID to String to match usages

    public MilitaryRank(String rankName, int level, double salary, String kingdomName) {
        this.rankId = UUID.randomUUID();
        this.rankName = rankName;
        this.level = level;
        this.salary = salary;
        this.kingdomName = kingdomName;
        this.permissions = "";
    }

    public UUID getRankId() {
        return rankId;
    }

    public String getRankName() {
        return rankName;
    }

    public int getLevel() {
        return level;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getKingdomName() {
        return kingdomName;
    }
}
