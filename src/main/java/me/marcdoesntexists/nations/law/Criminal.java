package me.marcdoesntexists.nations.law;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Criminal {
    private UUID criminalId;
    private String townId;
    private List<Crime> crimes;
    private int wantedLevel;
    private boolean arrested;
    private long arrestDate;
    private double totalFines;

    public Criminal(UUID criminalId, String townId) {
        this.criminalId = criminalId;
        this.townId = townId;
        this.crimes = new ArrayList<>();
        this.wantedLevel = 0;
        this.arrested = false;
        this.totalFines = 0;
    }

    public void addCrime(Crime crime) {
        crimes.add(crime);
        wantedLevel++;
    }

    public void arrest() {
        this.arrested = true;
        this.arrestDate = System.currentTimeMillis();
    }

    public void release() {
        this.arrested = false;
    }

    public UUID getCriminalId() {
        return criminalId;
    }

    public String getTownId() {
        return townId;
    }

    public List<Crime> getCrimes() {
        return new ArrayList<>(crimes);
    }

    public int getWantedLevel() {
        return wantedLevel;
    }

    public boolean isArrested() {
        return arrested;
    }

    public long getArrestDate() {
        return arrestDate;
    }

    public double getTotalFines() {
        return totalFines;
    }

    public void addFine(double amount) {
        this.totalFines += amount;
    }
}
