package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.law.Crime;
import me.marcdoesntexists.nations.law.Criminal;
import me.marcdoesntexists.nations.law.Trial;

import java.util.*;

public class LawManager {
    private static LawManager instance;
    private Map<UUID, Crime> crimes;
    private Map<UUID, Criminal> criminals;
    private Map<UUID, Trial> trials;

    private LawManager() {
        this.crimes = new HashMap<>();
        this.criminals = new HashMap<>();
        this.trials = new HashMap<>();
    }

    public static LawManager getInstance() {
        if (instance == null) {
            instance = new LawManager();
        }
        return instance;
    }

    public void registerCrime(Crime crime) {
        crimes.put(crime.getCrimeId(), crime);
        UUID criminalId = crime.getCriminalId();
        Criminal criminal = criminals.computeIfAbsent(criminalId, k -> new Criminal(criminalId, crime.getTownId()));
        criminal.addCrime(crime);
    }

    public Crime getCrime(UUID id) { return crimes.get(id); }
    public Criminal getCriminal(UUID id) { return criminals.get(id); }
    public Collection<Crime> getAllCrimes() { return new ArrayList<>(crimes.values()); }
    public Collection<Criminal> getAllCriminals() { return new ArrayList<>(criminals.values()); }

    public void registerTrial(Trial trial) { trials.put(trial.getTrialId(), trial); }
    public Trial getTrial(UUID id) { return trials.get(id); }
    public Collection<Trial> getAllTrials() { return new ArrayList<>(trials.values()); }
}
