package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import me.marcdoesntexists.nations.economy.EconomyService;
import me.marcdoesntexists.nations.utils.MessageUtils;
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
    private final EconomyService economyService;

    public TownCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.economyService = EconomyService.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
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
            player.sendMessage(MessageUtils.get("town.usage_create"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() != null) {
            player.sendMessage(MessageUtils.get("town.already_in_town"));
            return true;
        }

        String townName = args[1];

        if (societiesManager.getTown(townName) != null) {
            player.sendMessage(MessageUtils.get("town.already_exists"));
            return true;
        }

        // Try to charge the player for town creation
        boolean charged = true;
        if (economyService != null) {
            charged = economyService.withdrawFromPlayer(player.getUniqueId(), TOWN_CREATION_COST);
        }

        if (!charged) {
            player.sendMessage(MessageUtils.format("town.not_enough_money", java.util.Map.of("needed", String.valueOf(TOWN_CREATION_COST), "have", String.valueOf(0))));
            return true;
        }

        Town newTown = new Town(townName, player.getUniqueId());
        // Credit the town with the creation fee as starting balance
        newTown.addMoney(TOWN_CREATION_COST);

        societiesManager.registerTown(newTown);
        data.setTown(townName);

        // Persist immediately
        try {
            plugin.getDataManager().saveTown(newTown);
            plugin.getDataManager().savePlayerData(player.getUniqueId());
        } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("town.created", java.util.Map.of("name", townName)));
        player.sendMessage(MessageUtils.get("town.created_hint"));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String townName;

        if (args.length < 2) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getTown() == null) {
                player.sendMessage(MessageUtils.get("town.not_in_town_usage"));
                return true;
            }
            townName = data.getTown();
        } else {
            townName = args[1];
        }

        Town town = societiesManager.getTown(townName);
        if (town == null) {
            player.sendMessage(MessageUtils.get("town.not_found"));
            return true;
        }

        player.sendMessage(MessageUtils.format("town.info.header", java.util.Map.of("name", town.getName())));
        player.sendMessage(MessageUtils.format("town.info.mayor", java.util.Map.of("mayor", plugin.getServer().getOfflinePlayer(town.getMayor()).getName())));
        player.sendMessage(MessageUtils.format("town.info.members", java.util.Map.of("count", String.valueOf(town.getMembers().size()))));
        player.sendMessage(MessageUtils.format("town.info.claims", java.util.Map.of("count", String.valueOf(town.getClaims().size()))));
        player.sendMessage(MessageUtils.format("town.info.balance", java.util.Map.of("balance", String.valueOf(town.getBalance()))));
        if (town.getKingdom() != null) {
            player.sendMessage(MessageUtils.format("town.info.kingdom", java.util.Map.of("kingdom", town.getKingdom())));
        }
        player.sendMessage(MessageUtils.format("town.info.level", java.util.Map.of("level", String.valueOf(town.getProgressionLevel()))));
        player.sendMessage(MessageUtils.get("town.info.footer"));

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_invite"));
            return true;
        }

        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId());
        if (senderData.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(senderData.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (targetData.getTown() != null) {
            player.sendMessage(MessageUtils.get("town.player_already_in_town"));
            return true;
        }

        targetData.addTownInvite(town.getName());

        player.sendMessage(MessageUtils.format("town.invite_sent", java.util.Map.of("player", target.getName(), "town", town.getName())));
        target.sendMessage(MessageUtils.format("town.invite_receive", java.util.Map.of("town", town.getName())));
        target.sendMessage(MessageUtils.format("town.invite_accept_hint", java.util.Map.of("town", town.getName())));

        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_accept"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() != null) {
            player.sendMessage(MessageUtils.get("town.already_in_town"));
            return true;
        }

        String townName = args[1];
        if (!data.getTownInvites().contains(townName)) {
            player.sendMessage(MessageUtils.get("town.no_invite"));
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

        player.sendMessage(MessageUtils.format("town.joined", java.util.Map.of("town", townName)));

        Player mayor = plugin.getServer().getPlayer(town.getMayor());
        if (mayor != null) {
            mayor.sendMessage(MessageUtils.format("town.notify_join", java.util.Map.of("player", player.getName(), "town", townName)));
        }

        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_kick"));
            return true;
        }

        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId());
        if (senderData.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(senderData.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!town.getMembers().contains(target.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.player_not_in_your_town"));
            return true;
        }

        if (town.isMayor(target.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.cannot_kick_self"));
            return true;
        }

        town.removeMember(target.getUniqueId());
        targetData.setTown(null);

        player.sendMessage(MessageUtils.format("town.kick_success", java.util.Map.of("player", target.getName(), "town", town.getName())));
        target.sendMessage(MessageUtils.format("town.kicked_notify", java.util.Map.of("town", town.getName())));

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());

        if (town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.cannot_leave_mayor"));
            return true;
        }

        town.removeMember(player.getUniqueId());
        data.setTown(null);

        player.sendMessage(MessageUtils.format("town.left", java.util.Map.of("town", town.getName())));

        Player mayor = plugin.getServer().getPlayer(town.getMayor());
        if (mayor != null) {
            mayor.sendMessage(MessageUtils.format("town.notify_left", java.util.Map.of("player", player.getName(), "town", town.getName())));
        }

        return true;
    }

    private boolean handleClaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor_claim"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();

        me.marcdoesntexists.nations.managers.HybridClaimManager.ClaimResult result =
                plugin.getHybridClaimManager().claimChunk(player, town, chunk);

        if (result.isSuccess()) {
            player.sendMessage(MessageUtils.get("town.claim_success_prefix") + result.getMessage());
        } else {
            player.sendMessage(MessageUtils.get("town.claim_fail_prefix") + result.getMessage());
        }

        return true;
    }

    private boolean handleUnclaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor_claim"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();

        me.marcdoesntexists.nations.managers.HybridClaimManager.ClaimResult result =
                plugin.getHybridClaimManager().unclaimChunk(town, chunk);

        if (result.isSuccess()) {
            player.sendMessage(MessageUtils.get("town.claim_success_prefix") + result.getMessage());
        } else {
            player.sendMessage(MessageUtils.get("town.claim_fail_prefix") + result.getMessage());
        }

        return true;
    }

    private boolean handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_deposit"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtils.get("commands.invalid_number"));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(MessageUtils.get("town.amount_must_be_positive"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());

        // Try to withdraw from player's external account (Vault/Essentials) first
        boolean success = economyService.withdrawFromPlayer(player.getUniqueId(), amount);
        if (!success) {
            player.sendMessage(MessageUtils.get("town.withdraw_external_failed"));
            return true;
        }

        // Add to town treasury
        town.addMoney(amount);

        // Persist immediately
        try {
            plugin.getDataManager().saveTown(town);
            plugin.getDataManager().savePlayerMoney(player.getUniqueId());
        } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("town.deposit_success", java.util.Map.of("amount", String.valueOf(amount))));
        player.sendMessage(MessageUtils.format("town.new_balance", java.util.Map.of("balance", String.valueOf(town.getBalance()))));

        return true;
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_withdraw"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtils.get("commands.invalid_number"));
            return true;
        }

        if (town.removeMoney(amount)) {
            // Try to deposit to player's external account
            boolean depositOk = economyService.depositToPlayer(player.getUniqueId(), amount);
            if (!depositOk) {
                // Rollback town withdrawal if external deposit failed
                town.addMoney(amount);

                // Persist town change
                try { plugin.getDataManager().saveTown(town); } catch (Throwable ignored) {}

                player.sendMessage(MessageUtils.get("town.deposit_failed_external"));
                return true;
            }

            // Persist both town and player data
            try {
                plugin.getDataManager().saveTown(town);
                plugin.getDataManager().savePlayerMoney(player.getUniqueId());
            } catch (Throwable ignored) {}

            player.sendMessage(MessageUtils.format("town.withdraw_success", java.util.Map.of("amount", String.valueOf(amount))));
            player.sendMessage(MessageUtils.format("town.new_balance", java.util.Map.of("balance", String.valueOf(town.getBalance()))));
        } else {
            player.sendMessage(MessageUtils.format("town.insufficient_funds", java.util.Map.of("balance", String.valueOf(town.getBalance()))));
        }

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Town> towns = societiesManager.getAllTowns();

        if (towns.isEmpty()) {
            player.sendMessage(MessageUtils.get("town.no_towns_exist"));
            return true;
        }

        player.sendMessage(MessageUtils.format("town.list_header", java.util.Map.of("count", String.valueOf(towns.size()))));

        for (Town town : towns) {
            String mayorName = plugin.getServer().getOfflinePlayer(town.getMayor()).getName();
            player.sendMessage(MessageUtils.format("town.list_item", java.util.Map.of("name", town.getName(), "mayor", mayorName, "members", String.valueOf(town.getMembers().size()))));
        }

        player.sendMessage(MessageUtils.get("town.list_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("town.help.header"));
        player.sendMessage(MessageUtils.get("town.help.create"));
        player.sendMessage(MessageUtils.get("town.help.info"));
        player.sendMessage(MessageUtils.get("town.help.invite"));
        player.sendMessage(MessageUtils.get("town.help.accept"));
        player.sendMessage(MessageUtils.get("town.help.kick"));
        player.sendMessage(MessageUtils.get("town.help.leave"));
        player.sendMessage(MessageUtils.get("town.help.claim"));
        player.sendMessage(MessageUtils.get("town.help.unclaim"));
        player.sendMessage(MessageUtils.get("town.help.deposit"));
        player.sendMessage(MessageUtils.get("town.help.withdraw"));
        player.sendMessage(MessageUtils.get("town.help.list"));
        player.sendMessage(MessageUtils.get("town.help.footer"));
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
