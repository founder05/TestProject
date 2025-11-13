package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.LeaderboardManager.LeaderboardEntry;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaderboardGUIManager {
    private final Nations plugin;
    private final FileConfiguration cfg;

    public LeaderboardGUIManager(Nations plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigurationManager().getNationsGuiconfig();
    }

    public void openLeaderboard(Player player, String category, List<LeaderboardEntry> entries) {
        // Prefer new unified gui.leaderboards.defaults, fallback to legacy top-level 'leaderboard'
        final String defaultsPath = "gui.leaderboards.defaults";
        String titleTemplate = cfg.getString(defaultsPath + ".title", cfg.getString("leaderboard.title", MessageUtils.get("gui.leaderboard_title")));
        String title = titleTemplate.replace("%prefix%", MessageUtils.get("general.prefix")).replace("%category%", category);
        int size = cfg.getInt(defaultsPath + ".open_size", cfg.getInt("leaderboard.size", 54));
        Inventory inv = Bukkit.createInventory(null, size, title);

        // filler and navigation will be simple: entries from slot 0..(items-per-page-1)
        int perPage = cfg.getInt(defaultsPath + ".items-per-page", cfg.getInt("leaderboard.items-per-page", 45));
        int slot = 0;
        for (int i = 0; i < Math.min(perPage, entries.size()); i++) {
            LeaderboardEntry e = entries.get(i);
            ItemStack item = createEntryItem(e, i + 1, category);
            inv.setItem(slot++, item);
        }

        // navigation slots (from unified defaults or legacy)
        int prevSlot = cfg.getInt(defaultsPath + ".navigation.previous-slot", cfg.getInt("leaderboard.navigation.previous-slot", 45));
        int nextSlot = cfg.getInt(defaultsPath + ".navigation.next-slot", cfg.getInt("leaderboard.navigation.next-slot", 53));
        int closeSlot = cfg.getInt(defaultsPath + ".navigation.close-slot", cfg.getInt("leaderboard.navigation.close-slot", 49));

        inv.setItem(closeSlot, createNavItem(Material.BARRIER, MessageUtils.get("commands.usage")));
        inv.setItem(prevSlot, createNavItem(Material.ARROW, MessageUtils.format("gui.leaderboard.prev", Map.of("prev", "Prev"))));
        inv.setItem(nextSlot, createNavItem(Material.ARROW, MessageUtils.format("gui.leaderboard.next", Map.of("next", "Next"))));

        player.openInventory(inv);

        // play open sound (use SoundUtils to normalize names)
        try {
            if (cfg.getBoolean("leaderboard.sounds.enabled", true)) {
                String soundName = cfg.getString("leaderboard.sounds.click.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
                Sound s = SoundUtils.parseSound(soundName);
                if (s != null)
                    player.playSound(player.getLocation(), s, (float) cfg.getDouble("leaderboard.sounds.click.volume", 1.0), (float) cfg.getDouble("leaderboard.sounds.click.pitch", 1.0));
            }
        } catch (Throwable ignored) {
        }
    }

    private ItemStack createEntryItem(LeaderboardEntry e, int rank, String category) {
        // Read template from unified defaults (fallback to legacy leaderboard.templates.entry)
        final String defaultsPath = "gui.leaderboards.defaults";
        String matName = cfg.getString(defaultsPath + ".templates.entry.material", cfg.getString("leaderboard.templates.entry.material", "PLAYER_HEAD"));
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) skull.getItemMeta();
        try {
            Material mm = Material.matchMaterial(matName);
            if (mm != null) {
                skull = new ItemStack(mm);
                sm = (SkullMeta) skull.getItemMeta();
            }
        } catch (Throwable ignored) {
        }

        String nameTemplate = cfg.getString(defaultsPath + ".templates.entry.name", cfg.getString("leaderboard.templates.entry.name", "{rank}. {name}"));
        String displayName = nameTemplate.replace("{rank}", String.valueOf(rank)).replace("{name}", e.getName() == null ? "Unknown" : e.getName());
        if (sm != null) {
            if (e.getName() != null) {
                try {
                    sm.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(e.getName()));
                } catch (Throwable ignored) {
                }
            }
            sm.setDisplayName(displayName);
        }

        List<String> lore = new ArrayList<>();
        List<String> loreConfig = cfg.getStringList(defaultsPath + ".templates.entry.lore");
        if (loreConfig == null || loreConfig.isEmpty())
            loreConfig = cfg.getStringList("leaderboard.templates.entry.lore");
        for (String l : loreConfig) {
            lore.add(l.replace("{value}", e.getFormattedValue()).replace("{category}", category).replace("{name}", e.getName()));
        }
        sm.setLore(lore);
        skull.setItemMeta(sm);
        return skull;
    }

    private ItemStack createNavItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.setDisplayName(name);
            it.setItemMeta(im);
        }
        return it;
    }
}
