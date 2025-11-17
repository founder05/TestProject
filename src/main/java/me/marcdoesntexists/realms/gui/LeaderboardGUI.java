package me.marcdoesntexists.realms.gui;
import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.LeaderboardManager;
import me.marcdoesntexists.realms.managers.LeaderboardManager.LeaderboardEntry;
import me.marcdoesntexists.realms.utils.SoundUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardGUI {
    private final Realms plugin;
    private final LeaderboardManager lbManager;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public LeaderboardGUI(Realms plugin) {
        this.plugin = plugin;
        this.lbManager = LeaderboardManager.getInstance(plugin);
    }

    /**
     * Converti una stringa con codici & in Component
     */
    private static Component colorize(String text) {
        if (text == null) return Component.empty();
        return LEGACY_SERIALIZER.deserialize(text);
    }

    /**
     * Converti una lista di stringhe con codici & in lista di Component
     */
    private static List<Component> colorizeList(List<String> texts) {
        if (texts == null) return new ArrayList<>();
        List<Component> components = new ArrayList<>();
        for (String text : texts) {
            components.add(colorize(text));
        }
        return components;
    }

    public void open(Player player, String boardKey) {
        open(player, boardKey, 1);
    }

    public void open(Player player, String boardKey, int page) {
        FileConfiguration config = plugin.getConfigurationManager().getRealmsGuiconfig();
        String path = "gui.leaderboard." + boardKey;
        String globalPath = "gui.leaderboard";

        String titleTemplate = config.getString(path + ".title",
                config.getString(globalPath + ".title", "&6&l⚔ Classifica &r&7- &e{category}"));

        // Ottieni il prefix dal messages.yml se necessario
        String prefix = plugin.getConfigurationManager().getConfig("messages.yml")
                .getString("general.prefix", "&6⚜ [Reami] &r");

        String title = titleTemplate
                .replace("%prefix%", prefix)
                .replace("{page}", String.valueOf(page))
                .replace("{category}", boardKey)
                .replace("{total}", "?");

        Component titleComp = colorize(title);

        int size = Math.max(9, config.getInt(path + ".size",
                config.getInt(globalPath + ".size", 54)));
        Inventory inv = Bukkit.createInventory(new LeaderboardHolder(boardKey, page), size, titleComp);

        String metric = config.getString(path + ".metric",
                config.getString(globalPath + ".metric", ""));
        int perPage = Math.max(1, config.getInt(path + ".per_page",
                config.getInt(globalPath + ".per_page", 28)));

        int fetchLimit = Math.max(1000, perPage * 10);
        List<LeaderboardEntry> allEntries = switch (metric) {
            case "towns.population" -> lbManager.getTopTownsByPopulation(fetchLimit);
            case "towns.wealth" -> lbManager.getTopTownsByWealth(fetchLimit);
            case "towns.claims" -> lbManager.getTopTownsByClaims(fetchLimit);
            case "towns.level" -> lbManager.getTopTownsByLevel(fetchLimit);
            case "kingdoms.population" -> lbManager.getTopKingdomsByPopulation(fetchLimit);
            case "kingdoms.towns" -> lbManager.getTopKingdomsByTowns(fetchLimit);
            case "kingdoms.wealth" -> lbManager.getTopKingdomsByWealth(fetchLimit);
            case "kingdoms.vassals" -> lbManager.getTopKingdomsByVassals(fetchLimit);
            case "kingdoms.level" -> lbManager.getTopKingdomsByLevel(fetchLimit);
            case "players.money" -> lbManager.getTopPlayersByWealth(fetchLimit);
            case "players.noble" -> lbManager.getTopPlayersByNobleTier(fetchLimit);
            case "empires.population" -> lbManager.getTopEmpiresByPopulation(fetchLimit);
            case "empires.kingdoms" -> lbManager.getTopEmpiresByKingdoms(fetchLimit);
            case "empires.claims" -> lbManager.getTopEmpiresByTerritory(fetchLimit);
            case "empires.level" -> lbManager.getTopEmpiresByLevel(fetchLimit);
            default -> List.of();
        };

        int totalEntries = allEntries.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / perPage));
        int currentPage = Math.min(Math.max(1, page), totalPages);
        int startIndex = (currentPage - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, totalEntries);

        List<LeaderboardEntry> entries = allEntries.subList(
                Math.min(startIndex, totalEntries),
                Math.min(endIndex, totalEntries)
        );

        // Popola gli slot
        int slot = 0;
        for (int i = 0; i < entries.size() && slot < size; i++) {
            LeaderboardEntry e = entries.get(i);
            ItemStack item = createItemForEntry(e, startIndex + i + 1, boardKey, config);
            inv.setItem(slot++, item);
        }

        // Pulsanti di navigazione
        int prevSlot = config.getInt(path + ".navigation.prev_button.slot",
                config.getInt(globalPath + ".navigation.prev_button.slot", 45));
        int nextSlot = config.getInt(path + ".navigation.next_button.slot",
                config.getInt(globalPath + ".navigation.next_button.slot", 53));
        int closeSlot = config.getInt(path + ".navigation.close_button.slot",
                config.getInt(globalPath + ".navigation.close_button.slot", 49));
        int pageInfoSlot = config.getInt(path + ".navigation.page_info.slot",
                config.getInt(globalPath + ".navigation.page_info.slot", 50));

        String prevName = config.getString(path + ".navigation.prev_button.display_name",
                config.getString(globalPath + ".navigation.prev_button.display_name", "&e◀ Pagina precedente"));
        String nextName = config.getString(path + ".navigation.next_button.display_name",
                config.getString(globalPath + ".navigation.next_button.display_name", "&ePagina successiva ▶"));
        String closeName = config.getString(path + ".navigation.close_button.display_name",
                config.getString(globalPath + ".navigation.close_button.display_name", "&cChiudi"));

        inv.setItem(prevSlot, createNavItem(Material.ARROW, colorize(prevName)));
        inv.setItem(nextSlot, createNavItem(Material.ARROW, colorize(nextName)));
        inv.setItem(closeSlot, createNavItem(Material.BARRIER, colorize(closeName)));

        String pageInfoText = config.getString(path + ".navigation.page_info.display_name",
                config.getString(globalPath + ".navigation.page_info.display_name", "&7Pagina {page}/{total}"));
        pageInfoText = pageInfoText
                .replace("{page}", String.valueOf(currentPage))
                .replace("{total}", String.valueOf(totalPages));
        inv.setItem(pageInfoSlot, createNavItem(Material.PAPER, colorize(pageInfoText)));

        player.openInventory(inv);

        // Suono apertura
        try {
            String openSound = config.getString(path + ".sounds.open",
                    config.getString(globalPath + ".sounds.open", null));
            double openVolume = config.getDouble(path + ".sounds.open_volume",
                    config.getDouble(globalPath + ".sounds.open_volume", 1.0));
            double openPitch = config.getDouble(path + ".sounds.open_pitch",
                    config.getDouble(globalPath + ".sounds.open_pitch", 1.0));

            if (openSound != null && !openSound.isEmpty()) {
                Sound s = SoundUtils.parseSound(openSound);
                if (s != null) {
                    player.playSound(player.getLocation(), s, (float) openVolume, (float) openPitch);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private ItemStack createItemForEntry(LeaderboardEntry e, int rank, String boardKey, FileConfiguration config) {
        String base = "gui.leaderboard." + boardKey;
        String global = "gui.leaderboard";

        String matName = config.getString(base + ".items.entry.material",
                config.getString(global + ".items.entry.material", "PLAYER_HEAD"));
        Material mat = Material.matchMaterial(matName.toUpperCase());
        if (mat == null) mat = Material.PLAYER_HEAD;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String nameTemplate = config.getString(base + ".items.entry.display_name",
                config.getString(global + ".items.entry.display_name", "&6{rank}. &e{name}"));
        String displayName = nameTemplate
                .replace("{rank}", String.valueOf(rank))
                .replace("{name}", e.getName() == null ? "Unknown" : e.getName())
                .replace("{value}", e.getFormattedValue());

        if (meta instanceof SkullMeta skullMeta) {
            if (e.getName() != null && config.getBoolean(base + ".items.entry.skull_owner_from_name", true)) {
                try {
                    java.util.UUID u = null;
                    try {
                        u = java.util.UUID.fromString(e.getName());
                    } catch (Exception ex) {
                        // Nome non è UUID
                    }
                    OfflinePlayer off = u != null ?
                            Bukkit.getOfflinePlayer(u) :
                            Bukkit.getOfflinePlayer(e.getName());
                    skullMeta.setOwningPlayer(off);
                } catch (Throwable ignored) {
                }
            }

            skullMeta.displayName(colorize(displayName));

            List<String> lore = config.getStringList(base + ".items.entry.lore");
            if (lore.isEmpty()) {
                lore = config.getStringList(global + ".items.entry.lore");
            }

            List<String> processedLore = lore.stream()
                    .map(l -> l.replace("{rank}", String.valueOf(rank))
                            .replace("{name}", e.getName() == null ? "Unknown" : e.getName())
                            .replace("{value}", e.getFormattedValue()))
                    .collect(Collectors.toList());

            skullMeta.lore(colorizeList(processedLore));
            item.setItemMeta(skullMeta);
            return item;
        }

        if (meta != null) {
            meta.displayName(colorize(displayName));

            List<String> lore = config.getStringList(base + ".items.entry.lore");
            if (lore.isEmpty()) {
                lore = config.getStringList(global + ".items.entry.lore");
            }

            List<String> processedLore = lore.stream()
                    .map(l -> l.replace("{rank}", String.valueOf(rank))
                            .replace("{name}", e.getName() == null ? "Unknown" : e.getName())
                            .replace("{value}", e.getFormattedValue()))
                    .collect(Collectors.toList());

            meta.lore(colorizeList(processedLore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavItem(Material mat, Component nameComp) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.displayName(nameComp);
            it.setItemMeta(im);
        }
        return it;
    }
}