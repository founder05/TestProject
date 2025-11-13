package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
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

@SuppressWarnings("unused")
public class MilitaryCommand implements CommandExecutor, TabCompleter {

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

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final MilitaryManager militaryManager;

    public MilitaryCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.militaryManager = plugin.getMilitaryManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("military.player_only"));
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
                    player.sendMessage(MessageUtils.get("military.no_permission_enlist"));
                    return true;
                }
                return handleEnlist(player, args);
            case "discharge":
                if (!player.hasPermission("nations.military.manage")) {
                    player.sendMessage(MessageUtils.get("military.no_permission_manage"));
                    return true;
                }
                return handleDischarge(player, args);
            case "promote":
                if (!player.hasPermission("nations.military.manage")) {
                    player.sendMessage(MessageUtils.get("military.no_permission_manage"));
                    return true;
                }
                return handlePromote(player, args);
            case "demote":
                if (!player.hasPermission("nations.military.manage")) {
                    player.sendMessage(MessageUtils.get("military.no_permission_manage"));
                    return true;
                }
                return handleDemote(player, args);
            case "info":
                if (!player.hasPermission("nations.military.view")) {
                    player.sendMessage(MessageUtils.get("military.no_permission_view"));
                    return true;
                }
                return handleInfo(player, args);
            case "list":
                if (!player.hasPermission("nations.military.view")) {
                    player.sendMessage(MessageUtils.get("military.no_permission_view"));
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
                player.sendMessage(MessageUtils.get("military.must_be_in_town"));
                return true;
            }

            Town town = societiesManager.getTown(data.getTown());
            if (!town.isMayor(player.getUniqueId())) {
                player.sendMessage(MessageUtils.get("military.only_mayor"));
                return true;
            }

            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(MessageUtils.get("military.player_not_found"));
                return true;
            }
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());

        if (targetData.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.target_must_be_in_town"));
            return true;
        }

        if (targetData.isMilitary()) {
            player.sendMessage(MessageUtils.format("military.already_in_military", Map.of("player", target.getName())));
            return true;
        }

        Town town = societiesManager.getTown(targetData.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("military.town_no_kingdom"));
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

        target.sendMessage(MessageUtils.format("military.enlist_target_success", Map.of("kingdom", kingdom.getName())));
        target.sendMessage(MessageUtils.format("military.enlist_target_rank", Map.of("rank", "RECRUIT")));
        target.sendMessage(MessageUtils.format("military.enlist_target_salary", Map.of("salary", String.valueOf((int) baseSalary))));

        if (!target.equals(player)) {
            player.sendMessage(MessageUtils.format("military.enlist_notify", Map.of("player", target.getName())));
        }

        return true;
    }

    private boolean handleDischarge(Player player, String[] args) {
        // /military discharge [player]
        Player target = player;

        if (args.length > 1) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getTown() == null) {
                player.sendMessage(MessageUtils.get("military.must_be_in_town"));
                return true;
            }

            Town town = societiesManager.getTown(data.getTown());
            if (!town.isMayor(player.getUniqueId())) {
                player.sendMessage(MessageUtils.get("military.only_mayor"));
                return true;
            }

            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(MessageUtils.get("military.player_not_found"));
                return true;
            }
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());

        if (!targetData.isMilitary()) {
            player.sendMessage(MessageUtils.format("military.info_player_not_in_military", Map.of("player", target.getName())));
            return true;
        }

        targetData.setMilitary(false);
        targetData.setMilitaryRank(null);

        target.sendMessage(MessageUtils.get("military.discharge_target_success"));

        if (!target.equals(player)) {
            player.sendMessage(MessageUtils.format("military.discharge_notify", Map.of("player", target.getName())));
        }

        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        // /military promote <player> <rank>
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("military.promote_usage"));
            player.sendMessage(MessageUtils.format("military.promote_ranks", Map.of("ranks", String.join(", ", RANK_HIERARCHY.keySet()))));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("military.town_no_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("evolution.only_capital_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("military.only_mayor"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("military.player_not_found"));
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.isMilitary()) {
            player.sendMessage(MessageUtils.format("military.info_player_not_in_military", Map.of("player", target.getName())));
            return true;
        }

        String newRank = args[2].toUpperCase();
        if (!RANK_HIERARCHY.containsKey(newRank)) {
            player.sendMessage(MessageUtils.get("military.promote_invalid_rank"));
            player.sendMessage(MessageUtils.format("military.promote_ranks", Map.of("ranks", String.join(", ", RANK_HIERARCHY.keySet()))));
            return true;
        }

        int newRankLevel = RANK_HIERARCHY.get(newRank);
        String currentRank = targetData.getMilitaryRank();
        int currentRankLevel = currentRank != null ? RANK_HIERARCHY.getOrDefault(currentRank, 0) : 0;

        if (newRankLevel <= currentRankLevel) {
            player.sendMessage(MessageUtils.get("military.promote_cannot_lower"));
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

        player.sendMessage(MessageUtils.format("military.promote_success_admin", Map.of("player", target.getName(), "rank", newRank)));
        target.sendMessage(MessageUtils.format("military.promote_success_target", Map.of("rank", newRank)));
        target.sendMessage(MessageUtils.format("military.promote_new_salary", Map.of("salary", String.valueOf((int) newSalary))));

        return true;
    }

    private boolean handleDemote(Player player, String[] args) {
        // /military demote <player> <rank>
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("military.demote_usage"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("military.only_mayor"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("military.player_not_found"));
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.isMilitary()) {
            player.sendMessage(MessageUtils.format("military.info_player_not_in_military", Map.of("player", target.getName())));
            return true;
        }

        String newRank = args[2].toUpperCase();
        if (!RANK_HIERARCHY.containsKey(newRank)) {
            player.sendMessage(MessageUtils.get("military.promote_invalid_rank"));
            return true;
        }

        targetData.setMilitaryRank(newRank);

        player.sendMessage(MessageUtils.format("military.demote_success_admin", Map.of("player", target.getName(), "rank", newRank)));
        target.sendMessage(MessageUtils.format("military.demote_notify_target", Map.of("rank", newRank)));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        Player target = player;

        if (args.length > 1) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(MessageUtils.get("military.player_not_found"));
                return true;
            }
        }

        PlayerData data = dataManager.getPlayerData(target.getUniqueId());

        if (!data.isMilitary()) {
            player.sendMessage(MessageUtils.format("military.info_player_not_in_military", Map.of("player", target.getName())));
            return true;
        }

        String rank = data.getMilitaryRank();
        int rankLevel = RANK_HIERARCHY.getOrDefault(rank, 0);

        player.sendMessage(MessageUtils.get("military.info_header"));
        player.sendMessage(MessageUtils.format("military.info_soldier", Map.of("player", target.getName())));
        player.sendMessage(MessageUtils.format("military.info_rank", Map.of("rank", rank)));
        player.sendMessage(MessageUtils.format("military.info_level", Map.of("level", String.valueOf(rankLevel))));

        if (data.getTown() != null) {
            Town town = societiesManager.getTown(data.getTown());
            if (town != null && town.getKingdom() != null) {
                player.sendMessage(MessageUtils.format("military.info_kingdom", Map.of("kingdom", town.getKingdom())));
            }
        }

        player.sendMessage(MessageUtils.get("military.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("military.town_no_kingdom"));
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

        player.sendMessage(MessageUtils.format("military.list_header", Map.of("kingdom", kingdom.getName())));
        player.sendMessage(MessageUtils.format("military.list_total_forces", Map.of("total", String.valueOf(totalMilitary))));
        player.sendMessage(MessageUtils.get("general.empty"));

        for (String rank : RANK_HIERARCHY.keySet()) {
            List<UUID> members = rankMembers.get(rank);
            if (!members.isEmpty()) {
                player.sendMessage(MessageUtils.format("military.list_rank_header", Map.of("rank", rank, "count", String.valueOf(members.size()))));
                for (UUID memberId : members) {
                    String memberName = plugin.getServer().getOfflinePlayer(memberId).getName();
                    player.sendMessage(MessageUtils.format("military.list_member_item", Map.of("name", memberName)));
                }
            }
        }

        player.sendMessage(MessageUtils.get("military.list_footer"));

        return true;
    }

    private boolean handleRanks(Player player, String[] args) {
        player.sendMessage(MessageUtils.get("military.ranks_header"));

        for (Map.Entry<String, Integer> entry : RANK_HIERARCHY.entrySet()) {
            String rank = entry.getKey();
            int level = entry.getValue();

            double salaryMultiplier = plugin.getConfigurationManager().getMilitaryConfig()
                    .getDouble("military-system.military-ranks." + rank.toLowerCase() + ".salary-multiplier", 1.0);

            player.sendMessage(MessageUtils.format("military.ranks_line", Map.of("level", String.valueOf(level), "rank", rank)));
            player.sendMessage(MessageUtils.format("military.ranks_salary_multiplier", Map.of("multiplier", String.valueOf(salaryMultiplier))));
        }

        player.sendMessage(MessageUtils.get("military.ranks_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("military.help_header"));
        player.sendMessage(MessageUtils.get("military.help_enlist"));
        player.sendMessage(MessageUtils.get("military.help_discharge"));
        player.sendMessage(MessageUtils.get("military.help_promote"));
        player.sendMessage(MessageUtils.get("military.help_demote"));
        player.sendMessage(MessageUtils.get("military.help_info"));
        player.sendMessage(MessageUtils.get("military.help_list"));
        player.sendMessage(MessageUtils.get("military.help_ranks"));
        player.sendMessage(MessageUtils.get("military.help_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(Arrays.asList("enlist", "discharge", "promote", "demote", "info", "list", "ranks"), args[0]);
        }

        // suggest online players for second argument (player names)
        if (args.length == 2) {
            if (Arrays.asList("enlist", "discharge", "promote", "demote", "info").contains(args[0].toLowerCase())) {
                return me.marcdoesntexists.nations.utils.TabCompletionUtils.onlinePlayers(args[1]);
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote")) {
                return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(new ArrayList<>(RANK_HIERARCHY.keySet()), args[2]);
            }
        }

        return new ArrayList<>();
    }
}