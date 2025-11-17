package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.economy.EconomyService;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

public class TownCommand implements CommandExecutor, TabCompleter {

    private static final int TOWN_CREATION_COST = 1000;
    private final Realms plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final EconomyService economyService;

    private final Map<java.util.UUID, Long> pendingDisbandConfirm = new ConcurrentHashMap<>(); // uuid -> expiry millis

    public TownCommand(Realms plugin) {
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
            case "trust":
                return handleTrust(player, args);
            case "untrust":
                return handleUntrust(player, args);
            case "pvp":
                return handlePvp(player, args);
            case "transfer":
                return handleTransfer(player, args);
            case "disband":
                return handleDisband(player, args);
            case "buychunk":
                return handleBuyChunk(player, args);
            case "claimgrant":
                return handleClaimGrant(player, args);
            case "claimrevoke":
                return handleClaimRevoke(player, args);
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

        // validate length using config values
        int min = plugin.getConfig().getInt("town.name_min", 3);
        int max = plugin.getConfig().getInt("town.name_max", 16);
        if (townName.length() < min || townName.length() > max) {
            player.sendMessage(MessageUtils.format("town.invalid_name_length", Map.of("min", String.valueOf(min), "max", String.valueOf(max))));
            return true;
        }

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
        } catch (Throwable ignored) {
        }

        player.sendMessage(MessageUtils.format("town.created", java.util.Map.of("name", townName)));
        player.sendMessage(MessageUtils.get("town.created_hint"));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String townName;

        if (args.length < 2) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data == null || data.getTown() == null) {
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
        // safe mayor name
        org.bukkit.OfflinePlayer mayorOff = plugin.getServer().getOfflinePlayer(town.getMayor());
        String mayorName = mayorOff != null && mayorOff.getName() != null ? mayorOff.getName() : (town.getMayor() != null ? town.getMayor().toString() : "-");
        player.sendMessage(MessageUtils.format("town.info.mayor", java.util.Map.of("mayor", mayorName)));
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
            player.sendMessage(MessageUtils.get("town.not_found_literal"));
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
        if (data == null || data.getTown() == null) {
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
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor_claim"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();

        me.marcdoesntexists.realms.managers.HybridClaimManager.ClaimResult result =
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
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor_claim"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();

        me.marcdoesntexists.realms.managers.HybridClaimManager.ClaimResult result =
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
        if (data == null || data.getTown() == null) {
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
        } catch (Throwable ignored) {
        }

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
        if (data == null || data.getTown() == null) {
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
                try {
                    plugin.getDataManager().saveTown(town);
                } catch (Throwable ignored) {
                }

                player.sendMessage(MessageUtils.get("town.deposit_failed_external"));
                return true;
            }

            // Persist both town and player data
            try {
                plugin.getDataManager().saveTown(town);
                plugin.getDataManager().savePlayerMoney(player.getUniqueId());
            } catch (Throwable ignored) {
            }

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
            var off = plugin.getServer().getOfflinePlayer(town.getMayor());
            String mayorName = off != null && off.getName() != null ? off.getName() : (town.getMayor() != null ? town.getMayor().toString() : "-");
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
        List<String> subs = Arrays.asList("create", "info", "invite", "kick", "claim", "unclaim", "deposit", "withdraw", "list", "leave", "accept", "trust", "untrust", "pvp", "transfer", "disband", "buychunk", "claimgrant", "claimrevoke");
        if (args.length == 1) {
            return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(subs, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("claimgrant") || sub.equals("claimrevoke")) {
                // suggest online/offline players
                return me.marcdoesntexists.realms.utils.TabCompletionUtils.onlinePlayers(args[1]);
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("claimgrant")) {
                return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(Arrays.asList("build", "container", "full"), args[2]);
            }
        }
        // fallback to existing logic
        // CommandExecutor/TabCompleter does not have a super implementation here — return empty list as fallback
        return java.util.Collections.emptyList();
    }

    // ========== Trust management commands ==========
    private boolean handleTrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_trust"));
            return true;
        }

        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId());
        if (senderData.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(senderData.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("town.not_found"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        if (!town.getMembers().contains(target.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.player_not_in_your_town"));
            return true;
        }

        boolean added = town.addTrusted(target.getUniqueId());
        String targetName = target.getName() != null ? target.getName() : args[1];
        if (added) {
            dataManager.saveTown(town);
            player.sendMessage(MessageUtils.format("town.trust_success", java.util.Map.of("player", targetName)));
            if (target.isOnline()) {
                ((Player) target.getPlayer()).sendMessage(MessageUtils.format("town.trusted_notify", java.util.Map.of("town", town.getName())));
            }
        } else {
            player.sendMessage(MessageUtils.format("town.already_trusted", java.util.Map.of("player", targetName)));
        }

        return true;
    }

    private boolean handleUntrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_untrust"));
            return true;
        }

        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId());
        if (senderData.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(senderData.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("town.not_found"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        boolean removed = town.removeTrusted(target.getUniqueId());
        String targetName = target.getName() != null ? target.getName() : args[1];
        if (removed) {
            dataManager.saveTown(town);
            player.sendMessage(MessageUtils.format("town.untrust_success", java.util.Map.of("player", targetName)));
            if (target.isOnline()) {
                ((Player) target.getPlayer()).sendMessage(MessageUtils.format("town.untrusted_notify", java.util.Map.of("town", town.getName())));
            }
        } else {
            player.sendMessage(MessageUtils.format("town.not_trusted", java.util.Map.of("player", targetName)));
        }

        return true;
    }

    private boolean handlePvp(Player player, String[] args) {
        // Usage: /town pvp <toggle|on|off>
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("town.not_found"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaimAt(chunk);
        if (claim == null) {
            player.sendMessage(MessageUtils.get("claim.no_claim"));
            return true;
        }

        if (!claim.getTownName().equals(town.getName())) {
            player.sendMessage(MessageUtils.get("claim.pvp_not_owner"));
            return true;
        }

        String action = "toggle";
        if (args.length >= 2) action = args[1].toLowerCase();

        boolean target;
        switch (action) {
            case "on":
                target = true;
                break;
            case "off":
                target = false;
                break;
            default:
                target = claim.isPvpEnabled();
                break;
        }

        claim.setPvpEnabled(target);
        // persist town since claims are saved inside town files
        try {
            plugin.getDataManager().saveTown(town);
        } catch (Throwable ignored) {}

        if (target) {
            player.sendMessage(MessageUtils.format("claim.pvp_enabled", Map.of("town", town.getName())));
        } else {
            player.sendMessage(MessageUtils.format("claim.pvp_disabled", Map.of("town", town.getName())));
        }

        return true;
    }

    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("town.usage_transfer"));
            return true;
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("town.not_found"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        // Transfer mayor
        UUID oldMayor = town.getMayor();
        town.setMayor(target.getUniqueId());

        // Persist town and player data
        try {
            dataManager.saveTown(town);
        } catch (Throwable ignored) {}

        // Update PlayerData for new mayor if present
        try {
            PlayerData newMayorData = dataManager.getPlayerData(target.getUniqueId());
            newMayorData.setTown(town.getName());
            dataManager.savePlayerData(target.getUniqueId());
        } catch (Throwable ignored) {}

        // Notify executor and new mayor
        player.sendMessage(MessageUtils.format("town.transfer_success", java.util.Map.of("town", town.getName(), "newMayor", target.getName() != null ? target.getName() : args[1])));
        if (target.isOnline()) {
            ((Player) target.getPlayer()).sendMessage(MessageUtils.format("town.transfer_notify", java.util.Map.of("town", town.getName(), "oldMayor", player.getName())));
        }

        // Notify all town members about transfer
        for (UUID memberId : new java.util.HashSet<>(town.getMembers())) {
            try {
                var off = plugin.getServer().getOfflinePlayer(memberId);
                String oldMayorName = "?";
                var oldOff = plugin.getServer().getOfflinePlayer(oldMayor);
                if (oldOff != null && oldOff.getName() != null) oldMayorName = oldOff.getName();
                String msg = MessageUtils.format("town.notify_transfer_member", Map.of("town", town.getName(), "newMayor", target.getName() != null ? target.getName() : args[1], "oldMayor", oldMayorName));
                if (off != null && off.isOnline() && off.getPlayer() != null) {
                    off.getPlayer().sendMessage(msg);
                } else if (off != null) {
                    try {
                        var memberData = dataManager.getPlayerData(memberId);
                        memberData.addNotification(msg);
                        dataManager.savePlayerData(memberId);
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
        return true;
    }

    private boolean handleDisband(Player player, String[] args) {
        // Confirmation flow: require '/town disband confirm' or previous pending confirm within 30s
        final long now = System.currentTimeMillis();
        final long TTL = 30_000L;

        if (args.length >= 2 && (args[1].equalsIgnoreCase("confirm") || args[1].equalsIgnoreCase("yes"))) {
            // explicit confirm token, proceed
        } else {
            Long exp = pendingDisbandConfirm.get(player.getUniqueId());
            if (exp == null || exp < now) {
                // set pending and ask for confirmation
                pendingDisbandConfirm.put(player.getUniqueId(), now + TTL);
                player.sendMessage(MessageUtils.get("misc.confirm_prompt"));
                player.sendMessage(MessageUtils.get("town.disband_confirm_hint"));
                return true;
            }
            // if we reached here, confirmation existed and not expired -> proceed
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("town.not_found"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("town.only_mayor"));
            return true;
        }

        // Remove pending lock
        pendingDisbandConfirm.remove(player.getUniqueId());

        // Remove claims (via hybrid manager to support GP integration)
        try {
            plugin.getHybridClaimManager().deleteTownClaims(town.getName());
        } catch (Throwable ignored) {}

        // Notify members before clearing
        for (UUID memberId : new java.util.HashSet<>(town.getMembers())) {
            try {
                var off = plugin.getServer().getOfflinePlayer(memberId);
                String msg = MessageUtils.format("town.notify_disband_member", Map.of("town", town.getName(), "by", player.getName()));
                if (off != null && off.isOnline() && off.getPlayer() != null) {
                    off.getPlayer().sendMessage(msg);
                } else if (off != null) {
                    try {
                        var memberData = dataManager.getPlayerData(memberId);
                        memberData.addNotification(msg);
                        dataManager.savePlayerData(memberId);
                     } catch (Throwable ignored) {}
                 }
             } catch (Throwable ignored) {}
         }

        // Clear members' town data
        for (java.util.UUID member : new java.util.HashSet<>(town.getMembers())) {
            try {
                PlayerData md = dataManager.getPlayerData(member);
                if (md != null) {
                    md.setTown(null);
                    dataManager.savePlayerData(member);
                }
            } catch (Throwable ignored) {}
        }

        // Remove from SocietiesManager and persist
        societiesManager.removeTown(town.getName());
        try {
            // delete town file if exists
            java.io.File townFile = new java.io.File(plugin.getDataFolder(), "towns" + java.io.File.separator + town.getName() + ".yml");
            if (townFile.exists()) townFile.delete();
        } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("town.disbanded", java.util.Map.of("town", town.getName())));
        return true;
    }

    private boolean handleBuyChunk(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("town.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("town.not_found"));
            return true;
        }

        // Check policy
        String policy = plugin.getConfig().getString("subclaims.purchase_policy", "trusted");
        java.util.UUID pid = player.getUniqueId();
        boolean allowed = false;
        switch (policy.toLowerCase(Locale.ROOT)) {
            case "mayor" -> allowed = town.isMayor(pid);
            case "trusted" -> allowed = town.isTrusted(pid);
            case "members" -> allowed = town.getMembers().contains(pid);
            case "permission" -> allowed = player.hasPermission(plugin.getConfig().getString("subclaims.purchase_permission_node", "realms.claims.buychunk"));
            case "social", "class", "social_class" -> {
                // read allowed social classes from config (case-insensitive)
                java.util.List<String> allowedClasses = plugin.getConfig().getStringList("subclaims.required_social_classes");
                if (allowedClasses == null || allowedClasses.isEmpty()) {
                    allowed = false; // misconfigured -> deny
                } else {
                    PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
                    String playerClass = pd != null && pd.getSocialClass() != null ? pd.getSocialClass() : "";
                    allowed = allowedClasses.stream().anyMatch(s -> s.equalsIgnoreCase(playerClass));
                }
            }
            default -> allowed = town.isTrusted(pid);
        }

        if (!allowed) {
            // send specific hint based on policy
            switch (policy.toLowerCase(Locale.ROOT)) {
                case "mayor" -> player.sendMessage(MessageUtils.get("town.only_mayor_buychunk"));
                case "trusted" -> player.sendMessage(MessageUtils.get("town.only_mayor_or_trusted_buychunk"));
                case "members" -> player.sendMessage(MessageUtils.get("town.only_members_buychunk"));
                case "permission" -> player.sendMessage(MessageUtils.get("town.purchase_permission_required"));
                case "social", "class", "social_class" -> player.sendMessage(MessageUtils.get("town.only_socialclass_buychunk"));
                default -> player.sendMessage(MessageUtils.get("town.only_mayor_or_trusted_buychunk"));
            }
            return true;
        }

        // If using GriefPrevention integration via HybridClaimManager, disable player-purchase flow
        if (plugin.getHybridClaimManager() != null && plugin.getHybridClaimManager().isUsingGriefPrevention()) {
            player.sendMessage(MessageUtils.get("town.buychunk_gp_unsupported"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        ClaimManager cm = ClaimManager.getInstance(plugin);
        if (cm == null) {
            player.sendMessage(MessageUtils.get("town.claim_manager_unavailable"));
            return true;
        }

        ClaimManager.ClaimResult result = cm.claimChunkByPlayer(chunk, town, player.getUniqueId());
        if (result.isSuccess()) {
            // persist town and player data
            try {
                plugin.getDataManager().saveTown(town);
                plugin.getDataManager().savePlayerData(player.getUniqueId());
            } catch (Throwable ignored) {}

            // Refresh GUI category
            try { me.marcdoesntexists.realms.gui.RealmsGUI.refreshGUIsForCategory("TOWNS"); } catch (Throwable ignored) {}

            // localized success
            player.sendMessage(MessageUtils.get("town.claim_success_prefix") + result.getMessage());
        } else {
            player.sendMessage(MessageUtils.get("town.claim_fail_prefix") + result.getMessage());
        }

        return true;
    }

    private boolean handleClaimGrant(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("claim.usage_grant"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaimAt(chunk);
        if (claim == null) {
            player.sendMessage(MessageUtils.get("claim.no_claim"));
            return true;
        }

        // Only owner can grant
        String owner = claim.getOwnerUuid();
        if (owner == null || !owner.equals(player.getUniqueId().toString())) {
            player.sendMessage(MessageUtils.get("claim.not_owner"));
            return true;
        }

        String targetName = args[1];
        java.util.UUID targetUuid = plugin.getServer().getOfflinePlayer(targetName).getUniqueId();
        String perm = args[2].toLowerCase();
        Claim.ClaimPermission cp = switch (perm) {
            case "build" -> Claim.ClaimPermission.BUILD;
            case "container" -> Claim.ClaimPermission.CONTAINER;
            case "full" -> Claim.ClaimPermission.FULL;
            default -> null;
        };
        if (cp == null) {
            player.sendMessage(MessageUtils.get("claim.invalid_permission"));
            return true;
        }

        claim.grantPermission(targetUuid, cp);
        // persist claim meta
        try { ClaimManager.getInstance(plugin).saveClaimMeta(claim); } catch (Throwable ignored) {}
        player.sendMessage(MessageUtils.format("claim.grant_success", Map.of("player", targetName, "perm", perm)));
        // notify target if online
        var p = plugin.getServer().getPlayer(targetUuid);
        if (p != null) p.sendMessage(MessageUtils.format("claim.granted_notify", Map.of("by", player.getName(), "perm", perm)));

        return true;
    }

    private boolean handleClaimRevoke(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("claim.usage_revoke"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaimAt(chunk);
        if (claim == null) {
            player.sendMessage(MessageUtils.get("claim.no_claim"));
            return true;
        }

        String owner = claim.getOwnerUuid();
        if (owner == null || !owner.equals(player.getUniqueId().toString())) {
            player.sendMessage(MessageUtils.get("claim.not_owner"));
            return true;
        }

        String targetName = args[1];
        java.util.UUID targetUuid = plugin.getServer().getOfflinePlayer(targetName).getUniqueId();
        claim.revokePermission(targetUuid);
        try { ClaimManager.getInstance(plugin).saveClaimMeta(claim); } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("claim.revoke_success", Map.of("player", targetName)));
        var p = plugin.getServer().getPlayer(targetUuid);
        if (p != null) p.sendMessage(MessageUtils.format("claim.revoked_notify", Map.of("by", player.getName())));

        return true;
    }
}
