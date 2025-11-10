package me.marcdoesntexists.nations.utils;

import me.marcdoesntexists.nations.societies.NobleTier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayerData {

    private final Map<String, Integer> godAffinities = new HashMap<>();
    private String town = null;
    private String religion = null;
    private String job = null;
    private String socialClass = "Commoner";
    private String clergyRank = null;
    private int money;
    private NobleTier nobleTier = NobleTier.COMMONER;
    private String suzerain = null;
    private boolean isMilitary = false;
    private String militaryRank = null;
    private long nobleTierExperience = 0;
    private long jobExperience = 0;
    private long classExperience = 0;

    public PlayerData(int startingMoney) {
        this.money = startingMoney;
    }

    public int getMoney() {
        return money;
    }

    public void addMoney(int amount) {
        if (amount > 0) {
            this.money += amount;
        }
    }

    public boolean removeMoney(int amount) {
        if (amount > 0 && this.money >= amount) {
            this.money -= amount;
            return true;
        }
        return false;
    }

    public int getGodAffinity(String godName) {
        return godAffinities.getOrDefault(godName, 0);
    }

    public void setGodAffinity(String godName, int affinity) {
        godAffinities.put(godName, affinity);
    }

    public Map<String, Integer> getGodAffinities() {
        return Collections.unmodifiableMap(godAffinities);
    }

    public String getTown() {
        return town;
    }

    public void setTown(String t) {
        town = t;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String r) {
        religion = r;
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

    public String getClergyRank() {
        return clergyRank;
    }

    public void setClergyRank(String r) {
        clergyRank = r;
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
}
