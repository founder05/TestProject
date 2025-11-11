package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.MilitaryManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.military.MilitaryRank;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
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
public class MilitaryCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final MilitaryManager militaryManager;

    // Predefined military ranks
    private static final Map<String, Integer> RANK_HIERARCHY = new LinkedHashMap<>();
    static {
        RANK_HIERARCHY.put("RECRUIT", 1);
        RANK_HIERARCHY.put("SOLDIER", 4);
        RANK_HIERARCHY.put("SERGEANT", 5);
        RANK_HIERARCHY.put("LIEUTENANT", 6);
        RANK_HIERARCHY.put("CAPTAIN", 7);
        RANK_HIERARCHY.put("COMMANDER", 8);
        RANK_HIERARCHY.put("GENERAL", 9);
        RANK_HIERARCHY.put("SUPREME_COMMANDER", 10);
    }

    public MilitaryCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.militaryManager = plugin.getMilitaryManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
            case "enlist":
                // enlist others requires permission
                if (args.length > 1 && !player.hasPermission("nations.military.enlist")) {
                    player.sendMessage("§cYou do not have permission to enlist others.");
                    return true;
                }
                return handleEnlist(player, args);
            case "discharge":
                if (!player.hasPermission("nations.military.manage")) {
                    player.sendMessage("§cYou do not have permission to discharge members.");
                    return true;
                }
                return handleDischarge(player, args);
            case "promote":
                if (!player.hasPermission("nations.military.manage")) {
                    player.sendMessage("§cYou do not have permission to promote members.");
                    return true;
                }
                return handlePromote(player, args);
            case "demote":
                if (!player.hasPermission("nations.military.manage")) {
                    player.sendMessage("§cYou do not have permission to demote members.");
                    return true;
                }
                return handleDemote(player, args);
            case "info":
                if (!player.hasPermission("nations.military.view")) {
                    player.sendMessage("§cYou do not have permission to view military info.");
                    return true;
                }
                return handleInfo(player, args);
            case "list":
                if (!player.hasPermission("nations.military.view")) {
                    player.sendMessage("§cYou do not have permission to view the military list.");
                    return true;
                }
                return handleList(player, args);
            case "ranks":
                return handleRanks(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleEnlist(Player player, String[] args) {
        // /military enlist [player]
        Player target = player;

        if (args.length > 1) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getTown() == null) {
                player.sendMessage("§cYou must be in a town!");
                return true;
            }

            Town town = societiesManager.getTown(data.getTown());
            if (!town.isMayor(player.getUniqueId())) {
                player.sendMessage("§cOnly the mayor can enlist others!");
                return true;
            }

            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());

        if (targetData.getTown() == null) {
            player.sendMessage("§cTarget must be in a town!");
            return true;
        }

        if (targetData.isMilitary()) {
            player.sendMessage("§c" + target.getName() + " is already in the military!");
            return true;
        }

        Town town = societiesManager.getTown(targetData.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom to have a military!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        // Create and register military rank
        double baseSalary = plugin.getConfigurationManager().getMilitaryConfig()
                .getDouble("military-system.military-ranks.recruit.salary-multiplier", 0.5) * 100;

        MilitaryRank rank = new MilitaryRank("RECRUIT", 1, baseSalary, kingdom.getName());
        militaryManager.registerRank(rank);

        targetData.setMilitary(true);
        targetData.setMilitaryRank("RECRUIT");

        target.sendMessage("§a✔ You have enlisted in the military of §6" + kingdom.getName() + "§a!");
        target.sendMessage("§7Rank: §eRECRUIT");
        target.sendMessage("§7Salary: §6$" + (int)baseSalary + "§7/day");

        if (!target.equals(player)) {
            player.sendMessage("§a✔ §6" + target.getName() + "§a has been enlisted!");
        }

        return true;
    }

    private boolean handleDischarge(Player player, String[] args) {
        // /military discharge [player]
        Player target = player;

        if (args.length > 1) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getTown() == null) {
                player.sendMessage("§cYou must be in a town!");
                return true;
            }

            Town town = societiesManager.getTown(data.getTown());
            if (!town.isMayor(player.getUniqueId())) {
                player.sendMessage("§cOnly the mayor can discharge others!");
                return true;
            }

            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());

        if (!targetData.isMilitary()) {
            player.sendMessage("§c" + target.getName() + " is not in the military!");
            return true;
        }

        targetData.setMilitary(false);
        targetData.setMilitaryRank(null);

        target.sendMessage("§c✘ You have been discharged from military service!");

        if (!target.equals(player)) {
            player.sendMessage("§a✔ §6" + target.getName() + "§a has been discharged!");
        }

        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        // /military promote <player> <rank>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /military promote <player> <rank>");
            player.sendMessage("§7Ranks: " + String.join(", ", RANK_HIERARCHY.keySet()));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage("§cOnly the capital's mayor can promote military ranks!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.isMilitary()) {
            player.sendMessage("§c" + target.getName() + " is not in the military!");
            return true;
        }

        String newRank = args[2].toUpperCase();
        if (!RANK_HIERARCHY.containsKey(newRank)) {
            player.sendMessage("§cInvalid rank!");
            player.sendMessage("§7Valid ranks: " + String.join(", ", RANK_HIERARCHY.keySet()));
            return true;
        }

        int newRankLevel = RANK_HIERARCHY.get(newRank);
        String currentRank = targetData.getMilitaryRank();
        int currentRankLevel = currentRank != null ? RANK_HIERARCHY.getOrDefault(currentRank, 0) : 0;

        if (newRankLevel <= currentRankLevel) {
            player.sendMessage("§cCannot promote to a lower or equal rank!");
            return true;
        }

        // Calculate new salary
        double salaryMultiplier = plugin.getConfigurationManager().getMilitaryConfig()
                .getDouble("military-system.military-ranks." + newRank.toLowerCase() + ".salary-multiplier", 1.0);
        double newSalary = salaryMultiplier * 100;

        // Create and register new rank
        MilitaryRank rank = new MilitaryRank(newRank, newRankLevel, newSalary, kingdom.getName());
        militaryManager.registerRank(rank);

        targetData.setMilitaryRank(newRank);

        player.sendMessage("§a✔ §6" + target.getName() + "§a promoted to §e" + newRank + "§a!");
        target.sendMessage("§a✔ You have been promoted to §e" + newRank + "§a!");
        target.sendMessage("§7New Salary: §6$" + (int)newSalary + "§7/day");

        return true;
    }

    private boolean handleDemote(Player player, String[] args) {
        // /military demote <player> <rank>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /military demote <player> <rank>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can demote military ranks!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.isMilitary()) {
            player.sendMessage("§c" + target.getName() + " is not in the military!");
            return true;
        }

        String newRank = args[2].toUpperCase();
        if (!RANK_HIERARCHY.containsKey(newRank)) {
            player.sendMessage("§cInvalid rank!");
            return true;
        }

        targetData.setMilitaryRank(newRank);

        player.sendMessage("§a✔ §6" + target.getName() + "§a demoted to §e" + newRank);
        target.sendMessage("§c✘ You have been demoted to §e" + newRank);

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        Player target = player;

        if (args.length > 1) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }
        }

        PlayerData data = dataManager.getPlayerData(target.getUniqueId());

        if (!data.isMilitary()) {
            player.sendMessage("§c" + target.getName() + " is not in the military!");
            return true;
        }

        String rank = data.getMilitaryRank();
        int rankLevel = RANK_HIERARCHY.getOrDefault(rank, 0);

        player.sendMessage("§7§m----------§r §6Military Info§7 §m----------");
        player.sendMessage("§eSoldier: §6" + target.getName());
        player.sendMessage("§eRank: §e" + rank);
        player.sendMessage("§eLevel: §6" + rankLevel);

        if (data.getTown() != null) {
            Town town = societiesManager.getTown(data.getTown());
            if (town != null && town.getKingdom() != null) {
                player.sendMessage("§eKingdom: §6" + town.getKingdom());
            }
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        Map<String, List<UUID>> rankMembers = new LinkedHashMap<>();
        for (String rank : RANK_HIERARCHY.keySet()) {
            rankMembers.put(rank, new ArrayList<>());
        }

        // Collect all military members from all kingdom towns
        for (String townName : kingdom.getTowns()) {
            Town t = societiesManager.getTown(townName);
            if (t != null) {
                for (UUID memberId : t.getMembers()) {
                    PlayerData memberData = dataManager.getPlayerData(memberId);
                    if (memberData.isMilitary() && memberData.getMilitaryRank() != null) {
                        rankMembers.get(memberData.getMilitaryRank()).add(memberId);
                    }
                }
            }
        }

        int totalMilitary = rankMembers.values().stream().mapToInt(List::size).sum();

        player.sendMessage("§7§m----------§r §6Military of " + kingdom.getName() + "§7 §m----------");
        player.sendMessage("§eTotal Forces: §6" + totalMilitary);
        player.sendMessage("");

        for (String rank : RANK_HIERARCHY.keySet()) {
            List<UUID> members = rankMembers.get(rank);
            if (!members.isEmpty()) {
                player.sendMessage("§e" + rank + "§7 (" + members.size() + "):");
                for (UUID memberId : members) {
                    String memberName = plugin.getServer().getOfflinePlayer(memberId).getName();
                    player.sendMessage("§7 • §6" + memberName);
                }
            }
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleRanks(Player player, String[] args) {
        player.sendMessage("§7§m----------§r §6Military Ranks§7 §m----------");

        for (Map.Entry<String, Integer> entry : RANK_HIERARCHY.entrySet()) {
            String rank = entry.getKey();
            int level = entry.getValue();

            double salaryMultiplier = plugin.getConfigurationManager().getMilitaryConfig()
                    .getDouble("military-system.military-ranks." + rank.toLowerCase() + ".salary-multiplier", 1.0);

            player.sendMessage("§e" + level + ". §6" + rank);
            player.sendMessage("§7   Salary Multiplier: §ax" + salaryMultiplier);
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Military Commands§7 §m----------");
        player.sendMessage("§e/military enlist [player]§7 - Enlist in military");
        player.sendMessage("§e/military discharge [player]§7 - Discharge from military");
        player.sendMessage("§e/military promote <p> <rank>§7 - Promote soldier");
        player.sendMessage("§e/military demote <p> <rank>§7 - Demote soldier");
        player.sendMessage("§e/military info [player]§7 - View military info");
        player.sendMessage("§e/military list§7 - List all soldiers");
        player.sendMessage("§e/military ranks§7 - View rank hierarchy");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("enlist", "discharge", "promote", "demote", "info", "list", "ranks")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // suggest online players for second argument (player names)
        if (args.length == 2) {
            if (Arrays.asList("enlist", "discharge", "promote", "demote", "info").contains(args[0].toLowerCase())) {
                String partial = args[1].toLowerCase();
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .toList();
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote")) {
                String partial = args[2].toUpperCase();
                return RANK_HIERARCHY.keySet().stream()
                        .filter(s -> s.startsWith(partial))
                        .toList();
            }
        }

        return new ArrayList<>();
    }
}