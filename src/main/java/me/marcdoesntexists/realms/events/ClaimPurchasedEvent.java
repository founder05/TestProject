package me.marcdoesntexists.realms.events;

import me.marcdoesntexists.realms.utils.Claim;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class ClaimPurchasedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final UUID purchaser;
    private final Claim claim;
    private final int cost;

    public ClaimPurchasedEvent(UUID purchaser, Claim claim, int cost) {
        this.purchaser = purchaser;
        this.claim = claim;
        this.cost = cost;
    }

    public UUID getPurchaser() { return purchaser; }
    public Claim getClaim() { return claim; }
    public int getCost() { return cost; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}

