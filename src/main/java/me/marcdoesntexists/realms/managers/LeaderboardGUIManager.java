package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.LeaderboardManager.LeaderboardEntry;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.SoundUtils;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
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
    private final Realms plugin;
    private final FileConfiguration cfg;

    public LeaderboardGUIManager(Realms plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigurationManager().getRealmsGuiconfig();
    }

    public void openLeaderboard(Player player, String category, List<LeaderboardEntry> entries) {
        // unified path in gui.yml
        final String base = "gui.leaderboard";
        String titleTemplate = cfg.getString(base + ".title", cfg.getString("leaderboard_title", MessageUtils.get("gui.leaderboard_title")));
        String title = titleTemplate.replace("%prefix%", MessageUtils.getPrefix()).replace("{category}", category).replace("%category%", category);
        Component titleComp = MessageUtils.toComponent(title);

        int size = cfg.getInt(base + ".size", cfg.getInt("leaderboard.size", 54));
        Inventory inv = Bukkit.createInventory(null, size, titleComp);

        int perPage = cfg.getInt(base + ".per_page", cfg.getInt("leaderboard.per_page", 28));
        int slot = 0;
        for (int i = 0; i < Math.min(perPage, entries.size()) && slot < size; i++) {
            LeaderboardEntry e = entries.get(i);
            ItemStack item = createEntryItem(e, i + 1, category);
            inv.setItem(slot++, item);
        }

        int prevSlot = cfg.getInt(base + ".navigation.prev_button.slot", cfg.getInt("leaderboard.navigation.previous-slot", 45));
        int nextSlot = cfg.getInt(base + ".navigation.next_button.slot", cfg.getInt("leaderboard.navigation.next-slot", 53));
        int closeSlot = cfg.getInt(base + ".navigation.close_button.slot", cfg.getInt("leaderboard.navigation.close-slot", 49));

        String prevName = cfg.getString(base + ".navigation.prev_button.display_name", cfg.getString("nav_prev", "&e◀ Indietro"));
        String nextName = cfg.getString(base + ".navigation.next_button.display_name", cfg.getString("nav_next", "&eAvanti ▶"));
        String closeName = cfg.getString(base + ".navigation.close_button.display_name", cfg.getString("close_button", "&cChiudi"));

        inv.setItem(closeSlot, createNavItem(Material.BARRIER, MessageUtils.toComponent(closeName)));
        inv.setItem(prevSlot, createNavItem(Material.ARROW, MessageUtils.toComponent(prevName)));
        inv.setItem(nextSlot, createNavItem(Material.ARROW, MessageUtils.toComponent(nextName)));

        player.openInventory(inv);

        // play open sound (use SoundUtils to normalize names)
        try {
            String soundName = cfg.getString(base + ".sounds.open", cfg.getString("leaderboard.sounds.open", null));
            double vol = cfg.getDouble(base + ".sounds.open_volume", cfg.getDouble("leaderboard.sounds.open_volume", 1.0));
            double pitch = cfg.getDouble(base + ".sounds.open_pitch", cfg.getDouble("leaderboard.sounds.open_pitch", 1.0));
            if (soundName != null && !soundName.isEmpty()) {
                Sound s = SoundUtils.parseSound(soundName);
                if (s != null) player.playSound(player.getLocation(), s, (float) vol, (float) pitch);
            }
        } catch (Throwable ignored) {
        }
    }

    private ItemStack createEntryItem(LeaderboardEntry e, int rank, String category) {
        final String base = "gui.leaderboard";
        String matName = cfg.getString(base + ".items.entry.material", cfg.getString("leaderboard.items.entry.material", "PLAYER_HEAD"));
        Material mat = Material.matchMaterial(matName.toUpperCase());
        if (mat == null) mat = Material.PLAYER_HEAD;
        ItemStack item = new ItemStack(mat);

        ItemMeta meta = item.getItemMeta();

        String nameTemplate = cfg.getString(base + ".items.entry.display_name", cfg.getString("leaderboard.items.entry.display_name", "{rank}. {name}"));
        String displayName = nameTemplate.replace("{rank}", String.valueOf(rank)).replace("{name}", e.getName() == null ? "Unknown" : e.getName()).replace("{value}", e.getFormattedValue()).replace("{category}", category);
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);

        // handle skull owner when material is player head
        if (meta instanceof SkullMeta skullMeta) {
            if (e.getName() != null && cfg.getBoolean(base + ".items.entry.skull_owner_from_name", true)) {
                try {
                    // attempt by name then by uuid
                    org.bukkit.OfflinePlayer off = null;
                    try {
                        java.util.UUID u = java.util.UUID.fromString(e.getName());
                        off = Bukkit.getOfflinePlayer(u);
                    } catch (Exception ex) {
                        off = Bukkit.getOfflinePlayer(e.getName());
                    }
                    skullMeta.setOwningPlayer(off);
                } catch (Throwable ignored) {
                }
            }
            skullMeta.displayName(MessageUtils.toComponent(displayName));
            List<String> lore = new ArrayList<>();
            List<String> loreConfig = cfg.getStringList(base + ".items.entry.lore");
            if (loreConfig.isEmpty()) loreConfig = cfg.getStringList("leaderboard.items.entry.lore");
            for (String l : loreConfig) {
                if (l == null) continue;
                lore.add(l.replace("{value}", e.getFormattedValue()).replace("{category}", category).replace("{name}", e.getName() == null ? "Unknown" : e.getName()).replace("{rank}", String.valueOf(rank)));
            }
            skullMeta.lore(MessageUtils.toComponentList(lore));
            item.setItemMeta(skullMeta);
            return item;
        }

        // non-skull items
        if (meta != null) {
            meta.displayName(MessageUtils.toComponent(displayName));
            List<String> lore = new ArrayList<>();
            List<String> loreConfig = cfg.getStringList(base + ".items.entry.lore");
            if (loreConfig.isEmpty()) loreConfig = cfg.getStringList("leaderboard.items.entry.lore");
            for (String l : loreConfig) {
                if (l == null) continue;
                lore.add(l.replace("{value}", e.getFormattedValue()).replace("{category}", category).replace("{name}", e.getName() == null ? "Unknown" : e.getName()).replace("{rank}", String.valueOf(rank)));
            }
            meta.lore(MessageUtils.toComponentList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavItem(Material mat, Component comp) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(comp);
            it.setItemMeta(im);
        }
        return it;
    }
}
