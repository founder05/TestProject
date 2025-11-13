package me.marcdoesntexists.nations.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LeaderboardHolder implements InventoryHolder {
    private final String boardKey;

    public LeaderboardHolder(String boardKey) {
        this.boardKey = boardKey;
    }

    public String getBoardKey() {
        return boardKey;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

