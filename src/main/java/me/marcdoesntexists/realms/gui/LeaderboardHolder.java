package me.marcdoesntexists.realms.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LeaderboardHolder implements InventoryHolder {
    private final String boardKey;
    private final int page;

    public LeaderboardHolder(String boardKey) {
        this(boardKey, 1);
    }

    public LeaderboardHolder(String boardKey, int page) {
        this.boardKey = boardKey;
        this.page = Math.max(1, page);
    }

    public String getBoardKey() {
        return boardKey;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
