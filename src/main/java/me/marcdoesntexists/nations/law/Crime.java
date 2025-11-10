package me.marcdoesntexists.nations.law;

import me.marcdoesntexists.nations.enums.CrimeType;
import java.util.UUID;

public class Crime {
    private UUID crimeId;
    private UUID criminalId;
    private CrimeType crimeType;
    private String townId;
    private long timestamp;
    private String location;
    private String evidence;
    private boolean solved;

    public Crime(UUID criminalId, CrimeType crimeType, String townId, String location) {
        this.crimeId = UUID.randomUUID();
        this.criminalId = criminalId;
        this.crimeType = crimeType;
        this.townId = townId;
        this.timestamp = System.currentTimeMillis();
        this.location = location;
        this.solved = false;
    }

    public UUID getCrimeId() { return crimeId; }
    public UUID getCriminalId() { return criminalId; }
    public CrimeType getCrimeType() { return crimeType; }
    public String getTownId() { return townId; }
    public long getTimestamp() { return timestamp; }
    public String getLocation() { return location; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
}
