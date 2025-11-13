package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.gui.LeaderboardHolder;
import me.marcdoesntexists.nations.managers.LeaderboardManager;
import me.marcdoesntexists.nations.managers.LeaderboardManager.LeaderboardEntry;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.utils.SoundUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LeaderboardClickListener implements Listener {
    private final Nations plugin;
    private final LeaderboardManager lbManager;

    public LeaderboardClickListener(Nations plugin) {
        this.plugin = plugin;
        this.lbManager = LeaderboardManager.getInstance(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof LeaderboardHolder holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String boardKey = holder.getBoardKey();

        // On click: run default action: send chat with entry info
        int slot = e.getSlot();
        // reconstruct entries for this board
        List<LeaderboardEntry> entries = switch (boardKey) {
            case "towns_population" -> lbManager.getTopTownsByPopulation(36);
            case "players_money" -> lbManager.getTopPlayersByWealth(36);
            default -> List.of();
        };

        if (slot >= 0 && slot < entries.size()) {
            LeaderboardEntry entry = entries.get(slot);
            p.sendMessage(MessageUtils.format("gui.leaderboard.click_message", java.util.Map.of("board", boardKey, "name", entry.getName(), "value", entry.getFormattedValue())));
            // Play click sound if configured
            try {
                var cfg = plugin.getConfigurationManager().getNationsGuiconfig();
                String clickSound = cfg.getString("gui.leaderboards." + boardKey + ".sound.click", cfg.getString("gui.leaderboards.defaults.click_sound", null));
                double clickVolume = cfg.getDouble("gui.leaderboards." + boardKey + ".sound.click_volume", cfg.getDouble("gui.leaderboards.defaults.click_volume", 1.0));
                double clickPitch = cfg.getDouble("gui.leaderboards." + boardKey + ".sound.click_pitch", cfg.getDouble("gui.leaderboards.defaults.click_pitch", 1.0));
                if (clickSound != null && !clickSound.isEmpty()) {
                    Sound s = SoundUtils.parseSound(clickSound);
                    if (s != null) {
                        p.playSound(p.getLocation(), s, (float) clickVolume, (float) clickPitch);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
