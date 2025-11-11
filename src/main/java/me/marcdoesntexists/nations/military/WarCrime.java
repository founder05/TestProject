package me.marcdoesntexists.nations.military;

import me.marcdoesntexists.nations.enums.WarCrimeType;

import java.util.UUID;

public class WarCrime {
    private UUID warCrimeId;
    private UUID perpetratorId;
    private WarCrimeType crimeType;
    private String kingdom1;
    private String kingdom2;
    private long timestamp;
    private String description;
    private int witnesses;
    private boolean prosecuted;

    public WarCrime(UUID perpetratorId, WarCrimeType crimeType, String kingdom1, String kingdom2, String description) {
        this.warCrimeId = UUID.randomUUID();
        this.perpetratorId = perpetratorId;
        this.crimeType = crimeType;
        this.kingdom1 = kingdom1;
        this.kingdom2 = kingdom2;
        this.timestamp = System.currentTimeMillis();
        this.description = description;
        this.witnesses = 0;
        this.prosecuted = false;
    }

    public void addWitness() {
        this.witnesses++;
    }

    public void prosecute() {
        this.prosecuted = true;
    }

    public UUID getWarCrimeId() {
        return warCrimeId;
    }

    public UUID getPerpetratorId() {
        return perpetratorId;
    }

    public WarCrimeType getCrimeType() {
        return crimeType;
    }

    public String getKingdom1() {
        return kingdom1;
    }

    public String getKingdom2() {
        return kingdom2;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public int getWitnesses() {
        return witnesses;
    }

    public boolean isProsecuted() {
        return prosecuted;
    }
}
