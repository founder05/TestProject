package me.marcdoesntexists.nations.military;

import java.util.UUID;

public class MilitaryRank {
    private UUID rankId;
    private String rankName;
    private int level;
    private double salary;
    private String permissions;
    private UUID kingdomId;

    public MilitaryRank(String rankName, int level, double salary, UUID kingdomId) {
        this.rankId = UUID.randomUUID();
        this.rankName = rankName;
        this.level = level;
        this.salary = salary;
        this.kingdomId = kingdomId;
        this.permissions = "";
    }

    public UUID getRankId() { return rankId; }
    public String getRankName() { return rankName; }
    public int getLevel() { return level; }
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public UUID getKingdomId() { return kingdomId; }
}
