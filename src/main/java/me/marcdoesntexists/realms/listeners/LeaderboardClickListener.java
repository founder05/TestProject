package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.gui.LeaderboardHolder;
import me.marcdoesntexists.realms.gui.LeaderboardGUI;
import me.marcdoesntexists.realms.managers.LeaderboardManager;
import me.marcdoesntexists.realms.managers.LeaderboardManager.LeaderboardEntry;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.SoundUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class LeaderboardClickListener implements Listener {
    private final Realms plugin;
    private final LeaderboardManager lbManager;

    public LeaderboardClickListener(Realms plugin) {
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
        int currentPage = holder.getPage();

        FileConfiguration cfg = plugin.getConfigurationManager().getRealmsGuiconfig();
        String base = "gui.leaderboard." + boardKey;
        String global = "gui.leaderboard";

        int prevSlot = cfg.getInt(base + ".navigation.prev_button.slot", cfg.getInt(global + ".navigation.prev_button.slot", 45));
        int nextSlot = cfg.getInt(base + ".navigation.next_button.slot", cfg.getInt(global + ".navigation.next_button.slot", 53));
        int closeSlot = cfg.getInt(base + ".navigation.close_button.slot", cfg.getInt(global + ".navigation.close_button.slot", 49));
        // removed unused pageInfoSlot to silence warning

        int slot = e.getSlot();

        // navigation: prev
        if (slot == prevSlot) {
            if (currentPage > 1) {
                // play click sound
                playSoundFor(p, cfg, base, global, "click");
                new LeaderboardGUI(plugin).open(p, boardKey, currentPage - 1);
            } else {
                playSoundFor(p, cfg, base, global, "error");
            }
            return;
        }

        // navigation: next
        if (slot == nextSlot) {
            // determine total pages via a lightweight approach: use per_page and total entries fetched in GUI (approximate)
            int perPage = Math.max(1, cfg.getInt(base + ".per_page", cfg.getInt(global + ".per_page", 28)));
            int fetchLimit = Math.max(1000, perPage * 10);
            List<LeaderboardEntry> all = switch (boardKey) {
                case "towns_population" -> lbManager.getTopTownsByPopulation(fetchLimit);
                case "players_money" -> lbManager.getTopPlayersByWealth(fetchLimit);
                default -> List.of();
            };
            int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / perPage));
            if (currentPage < totalPages) {
                playSoundFor(p, cfg, base, global, "click");
                new LeaderboardGUI(plugin).open(p, boardKey, currentPage + 1);
            } else {
                playSoundFor(p, cfg, base, global, "error");
            }
            return;
        }

        // close
        if (slot == closeSlot) {
            p.closeInventory();
            playSoundFor(p, cfg, base, global, "close");
            return;
        }

        // click on entry: compute global index
        int perPage = Math.max(1, cfg.getInt(base + ".per_page", cfg.getInt(global + ".per_page", 28)));
        // ensure click is inside the entries area: best-effort guard (prevents clicks on empty/navigation slots)
        if (slot < 0 || slot >= perPage) {
            playSoundFor(p, cfg, base, global, "error");
            return;
        }

        int globalIndex = (currentPage - 1) * perPage + slot;
        // fetch up to this page to be sure we have the requested element available
        int fetchLimit = currentPage * perPage;

        List<LeaderboardEntry> entries = switch (boardKey) {
            case "towns_population" -> lbManager.getTopTownsByPopulation(fetchLimit);
            case "players_money" -> lbManager.getTopPlayersByWealth(fetchLimit);
            default -> List.of();
        };

        if (globalIndex >= 0 && globalIndex < entries.size()) {
            LeaderboardEntry entry = entries.get(globalIndex);
            p.sendMessage(MessageUtils.format("gui.leaderboard.click_message", java.util.Map.of("board", boardKey, "name", entry.getName(), "value", entry.getFormattedValue())));
            playSoundFor(p, cfg, base, global, "click");
        } else {
            playSoundFor(p, cfg, base, global, "error");
        }
    }

    private void playSoundFor(Player p, FileConfiguration cfg, String base, String global, String kind) {
        try {
            String soundPath = base + ".sounds." + kind;
            String soundName = cfg.getString(soundPath, cfg.getString(global + ".sounds." + kind, null));
            double vol = cfg.getDouble(base + ".sounds." + kind + "_volume", cfg.getDouble(global + ".sounds." + kind + "_volume", 1.0));
            double pitch = cfg.getDouble(base + ".sounds." + kind + "_pitch", cfg.getDouble(global + ".sounds." + kind + "_pitch", 1.0));
            if (soundName != null && !soundName.isEmpty()) {
                Sound s = SoundUtils.parseSound(soundName);
                if (s != null) p.playSound(p.getLocation(), s, (float) vol, (float) pitch);
            }
        } catch (Throwable ignored) {
        }
    }
}
