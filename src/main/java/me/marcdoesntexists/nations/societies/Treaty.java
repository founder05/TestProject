package me.marcdoesntexists.nations.societies;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Treaty {
    private final UUID treatyId;
    private final String name;
    private final String kingdom1;
    private final String kingdom2;
    private final TreatyType type;
    private final long createdAt;
    private long expiresAt;
    private TreatyStatus status = TreatyStatus.ACTIVE;
    private Map<String, String> terms = new HashMap<>();

    public Treaty(String name, String kingdom1, String kingdom2, TreatyType type, long durationDays) {
        this.treatyId = UUID.randomUUID();
        this.name = name;
        this.kingdom1 = kingdom1;
        this.kingdom2 = kingdom2;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + (durationDays * 24 * 60 * 60 * 1000);
    }

    public UUID getTreatyId() {
        return treatyId;
    }

    public String getName() {
        return name;
    }

    public String getKingdom1() {
        return kingdom1;
    }

    public String getKingdom2() {
        return kingdom2;
    }

    public TreatyType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long time) {
        this.expiresAt = time;
    }

    public TreatyStatus getStatus() {
        return status;
    }

    public void setStatus(TreatyStatus s) {
        this.status = s;
    }

    public Map<String, String> getTerms() {
        return terms;
    }

    public void addTerm(String key, String value) {
        terms.put(key, value);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isActive() {
        return status == TreatyStatus.ACTIVE && !isExpired();
    }

    public String getOtherKingdom(String kingdom) {
        return kingdom.equals(kingdom1) ? kingdom2 : kingdom1;
    }

    public long getDaysRemaining() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining / (24 * 60 * 60 * 1000));
    }

    public enum TreatyType {
        PEACE,
        TRADE,
        NON_AGGRESSION,
        MUTUAL_DEFENSE,
        NEUTRALITY
    }

    public enum TreatyStatus {
        PENDING,
        ACTIVE,
        EXPIRED,
        TERMINATED,
        VIOLATED
    }
}
