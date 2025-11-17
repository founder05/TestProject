package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Kingdom;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.marcdoesntexists.realms.economy.EconomyService;

public class KingdomCommand implements CommandExecutor, TabCompleter {

    private static final int MIN_TOWN_MEMBERS = 3;
    private final Realms plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;

    private final Map<java.util.UUID, Long> pendingDisbandConfirm = new ConcurrentHashMap<>();

    public KingdomCommand(Realms plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("kingdom.player_only"));
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
            case "claim":
                return handleClaim(player, args);
            case "unclaim":
                return handleUnclaim(player, args);
            case "deposit":
                return handleDeposit(player, args);
            case "withdraw":
                return handleWithdraw(player, args);
            case "leave":
                return handleLeave(player, args);
            case "accept":
                return handleAccept(player, args);
            case "kick":
                return handleKick(player, args);
            case "info":
                return handleInfo(player, args);
            case "invite":
                return handleInvite(player, args);
            case "vassalize":
                return handleVassalize(player, args);
            case "list":
                return handleList(player, args);
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
            player.sendMessage(MessageUtils.get("kingdom.usage_create"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("kingdom.only_mayor"));
            return true;
        }

        if (town.getKingdom() != null) {
            player.sendMessage(MessageUtils.get("kingdom.already_in_kingdom"));
            return true;
        }

        if (town.getMembers().size() < MIN_TOWN_MEMBERS) {
            player.sendMessage(MessageUtils.format("kingdom.not_enough_members", java.util.Map.of("min", String.valueOf(MIN_TOWN_MEMBERS))));
            return true;
        }

        String kingdomName = args[1];

        // validate length using same town config limits
        int min = plugin.getConfig().getInt("town.name_min", 3);
        int max = plugin.getConfig().getInt("town.name_max", 16);
        if (kingdomName.length() < min || kingdomName.length() > max) {
            player.sendMessage(MessageUtils.format("kingdom.invalid_name_length", java.util.Map.of("min", String.valueOf(min), "max", String.valueOf(max))));
            return true;
        }

        if (societiesManager.getKingdom(kingdomName) != null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_exists"));
            return true;
        }

        Kingdom newKingdom = new Kingdom(kingdomName, town.getName());
        societiesManager.registerKingdom(newKingdom);
        town.setKingdom(kingdomName);

        player.sendMessage(MessageUtils.format("kingdom.kingdom_created", Map.of("kingdom", kingdomName)));
        player.sendMessage(MessageUtils.format("kingdom.kingdom_capital_set", Map.of("town", town.getName())));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String kingdomName;

        if (args.length < 2) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data == null || data.getTown() == null) {
                player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
                return true;
            }

            Town town = societiesManager.getTown(data.getTown());
            if (town.getKingdom() == null) {
                player.sendMessage(MessageUtils.get("kingdom.already_in_kingdom"));
                return true;
            }

            kingdomName = town.getKingdom();
        } else {
            kingdomName = args[1];
        }

        Kingdom kingdom = societiesManager.getKingdom(kingdomName);
        if (kingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_header", Map.of("kingdom", kingdom.getName())));
        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_capital", Map.of("capital", kingdom.getCapital())));
        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_towns", Map.of("count", String.valueOf(kingdom.getTowns().size()))));
        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_allies", Map.of("count", String.valueOf(kingdom.getAllies().size()))));
        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_enemies", Map.of("count", String.valueOf(kingdom.getEnemies().size()))));
        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_wars", Map.of("count", String.valueOf(kingdom.getWars().size()))));
        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_vassals", Map.of("count", String.valueOf(kingdom.getVassals().size()))));

        if (kingdom.getSuzerain() != null) {
            player.sendMessage(MessageUtils.format("kingdom.kingdom_info_suzerain", Map.of("suzerain", kingdom.getSuzerain())));
        }

        if (kingdom.getEmpire() != null) {
            player.sendMessage(MessageUtils.format("kingdom.kingdom_info_empire", Map.of("empire", kingdom.getEmpire())));
        }

        player.sendMessage(MessageUtils.format("kingdom.kingdom_info_level", Map.of("level", String.valueOf(kingdom.getProgressionLevel()))));
        player.sendMessage(MessageUtils.get("kingdom.kingdom_info_header").replace("{kingdom}", ""));

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("kingdom.usage_invite"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town senderTown = societiesManager.getTown(data.getTown());
        if (senderTown.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.already_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(senderTown.getKingdom());
        if (!kingdom.isKing(senderTown.getName())) {
            player.sendMessage(MessageUtils.get("kingdom.only_mayor"));
            return true;
        }

        if (!senderTown.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("kingdom.only_mayor"));
            return true;
        }

        String targetTownName = args[1];
        Town targetTown = societiesManager.getTown(targetTownName);

        if (targetTown == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Town"));
            return true;
        }

        if (targetTown.getKingdom() != null) {
            player.sendMessage(MessageUtils.get("kingdom.already_in_kingdom"));
            return true;
        }

        targetTown.setKingdom(kingdom.getName());
        kingdom.addTown(targetTownName);

        player.sendMessage(MessageUtils.get("kingdom.kingdom_created").replace("{kingdom}", targetTownName));

        Player targetMayor = plugin.getServer().getPlayer(targetTown.getMayor());
        if (targetMayor != null) {
            targetMayor.sendMessage(MessageUtils.format("alliance.invite_notify", Map.of("alliance", kingdom.getName())));
        }

        return true;
    }

    private boolean handleVassalize(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("kingdom.usage_vassalize"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.already_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("kingdom.only_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("kingdom.only_mayor"));
            return true;
        }

        String targetKingdomName = args[1];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);

        if (targetKingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        if (targetKingdom.getName().equals(kingdom.getName())) {
            player.sendMessage(MessageUtils.get("commands.invalid_number"));
            return true;
        }

        if (targetKingdom.getSuzerain() != null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        kingdom.addVassal(targetKingdomName);
        targetKingdom.setSuzerain(kingdom.getName());

        player.sendMessage(MessageUtils.format("kingdom.kingdom_created", Map.of("kingdom", targetKingdomName)));

        Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
        if (targetCapital != null) {
            Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
            if (targetKing != null) {
                targetKing.sendMessage(MessageUtils.format("kingdom.kingdom_info_header", Map.of("kingdom", kingdom.getName())));
            }
        }

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Kingdom> kingdoms = societiesManager.getAllKingdoms();

        if (kingdoms.isEmpty()) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Kingdoms"));
            return true;
        }

        player.sendMessage(MessageUtils.format("kingdom.kings_list_header", Map.of("count", String.valueOf(kingdoms.size()))));

        for (Kingdom kingdom : kingdoms) {
            String capital = kingdom.getCapital() != null ? kingdom.getCapital() : "-";
            player.sendMessage(MessageUtils.format("kingdom.kings_list_item", Map.of("name", kingdom.getName(), "capital", capital, "count", String.valueOf(kingdom.getTowns().size()))));
        }

        player.sendMessage(MessageUtils.get("kingdom.kings_list_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("kingdom.help_header"));
        player.sendMessage(MessageUtils.get("kingdom.help_create"));
        player.sendMessage(MessageUtils.get("kingdom.help_info"));
        player.sendMessage(MessageUtils.get("kingdom.help_invite"));
        player.sendMessage(MessageUtils.get("kingdom.help_vassalize"));
        player.sendMessage(MessageUtils.get("kingdom.help_list"));
        player.sendMessage(MessageUtils.get("kingdom.help_trust"));
        player.sendMessage(MessageUtils.get("kingdom.help_untrust"));
        player.sendMessage(MessageUtils.get("kingdom.help_footer"));
    }

    // ========== Trust management ==========
    private boolean handleTrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("kingdom.usage_trust"));
            return true;
        }

        String kingdomName = args[1];
        Kingdom kingdom = societiesManager.getKingdom(kingdomName);
        if (kingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null || !kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("kingdom.only_king_may_manage"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("kingdom.only_king_may_manage"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("kingdom.usage_trust"));
            return true;
        }

        var target = plugin.getServer().getOfflinePlayer(args[2]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        boolean added = kingdom.addTrusted(target.getUniqueId());
        String tname = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        if (added) {
            dataManager.saveKingdom(kingdom);
            player.sendMessage(MessageUtils.format("kingdom.trust_success", java.util.Map.of("player", tname)));
            if (target.isOnline()) {
                target.getPlayer().sendMessage(MessageUtils.format("kingdom.trusted_notify", java.util.Map.of("kingdom", kingdom.getName())));
            }
        } else {
            player.sendMessage(MessageUtils.format("kingdom.already_trusted", java.util.Map.of("player", tname)));
        }

        return true;
    }

    private boolean handleUntrust(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("kingdom.usage_untrust"));
            return true;
        }

        String kingdomName = args[1];
        Kingdom kingdom = societiesManager.getKingdom(kingdomName);
        if (kingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null || !kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("kingdom.only_king_may_manage"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("kingdom.only_king_may_manage"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("kingdom.usage_untrust"));
            return true;
        }

        var target = plugin.getServer().getOfflinePlayer(args[2]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        boolean removed = kingdom.removeTrusted(target.getUniqueId());
        String tname = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        if (removed) {
            dataManager.saveKingdom(kingdom);
            player.sendMessage(MessageUtils.format("kingdom.untrust_success", java.util.Map.of("player", tname)));
            if (target.isOnline()) {
                target.getPlayer().sendMessage(MessageUtils.format("kingdom.untrusted_notify", java.util.Map.of("kingdom", kingdom.getName())));
            }
        } else {
            player.sendMessage(MessageUtils.format("kingdom.not_trusted", java.util.Map.of("player", tname)));
        }

        return true;
    }

    private boolean handlePvp(Player player, String[] args) {
        // Usage: /kingdom pvp <on|off|toggle>
        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null || town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        // only the ruler (mayor of capital town) can toggle kingdom-level claim pvp
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("kingdom.only_king_may_manage"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaimAt(chunk);
        if (claim == null) {
            player.sendMessage(MessageUtils.get("claim.no_claim"));
            return true;
        }

        if (!kingdom.getTowns().contains(claim.getTownName())) {
            player.sendMessage(MessageUtils.get("claim.pvp_not_owner"));
            return true;
        }

        String action = args.length >= 2 ? args[1].toLowerCase() : "toggle";
        boolean target;
        switch (action) {
            case "on": target = true; break;
            case "off": target = false; break;
            default: target = claim.isPvpEnabled(); break;
        }

        claim.setPvpEnabled(target);
        // persist changed town
        Town claimTown = societiesManager.getTown(claim.getTownName());
        if (claimTown != null) {
            try { plugin.getDataManager().saveTown(claimTown); } catch (Throwable ignored) {}
        }

        player.sendMessage(MessageUtils.format(target ? "claim.pvp_enabled" : "claim.pvp_disabled", Map.of("town", claim.getTownName())));
        return true;
    }

    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("kingdom.usage_transfer"));
            return true;
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null || town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        // only ruler (mayor of capital) can transfer
        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("kingdom.only_king_may_manage"));
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        // change mayor of capital town
        Town capital = societiesManager.getTown(kingdom.getCapital());
        if (capital == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        UUID oldMayor = capital.getMayor();
        capital.setMayor(target.getUniqueId());
        try { dataManager.saveTown(capital); } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("kingdom.transfer_success", Map.of("kingdom", kingdom.getName(), "newOwner", target.getName() != null ? target.getName() : args[1])));
        if (target.isOnline()) ((Player) target.getPlayer()).sendMessage(MessageUtils.format("kingdom.transfer_notify", Map.of("kingdom", kingdom.getName())));

        // Notify kingdom members
         for (String townName : new ArrayList<>(kingdom.getTowns())) {
             Town t = societiesManager.getTown(townName);
             if (t == null) continue;
             for (UUID member : new ArrayList<>(t.getMembers())) {
                var off = plugin.getServer().getOfflinePlayer(member);
                var oldOff = plugin.getServer().getOfflinePlayer(oldMayor);
                String oldOwnerName = (oldOff != null && oldOff.getName() != null) ? oldOff.getName() : (oldMayor != null ? oldMayor.toString() : "?");
                String newOwnerName = target.getName() != null ? target.getName() : args[1];
                String msg = MessageUtils.format("kingdom.notify_transfer_member", Map.of("kingdom", kingdom.getName(), "newOwner", newOwnerName, "oldOwner", oldOwnerName));
                if (off != null && off.isOnline() && off.getPlayer() != null) {
                    off.getPlayer().sendMessage(msg);
                } else if (off != null) {
                    try {
                        var memberData = dataManager.getPlayerData(member);
                        memberData.addNotification(msg);
                        dataManager.savePlayerData(member);
                    } catch (Throwable ignored) {}
                }
             }
         }

        return true;
    }

    private boolean handleDisband(Player player, String[] args) {
        final long now = System.currentTimeMillis();
        final long TTL = 30_000L;

        if (args.length >= 2 && (args[1].equalsIgnoreCase("confirm") || args[1].equalsIgnoreCase("yes"))) {
            // proceed
        } else {
            Long exp = pendingDisbandConfirm.get(player.getUniqueId());
            if (exp == null || exp < now) {
                pendingDisbandConfirm.put(player.getUniqueId(), now + TTL);
                player.sendMessage(MessageUtils.get("misc.confirm_prompt"));
                player.sendMessage(MessageUtils.get("kingdom.disband_confirm_hint"));
                return true;
            }
        }

        // remove pending
        pendingDisbandConfirm.remove(player.getUniqueId());

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town t = societiesManager.getTown(pd.getTown());
        if (t == null || t.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(t.getKingdom());
        if (kingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        // only king (mayor of capital town) can disband
        if (!kingdom.isKing(t.getName()) || !t.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("kingdom.only_king_may_manage"));
            return true;
        }

        // For each town in the kingdom, remove kingdom association and delete claims
        for (String townName : new ArrayList<>(kingdom.getTowns())) {
            Town town = societiesManager.getTown(townName);
            if (town != null) {
                town.setKingdom(null);
                try { dataManager.saveTown(town); } catch (Throwable ignored) {}
                try { plugin.getHybridClaimManager().deleteTownClaims(townName); } catch (Throwable ignored) {}

                // notify members
                for (UUID member : new ArrayList<>(town.getMembers())) {
                    var off = plugin.getServer().getOfflinePlayer(member);
                    String msg = MessageUtils.format("kingdom.notify_disband_member", Map.of("kingdom", kingdom.getName(), "by", player.getName()));
                    if (off != null && off.isOnline() && off.getPlayer() != null) {
                        off.getPlayer().sendMessage(msg);
                    } else if (off != null) {
                        try {
                            var memberData = dataManager.getPlayerData(member);
                            memberData.addNotification(msg);
                            dataManager.savePlayerData(member);
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }

        // remove kingdom from manager
        societiesManager.removeKingdom(kingdom.getName());

        // delete kingdom file
        try {
            java.io.File kingdomFile = new java.io.File(plugin.getDataFolder(), "kingdoms" + java.io.File.separator + kingdom.getName() + ".yml");
            if (kingdomFile.exists()) kingdomFile.delete();
        } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("kingdom.disbanded", Map.of("kingdom", kingdom.getName())));
        return true;
    }

    // ========== Town-like commands (operate on player's town) ==========
    private boolean handleClaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (town == null) { player.sendMessage(MessageUtils.get("town.not_found")); return true; }
        if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor_claim")); return true; }
        Chunk chunk = player.getLocation().getChunk();
        var result = plugin.getHybridClaimManager().claimChunk(player, town, chunk);
        if (result.isSuccess()) player.sendMessage(MessageUtils.get("town.claim_success_prefix") + result.getMessage()); else player.sendMessage(MessageUtils.get("town.claim_fail_prefix") + result.getMessage());
        return true;
    }

    private boolean handleUnclaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (town == null) { player.sendMessage(MessageUtils.get("town.not_found")); return true; }
        if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor_claim")); return true; }
        Chunk chunk = player.getLocation().getChunk();
        var result = plugin.getHybridClaimManager().unclaimChunk(town, chunk);
        if (result.isSuccess()) player.sendMessage(MessageUtils.get("town.claim_success_prefix") + result.getMessage()); else player.sendMessage(MessageUtils.get("town.claim_fail_prefix") + result.getMessage());
        return true;
    }

    private boolean handleDeposit(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_deposit")); return true; }
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        int amount; try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { player.sendMessage(MessageUtils.get("commands.invalid_number")); return true; }
        if (amount <= 0) { player.sendMessage(MessageUtils.get("town.amount_must_be_positive")); return true; }
        Town town = societiesManager.getTown(data.getTown());
        EconomyService econ = EconomyService.getInstance();
        boolean success = econ.withdrawFromPlayer(player.getUniqueId(), amount);
        if (!success) { player.sendMessage(MessageUtils.get("town.withdraw_external_failed")); return true; }
        town.addMoney(amount);
        try { plugin.getDataManager().saveTown(town); plugin.getDataManager().savePlayerMoney(player.getUniqueId()); } catch (Throwable ignored) {}
        player.sendMessage(MessageUtils.format("town.deposit_success", Map.of("amount", String.valueOf(amount))));
        player.sendMessage(MessageUtils.format("town.new_balance", Map.of("balance", String.valueOf(town.getBalance()))));
        return true;
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_withdraw")); return true; }
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor")); return true; }
        int amount; try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { player.sendMessage(MessageUtils.get("commands.invalid_number")); return true; }
        if (town.removeMoney(amount)) {
            boolean depositOk = EconomyService.getInstance().depositToPlayer(player.getUniqueId(), amount);
            if (!depositOk) { town.addMoney(amount); try { plugin.getDataManager().saveTown(town); } catch (Throwable ignored) {} player.sendMessage(MessageUtils.get("town.deposit_failed_external")); return true; }
            try { plugin.getDataManager().saveTown(town); plugin.getDataManager().savePlayerMoney(player.getUniqueId()); } catch (Throwable ignored) {}
            player.sendMessage(MessageUtils.format("town.withdraw_success", Map.of("amount", String.valueOf(amount))));
            player.sendMessage(MessageUtils.format("town.new_balance", Map.of("balance", String.valueOf(town.getBalance()))));
        } else {
            player.sendMessage(MessageUtils.format("town.insufficient_funds", Map.of("balance", String.valueOf(town.getBalance()))));
        }
        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.cannot_leave_mayor")); return true; }
        town.removeMember(player.getUniqueId()); data.setTown(null);
        player.sendMessage(MessageUtils.format("town.left", Map.of("town", town.getName())));
        Player mayor = plugin.getServer().getPlayer(town.getMayor()); if (mayor != null) mayor.sendMessage(MessageUtils.format("town.notify_left", Map.of("player", player.getName(), "town", town.getName())));
        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_accept")); return true; }
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data.getTown() != null) { player.sendMessage(MessageUtils.get("town.already_in_town")); return true; }
        String townName = args[1]; if (!data.getTownInvites().contains(townName)) { player.sendMessage(MessageUtils.get("town.no_invite")); return true; }
        Town town = societiesManager.getTown(townName); if (town == null) { player.sendMessage(MessageUtils.get("town.not_found_literal")); data.removeTownInvite(townName); return true; }
        town.addMember(player.getUniqueId()); data.setTown(townName); data.removeTownInvite(townName);
        player.sendMessage(MessageUtils.format("town.joined", Map.of("town", townName)));
        Player mayor = plugin.getServer().getPlayer(town.getMayor()); if (mayor != null) mayor.sendMessage(MessageUtils.format("town.notify_join", Map.of("player", player.getName(), "town", townName)));
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_kick")); return true; }
        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId()); if (senderData.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(senderData.getTown()); if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor")); return true; }
        Player target = plugin.getServer().getPlayer(args[1]); if (target == null) { player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player")); return true; }
        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId()); if (!town.getMembers().contains(target.getUniqueId())) { player.sendMessage(MessageUtils.get("town.player_not_in_your_town")); return true; }
        if (town.isMayor(target.getUniqueId())) { player.sendMessage(MessageUtils.get("town.cannot_kick_self")); return true; }
        town.removeMember(target.getUniqueId()); targetData.setTown(null);
        player.sendMessage(MessageUtils.format("town.kick_success", Map.of("player", target.getName(), "town", town.getName())));
        target.sendMessage(MessageUtils.format("town.kicked_notify", Map.of("town", town.getName())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = Arrays.asList("create", "info", "invite", "vassalize", "list", "transfer", "disband", "claim", "unclaim", "deposit", "withdraw", "accept", "leave", "kick", "trust", "untrust", "pvp");
        if (args.length == 1) return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(subs, args[0]);

        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            switch (sub) {
                case "create":
                    return List.of();
                case "info":
                case "vassalize":
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.kingdoms(societiesManager, args[1]);
                case "invite":
                    // suggest towns without kingdom
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(
                            societiesManager.getAllTowns().stream().filter(t -> t.getKingdom() == null).map(Town::getName).collect(java.util.stream.Collectors.toList()),
                            args[1]
                    );
                case "transfer":
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.onlinePlayers(args[1]);
                case "disband":
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(List.of("confirm"), args[1]);
                case "kick":
                case "trust":
                case "untrust":
                    if (!(sender instanceof Player)) return List.of();
                    PlayerData pd = dataManager.getPlayerData(((Player) sender).getUniqueId());
                    if (pd.getTown() == null) return List.of();
                    Town town = societiesManager.getTown(pd.getTown());
                    if (town == null) return List.of();
                    List<String> members = new ArrayList<>();
                    for (UUID m : town.getMembers()) members.add(plugin.getServer().getOfflinePlayer(m).getName() != null ? plugin.getServer().getOfflinePlayer(m).getName() : m.toString());
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(members, args[1]);
                default:
                    return List.of();
            }
        }

        if (args.length == 3) {
            if (sub.equals("withdraw") || sub.equals("deposit")) {
                return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(Arrays.asList("10","50","100","500","1000"), args[2]);
            }
        }

        return List.of();
    }

    // ========== SUBCLAIMS SUPPORT (Kingdom members can buy chunks in kingdom territory) ==========

    private boolean handleBuyChunk(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town == null || town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null) {
            player.sendMessage(MessageUtils.get("kingdom.kingdom_not_found"));
            return true;
        }

        // Policy check
        String policy = plugin.getConfig().getString("subclaims.purchase_policy", "trusted");
        java.util.UUID pid = player.getUniqueId();
        boolean allowed = false;
        switch (policy.toLowerCase(Locale.ROOT)) {
            case "mayor" -> allowed = town.isMayor(pid) || kingdom.isKing(String.valueOf(pid));
            case "trusted" -> allowed = kingdom.isTrusted(pid) || town.isTrusted(pid);
            case "members" -> allowed = kingdom.getAllMembers(societiesManager).contains(pid);
            case "permission" -> allowed = player.hasPermission(plugin.getConfig().getString("subclaims.purchase_permission_node", "realms.claims.buychunk"));
            case "social", "class", "social_class" -> {
                java.util.List<String> allowedClasses = plugin.getConfig().getStringList("subclaims.required_social_classes");
                if (allowedClasses.isEmpty()) {
                } else {
                    PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
                    String playerClass = pd != null && pd.getSocialClass() != null ? pd.getSocialClass() : "";
                    allowed = allowedClasses.stream().anyMatch(s -> s.equalsIgnoreCase(playerClass));
                }
            }
            default -> allowed = kingdom.isTrusted(pid) || town.isTrusted(pid);
        }

        if (!allowed) {
            switch (policy.toLowerCase(Locale.ROOT)) {
                case "mayor" -> player.sendMessage(MessageUtils.get("kingdom.only_capital_mayor"));
                case "trusted" -> player.sendMessage(MessageUtils.get("kingdom.only_members_can_buychunk"));
                case "members" -> player.sendMessage(MessageUtils.get("kingdom.only_members_can_buychunk"));
                case "permission" -> player.sendMessage(MessageUtils.get("kingdom.purchase_permission_required"));
                case "social" , "class", "social_class" -> player.sendMessage(MessageUtils.get("kingdom.only_socialclass_buychunk"));
                default -> player.sendMessage(MessageUtils.get("kingdom.only_members_can_buychunk"));
            }
            return true;
        }

        // If using GriefPrevention integration via HybridClaimManager, disable player-purchase flow
        if (plugin.getHybridClaimManager() != null && plugin.getHybridClaimManager().isUsingGriefPrevention()) {
            player.sendMessage(MessageUtils.get("kingdom.buychunk_gp_unsupported"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        me.marcdoesntexists.realms.managers.ClaimManager cm = me.marcdoesntexists.realms.managers.ClaimManager.getInstance(plugin);
        if (cm == null) {
            player.sendMessage(MessageUtils.get("kingdom.claim_manager_unavailable"));
            return true;
        }

        // Use the player's town for claiming, but player becomes owner
        me.marcdoesntexists.realms.managers.ClaimManager.ClaimResult result = cm.claimChunkByPlayer(chunk, town, player.getUniqueId());
        if (result.isSuccess()) {
            // persist town and player data
            try {
                plugin.getDataManager().saveTown(town);
                plugin.getDataManager().savePlayerData(player.getUniqueId());
            } catch (Throwable ignored) {}

            // Refresh GUI category
            try { me.marcdoesntexists.realms.gui.RealmsGUI.refreshGUIsForCategory("KINGDOMS"); } catch (Throwable ignored) {}

            player.sendMessage(MessageUtils.get("kingdom.claim_success_prefix") + result.getMessage());
        } else {
            player.sendMessage(MessageUtils.get("kingdom.claim_fail_prefix") + result.getMessage());
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
        try { me.marcdoesntexists.realms.managers.ClaimManager.getInstance(plugin).saveClaimMeta(claim); } catch (Throwable ignored) {}
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
        try { me.marcdoesntexists.realms.managers.ClaimManager.getInstance(plugin).saveClaimMeta(claim); } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("claim.revoke_success", Map.of("player", targetName)));
        var p = plugin.getServer().getPlayer(targetUuid);
        if (p != null) p.sendMessage(MessageUtils.format("claim.revoked_notify", Map.of("by", player.getName())));

        return true;
    }
}
