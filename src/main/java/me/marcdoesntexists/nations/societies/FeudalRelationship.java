package me.marcdoesntexists.nations.societies;

import java.util.UUID;

public class FeudalRelationship {
    private final UUID relationshipId;
    private final String suzerain;
    private final String vassal;
    private final long createdAt;
    private long expiresAt;
    private double tributeAmount;
    private String obligations;
    private boolean isActive;

    public FeudalRelationship(String suzerain, String vassal, double tributeAmount, String obligations) {
        this.relationshipId = UUID.randomUUID();
        this.suzerain = suzerain;
        this.vassal = vassal;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = Long.MAX_VALUE;
        this.tributeAmount = tributeAmount;
        this.obligations = obligations;
        this.isActive = true;
    }

    public UUID getRelationshipId() {
        return relationshipId;
    }

    public String getSuzerain() {
        return suzerain;
    }

    public String getVassal() {
        return vassal;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public double getTributeAmount() {
        return tributeAmount;
    }

    public void setTributeAmount(double amount) {
        this.tributeAmount = amount;
    }

    public String getObligations() {
        return obligations;
    }

    public void setObligations(String obligations) {
        this.obligations = obligations;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}
