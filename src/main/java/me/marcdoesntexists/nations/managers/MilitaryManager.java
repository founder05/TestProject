package me.marcdoesntexists.nations.managers;


import me.marcdoesntexists.nations.military.MilitaryRank;
import me.marcdoesntexists.nations.military.War;
import me.marcdoesntexists.nations.military.WarCrime;

import java.util.*;

public class MilitaryManager {
    private static MilitaryManager instance;
    private Map<UUID, War> wars;
    private Map<UUID, WarCrime> warCrimes;
    private Map<UUID, MilitaryRank> ranks;

    private MilitaryManager() {
        this.wars = new HashMap<>();
        this.warCrimes = new HashMap<>();
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

    public void recordWarCrime(WarCrime crime) {
        warCrimes.put(crime.getWarCrimeId(), crime);
    }

    public WarCrime getWarCrime(UUID id) {
        return warCrimes.get(id);
    }

    public Collection<WarCrime> getAllWarCrimes() {
        return new ArrayList<>(warCrimes.values());
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
