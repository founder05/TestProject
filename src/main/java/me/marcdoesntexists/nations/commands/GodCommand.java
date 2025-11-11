package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.*;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class GodCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final ReligionService religionService;

    public GodCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.religionService = plugin.getReligionService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(player, args);
            case "worship":
                return handleWorship(player, args);
            case "altar":
                return handleAltar(player, args);
            case "sacrifice":
                return handleSacrifice(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player, args);
            case "followers":
                return handleFollowers(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        // /god create <name> <domain> <description...>
        if (args.length < 4) {
            player.sendMessage("§cUsage: /god create <name> <domain> <description...>");
            player.sendMessage("§7Example: /god create Zeus Lightning God of thunder and sky");
            return true;
        }

        // permission check
        if (!player.hasPermission("nations.god.create")) {
            player.sendMessage("§cYou do not have permission to create gods.");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() == null) {
            player.sendMessage("§cYou must be part of a religion to create a god!");
            player.sendMessage("§7Use §e/religion create <name>§7 first!");
            return true;
        }

        Religion religion = societiesManager.getReligion(data.getReligion());
        if (religion == null) {
            player.sendMessage("§cReligion not found!");
            return true;
        }

        if (!religion.getFounder().equals(player.getUniqueId())) {
            player.sendMessage("§cOnly the religion founder can create gods!");
            return true;
        }

        // Check max gods per religion
        int maxGods = plugin.getConfigurationManager().getReligionConfig()
                .getInt("religion-system.gods.max-gods-per-religion", 5);

        long currentGods = societiesManager.getAllGods().stream()
                .filter(g -> g.getRelatedReligion().equals(religion))
                .count();

        if (currentGods >= maxGods) {
            player.sendMessage("§cMaximum gods per religion reached! (§6" + maxGods + "§c)");
            return true;
        }

        String godName = args[1];
        String domain = args[2];
        StringBuilder descBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            descBuilder.append(args[i]).append(" ");
        }
        String description = descBuilder.toString().trim();

        // Check if god already exists
        if (societiesManager.getGod(godName) != null) {
            player.sendMessage("§cA god with this name already exists!");
            return true;
        }

        int creationCost = plugin.getConfigurationManager().getReligionConfig()
                .getInt("religion-system.gods.god-creation-cost", 2000);

        if (data.getMoney() < creationCost) {
            player.sendMessage("§cInsufficient funds! Need §6$" + creationCost);
            player.sendMessage("§7Your balance: §6$" + data.getMoney());
            return true;
        }

        if (religionService.createGod(godName, description, domain, player.getUniqueId(), data.getReligion())) {
            data.removeMoney(creationCost);

            player.sendMessage("§a✔ God §6" + godName + "§a created!");
            player.sendMessage("§7Domain: §e" + domain);
            player.sendMessage("§7Description: §e" + description);
            player.sendMessage("§7Religion: §6" + religion.getName());
            player.sendMessage("§7Cost: §6$" + creationCost);

            // Notify all followers
            for (UUID followerId : religion.getFollowers()) {
                Player follower = plugin.getServer().getPlayer(followerId);
                if (follower != null && !follower.equals(player)) {
                    follower.sendMessage("§7[§6" + religion.getName() + "§7] §eA new god has been created: §6" + godName);
                    follower.sendMessage("§7Domain: §e" + domain);
                }
            }
        } else {
            player.sendMessage("§cFailed to create god!");
        }

        return true;
    }

    private boolean handleWorship(Player player, String[] args) {
        // /god worship <name>
        if (args.length < 2) {
            player.sendMessage("§cUsage: /god worship <name>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() == null) {
            player.sendMessage("§cYou must be part of a religion to worship gods!");
            return true;
        }

        String godName = args[1];
        God god = societiesManager.getGod(godName);

        if (god == null) {
            player.sendMessage("§cGod not found!");
            return true;
        }

        Religion religion = societiesManager.getReligion(data.getReligion());
        if (!god.getRelatedReligion().equals(religion)) {
            player.sendMessage("§cThis god is not part of your religion!");
            return true;
        }

        if (god.getFollowers().contains(player.getUniqueId())) {
            player.sendMessage("§cYou already worship §6" + godName + "§c!");
            return true;
        }

        god.addFollower(player.getUniqueId());

        int affinityGain = plugin.getConfigurationManager().getReligionConfig()
                .getInt("religion-system.faith.affinity-gain-per-action", 5);
        int currentAffinity = data.getGodAffinity(godName);
        data.setGodAffinity(godName, currentAffinity + affinityGain);

        player.sendMessage("§a✔ You now worship §6" + godName + "§a!");
        player.sendMessage("§7Domain: §e" + god.getDomain());
        player.sendMessage("§7Affinity: §6" + data.getGodAffinity(godName));

        return true;
    }

    private boolean handleAltar(Player player, String[] args) {
        // /god altar <create|remove> <godName>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /god altar <create|remove> <godName>");
            return true;
        }

        String action = args[1].toLowerCase();
        String godName = args[2];

        God god = societiesManager.getGod(godName);
        if (god == null) {
            player.sendMessage("§cGod not found!");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        Religion religion = societiesManager.getReligion(data.getReligion());

        if (religion == null || !god.getRelatedReligion().equals(religion)) {
            player.sendMessage("§cThis god is not part of your religion!");
            return true;
        }

        String location = player.getLocation().getWorld().getName() + "," +
                player.getLocation().getBlockX() + "," +
                player.getLocation().getBlockY() + "," +
                player.getLocation().getBlockZ();

        if (action.equals("create")) {
            int altarCost = plugin.getConfigurationManager().getReligionConfig()
                    .getInt("religion-system.religious-buildings.altar-construction-cost", 1000);

            if (data.getMoney() < altarCost) {
                player.sendMessage("§cInsufficient funds! Need §6$" + altarCost);
                return true;
            }

            if (religionService.addAltarToGod(godName, location)) {
                data.removeMoney(altarCost);

                int powerGain = plugin.getConfigurationManager().getReligionConfig()
                        .getInt("religion-system.gods.power-gain-per-action", 1);
                religionService.addGodPower(godName, powerGain * 10); // Altars give more power

                player.sendMessage("§a✔ Altar to §6" + godName + "§a built!");
                player.sendMessage("§7Location: §e" + location);
                player.sendMessage("§7Cost: §6$" + altarCost);
                player.sendMessage("§7God power increased by §6" + (powerGain * 10));
            } else {
                player.sendMessage("§cFailed to create altar!");
            }

        } else if (action.equals("remove")) {
            if (god.hasAltar(location)) {
                god.removeAltar(location);
                player.sendMessage("§a✔ Altar removed!");
            } else {
                player.sendMessage("§cNo altar found at this location!");
            }
        } else {
            player.sendMessage("§cUsage: /god altar <create|remove> <godName>");
        }

        return true;
    }

    private boolean handleSacrifice(Player player, String[] args) {
        // /god sacrifice <godName> <amount>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /god sacrifice <godName> <amount>");
            return true;
        }

        String godName = args[1];
        God god = societiesManager.getGod(godName);

        if (god == null) {
            player.sendMessage("§cGod not found!");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (!god.getFollowers().contains(player.getUniqueId())) {
            player.sendMessage("§cYou must worship §6" + godName + "§c to make sacrifices!");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                player.sendMessage("§cAmount must be positive!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return true;
        }

        if (data.getMoney() < amount) {
            player.sendMessage("§cInsufficient funds!");
            return true;
        }

        data.removeMoney(amount);

        // Calculate power gain (1 power per 10 coins)
        int powerGain = amount / 10;
        religionService.addGodPower(godName, powerGain);

        // Increase affinity
        int affinityGain = amount / 100;
        int currentAffinity = data.getGodAffinity(godName);
        data.setGodAffinity(godName, currentAffinity + affinityGain);

        player.sendMessage("§a✔ Sacrifice made to §6" + godName + "§a!");
        player.sendMessage("§7Offering: §6$" + amount);
        player.sendMessage("§7God power increased: §6+" + powerGain);
        player.sendMessage("§7Your affinity increased: §6+" + affinityGain + " §7(Total: §6" + data.getGodAffinity(godName) + "§7)");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /god info <name>");
            return true;
        }

        String godName = args[1];
        God god = societiesManager.getGod(godName);

        if (god == null) {
            player.sendMessage("§cGod not found!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6" + god.getName() + "§7 §m----------");
        player.sendMessage("§eDomain: §6" + god.getDomain());
        player.sendMessage("§eDescription: §7" + god.getDescription());
        player.sendMessage("§eReligion: §6" + god.getRelatedReligion().getName());
        player.sendMessage("§ePower: §6" + god.getPower());
        player.sendMessage("§eFollowers: §6" + god.getFollowerCount());
        player.sendMessage("§eAltars: §6" + god.getAltars().size());

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (god.getFollowers().contains(player.getUniqueId())) {
            player.sendMessage("§aYou worship this god!");
            player.sendMessage("§7Your affinity: §6" + data.getGodAffinity(godName));
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<God> gods = societiesManager.getAllGods();

        if (gods.isEmpty()) {
            player.sendMessage("§cNo gods exist yet!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Gods §7(" + gods.size() + ")§m----------");

        for (God god : gods) {
            player.sendMessage("§e• §6" + god.getName() + " §7- §e" + god.getDomain());
            player.sendMessage("§7  Religion: §6" + god.getRelatedReligion().getName() +
                    " §7| Power: §6" + god.getPower() +
                    " §7| Followers: §6" + god.getFollowerCount());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleFollowers(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /god followers <name>");
            return true;
        }

        String godName = args[1];
        God god = societiesManager.getGod(godName);

        if (god == null) {
            player.sendMessage("§cGod not found!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Followers of " + god.getName() + "§7 §m----------");
        player.sendMessage("§eTotal: §6" + god.getFollowerCount());
        player.sendMessage("");

        for (UUID followerId : god.getFollowers()) {
            String followerName = plugin.getServer().getOfflinePlayer(followerId).getName();
            player.sendMessage("§7• §e" + followerName);
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6God Commands§7 §m----------");
        player.sendMessage("§e/god create <name> <domain> <desc...>§7 - Create god");
        player.sendMessage("§e/god worship <name>§7 - Worship a god");
        player.sendMessage("§e/god altar create <name>§7 - Build altar");
        player.sendMessage("§e/god altar remove <name>§7 - Remove altar");
        player.sendMessage("§e/god sacrifice <name> <$>§7 - Make sacrifice");
        player.sendMessage("§e/god info <name>§7 - View god info");
        player.sendMessage("§e/god list§7 - List all gods");
        player.sendMessage("§e/god followers <name>§7 - View followers");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "worship", "altar", "sacrifice", "info", "list", "followers")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("altar")) {
                return Arrays.asList("create", "remove")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }

            if (args[0].equalsIgnoreCase("worship") || args[0].equalsIgnoreCase("info") ||
                    args[0].equalsIgnoreCase("sacrifice") || args[0].equalsIgnoreCase("followers")) {
                return societiesManager.getAllGods().stream()
                        .map(God::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("altar")) {
            return societiesManager.getAllGods().stream()
                    .map(God::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return new ArrayList<>();
    }
}