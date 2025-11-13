package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.NobleTier;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NobleCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;

    public NobleCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("noble.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                return handleInfo(player, args);
            case "promote":
                return handlePromote(player, args);
            case "demote":
                return handleDemote(player, args);
            case "list":
                return handleList(player, args);
            case "requirements":
                return handleRequirements(player, args);
            case "upgrade":
                return handleUpgrade(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleInfo(Player player, String[] args) {
        Player target = player;

        if (args.length > 1) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
                return true;
            }
        }

        PlayerData data = dataManager.getPlayerData(target.getUniqueId());
        NobleTier tier = data.getNobleTier();

        player.sendMessage(MessageUtils.get("noble.info_header"));
        player.sendMessage(MessageUtils.format("noble.info_player", Map.of("player", target.getName())));
        player.sendMessage(MessageUtils.format("noble.info_tier", Map.of("tier", tier.name())));
        player.sendMessage(MessageUtils.format("noble.info_level", Map.of("level", String.valueOf(tier.getLevel()))));
        player.sendMessage(MessageUtils.format("noble.info_tax", Map.of("tax", String.valueOf((int) (tier.getTaxBenefitPercentage() * 100)))));

        player.sendMessage(MessageUtils.format("noble.info_required_land", Map.of("value", String.valueOf(tier.getRequiredLandValue()))));
        player.sendMessage(MessageUtils.format("noble.info_social_class", Map.of("class", data.getSocialClass())));

        if (tier != NobleTier.COMMONER && tier != NobleTier.KING) {
            NobleTier nextTier = NobleTier.getByLevel(tier.getLevel() + 1);
            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.format("noble.info_next_tier", Map.of("next", nextTier.name())));
            player.sendMessage(MessageUtils.format("noble.info_next_required", Map.of("required", String.valueOf(nextTier.getRequiredLandValue()))));
        }

        player.sendMessage(MessageUtils.get("noble.info_footer"));

        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        // /noble promote <player> <tier>
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("noble.usage_promote"));
            player.sendMessage(MessageUtils.get("noble.usage_promote_hint"));
            return true;
        }

        // permission check
        if (!player.hasPermission("nations.noble.promote")) {
            player.sendMessage(MessageUtils.get("general.no_permission"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("noble.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("noble.only_mayor"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.getTown().equals(data.getTown())) {
            player.sendMessage(MessageUtils.get("noble.not_in_town"));
            return true;
        }

        NobleTier newTier;
        try {
            newTier = NobleTier.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("noble.invalid_tier"));
            player.sendMessage(MessageUtils.get("noble.valid_tiers"));
            return true;
        }

        if (newTier == NobleTier.COMMONER) {
            player.sendMessage(MessageUtils.get("noble.use_demote"));
            return true;
        }

        // Check if promoter has authority
        NobleTier promoterTier = data.getNobleTier();
        if (promoterTier.getLevel() <= newTier.getLevel()) {
            player.sendMessage(MessageUtils.get("noble.cannot_promote_higher"));
            return true;
        }

        targetData.setNobleTier(newTier);
        targetData.setSocialClass(newTier.name());

        player.sendMessage(MessageUtils.format("noble.promote_success", Map.of("player", target.getName(), "tier", newTier.name())));
        target.sendMessage(MessageUtils.format("noble.promoted_notify", Map.of("tier", newTier.name())));
        target.sendMessage(MessageUtils.format("noble.promoted_benefit_tax", Map.of("tax", String.valueOf((int) (newTier.getTaxBenefitPercentage() * 100)))));
        target.sendMessage(MessageUtils.format("noble.promoted_benefit_status", Map.of("status", newTier.name())));

        return true;
    }

    private boolean handleDemote(Player player, String[] args) {
        // /noble demote <player>
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("noble.usage_demote"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("noble.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("noble.only_mayor"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Player"));
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.getTown().equals(data.getTown())) {
            player.sendMessage(MessageUtils.get("noble.not_in_town"));
            return true;
        }

        NobleTier currentTier = targetData.getNobleTier();
        if (currentTier == NobleTier.COMMONER) {
            player.sendMessage(MessageUtils.get("noble.already_commoner"));
            return true;
        }

        targetData.setNobleTier(NobleTier.COMMONER);
        targetData.setSocialClass("Commoner");

        player.sendMessage(MessageUtils.format("noble.demote_success", Map.of("player", target.getName())));
        target.sendMessage(MessageUtils.get("noble.demoted_notify"));
        target.sendMessage(MessageUtils.get("noble.demoted_lost_privileges"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("noble.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());

        Map<NobleTier, List<UUID>> nobleTiers = new HashMap<>();
        for (NobleTier tier : NobleTier.values()) {
            nobleTiers.put(tier, new ArrayList<>());
        }

        for (UUID memberId : town.getMembers()) {
            PlayerData memberData = dataManager.getPlayerData(memberId);
            NobleTier tier = memberData.getNobleTier();
            nobleTiers.get(tier).add(memberId);
        }

        player.sendMessage(MessageUtils.format("noble.list_header", Map.of("town", town.getName())));

        for (NobleTier tier : NobleTier.values()) {
            if (tier == NobleTier.COMMONER) continue; // Skip commoners

            List<UUID> members = nobleTiers.get(tier);
            if (!members.isEmpty()) {
                player.sendMessage(MessageUtils.get("general.empty"));
                player.sendMessage(MessageUtils.format("noble.list_tier_header", Map.of("tier", tier.name(), "count", String.valueOf(members.size()))));
                for (UUID memberId : members) {
                    String memberName = plugin.getServer().getOfflinePlayer(memberId).getName();
                    player.sendMessage(MessageUtils.format("noble.list_member_item", Map.of("name", memberName)));
                }
            }
        }

        int commonerCount = nobleTiers.get(NobleTier.COMMONER).size();
        player.sendMessage(MessageUtils.get("general.empty"));
        player.sendMessage(MessageUtils.format("noble.list_commoners", Map.of("count", String.valueOf(commonerCount))));
        player.sendMessage(MessageUtils.get("noble.list_footer"));

        return true;
    }

    private boolean handleRequirements(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("noble.usage_requirements"));
            return true;
        }

        NobleTier tier;
        try {
            tier = NobleTier.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("noble.invalid_tier"));
            player.sendMessage(MessageUtils.get("noble.valid_tiers"));
            return true;
        }

        player.sendMessage(MessageUtils.format("noble.requirements_header", Map.of("tier", tier.name())));
        player.sendMessage(MessageUtils.format("noble.requirements_level", Map.of("level", String.valueOf(tier.getLevel()))));
        player.sendMessage(MessageUtils.format("noble.requirements_land", Map.of("value", String.valueOf(tier.getRequiredLandValue()))));
        player.sendMessage(MessageUtils.format("noble.requirements_tax", Map.of("tax", String.valueOf((tier.getTaxBenefitPercentage() * 100)))));

        // Show requirements from social.yml
        String tierLower = tier.name().toLowerCase();
        int requiredWealth = plugin.getConfigurationManager().getSocialConfig()
                .getInt("social-classes.nobles." + tierLower + ".required-wealth", 0);
        int requiredExp = plugin.getConfigurationManager().getSocialConfig()
                .getInt("social-classes.nobles." + tierLower + ".required-experience", 0);

        if (requiredWealth > 0) {
            player.sendMessage(MessageUtils.format("noble.requirements_wealth", Map.of("amount", String.valueOf(requiredWealth))));
        }
        if (requiredExp > 0) {
            player.sendMessage(MessageUtils.format("noble.requirements_exp", Map.of("amount", String.valueOf(requiredExp))));
        }

        player.sendMessage(MessageUtils.get("general.empty"));
        player.sendMessage(MessageUtils.get("noble.privileges_header"));
        player.sendMessage(MessageUtils.format("noble.privilege_item", Map.of("text", "Reduced taxes")));
        player.sendMessage(MessageUtils.format("noble.privilege_item", Map.of("text", "Increased land claims")));
        player.sendMessage(MessageUtils.format("noble.privilege_item", Map.of("text", "Government positions access")));

        if (tier.getLevel() >= NobleTier.COUNT.getLevel()) {
            player.sendMessage(MessageUtils.format("noble.privilege_item", Map.of("text", "Military command")));
        }
        if (tier.getLevel() >= NobleTier.DUKE.getLevel()) {
            player.sendMessage(MessageUtils.format("noble.privilege_item", Map.of("text", "Judicial authority")));
        }

        player.sendMessage(MessageUtils.get("noble.info_footer"));

        return true;
    }

    private boolean handleUpgrade(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        NobleTier currentTier = data.getNobleTier();

        if (currentTier == NobleTier.KING) {
            player.sendMessage(MessageUtils.get("noble.already_highest"));
            return true;
        }

        NobleTier nextTier = NobleTier.getByLevel(currentTier.getLevel() + 1);

        // Check requirements
        String tierLower = nextTier.name().toLowerCase();
        int requiredWealth = plugin.getConfigurationManager().getSocialConfig()
                .getInt("social-classes.nobles." + tierLower + ".required-wealth", 0);
        int requiredExp = plugin.getConfigurationManager().getSocialConfig()
                .getInt("social-classes.nobles." + tierLower + ".required-experience", 0);

        boolean hasWealth = data.getMoney() >= requiredWealth;
        boolean hasExp = data.getNobleTierExperience() >= requiredExp;

        player.sendMessage(MessageUtils.format("noble.upgrade_header", Map.of("next", nextTier.name())));
        player.sendMessage(MessageUtils.format("noble.upgrade_weath", Map.of("have", String.valueOf(data.getMoney()), "need", String.valueOf(requiredWealth), "ok", (hasWealth ? "true" : "false"))));
        player.sendMessage(MessageUtils.format("noble.upgrade_exp", Map.of("have", String.valueOf(data.getNobleTierExperience()), "need", String.valueOf(requiredExp), "ok", (hasExp ? "true" : "false"))));

        if (hasWealth && hasExp) {
            data.removeMoney(requiredWealth);
            // persist player money immediately
            try {
                plugin.getDataManager().savePlayerMoney(player.getUniqueId());
            } catch (Throwable ignored) {
            }

            data.setNobleTier(nextTier);
            data.setSocialClass(nextTier.name());
            data.setNobleTierExperience(0);

            // persist full player data (tier changed)
            try {
                plugin.getDataManager().savePlayerData(player.getUniqueId());
            } catch (Throwable ignored) {
            }

            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.get("noble.upgrade_promoted_title"));
            player.sendMessage(MessageUtils.format("noble.upgrade_now", Map.of("tier", nextTier.name())));
            player.sendMessage(MessageUtils.get("noble.upgrade_benefit_status"));
            player.sendMessage(MessageUtils.format("noble.upgrade_benefit_tax", Map.of("tax", String.valueOf((nextTier.getTaxBenefitPercentage() * 100)))));

            // Broadcast to town
            if (data.getTown() != null) {
                Town town = societiesManager.getTown(data.getTown());
                if (town != null) {
                    for (UUID memberId : town.getMembers()) {
                        Player member = plugin.getServer().getPlayer(memberId);
                        if (member != null && !member.equals(player)) {
                            member.sendMessage(MessageUtils.format("noble.promoted_broadcast", Map.of("player", player.getName(), "tier", nextTier.name())));
                        }
                    }
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.get("noble.requirements_not_met"));
            player.sendMessage(MessageUtils.get("noble.requirements_help"));
        }

        player.sendMessage(MessageUtils.get("noble.info_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("noble.help.header"));
        player.sendMessage(MessageUtils.get("noble.help.info"));
        player.sendMessage(MessageUtils.get("noble.help.promote"));
        player.sendMessage(MessageUtils.get("noble.help.demote"));
        player.sendMessage(MessageUtils.get("noble.help.list"));
        player.sendMessage(MessageUtils.get("noble.help.requirements"));
        player.sendMessage(MessageUtils.get("noble.help.upgrade"));
        player.sendMessage(MessageUtils.get("noble.help.footer"));
        player.sendMessage(MessageUtils.get("noble.help.tiers"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(Arrays.asList("info", "promote", "demote", "list", "requirements", "upgrade"), args[0]);
        }

        // suggest player names for promote/demote
        if (args.length == 2 && (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote"))) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.onlinePlayers(args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("promote")) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(Arrays.asList("KNIGHT", "BARON", "COUNT", "DUKE", "PRINCE", "KING"), args[2]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("requirements")) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(Arrays.asList("KNIGHT", "BARON", "COUNT", "DUKE", "PRINCE", "KING"), args[1]);
        }

        return new ArrayList<>();
    }
}