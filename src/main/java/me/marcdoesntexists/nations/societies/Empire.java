package me.marcdoesntexists.nations.societies;

import me.marcdoesntexists.nations.utils.FunctionalArea;

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
}