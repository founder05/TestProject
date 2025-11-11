package me.marcdoesntexists.nations.societies;


import me.marcdoesntexists.nations.utils.FunctionalArea;

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
}