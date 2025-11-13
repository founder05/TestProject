package me.marcdoesntexists.nations.utils;

import me.marcdoesntexists.nations.societies.NobleTier;

import java.util.HashSet;
import java.util.Set;

public class PlayerData {

    private final Set<String> townInvites = new HashSet<>();
    private String town = null;
    private String job = null;
    private int money = 0;
    private String socialClass = "Commoner";
    private NobleTier nobleTier = NobleTier.COMMONER;
    private String suzerain = null;
    private boolean isMilitary = false;
    private String militaryRank = null;
    private long nobleTierExperience = 0;
    private long jobExperience = 0;
    private long classExperience = 0;
    // Chat channel preference (GLOBAL, TOWN, KINGDOM, EMPIRE, RELIGION, ALLIANCE)
    private String chatChannel = "GLOBAL";

    public PlayerData() {
        this(0);
    }

    public PlayerData(int startingMoney) {
        this.money = Math.max(0, startingMoney);
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int amount) {
        this.money = Math.max(0, amount);
    }

    public void addMoney(int amount) {
        if (amount > 0) {
            this.money += amount;
        }
    }

    public boolean removeMoney(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (money >= amount) {
            money -= amount;
            return true;
        }
        return false;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String t) {
        town = t;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String j) {
        job = j;
    }

    public String getSocialClass() {
        return socialClass;
    }

    public void setSocialClass(String c) {
        socialClass = c;
    }

    public NobleTier getNobleTier() {
        return nobleTier;
    }

    public void setNobleTier(NobleTier tier) {
        this.nobleTier = tier;
    }

    public String getSuzerain() {
        return suzerain;
    }

    public void setSuzerain(String s) {
        this.suzerain = s;
    }

    public long getNobleTierExperience() {
        return nobleTierExperience;
    }

    public void setNobleTierExperience(long exp) {
        this.nobleTierExperience = exp;
    }

    public void addNobleTierExperience(long exp) {
        this.nobleTierExperience += exp;
    }

    public long getJobExperience() {
        return jobExperience;
    }

    public void setJobExperience(long exp) {
        this.jobExperience = exp;
    }

    public void addJobExperience(long exp) {
        this.jobExperience += exp;
    }

    public long getClassExperience() {
        return classExperience;
    }

    public void setClassExperience(long exp) {
        this.classExperience = exp;
    }

    public void addClassExperience(long exp) {
        this.classExperience += exp;
    }

    public boolean isMilitary() {
        return isMilitary;
    }

    public void setMilitary(boolean military) {
        this.isMilitary = military;
    }

    public String getMilitaryRank() {
        return militaryRank;
    }

    public void setMilitaryRank(String rank) {
        this.militaryRank = rank;
    }

    public Set<String> getTownInvites() {
        return new HashSet<>(townInvites);
    }

    public void addTownInvite(String townName) {
        townInvites.add(townName);
    }

    public void removeTownInvite(String townName) {
        townInvites.remove(townName);
    }

    public String getChatChannel() {
        return chatChannel == null ? "GLOBAL" : chatChannel;
    }

    public void setChatChannel(String channel) {
        if (channel == null) channel = "GLOBAL";
        this.chatChannel = channel.toUpperCase();
    }
}
