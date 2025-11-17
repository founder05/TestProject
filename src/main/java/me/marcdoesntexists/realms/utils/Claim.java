package me.marcdoesntexists.realms.utils;

import java.util.*;

public class Claim {
    private final String chunkKey;
    private final String townName;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final long claimDate;

    // owner UUID string (nullable). If null, claim is town-owned/general
    private String ownerUuid = null;

    private final Map<String, ClaimPermission> permissions = new HashMap<>();

    // NEW: players explicitly allowed to grief/perform criminal actions by owner
    private final Set<String> allowedToGrief = new HashSet<>();

    private boolean pvpEnabled = false;
    private boolean mobSpawningEnabled = true;
    private boolean explosionsEnabled = false;
    private boolean fireSpreadEnabled = false;

    public Claim(String chunkKey, String townName, String worldName, int chunkX, int chunkZ) {
        this.chunkKey = chunkKey;
        this.townName = townName;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.claimDate = System.currentTimeMillis();
    }

    // New constructor variant with owner
    public Claim(String chunkKey, String townName, String worldName, int chunkX, int chunkZ, String ownerUuid) {
        this(chunkKey, townName, worldName, chunkX, chunkZ);
        this.ownerUuid = ownerUuid;
    }

    public String getChunkKey() {
        return chunkKey;
    }

    public String getTownName() {
        return townName;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public long getClaimDate() {
        return claimDate;
    }

    public boolean isPvpEnabled() {
        return !pvpEnabled;
    }

    public void setPvpEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
    }

    public boolean isMobSpawningEnabled() {
        return mobSpawningEnabled;
    }

    public void setMobSpawningEnabled(boolean enabled) {
        this.mobSpawningEnabled = enabled;
    }

    public boolean isExplosionsEnabled() {
        return !explosionsEnabled;
    }

    public void setExplosionsEnabled(boolean enabled) {
        this.explosionsEnabled = enabled;
    }

    public boolean isFireSpreadEnabled() {
        return fireSpreadEnabled;
    }

    public void setFireSpreadEnabled(boolean enabled) {
        this.fireSpreadEnabled = enabled;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    // Grant permission for a player (owner or mayor should call)
    public void grantPermission(UUID playerId, ClaimPermission permission) {
        if (playerId == null) return;
        permissions.put(playerId.toString(), permission == null ? ClaimPermission.NONE : permission);
    }

    public void revokePermission(UUID playerId) {
        if (playerId == null) return;
        permissions.remove(playerId.toString());
    }

    // NEW: allow griefing for a specific player
    public void allowGrief(UUID playerId) {
        if (playerId == null) return;
        allowedToGrief.add(playerId.toString());
    }

    public void disallowGrief(UUID playerId) {
        if (playerId == null) return;
        allowedToGrief.remove(playerId.toString());
    }

    public boolean isAllowedToGrief(UUID playerId) {
        if (playerId == null) return false;
        return allowedToGrief.contains(playerId.toString());
    }

    public Set<String> getAllowedToGriefSet() {
        return Collections.unmodifiableSet(new HashSet<>(allowedToGrief));
    }

    // Checks if player has at least the required permission
    public boolean hasPermission(UUID playerId, ClaimPermission required) {
        if (required == null || required == ClaimPermission.NONE) return true;
        if (playerId == null) return false;
        ClaimPermission p = permissions.getOrDefault(playerId.toString(), ClaimPermission.NONE);
        // SIMPLE ordering: NONE < CONTAINER < BUILD < FULL
        int val = switch (p) {
            case NONE -> 0;
            case CONTAINER -> 1;
            case BUILD -> 2;
            case FULL -> 3;
        };
        int req = switch (required) {
            case CONTAINER -> 1;
            case BUILD -> 2;
            case FULL -> 3;
            default -> throw new IllegalStateException("Unexpected value: " + required);
        };
        return val >= req;
    }

    public void setPermission(String playerId, ClaimPermission permission) {
        permissions.put(playerId, permission);
    }

    public ClaimPermission getPermission(String playerId) {
        return permissions.getOrDefault(playerId, ClaimPermission.NONE);
    }

    // Return an unmodifiable copy of the permissions map (playerUuidString -> ClaimPermission)
    public Map<String, ClaimPermission> getAllPermissions() {
        return Collections.unmodifiableMap(new HashMap<>(permissions));
    }

    public enum ClaimPermission {
        NONE,
        BUILD,
        CONTAINER,
        FULL
    }
}
