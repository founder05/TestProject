package me.marcdoesntexists.nations.events;

import me.marcdoesntexists.nations.enums.CrimeType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class CrimeCommittedEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID criminalId;
    private final Player criminal; // may be null in some usages
    private final CrimeType crimeType;
    private final String townName;
    private final Location location;
    private final String evidence;
    // context flags
    private final boolean shouldProsecute;
    private final boolean sameKingdom;
    private final boolean allied;
    private final boolean enemy;
    private boolean cancelled;

    public CrimeCommittedEvent(Player criminal, UUID criminalId, CrimeType crimeType, String townName, Location location, String evidence,
                               boolean shouldProsecute, boolean sameKingdom, boolean allied, boolean enemy) {
        this.criminal = criminal;
        this.criminalId = criminalId;
        this.crimeType = crimeType;
        this.townName = townName;
        this.location = location;
        this.evidence = evidence;
        this.shouldProsecute = shouldProsecute;
        this.sameKingdom = sameKingdom;
        this.allied = allied;
        this.enemy = enemy;
        this.cancelled = false;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public UUID getCriminalId() {
        return criminalId;
    }

    public Player getCriminal() {
        return criminal;
    }

    public CrimeType getCrimeType() {
        return crimeType;
    }

    public String getTownName() {
        return townName;
    }

    public Location getLocation() {
        return location;
    }

    public String getEvidence() {
        return evidence;
    }

    public boolean shouldProsecute() {
        return shouldProsecute;
    }

    public boolean isSameKingdom() {
        return sameKingdom;
    }

    public boolean isAllied() {
        return allied;
    }

    public boolean isEnemy() {
        return enemy;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
