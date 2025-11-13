package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Alliance;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class AllianceCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;

    public AllianceCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("alliance.player_only"));
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
            case "invite":
                return handleInvite(player, args);
            case "accept":
                return handleAccept(player, args);
            case "leave":
                return handleLeave(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("alliance.usage_create"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("alliance.only_capital_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("alliance.must_be_mayor"));
            return true;
        }

        String allianceName = args[1];

        for (Alliance a : societiesManager.getAlliances()) {
            if (a.getName().equalsIgnoreCase(allianceName)) {
                player.sendMessage(MessageUtils.get("alliance.alliance_exists"));
                return true;
            }
        }

        Alliance alliance = new Alliance(allianceName, kingdom.getName());
        societiesManager.registerAlliance(alliance);
        kingdom.joinAlliance(allianceName);

        player.sendMessage(MessageUtils.format("alliance.alliance_created", Map.of("alliance", allianceName)));
        player.sendMessage(MessageUtils.get("alliance.alliance_now_leader"));

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("alliance.usage_invite"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("alliance.only_capital_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("alliance.must_be_mayor"));
            return true;
        }

        String allianceName = args[1];
        Alliance alliance = null;

        for (Alliance a : societiesManager.getAlliances()) {
            if (a.getName().equalsIgnoreCase(allianceName)) {
                alliance = a;
                break;
            }
        }

        if (alliance == null) {
            player.sendMessage(MessageUtils.get("alliance.alliance_not_found"));
            return true;
        }

        if (!alliance.getLeader().equals(kingdom.getName())) {
            player.sendMessage(MessageUtils.get("alliance.only_leader_can_invite"));
            return true;
        }

        String targetKingdomName = args[2];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);

        if (targetKingdom == null) {
            player.sendMessage(MessageUtils.format("commands.not_found", Map.of("entity", "Kingdom")));
            return true;
        }

        if (alliance.isMember(targetKingdomName)) {
            player.sendMessage(MessageUtils.get("alliance.already_in_alliance"));
            return true;
        }

        alliance.addInvite(targetKingdomName);

        player.sendMessage(MessageUtils.format("alliance.invite_success", Map.of("kingdom", targetKingdomName)));

        Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
        if (targetCapital != null) {
            Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
            if (targetKing != null) {
                targetKing.sendMessage(MessageUtils.format("alliance.invite_notify", Map.of("alliance", allianceName)));
                targetKing.sendMessage(MessageUtils.format("alliance.invite_hint", Map.of("alliance", allianceName)));
            }
        }

        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("alliance.usage_accept"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("alliance.only_capital_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("alliance.must_be_mayor"));
            return true;
        }

        String allianceName = args[1];
        Alliance alliance = null;

        for (Alliance a : societiesManager.getAlliances()) {
            if (a.getName().equalsIgnoreCase(allianceName)) {
                alliance = a;
                break;
            }
        }

        if (alliance == null) {
            player.sendMessage(MessageUtils.get("alliance.alliance_not_found"));
            return true;
        }

        if (!alliance.hasInvite(kingdom.getName())) {
            player.sendMessage(MessageUtils.get("alliance.alliance_not_found"));
            return true;
        }

        alliance.removePendingInvite(kingdom.getName());
        alliance.addMember(kingdom.getName());
        kingdom.joinAlliance(allianceName);

        player.sendMessage(MessageUtils.format("alliance.joined_alliance", Map.of("alliance", allianceName)));

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("alliance.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("alliance.only_capital_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("alliance.must_be_mayor"));
            return true;
        }

        if (kingdom.getAlliances().isEmpty()) {
            player.sendMessage(MessageUtils.get("alliance.not_in_any_alliance"));
            return true;
        }

        String allianceName = kingdom.getAlliances().iterator().next();

        for (Alliance a : societiesManager.getAlliances()) {
            if (a.getName().equalsIgnoreCase(allianceName)) {
                if (a.getLeader().equals(kingdom.getName())) {
                    player.sendMessage(MessageUtils.get("alliance.cannot_leave_leader"));
                    return true;
                }

                a.removeMember(kingdom.getName());
                kingdom.leaveAlliance(allianceName);

                player.sendMessage(MessageUtils.format("alliance.left_alliance", Map.of("alliance", allianceName)));
                return true;
            }
        }

        player.sendMessage(MessageUtils.get("alliance.alliance_not_found"));
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("alliance.usage_info"));
            return true;
        }

        String allianceName = args[1];
        Alliance alliance = null;

        for (Alliance a : societiesManager.getAlliances()) {
            if (a.getName().equalsIgnoreCase(allianceName)) {
                alliance = a;
                break;
            }
        }

        if (alliance == null) {
            player.sendMessage(MessageUtils.get("alliance.alliance_not_found"));
            return true;
        }

        player.sendMessage(MessageUtils.format("alliance.info_header", Map.of("alliance", alliance.getName())));
        player.sendMessage(MessageUtils.format("alliance.info_leader", Map.of("leader", alliance.getLeader())));
        player.sendMessage(MessageUtils.format("alliance.info_members", Map.of("count", String.valueOf(alliance.getMemberCount()))));
        player.sendMessage(MessageUtils.get("alliance.info_footer"));

        for (String member : alliance.getMembers()) {
            player.sendMessage(MessageUtils.format("alliance.info_member_list_item", Map.of("name", member)));
        }

        player.sendMessage(MessageUtils.get("alliance.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Alliance> alliances = societiesManager.getAlliances();

        if (alliances.isEmpty()) {
            player.sendMessage(MessageUtils.get("alliance.list_empty"));
            return true;
        }

        player.sendMessage(MessageUtils.format("alliance.list_header", Map.of("count", String.valueOf(alliances.size()))));

        for (Alliance alliance : alliances) {
            player.sendMessage(MessageUtils.format("alliance.list_item", Map.of("name", alliance.getName(), "leader", alliance.getLeader(), "count", String.valueOf(alliance.getMemberCount()))));
        }

        player.sendMessage(MessageUtils.get("alliance.list_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("alliance.help_header"));
        player.sendMessage(MessageUtils.get("alliance.help_create"));
        player.sendMessage(MessageUtils.get("alliance.help_invite"));
        player.sendMessage(MessageUtils.get("alliance.help_accept"));
        player.sendMessage(MessageUtils.get("alliance.help_leave"));
        player.sendMessage(MessageUtils.get("alliance.help_info"));
        player.sendMessage(MessageUtils.get("alliance.help_list"));
        player.sendMessage(MessageUtils.get("alliance.help_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(
                    Arrays.asList("create", "invite", "accept", "leave", "info", "list"),
                    args[0]
            );
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("accept")) {
                return me.marcdoesntexists.nations.utils.TabCompletionUtils.matchDistinct(
                        societiesManager.getAlliances().stream().map(Alliance::getName).collect(java.util.stream.Collectors.toList()),
                        args[1]
                );
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.kingdoms(societiesManager, args[2]);
        }

        return new ArrayList<>();
    }
}
