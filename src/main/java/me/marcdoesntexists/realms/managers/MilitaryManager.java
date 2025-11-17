package me.marcdoesntexists.realms.managers;


import me.marcdoesntexists.realms.military.MilitaryRank;
import me.marcdoesntexists.realms.military.War;

import java.util.*;

public class MilitaryManager {
    private static MilitaryManager instance;
    private Map<UUID, War> wars;
    private Map<UUID, MilitaryRank> ranks;

    private MilitaryManager() {
        this.wars = new HashMap<>();
        this.ranks = new HashMap<>();
    }

    public static MilitaryManager getInstance() {
        if (instance == null) {
            instance = new MilitaryManager();
        }
        return instance;
    }

    public void declareWar(War war) {
        wars.put(war.getWarId(), war);
        war.declareWar();
    }

    public War getWar(UUID id) {
        return wars.get(id);
    }

    public Collection<War> getAllWars() {
        return new ArrayList<>(wars.values());
    }

    public void registerRank(MilitaryRank rank) {
        ranks.put(rank.getRankId(), rank);
    }

    public MilitaryRank getRank(UUID id) {
        return ranks.get(id);
    }

    public Collection<MilitaryRank> getAllRanks() {
        return new ArrayList<>(ranks.values());
    }
}
