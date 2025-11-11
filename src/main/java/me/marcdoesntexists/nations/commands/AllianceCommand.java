package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Alliance;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
            player.sendMessage("§cUsage: /alliance create <name>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to create an alliance!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom to create an alliance!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage("§cOnly the capital's mayor can create an alliance!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor to create an alliance!");
            return true;
        }

        String allianceName = args[1];

        for (Alliance a : societiesManager.getAlliances()) {
            if (a.getName().equalsIgnoreCase(allianceName)) {
                player.sendMessage("§cAn alliance with this name already exists!");
                return true;
            }
        }

        Alliance alliance = new Alliance(allianceName, kingdom.getName());
        societiesManager.registerAlliance(alliance);
        kingdom.joinAlliance(allianceName);

        player.sendMessage("§a✔ Alliance §6" + allianceName + "§a created successfully!");
        player.sendMessage("§7Your kingdom is now the leader of this alliance.");

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /alliance invite <alliance> <kingdom>");
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
            player.sendMessage("§cOnly the capital's mayor can invite to an alliance!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor!");
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
            player.sendMessage("§cAlliance not found!");
            return true;
        }

        if (!alliance.getLeader().equals(kingdom.getName())) {
            player.sendMessage("§cOnly the alliance leader can invite kingdoms!");
            return true;
        }

        String targetKingdomName = args[2];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);

        if (targetKingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return true;
        }

        if (alliance.isMember(targetKingdomName)) {
            player.sendMessage("§cThat kingdom is already in this alliance!");
            return true;
        }

        alliance.addInvite(targetKingdomName);

        player.sendMessage("§a✔ Invited §6" + targetKingdomName + "§a to the alliance!");

        Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
        if (targetCapital != null) {
            Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
            if (targetKing != null) {
                targetKing.sendMessage("§7[§6" + allianceName + "§7] §eYour kingdom has been invited to join §6" + allianceName + "§e!");
                targetKing.sendMessage("§7Use §e/alliance accept " + allianceName + "§7 to join!");
            }
        }

        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /alliance accept <alliance>");
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
            player.sendMessage("§cOnly the capital's mayor can accept alliance invites!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor!");
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
            player.sendMessage("§cAlliance not found!");
            return true;
        }

        if (!alliance.hasInvite(kingdom.getName())) {
            player.sendMessage("§cYou don't have an invite to this alliance!");
            return true;
        }

        alliance.removePendingInvite(kingdom.getName());
        alliance.addMember(kingdom.getName());
        kingdom.joinAlliance(allianceName);

        player.sendMessage("§a✔ Your kingdom has joined §6" + allianceName + "§a!");

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
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
            player.sendMessage("§cOnly the capital's mayor can leave alliances!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor!");
            return true;
        }

        if (kingdom.getAlliances().isEmpty()) {
            player.sendMessage("§cYour kingdom is not in any alliance!");
            return true;
        }

        String allianceName = kingdom.getAlliances().iterator().next();

        for (Alliance a : societiesManager.getAlliances()) {
            if (a.getName().equalsIgnoreCase(allianceName)) {
                if (a.getLeader().equals(kingdom.getName())) {
                    player.sendMessage("§cYou cannot leave an alliance you lead! Disband it instead.");
                    return true;
                }

                a.removeMember(kingdom.getName());
                kingdom.leaveAlliance(allianceName);

                player.sendMessage("§a✔ Your kingdom has left §6" + allianceName + "§a!");
                return true;
            }
        }

        player.sendMessage("§cAlliance not found!");
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /alliance info <name>");
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
            player.sendMessage("§cAlliance not found!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6" + alliance.getName() + "§7 §m----------");
        player.sendMessage("§eLeader: §6" + alliance.getLeader());
        player.sendMessage("§eMembers: §6" + alliance.getMemberCount());
        player.sendMessage("§7Member Kingdoms:");

        for (String member : alliance.getMembers()) {
            player.sendMessage("§7  • §e" + member);
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Alliance> alliances = societiesManager.getAlliances();

        if (alliances.isEmpty()) {
            player.sendMessage("§cNo alliances exist yet!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Alliances §7(" + alliances.size() + ")§m----------");

        for (Alliance alliance : alliances) {
            player.sendMessage("§e• §6" + alliance.getName() + " §7- Leader: §e" + alliance.getLeader() + " §7- Members: §e" + alliance.getMemberCount());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Alliance Commands§7 §m----------");
        player.sendMessage("§e/alliance create <name>§7 - Create an alliance");
        player.sendMessage("§e/alliance invite <alliance> <kingdom>§7 - Invite a kingdom");
        player.sendMessage("§e/alliance accept <alliance>§7 - Accept invite");
        player.sendMessage("§e/alliance leave§7 - Leave alliance");
        player.sendMessage("§e/alliance info <name>§7 - View alliance info");
        player.sendMessage("§e/alliance list§7 - List all alliances");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "invite", "accept", "leave", "info", "list")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("accept")) {
                return societiesManager.getAlliances().stream()
                        .map(Alliance::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {
            return societiesManager.getAllKingdoms().stream()
                    .map(Kingdom::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
