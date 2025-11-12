package me.marcdoesntexists.nations.gui;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Empire;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class NationsGUI implements Listener {

    private static final Map<UUID, String> playerCurrentCategory = new HashMap<>();

    private static final String CAT_TOWNS = "TOWNS";
    private static final String CAT_KINGDOMS = "KINGDOMS";
    private static final String CAT_EMPIRES = "EMPIRES";

    public static void openMainGUI(Player player, String category) {
        if (player == null || category == null) return;
        category = category.toUpperCase(Locale.ROOT);
        if (!category.equals(CAT_TOWNS) && !category.equals(CAT_KINGDOMS) && !category.equals(CAT_EMPIRES)) {
            category = CAT_TOWNS;
        }

        playerCurrentCategory.put(player.getUniqueId(), category);

        Nations plugin = Nations.getInstance();
        FileConfiguration config = plugin.getConfigurationManager().getConfig("gui.yml");

        String title = config.getString("gui." + category.toLowerCase(Locale.ROOT) + ".title",
                "Nations - " + category);
        int size = config.getInt("gui." + category.toLowerCase(Locale.ROOT) + ".size", 54);
        // Ensure size is multiple of 9 and at least 9
        if (size < 9 || size % 9 != 0) {
            size = 54;
        }

        Inventory inv = Bukkit.createInventory(null, size, title);

        addNavigationButtons(inv, category, config);

        int maxSlot = size - 9; // reserve bottom row for navigation/filler
        switch (category) {
            case CAT_TOWNS:
                addTownsToGUI(inv, config, maxSlot);
                break;
            case CAT_KINGDOMS:
                addKingdomsToGUI(inv, config, maxSlot);
                break;
            case CAT_EMPIRES:
                addEmpiresToGUI(inv, config, maxSlot);
                break;
            default:
                // Should never happen
                addTownsToGUI(inv, config, maxSlot);
                break;
        }

        player.openInventory(inv);
    }

    private static void addNavigationButtons(Inventory inv, String currentCategory, FileConfiguration config) {
        int townsSlot = config.getInt("gui.navigation.towns-button.slot", 45);
        int kingdomsSlot = config.getInt("gui.navigation.kingdoms-button.slot", 49);
        int empiresSlot = config.getInt("gui.navigation.empires-button.slot", 53);

        ItemStack townsButton = createNavigationButton(
                CAT_TOWNS,
                config.getString("gui.navigation.towns-button.material", "OAK_SAPLING"),
                config.getString("gui.navigation.towns-button.name", "§6Towns"),
                config.getStringList("gui.navigation.towns-button.lore"),
                currentCategory.equals(CAT_TOWNS)
        );
        inv.setItem(townsSlot, townsButton);

        ItemStack kingdomsButton = createNavigationButton(
                CAT_KINGDOMS,
                config.getString("gui.navigation.kingdoms-button.material", "IRON_SWORD"),
                config.getString("gui.navigation.kingdoms-button.name", "§6Kingdoms"),
                config.getStringList("gui.navigation.kingdoms-button.lore"),
                currentCategory.equals(CAT_KINGDOMS)
        );
        inv.setItem(kingdomsSlot, kingdomsButton);

        ItemStack empiresButton = createNavigationButton(
                CAT_EMPIRES,
                config.getString("gui.navigation.empires-button.material", "DIAMOND_SWORD"),
                config.getString("gui.navigation.empires-button.name", "§6Empires"),
                config.getStringList("gui.navigation.empires-button.lore"),
                currentCategory.equals(CAT_EMPIRES)
        );
        inv.setItem(empiresSlot, empiresButton);

        Material fillMaterial;
        try {
            fillMaterial = Material.valueOf(config.getString("gui.fill-material", "GRAY_STAINED_GLASS_PANE"));
        } catch (IllegalArgumentException e) {
            fillMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack filler = new ItemStack(fillMaterial);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        // fill bottom row if empty
        int size = inv.getSize();
        int start = size - 9;
        for (int i = start; i < size; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, filler);
            }
        }
    }

    private static ItemStack createNavigationButton(String category, String materialName, String name, List<String> lore, boolean current) {
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.PAPER;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (current) {
            meta.setDisplayName(name + " §a✔");
            List<String> newLore = new ArrayList<>(lore);
            newLore.add("");
            newLore.add("§a▶ Currently viewing");
            meta.setLore(newLore);
        } else {
            meta.setDisplayName(name);
            List<String> newLore = new ArrayList<>(lore);
            newLore.add("");
            newLore.add("§7Click to view");
            meta.setLore(newLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static void addTownsToGUI(Inventory inv, FileConfiguration config, int maxSlot) {
        SocietiesManager manager = Nations.getInstance().getSocietiesManager();
        Collection<Town> towns = manager.getAllTowns();
        int slot = 0;

        for (Town town : towns) {
            if (slot >= maxSlot) break;
            ItemStack townItem = createTownItem(town, config);
            inv.setItem(slot, townItem);
            slot++;
        }
    }

    private static void addKingdomsToGUI(Inventory inv, FileConfiguration config, int maxSlot) {
        SocietiesManager manager = Nations.getInstance().getSocietiesManager();
        Collection<Kingdom> kingdoms = manager.getAllKingdoms();
        int slot = 0;

        for (Kingdom kingdom : kingdoms) {
            if (slot >= maxSlot) break;
            ItemStack kingdomItem = createKingdomItem(kingdom, config);
            inv.setItem(slot, kingdomItem);
            slot++;
        }
    }

    private static void addEmpiresToGUI(Inventory inv, FileConfiguration config, int maxSlot) {
        SocietiesManager manager = Nations.getInstance().getSocietiesManager();
        Collection<Empire> empires = manager.getAllEmpires();
        int slot = 0;

        for (Empire empire : empires) {
            if (slot >= maxSlot) break;
            ItemStack empireItem = createEmpireItem(empire, config);
            inv.setItem(slot, empireItem);
            slot++;
        }
    }

    private static ItemStack createTownItem(Town town, FileConfiguration config) {
        OfflinePlayer mayor = Bukkit.getOfflinePlayer(town.getMayor());
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(mayor);
            String nameFormat = config.getString("gui.towns.item.name", "§6{name}");
            meta.setDisplayName(nameFormat.replace("{name}", town.getName()));

            List<String> loreFormat = config.getStringList("gui.towns.item.lore");
            List<String> lore = new ArrayList<>();
            for (String line : loreFormat) {
                String replaced = line
                        .replace("{name}", town.getName())
                        .replace("{mayor}", (mayor.getName() != null ? mayor.getName() : "Unknown"))
                        .replace("{members}", String.valueOf(town.getMembers().size()))
                        .replace("{claims}", String.valueOf(town.getClaims().size()))
                        .replace("{balance}", String.valueOf(town.getBalance()))
                        .replace("{level}", String.valueOf(town.getProgressionLevel()))
                        .replace("{kingdom}", town.getKingdom() != null ? town.getKingdom() : "None");
                lore.add(replaced);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createKingdomItem(Kingdom kingdom, FileConfiguration config) {
        SocietiesManager manager = Nations.getInstance().getSocietiesManager();
        Town capitalTown = manager.getTown(kingdom.getCapital());
        OfflinePlayer king = (capitalTown != null ? Bukkit.getOfflinePlayer(capitalTown.getMayor()) : null);

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (king != null) {
                meta.setOwningPlayer(king);
            }
            String nameFormat = config.getString("gui.kingdoms.item.name", "§6{name}");
            meta.setDisplayName(nameFormat.replace("{name}", kingdom.getName()));

            List<String> loreFormat = config.getStringList("gui.kingdoms.item.lore");
            List<String> lore = new ArrayList<>();

            int totalPopulation = 0;
            int totalClaims = 0;
            for (String townName : kingdom.getTowns()) {
                Town town = manager.getTown(townName);
                if (town != null) {
                    totalPopulation += town.getMembers().size();
                    totalClaims += town.getClaims().size();
                }
            }

            for (String line : loreFormat) {
                String replaced = line
                        .replace("{name}", kingdom.getName())
                        .replace("{capital}", kingdom.getCapital())
                        .replace("{king}", (king != null && king.getName() != null ? king.getName() : "Unknown"))
                        .replace("{towns}", String.valueOf(kingdom.getTowns().size()))
                        .replace("{population}", String.valueOf(totalPopulation))
                        .replace("{claims}", String.valueOf(totalClaims))
                        .replace("{vassals}", String.valueOf(kingdom.getVassals().size()))
                        .replace("{allies}", String.valueOf(kingdom.getAllies().size()))
                        .replace("{wars}", String.valueOf(kingdom.getWars().size()))
                        .replace("{level}", String.valueOf(kingdom.getProgressionLevel()))
                        .replace("{empire}", kingdom.getEmpire() != null ? kingdom.getEmpire() : "None");
                lore.add(replaced);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createEmpireItem(Empire empire, FileConfiguration config) {
        SocietiesManager manager = Nations.getInstance().getSocietiesManager();

        OfflinePlayer emperor = null;
        if (!empire.getKingdoms().isEmpty()) {
            String firstKingdomName = empire.getKingdoms().iterator().next();
            Kingdom firstKingdom = manager.getKingdom(firstKingdomName);
            if (firstKingdom != null) {
                Town capitalTown = manager.getTown(firstKingdom.getCapital());
                if (capitalTown != null) {
                    emperor = Bukkit.getOfflinePlayer(capitalTown.getMayor());
                }
            }
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (emperor != null) {
                meta.setOwningPlayer(emperor);
            }
            String nameFormat = config.getString("gui.empires.item.name", "§d{name}");
            meta.setDisplayName(nameFormat.replace("{name}", empire.getName()));

            List<String> loreFormat = config.getStringList("gui.empires.item.lore");
            List<String> lore = new ArrayList<>();

            int totalPopulation = 0;
            int totalClaims = 0;
            int totalTowns = 0;

            for (String kingdomName : empire.getKingdoms()) {
                Kingdom kingdom = manager.getKingdom(kingdomName);
                if (kingdom != null) {
                    for (String townName : kingdom.getTowns()) {
                        Town town = manager.getTown(townName);
                        if (town != null) {
                            totalPopulation += town.getMembers().size();
                            totalClaims += town.getClaims().size();
                            totalTowns++;
                        }
                    }
                }
            }

            for (String line : loreFormat) {
                String replaced = line
                        .replace("{name}", empire.getName())
                        .replace("{emperor}", (emperor != null && emperor.getName() != null ? emperor.getName() : "Unknown"))
                        .replace("{kingdoms}", String.valueOf(empire.getKingdoms().size()))
                        .replace("{towns}", String.valueOf(totalTowns))
                        .replace("{population}", String.valueOf(totalPopulation))
                        .replace("{claims}", String.valueOf(totalClaims))
                        .replace("{level}", String.valueOf(empire.getProgressionLevel()));
                lore.add(replaced);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        FileConfiguration config = Nations.getInstance().getConfigurationManager().getConfig("gui.yml");

        // check if this title matches any of the categories
        String titleTowns = config.getString("gui.towns.title", "Nations - Towns");
        String titleKingdoms = config.getString("gui.kingdoms.title", "Nations - Kingdoms");
        String titleEmpires = config.getString("gui.empires.title", "Nations - Empires");

        if (!title.equals(titleTowns) && !title.equals(titleKingdoms) && !title.equals(titleEmpires)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getSlot();
        int townsSlot = config.getInt("gui.navigation.towns-button.slot", 45);
        int kingdomsSlot = config.getInt("gui.navigation.kingdoms-button.slot", 49);
        int empiresSlot = config.getInt("gui.navigation.empires-button.slot", 53);

        if (slot == townsSlot) {
            openMainGUI(player, CAT_TOWNS);
        } else if (slot == kingdomsSlot) {
            openMainGUI(player, CAT_KINGDOMS);
        } else if (slot == empiresSlot) {
            openMainGUI(player, CAT_EMPIRES);
        }
    }
}
