package me.marcdoesntexists.nations.utils;

import java.util.HashMap;
import java.util.Map;

public class Claim {
    private final String chunkKey;
    private final String townName;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final long claimDate;

    private final Map<String, ClaimPermission> permissions = new HashMap<>();

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
        return pvpEnabled;
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
        return explosionsEnabled;
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

    public void setPermission(String playerId, ClaimPermission permission) {
        permissions.put(playerId, permission);
    }

    public ClaimPermission getPermission(String playerId) {
        return permissions.getOrDefault(playerId, ClaimPermission.NONE);
    }

    public enum ClaimPermission {
        NONE,
        BUILD,
        CONTAINER,
        FULL
    }
}
