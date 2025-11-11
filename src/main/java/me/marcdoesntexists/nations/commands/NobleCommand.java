package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
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
import java.util.stream.Collectors;

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
                player.sendMessage("§cPlayer not found!");
                return true;
            }
        }

        PlayerData data = dataManager.getPlayerData(target.getUniqueId());
        NobleTier tier = data.getNobleTier();

        player.sendMessage("§7§m----------§r §6Noble Status§7 §m----------");
        player.sendMessage("§ePlayer: §6" + target.getName());
        player.sendMessage("§eTier: §6" + tier.name());
        player.sendMessage("§eLevel: §6" + tier.getLevel());
        player.sendMessage("§eTax Benefit: §a" + (tier.getTaxBenefitPercentage() * 100) + "%");
        player.sendMessage("§eRequired Land Value: §6$" + tier.getRequiredLandValue());
        player.sendMessage("§eSocial Class: §6" + data.getSocialClass());

        if (tier != NobleTier.COMMONER && tier != NobleTier.KING) {
            NobleTier nextTier = NobleTier.getByLevel(tier.getLevel() + 1);
            player.sendMessage("");
            player.sendMessage("§7Next Tier: §e" + nextTier.name());
            player.sendMessage("§7Required: §6$" + nextTier.getRequiredLandValue());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        // /noble promote <player> <tier>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /noble promote <player> <tier>");
            player.sendMessage("§7Tiers: KNIGHT, BARON, COUNT, DUKE, PRINCE, KING");
            return true;
        }

        // permission check
        if (!player.hasPermission("nations.noble.promote")) {
            player.sendMessage("§cYou do not have permission to promote players.");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to promote players!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can promote players!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.getTown().equals(data.getTown())) {
            player.sendMessage("§cThat player is not in your town!");
            return true;
        }

        NobleTier newTier;
        try {
            newTier = NobleTier.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid tier!");
            player.sendMessage("§7Valid tiers: KNIGHT, BARON, COUNT, DUKE, PRINCE, KING");
            return true;
        }

        if (newTier == NobleTier.COMMONER) {
            player.sendMessage("§cUse /noble demote to demote to commoner!");
            return true;
        }

        // Check if promoter has authority
        NobleTier promoterTier = data.getNobleTier();
        if (promoterTier.getLevel() <= newTier.getLevel()) {
            player.sendMessage("§cYou cannot promote someone to your level or higher!");
            return true;
        }

        targetData.setNobleTier(newTier);
        targetData.setSocialClass(newTier.name());

        player.sendMessage("§a✔ §6" + target.getName() + "§a promoted to §6" + newTier.name() + "§a!");
        target.sendMessage("§a✔ You have been promoted to §6" + newTier.name() + "§a!");
        target.sendMessage("§7Benefits:");
        target.sendMessage("§7 • Tax Reduction: §a" + (newTier.getTaxBenefitPercentage() * 100) + "%");
        target.sendMessage("§7 • New Social Status: §6" + newTier.name());

        return true;
    }

    private boolean handleDemote(Player player, String[] args) {
        // /noble demote <player>
        if (args.length < 2) {
            player.sendMessage("§cUsage: /noble demote <player>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can demote players!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        PlayerData targetData = dataManager.getPlayerData(target.getUniqueId());
        if (!targetData.getTown().equals(data.getTown())) {
            player.sendMessage("§cThat player is not in your town!");
            return true;
        }

        NobleTier currentTier = targetData.getNobleTier();
        if (currentTier == NobleTier.COMMONER) {
            player.sendMessage("§cThat player is already a commoner!");
            return true;
        }

        targetData.setNobleTier(NobleTier.COMMONER);
        targetData.setSocialClass("Commoner");

        player.sendMessage("§a✔ §6" + target.getName() + "§a demoted to §7Commoner");
        target.sendMessage("§c✘ You have been demoted to §7Commoner");
        target.sendMessage("§7You have lost your noble privileges!");

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
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

        player.sendMessage("§7§m----------§r §6Nobility of " + town.getName() + "§7 §m----------");

        for (NobleTier tier : NobleTier.values()) {
            if (tier == NobleTier.COMMONER) continue; // Skip commoners

            List<UUID> members = nobleTiers.get(tier);
            if (!members.isEmpty()) {
                player.sendMessage("");
                player.sendMessage("§e" + tier.name() + "§7 (" + members.size() + "):");
                for (UUID memberId : members) {
                    String memberName = plugin.getServer().getOfflinePlayer(memberId).getName();
                    player.sendMessage("§7 • §6" + memberName);
                }
            }
        }

        int commonerCount = nobleTiers.get(NobleTier.COMMONER).size();
        player.sendMessage("");
        player.sendMessage("§7Commoners: §e" + commonerCount);
        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleRequirements(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /noble requirements <tier>");
            return true;
        }

        NobleTier tier;
        try {
            tier = NobleTier.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid tier!");
            player.sendMessage("§7Valid tiers: KNIGHT, BARON, COUNT, DUKE, PRINCE, KING");
            return true;
        }

        player.sendMessage("§7§m----------§r §6" + tier.name() + " Requirements§7 §m----------");
        player.sendMessage("§eLevel: §6" + tier.getLevel());
        player.sendMessage("§eRequired Land Value: §6$" + tier.getRequiredLandValue());
        player.sendMessage("§eTax Benefit: §a" + (tier.getTaxBenefitPercentage() * 100) + "%");

        // Show requirements from social.yml
        String tierLower = tier.name().toLowerCase();
        int requiredWealth = plugin.getConfigurationManager().getSocialConfig()
                .getInt("social-classes.nobles." + tierLower + ".required-wealth", 0);
        int requiredExp = plugin.getConfigurationManager().getSocialConfig()
                .getInt("social-classes.nobles." + tierLower + ".required-experience", 0);

        if (requiredWealth > 0) {
            player.sendMessage("§eRequired Wealth: §6$" + requiredWealth);
        }
        if (requiredExp > 0) {
            player.sendMessage("§eRequired Experience: §6" + requiredExp);
        }

        player.sendMessage("");
        player.sendMessage("§7Privileges:");
        player.sendMessage("§7 • Reduced taxes");
        player.sendMessage("§7 • Increased land claims");
        player.sendMessage("§7 • Government positions access");

        if (tier.getLevel() >= NobleTier.COUNT.getLevel()) {
            player.sendMessage("§7 • Military command");
        }
        if (tier.getLevel() >= NobleTier.DUKE.getLevel()) {
            player.sendMessage("§7 • Judicial authority");
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleUpgrade(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        NobleTier currentTier = data.getNobleTier();

        if (currentTier == NobleTier.KING) {
            player.sendMessage("§cYou are already at the highest noble tier!");
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

        player.sendMessage("§7§m----------§r §6Upgrade to " + nextTier.name() + "§7 §m----------");
        player.sendMessage((hasWealth ? "§a✔" : "§c✘") + " §7Wealth: §6$" + data.getMoney() + "§7/§6$" + requiredWealth);
        player.sendMessage((hasExp ? "§a✔" : "§c✘") + " §7Experience: §6" + data.getNobleTierExperience() + "§7/§6" + requiredExp);

        if (hasWealth && hasExp) {
            data.removeMoney(requiredWealth);
            data.setNobleTier(nextTier);
            data.setSocialClass(nextTier.name());
            data.setNobleTierExperience(0);

            player.sendMessage("");
            player.sendMessage("§a§l✔ PROMOTED!");
            player.sendMessage("§7You are now a §6" + nextTier.name() + "§7!");
            player.sendMessage("§7New Benefits:");
            player.sendMessage("§7 • Tax Reduction: §a" + (nextTier.getTaxBenefitPercentage() * 100) + "%");
            player.sendMessage("§7 • Increased Social Standing");

            // Broadcast to town
            if (data.getTown() != null) {
                Town town = societiesManager.getTown(data.getTown());
                if (town != null) {
                    for (UUID memberId : town.getMembers()) {
                        Player member = plugin.getServer().getPlayer(memberId);
                        if (member != null && !member.equals(player)) {
                            member.sendMessage("§6" + player.getName() + " §7has been promoted to §6" + nextTier.name() + "§7!");
                        }
                    }
                }
            }
        } else {
            player.sendMessage("");
            player.sendMessage("§c✘ Requirements not met!");
            player.sendMessage("§7Complete the requirements above to upgrade");
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Noble Commands§7 §m----------");
        player.sendMessage("§e/noble info [player]§7 - View noble status");
        player.sendMessage("§e/noble promote <player> <tier>§7 - Promote player");
        player.sendMessage("§e/noble demote <player>§7 - Demote player");
        player.sendMessage("§e/noble list§7 - List town nobility");
        player.sendMessage("§e/noble requirements <tier>§7 - View requirements");
        player.sendMessage("§e/noble upgrade§7 - Upgrade your tier");
        player.sendMessage("§7§m--------------------------------");
        player.sendMessage("§7Tiers: KNIGHT → BARON → COUNT → DUKE → PRINCE → KING");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "promote", "demote", "list", "requirements", "upgrade")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // suggest player names for promote/demote
        if (args.length == 2 && (args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote"))) {
            String partial = args[1].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("promote")) {
            String partial = args[2].toUpperCase();
            return Arrays.asList("KNIGHT", "BARON", "COUNT", "DUKE", "PRINCE", "KING")
                    .stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("requirements")) {
            String partial = args[1].toUpperCase();
            return Arrays.asList("KNIGHT", "BARON", "COUNT", "DUKE", "PRINCE", "KING")
                    .stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }

        return new ArrayList<>();
    }
}