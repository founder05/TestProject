package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Empire;
import me.marcdoesntexists.realms.societies.Kingdom;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.PlayerData;
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

public class EmpireCommand implements CommandExecutor, TabCompleter {
    private final Realms plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;

    private final Map<java.util.UUID, Long> pendingDisbandConfirm = new ConcurrentHashMap<>();

    public EmpireCommand(Realms plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
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

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "claim" -> handleClaim(player, args);
            case "unclaim" -> handleUnclaim(player, args);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "accept" -> handleAccept(player, args);
            case "leave" -> handleLeave(player, args);
            case "kick" -> handleKick(player, args);
            case "trust" -> handleTrust(player, args);
            case "untrust" -> handleUntrust(player, args);
            case "pvp" -> handlePvp(player, args);
            case "transfer" -> handleTransfer(player, args);
            case "disband" -> handleDisband(player, args);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean handleTrust(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("empire.usage_trust"));
            return true;
        }

        String empireName = args[1];
        Empire empire = societiesManager.getEmpire(empireName);
        if (empire == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd == null || pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("empire.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null || !town.getKingdom().equals(societiesManager.getKingdom(town.getKingdom()).getName())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        // Check if player is ruler (mayor of capital kingdom town)
        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null || !kingdom.getEmpire().equals(empire.getName())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        // Only allow mayor of the capital town to manage empire trust
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[2]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        boolean added = empire.addTrusted(target.getUniqueId());
        String targetName = target.getName() != null ? target.getName() : args[2];
        if (added) {
            dataManager.saveEmpire(empire);
            player.sendMessage(MessageUtils.format("empire.trust_success", java.util.Map.of("player", targetName)));
            if (target.isOnline()) {
                ((Player) target.getPlayer()).sendMessage(MessageUtils.format("empire.trusted_notify", java.util.Map.of("empire", empire.getName())));
            }
        } else {
            player.sendMessage(MessageUtils.format("empire.already_trusted", java.util.Map.of("player", targetName)));
        }

        return true;
    }

    private boolean handleUntrust(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("empire.usage_untrust"));
            return true;
        }

        String empireName = args[1];
        Empire empire = societiesManager.getEmpire(empireName);
        if (empire == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd == null || pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("empire.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null || !kingdom.getEmpire().equals(empire.getName())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[2]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        boolean removed = empire.removeTrusted(target.getUniqueId());
        String targetName = target.getName() != null ? target.getName() : args[2];
        if (removed) {
            dataManager.saveEmpire(empire);
            player.sendMessage(MessageUtils.format("empire.untrust_success", java.util.Map.of("player", targetName)));
            if (target.isOnline()) {
                ((Player) target.getPlayer()).sendMessage(MessageUtils.format("empire.untrusted_notify", java.util.Map.of("empire", empire.getName())));
            }
        } else {
            player.sendMessage(MessageUtils.format("empire.not_trusted", java.util.Map.of("player", targetName)));
        }

        return true;
    }

    private boolean handlePvp(Player player, String[] args) {
        // Usage: /empire pvp <on|off|toggle>
        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd == null || pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("empire.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null || kingdom.getEmpire() == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        Empire empire = societiesManager.getEmpire(kingdom.getEmpire());
        if (empire == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        // Only the mayor of the capital of the empire can toggle
        Town capitalTown = societiesManager.getTown(empire.getCapital());
        if (capitalTown == null || !capitalTown.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        Claim claim = plugin.getClaimManager().getClaimAt(chunk);
        if (claim == null) {
            player.sendMessage(MessageUtils.get("claim.no_claim"));
            return true;
        }

        if (!empire.getKingdoms().contains(societiesManager.getKingdom(claim.getTownName()).getName())) {
            // ensure the claim belongs to a kingdom that is part of this empire
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
        Town claimTown = societiesManager.getTown(claim.getTownName());
        if (claimTown != null) {
            try { plugin.getDataManager().saveTown(claimTown); } catch (Throwable ignored) {}
        }

        player.sendMessage(MessageUtils.format(target ? "claim.pvp_enabled" : "claim.pvp_disabled", Map.of("town", claim.getTownName())));
        return true;
    }

    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("empire.usage_transfer"));
            return true;
        }

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd == null || pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("empire.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null || kingdom.getEmpire() == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        Empire empire = societiesManager.getEmpire(kingdom.getEmpire());
        if (empire == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        // only capital's mayor can transfer
        Town capitalTown = societiesManager.getTown(empire.getCapital());
        if (capitalTown == null || !capitalTown.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        UUID oldMayor = capitalTown.getMayor();
        capitalTown.setMayor(target.getUniqueId());
        try { dataManager.saveTown(capitalTown); } catch (Throwable ignored) {}

        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        player.sendMessage(MessageUtils.format("empire.transfer_success", Map.of("empire", empire.getName(), "newOwner", targetName)));
        if (target.isOnline()) target.getPlayer().sendMessage(MessageUtils.format("empire.transfer_notify", Map.of("empire", empire.getName())));

        // notify all members of all kingdoms in the empire
        for (String kname : new ArrayList<>(empire.getKingdoms())) {
            Kingdom k = societiesManager.getKingdom(kname);
            if (k == null) continue;
            for (String townName : new ArrayList<>(k.getTowns())) {
                Town t = societiesManager.getTown(townName);
                if (t == null) continue;
                for (UUID member : new ArrayList<>(t.getMembers())) {
                    var off = plugin.getServer().getOfflinePlayer(member);
                    OfflinePlayer oldOff = plugin.getServer().getOfflinePlayer(oldMayor);
                    String oldName = (oldOff != null && oldOff.getName() != null) ? oldOff.getName() : (oldMayor != null ? oldMayor.toString() : "?");
                    String newName = targetName;
                    String msg = MessageUtils.format("empire.notify_transfer_member", Map.of("empire", empire.getName(), "newOwner", newName, "oldOwner", oldName));
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
                player.sendMessage(MessageUtils.get("empire.disband_confirm_hint"));
                return true;
            }
        }

        pendingDisbandConfirm.remove(player.getUniqueId());

        PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
        if (pd == null || pd.getTown() == null) {
            player.sendMessage(MessageUtils.get("empire.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(pd.getTown());
        if (town == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null || kingdom.getEmpire() == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        Empire empire = societiesManager.getEmpire(kingdom.getEmpire());
        if (empire == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        // only capital's mayor can disband
        Town capitalTown = societiesManager.getTown(empire.getCapital());
        if (capitalTown == null || !capitalTown.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("empire.only_capital_may_manage"));
            return true;
        }

        // For each kingdom under empire, detach and remove claims for each town
        for (String kingdomName : new ArrayList<>(empire.getKingdoms())) {
            Kingdom k = societiesManager.getKingdom(kingdomName);
            if (k == null) continue;

            for (String townName : new ArrayList<>(k.getTowns())) {
                Town t = societiesManager.getTown(townName);
                if (t != null) {
                    t.setKingdom(null);
                    try { dataManager.saveTown(t); } catch (Throwable ignored) {}
                    try { plugin.getHybridClaimManager().deleteTownClaims(townName); } catch (Throwable ignored) {}

                    // notify members
                    for (UUID member : new ArrayList<>(t.getMembers())) {
                        var off = plugin.getServer().getOfflinePlayer(member);
                        String msg = MessageUtils.format("empire.notify_disband_member", Map.of("empire", empire.getName(), "by", player.getName()));
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

            // remove kingdom's empire reference
            try {
                k.setEmpire(null);
                dataManager.saveKingdom(k);
            } catch (Throwable ignored) {}
        }

        // Remove empire from manager and delete file
        societiesManager.removeEmpire(empire.getName());
        try {
            java.io.File empireFile = new java.io.File(plugin.getDataFolder(), "empires" + java.io.File.separator + empire.getName() + ".yml");
            if (empireFile.exists()) empireFile.delete();
        } catch (Throwable ignored) {}

        player.sendMessage(MessageUtils.format("empire.disbanded", Map.of("empire", empire.getName())));
        return true;
    }

    // town-like helpers for empire context
    private boolean handleClaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (town == null) { player.sendMessage(MessageUtils.get("town.not_found")); return true; }
        if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor_claim")); return true; }
        Chunk chunk = player.getLocation().getChunk(); var result = plugin.getHybridClaimManager().claimChunk(player, town, chunk);
        if (result.isSuccess()) player.sendMessage(MessageUtils.get("town.claim_success_prefix") + result.getMessage()); else player.sendMessage(MessageUtils.get("town.claim_fail_prefix") + result.getMessage());
        return true;
    }

    private boolean handleUnclaim(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (town == null) { player.sendMessage(MessageUtils.get("town.not_found")); return true; }
        if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor_claim")); return true; }
        Chunk chunk = player.getLocation().getChunk(); var result = plugin.getHybridClaimManager().unclaimChunk(town, chunk);
        if (result.isSuccess()) player.sendMessage(MessageUtils.get("town.claim_success_prefix") + result.getMessage()); else player.sendMessage(MessageUtils.get("town.claim_fail_prefix") + result.getMessage());
        return true;
    }

    private boolean handleDeposit(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_deposit")); return true; }
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        int amount; try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { player.sendMessage(MessageUtils.get("commands.invalid_number")); return true; }
        if (amount <= 0) { player.sendMessage(MessageUtils.get("town.amount_must_be_positive")); return true; }
        Town town = societiesManager.getTown(data.getTown()); boolean success = EconomyService.getInstance().withdrawFromPlayer(player.getUniqueId(), amount);
        if (!success) { player.sendMessage(MessageUtils.get("town.withdraw_external_failed")); return true; }
        town.addMoney(amount); try { plugin.getDataManager().saveTown(town); plugin.getDataManager().savePlayerMoney(player.getUniqueId()); } catch (Throwable ignored) {}
        player.sendMessage(MessageUtils.format("town.deposit_success", Map.of("amount", String.valueOf(amount))));
        player.sendMessage(MessageUtils.format("town.new_balance", Map.of("balance", String.valueOf(town.getBalance()))));
        return true;
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_withdraw")); return true; }
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor")); return true; }
        int amount; try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { player.sendMessage(MessageUtils.get("commands.invalid_number")); return true; }
        if (town.removeMoney(amount)) { if (!EconomyService.getInstance().depositToPlayer(player.getUniqueId(), amount)) { town.addMoney(amount); try { plugin.getDataManager().saveTown(town); } catch (Throwable ignored) {} player.sendMessage(MessageUtils.get("town.deposit_failed_external")); return true; } try { plugin.getDataManager().saveTown(town); plugin.getDataManager().savePlayerMoney(player.getUniqueId()); } catch (Throwable ignored) {} player.sendMessage(MessageUtils.format("town.withdraw_success", Map.of("amount", String.valueOf(amount)))); player.sendMessage(MessageUtils.format("town.new_balance", Map.of("balance", String.valueOf(town.getBalance())))); } else { player.sendMessage(MessageUtils.format("town.insufficient_funds", Map.of("balance", String.valueOf(town.getBalance())))); }
        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_accept")); return true; }
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data.getTown() != null) { player.sendMessage(MessageUtils.get("town.already_in_town")); return true; }
        String townName = args[1]; if (!data.getTownInvites().contains(townName)) { player.sendMessage(MessageUtils.get("town.no_invite")); return true; }
        Town town = societiesManager.getTown(townName); if (town == null) { player.sendMessage(MessageUtils.get("town.not_found_literal")); data.removeTownInvite(townName); return true; }
        town.addMember(player.getUniqueId()); data.setTown(townName); data.removeTownInvite(townName); player.sendMessage(MessageUtils.format("town.joined", Map.of("town", townName))); Player mayor = plugin.getServer().getPlayer(town.getMayor()); if (mayor != null) mayor.sendMessage(MessageUtils.format("town.notify_join", Map.of("player", player.getName(), "town", townName))); return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId()); if (data == null || data.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(data.getTown()); if (town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.cannot_leave_mayor")); return true; } town.removeMember(player.getUniqueId()); data.setTown(null); player.sendMessage(MessageUtils.format("town.left", Map.of("town", town.getName()))); Player mayor = plugin.getServer().getPlayer(town.getMayor()); if (mayor != null) mayor.sendMessage(MessageUtils.format("town.notify_left", Map.of("player", player.getName(), "town", town.getName()))); return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(MessageUtils.get("town.usage_kick")); return true; }
        PlayerData senderData = dataManager.getPlayerData(player.getUniqueId()); if (senderData == null || senderData.getTown() == null) { player.sendMessage(MessageUtils.get("town.not_in_town")); return true; }
        Town town = societiesManager.getTown(senderData.getTown()); if (!town.isMayor(player.getUniqueId())) { player.sendMessage(MessageUtils.get("town.only_mayor")); return true; }
        Player target = plugin.getServer().getPlayer(args[1]); if (target == null) { player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player")); return true; }
        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId()); if (!town.getMembers().contains(target.getUniqueId())) { player.sendMessage(MessageUtils.get("town.player_not_in_your_town")); return true; }
        if (town.isMayor(target.getUniqueId())) { player.sendMessage(MessageUtils.get("town.cannot_kick_self")); return true; }
        town.removeMember(target.getUniqueId()); targetData.setTown(null); player.sendMessage(MessageUtils.format("town.kick_success", Map.of("player", target.getName(), "town", town.getName()))); target.sendMessage(MessageUtils.format("town.kicked_notify", Map.of("town", town.getName()))); return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("empire.help.header"));
        player.sendMessage(MessageUtils.get("town.help.claim"));
        player.sendMessage(MessageUtils.get("town.help.unclaim"));
        player.sendMessage(MessageUtils.get("town.help.deposit"));
        player.sendMessage(MessageUtils.get("town.help.withdraw"));
        player.sendMessage(MessageUtils.get("empire.help_trust"));
        player.sendMessage(MessageUtils.get("empire.help_untrust"));
        player.sendMessage(MessageUtils.get("empire.help_transfer"));
        player.sendMessage(MessageUtils.get("empire.help_disband"));
        player.sendMessage(MessageUtils.get("empire.help.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = Arrays.asList("trust", "untrust", "transfer", "disband", "claim", "unclaim", "deposit", "withdraw", "accept", "leave", "kick");
        if (args.length == 1) return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(subs, args[0]);

        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            switch (sub) {
                case "trust":
                case "untrust":
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.empires(societiesManager, args[1]);
                case "transfer":
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.onlinePlayers(args[1]);
                case "disband":
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(List.of("confirm"), args[1]);
                case "kick":
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.onlinePlayers(args[1]);
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

    // ========== SUBCLAIMS SUPPORT (Empire members can buy chunks in empire territory) ==========

    private boolean handleBuyChunk(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("empire.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town == null || town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("empire.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (kingdom == null || kingdom.getEmpire() == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        Empire empire = societiesManager.getEmpire(kingdom.getEmpire());
        if (empire == null) {
            player.sendMessage(MessageUtils.get("empire.not_found"));
            return true;
        }

        // Policy check
        String policy = plugin.getConfig().getString("subclaims.purchase_policy", "trusted");
        java.util.UUID pid = player.getUniqueId();
        boolean allowed = false;
        switch (policy.toLowerCase(Locale.ROOT)) {
            case "mayor" -> {
                if (town.isMayor(pid)) {
                    allowed = true;
                } else {
                    // check kingdom capital mayor
                    String kCapital = kingdom.getCapital();
                    if (kCapital != null) {
                        Town capTown = societiesManager.getTown(kCapital);
                        if (capTown != null && capTown.isMayor(pid)) allowed = true;
                    }
                    // check empire capital mayor
                    if (!allowed && empire.getCapital() != null) {
                        Town eCap = societiesManager.getTown(empire.getCapital());
                        if (eCap != null && eCap.isMayor(pid)) allowed = true;
                    }
                }
            }
            case "trusted" -> allowed = empire.isTrusted(pid) || kingdom.isTrusted(pid) || town.isTrusted(pid);
            case "members" -> allowed = empire.getAllMembers(societiesManager).contains(pid);
            case "permission" -> allowed = player.hasPermission(plugin.getConfig().getString("subclaims.purchase_permission_node", "realms.claims.buychunk"));
            case "social", "class", "social_class" -> {
                java.util.List<String> allowedClasses = plugin.getConfig().getStringList("subclaims.required_social_classes");
                if (allowedClasses == null || allowedClasses.isEmpty()) {
                } else {
                    PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
                    String playerClass = pd != null && pd.getSocialClass() != null ? pd.getSocialClass() : "";
                    allowed = allowedClasses.stream().anyMatch(s -> s.equalsIgnoreCase(playerClass));
                }
            }
            default -> allowed = empire.isTrusted(pid) || kingdom.isTrusted(pid) || town.isTrusted(pid);
        }

        if (!allowed) {
            switch (policy.toLowerCase(Locale.ROOT)) {
                case "mayor" -> player.sendMessage(MessageUtils.get("empire.only_capital_mayor"));
                case "trusted" -> player.sendMessage(MessageUtils.get("empire.only_members_can_buychunk"));
                case "members" -> player.sendMessage(MessageUtils.get("empire.only_members_can_buychunk"));
                case "permission" -> player.sendMessage(MessageUtils.get("empire.purchase_permission_required"));
                case "social", "class", "social_class" -> player.sendMessage(MessageUtils.get("empire.only_socialclass_buychunk"));
                default -> player.sendMessage(MessageUtils.get("empire.only_members_can_buychunk"));
             }
             return true;
         }

        // If using GriefPrevention integration via HybridClaimManager, disable player-purchase flow
        if (plugin.getHybridClaimManager() != null && plugin.getHybridClaimManager().isUsingGriefPrevention()) {
            player.sendMessage(MessageUtils.get("empire.buychunk_gp_unsupported"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        me.marcdoesntexists.realms.managers.ClaimManager cm = me.marcdoesntexists.realms.managers.ClaimManager.getInstance(plugin);
        if (cm == null) {
            player.sendMessage(MessageUtils.get("empire.claim_manager_unavailable"));
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
            try { me.marcdoesntexists.realms.gui.RealmsGUI.refreshGUIsForCategory("EMPIRES"); } catch (Throwable ignored) {}

            player.sendMessage(MessageUtils.get("empire.claim_success_prefix") + result.getMessage());
        } else {
            player.sendMessage(MessageUtils.get("empire.claim_fail_prefix") + result.getMessage());
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
