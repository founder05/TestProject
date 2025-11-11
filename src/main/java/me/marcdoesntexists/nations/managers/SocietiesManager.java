package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.societies.*;
import java.util.*;

public class SocietiesManager {
    private static SocietiesManager instance;
    private Map<String, Town> towns;
    private Map<String, Kingdom> kingdoms;
    private Map<String, Empire> empires;
    private Map<String, God> gods;
    private Map<String, Religion> religions;
    private Map<UUID, Alliance> alliances;
    private Map<UUID, Treaty> treaties;
    private Map<UUID, FeudalRelationship> feudalRelationships;

    private SocietiesManager() {
        this.towns = new HashMap<>();
        this.kingdoms = new HashMap<>();
        this.empires = new HashMap<>();
        this.gods = new HashMap<>();
        this.religions = new HashMap<>();
        this.alliances = new HashMap<>();
        this.treaties = new HashMap<>();
        this.feudalRelationships = new HashMap<>();
    }

    public static SocietiesManager getInstance() {
        if (instance == null) {
            instance = new SocietiesManager();
        }
        return instance;
    }

    public void registerTown(Town town) { towns.put(town.getName(), town); }
    public Town getTown(String name) { return towns.get(name); }
    public Collection<Town> getAllTowns() { return new ArrayList<>(towns.values()); }
    public void removeTown(String name) { towns.remove(name); }

    public void registerKingdom(Kingdom kingdom) { kingdoms.put(kingdom.getName(), kingdom); }
    public Kingdom getKingdom(String name) { return kingdoms.get(name); }
    public Collection<Kingdom> getAllKingdoms() { return new ArrayList<>(kingdoms.values()); }

    public void registerEmpire(Empire empire) { empires.put(empire.getName(), empire); }
    public Empire getEmpire(String name) { return empires.get(name); }
    public Collection<Empire> getAllEmpires() { return new ArrayList<>(empires.values()); }

    public void registerGod(God god) { gods.put(god.getName(), god); }
    public God getGod(String name) { return gods.get(name); }
    public Collection<God> getAllGods() { return new ArrayList<>(gods.values()); }

    public void registerReligion(Religion religion) { religions.put(religion.getName(), religion); }
    public Religion getReligion(String name) { return religions.get(name); }

    public void registerAlliance(Alliance alliance) { alliances.put(alliance.getAllianceId(), alliance); }
    public Alliance getAlliance(UUID id) { return alliances.get(id); }

    public void registerTreaty(Treaty treaty) { treaties.put(treaty.getTreatyId(), treaty); }
    public Treaty getTreaty(UUID id) { return treaties.get(id); }

    public void registerFeudalRelationship(FeudalRelationship relationship) { feudalRelationships.put(relationship.getRelationshipId(), relationship); }
    public FeudalRelationship getFeudalRelationship(UUID id) { return feudalRelationships.get(id); }
    public Collection<FeudalRelationship> getAllFeudalRelationships() { return new ArrayList<>(feudalRelationships.values()); }

    public Collection<Alliance> getAlliances() { return new ArrayList<>(alliances.values()); }
    public Collection<Treaty> getAllTreaties() { return new ArrayList<>(treaties.values()); }
    public Collection<Religion> getAllReligions() { return new ArrayList<>(religions.values()); }
}
