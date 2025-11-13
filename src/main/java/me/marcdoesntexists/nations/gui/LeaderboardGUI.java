package me.marcdoesntexists.nations.gui;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.LeaderboardManager;
import me.marcdoesntexists.nations.managers.LeaderboardManager.LeaderboardEntry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import me.marcdoesntexists.nations.utils.SoundUtils;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LeaderboardGUI {
    private final Nations plugin;
    private final LeaderboardManager lbManager;

    public LeaderboardGUI(Nations plugin) {
        this.plugin = plugin;
        this.lbManager = LeaderboardManager.getInstance(plugin);
    }

    public void open(Player player, String boardKey) {
        FileConfiguration config = plugin.getConfigurationManager().getNationsGuiconfig();
        String path = "gui.leaderboards." + boardKey;
        String title = config.getString(path + ".title", MessageUtils.get("leaderboard.title_default"));
        int size = config.getInt(path + ".size", 9);

        LeaderboardHolder holder = new LeaderboardHolder(boardKey);
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Populate from leaderboard
        String metric = config.getString(path + ".metric", "");
        int limit = Math.min(36, size);
        List<LeaderboardEntry> entries;
        switch (metric) {
            case "towns.population": entries = lbManager.getTopTownsByPopulation(limit); break;
            case "towns.wealth": entries = lbManager.getTopTownsByWealth(limit); break;
            case "towns.claims": entries = lbManager.getTopTownsByClaims(limit); break;
            case "towns.level": entries = lbManager.getTopTownsByLevel(limit); break;
            case "kingdoms.population": entries = lbManager.getTopKingdomsByPopulation(limit); break;
            case "kingdoms.towns": entries = lbManager.getTopKingdomsByTowns(limit); break;
            case "kingdoms.wealth": entries = lbManager.getTopKingdomsByWealth(limit); break;
            case "kingdoms.vassals": entries = lbManager.getTopKingdomsByVassals(limit); break;
            case "kingdoms.level": entries = lbManager.getTopKingdomsByLevel(limit); break;
            case "players.money": entries = lbManager.getTopPlayersByWealth(limit); break;
            case "players.noble": entries = lbManager.getTopPlayersByNobleTier(limit); break;
            case "empires.population": entries = lbManager.getTopEmpiresByPopulation(limit); break;
            case "empires.kingdoms": entries = lbManager.getTopEmpiresByKingdoms(limit); break;
            case "empires.claims": entries = lbManager.getTopEmpiresByTerritory(limit); break;
            case "empires.level": entries = lbManager.getTopEmpiresByLevel(limit); break;
            case "noble_exp": entries = lbManager.getTopPlayersByNobleTier(limit); break; // fallback
            case "job_exp": entries = lbManager.getTopPlayersByWealth(limit); break; // fallback
            case "gods.power": entries = lbManager.getTopGodsByPower(limit); break;
            case "religions.followers": entries = lbManager.getTopReligionsByFollowers(limit); break;
            default: entries = List.of(); break;
        }

        for (int i = 0; i < entries.size() && i < size; i++) {
            LeaderboardEntry e = entries.get(i);
            final int rank = i + 1;

            String matName = config.getString(path + ".item.material", "PAPER");
            Material mat = Material.matchMaterial(matName.toUpperCase());
            if (mat == null) mat = Material.PAPER;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            String name = config.getString(path + ".item.name", "§6{rank}. {name}");
            List<String> loreConfig = config.getStringList(path + ".item.lore");

            // If the configured material is a player head and the entry represents a player, set owner
            if (mat == Material.PLAYER_HEAD || mat == Material.PLAYER_WALL_HEAD) {
                if (meta instanceof SkullMeta skullMeta) {
                    OfflinePlayer offline = null;
                    try {
                        // try parse as UUID first
                        UUID u = UUID.fromString(e.getName());
                        offline = Bukkit.getOfflinePlayer(u);
                    } catch (Exception ex) {
                        // fallback: use name
                        try { offline = Bukkit.getOfflinePlayer(e.getName()); } catch (Exception ignored) {}
                    }
                    if (offline != null) skullMeta.setOwningPlayer(offline);
                    item.setItemMeta(skullMeta);
                    meta = skullMeta; // update meta reference
                }
            }

            if (meta != null) {
                String display = name
                        .replace("{rank}", String.valueOf(rank))
                        .replace("{name}", e.getName())
                        .replace("{value}", e.getFormattedValue())
                        .replace("{metric}", e.getMetric() == null ? "" : e.getMetric())
                        .replace("{extra}", e.getExtra() == null ? "" : e.getExtra());

                meta.setDisplayName(display);

                meta.setLore(loreConfig.stream()
                        .map(l -> l.replace("{rank}", String.valueOf(rank))
                                .replace("{name}", e.getName())
                                .replace("{value}", e.getFormattedValue())
                                .replace("{metric}", e.getMetric() == null ? "" : e.getMetric())
                                .replace("{extra}", e.getExtra() == null ? "" : e.getExtra()))
                        .collect(Collectors.toList()));

                item.setItemMeta(meta);
            }

            inv.setItem(i, item);
        }

        player.openInventory(inv);

        // Play open sound if configured (read per-board then fallback to defaults)
        try {
            FileConfiguration cfg = plugin.getConfigurationManager().getNationsGuiconfig();
            String openSound = cfg.getString(path + ".sound.open", cfg.getString("gui.leaderboards.defaults.open_sound", null));
            double openVolume = cfg.getDouble(path + ".sound.open_volume", cfg.getDouble("gui.leaderboards.defaults.open_volume", 1.0));
            double openPitch = cfg.getDouble(path + ".sound.open_pitch", cfg.getDouble("gui.leaderboards.defaults.open_pitch", 1.0));
            if (openSound != null && !openSound.isEmpty()) {
                Sound s = SoundUtils.parseSound(openSound);
                if (s != null) {
                    player.playSound(player.getLocation(), s, (float) openVolume, (float) openPitch);
                }
            }
        } catch (Throwable ignored) {}
    }
}
