package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Chunk;
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

public class TownCommand implements CommandExecutor, TabCompleter {

    private static final int TOWN_CREATION_COST = 1000;
    private static final int CHUNK_CLAIM_COST = 100;
    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;

    public TownCommand(Nations plugin) {
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
            case "info":
                return handleInfo(player, args);
            case "invite":
                return handleInvite(player, args);
            case "kick":
                return handleKick(player, args);
            case "claim":
                return handleClaim(player, args);
            case "unclaim":
                return handleUnclaim(player, args);
            case "deposit":
                return handleDeposit(player, args);
            case "withdraw":
                return handleWithdraw(player, args);
            case "list":
                return handleList(player, args);
            case "leave":
                return handleLeave(player, args);
            case "accept":
                return handleAccept(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town create <name>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() != null) {
            player.sendMessage("§cYou are already in a town!");
            return true;
        }

        String townName = args[1];

        if (societiesManager.getTown(townName) != null) {
            player.sendMessage("§cA town with this name already exists!");
            return true;
        }

        Town newTown = new Town(townName, player.getUniqueId());
        societiesManager.registerTown(newTown);
        data.setTown(townName);

        player.sendMessage("§a✔ Town §6" + townName + "§a created successfully!");
        player.sendMessage("§7Use §e/town claim§7 to claim your first chunk!");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String townName;

        if (args.length < 2) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getTown() == null) {
                player.sendMessage("§cYou are not in a town! Usage: /town info <name>");
                return true;
            }
            townName = data.getTown();
        } else {
            townName = args[1];
        }

        Town town = societiesManager.getTown(townName);
        if (town == null) {
            player.sendMessage("§cTown not found!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6" + town.getName() + "§7 §m----------");
        player.sendMessage("§eMayor: §6" + plugin.getServer().getOfflinePlayer(town.getMayor()).getName());
        player.sendMessage("§eMembers: §6" + town.getMembers().size());
        player.sendMessage("§eClaims: §6" + town.getClaims().size());
        player.sendMessage("§eBalance: §6$" + town.getBalance());
        if (town.getKingdom() != null) {
            player.sendMessage("§eKingdom: §6" + town.getKingdom());
        }
        player.sendMessage("§eLevel: §6" + town.getProgressionLevel());
        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town invite <player>");
            return true;
        }

        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId());
        if (senderData.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town town = societiesManager.getTown(senderData.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can invite players!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (targetData.getTown() != null) {
            player.sendMessage("§cThat player is already in a town!");
            return true;
        }

        targetData.addTownInvite(town.getName());

        player.sendMessage("§a✔ Invited §6" + target.getName() + "§a to your town!");
        target.sendMessage("§7[§6" + town.getName() + "§7] §eYou have been invited to join §6" + town.getName() + "§e!");
        target.sendMessage("§7Type §e/town accept " + town.getName() + "§7 to join!");

        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town accept <town>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() != null) {
            player.sendMessage("§cYou are already in a town!");
            return true;
        }

        String townName = args[1];
        if (!data.getTownInvites().contains(townName)) {
            player.sendMessage("§cYou don't have an invite from that town!");
            return true;
        }

        Town town = societiesManager.getTown(townName);
        if (town == null) {
            player.sendMessage("§cTown not found!");
            data.removeTownInvite(townName);
            return true;
        }

        town.addMember(player.getUniqueId());
        data.setTown(townName);
        data.removeTownInvite(townName);

        player.sendMessage("§a✔ You have joined §6" + townName + "§a!");

        Player mayor = plugin.getServer().getPlayer(town.getMayor());
        if (mayor != null) {
            mayor.sendMessage("§a✔ §6" + player.getName() + "§a has joined your town!");
        }

        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town kick <player>");
            return true;
        }

        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId());
        if (senderData.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town town = societiesManager.getTown(senderData.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can kick players!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        if (!town.getMembers().contains(target.getUniqueId())) {
            player.sendMessage("§cThat player is not in your town!");
            return true;
        }

        if (town.isMayor(target.getUniqueId())) {
            player.sendMessage("§cYou cannot kick yourself!");
            return true;
        }

        town.removeMember(target.getUniqueId());
        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        targetData.setTown(null);

        player.sendMessage("§a✔ Kicked §6" + target.getName() + "§a from the town!");
        target.sendMessage("§c✘ You have been kicked from §6" + town.getName() + "§c!");

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());

        if (town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou cannot leave your own town! Use /town disband instead.");
            return true;
        }

        town.removeMember(player.getUniqueId());
        data.setTown(null);

        player.sendMessage("§a✔ You have left §6" + town.getName() + "§a!");

        Player mayor = plugin.getServer().getPlayer(town.getMayor());
        if (mayor != null) {
            mayor.sendMessage("§c✘ §6" + player.getName() + "§c has left the town!");
        }

        return true;
    }

    private boolean handleClaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can claim chunks!");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();

        me.marcdoesntexists.nations.managers.HybridClaimManager.ClaimResult result =
                plugin.getHybridClaimManager().claimChunk(player, town, chunk);

        if (result.isSuccess()) {
            player.sendMessage("§a✔ " + result.getMessage());
        } else {
            player.sendMessage("§c✘ " + result.getMessage());
        }

        return true;
    }

    private boolean handleUnclaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can unclaim chunks!");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();

        me.marcdoesntexists.nations.managers.HybridClaimManager.ClaimResult result =
                plugin.getHybridClaimManager().unclaimChunk(town, chunk);

        if (result.isSuccess()) {
            player.sendMessage("§a✔ " + result.getMessage());
        } else {
            player.sendMessage("§c✘ " + result.getMessage());
        }

        return true;
    }

    private boolean handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town deposit <amount>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage("§cAmount must be positive!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        town.addMoney(amount);

        player.sendMessage("§a✔ Deposited §6$" + amount + "§a to town!");
        player.sendMessage("§eNew town balance: §6$" + town.getBalance());

        return true;
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /town withdraw <amount>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can withdraw money!");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return true;
        }

        if (town.removeMoney(amount)) {
            player.sendMessage("§a✔ Withdrew §6$" + amount + "§a from town!");
            player.sendMessage("§eNew town balance: §6$" + town.getBalance());
        } else {
            player.sendMessage("§cInsufficient funds! Town balance: §6$" + town.getBalance());
        }

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Town> towns = societiesManager.getAllTowns();

        if (towns.isEmpty()) {
            player.sendMessage("§cNo towns exist yet!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Towns §7(" + towns.size() + ")§m----------");

        for (Town town : towns) {
            String mayorName = plugin.getServer().getOfflinePlayer(town.getMayor()).getName();
            player.sendMessage("§e• §6" + town.getName() + " §7- Mayor: §e" + mayorName + " §7- Members: §e" + town.getMembers().size());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Town Commands§7 §m----------");
        player.sendMessage("§e/town create <name>§7 - Create a town");
        player.sendMessage("§e/town info [name]§7 - View town info");
        player.sendMessage("§e/town invite <player>§7 - Invite a player");
        player.sendMessage("§e/town accept <town>§7 - Accept town invite");
        player.sendMessage("§e/town kick <player>§7 - Kick a player");
        player.sendMessage("§e/town leave§7 - Leave your town");
        player.sendMessage("§e/town claim§7 - Claim current chunk");
        player.sendMessage("§e/town unclaim§7 - Unclaim current chunk");
        player.sendMessage("§e/town deposit <amount>§7 - Deposit money");
        player.sendMessage("§e/town withdraw <amount>§7 - Withdraw money");
        player.sendMessage("§e/town list§7 - List all towns");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "info", "invite", "kick", "claim", "unclaim", "deposit", "withdraw", "list", "leave", "accept")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("accept")) {
                return societiesManager.getAllTowns().stream()
                        .map(Town::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
