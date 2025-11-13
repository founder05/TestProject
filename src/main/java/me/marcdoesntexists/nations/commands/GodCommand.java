package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.*;
import me.marcdoesntexists.nations.utils.PlayerData;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class GodCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final ReligionService religionService;

    public GodCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.religionService = plugin.getReligionService();
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
            case "worship":
                return handleWorship(player, args);
            case "altar":
                return handleAltar(player, args);
            case "sacrifice":
                return handleSacrifice(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player, args);
            case "followers":
                return handleFollowers(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        // /god create <name> <domain> <description...>
        if (args.length < 4) {
            player.sendMessage(MessageUtils.format("commands.usage", Map.of("usage","/god create <name> <domain> <description...>")));
            player.sendMessage(MessageUtils.get("commands.usage") + " §7Example: /god create Zeus Lightning God of thunder and sky");
            return true;
        }

        // permission check
        if (!player.hasPermission("nations.god.create")) {
            player.sendMessage(MessageUtils.get("general.no_permission"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() == null) {
            player.sendMessage(MessageUtils.get("general.player_only"));
            player.sendMessage(MessageUtils.get("commands.usage") + " §7Use §e/religion create <name>§7 first!");
            return true;
        }

        Religion religion = societiesManager.getReligion(data.getReligion());
        if (religion == null) {
            player.sendMessage(MessageUtils.format("commands.not_found", Map.of("entity","Religion")));
            return true;
        }

        if (!religion.getFounder().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("god.only_founder"));
            return true;
        }

        // Check max gods per religion
        int maxGods = plugin.getConfigurationManager().getReligionConfig()
                .getInt("religion-system.gods.max-gods-per-religion", 5);

        long currentGods = societiesManager.getAllGods().stream()
                .filter(g -> g.getRelatedReligion().equals(religion))
                .count();

        if (currentGods >= maxGods) {
            player.sendMessage(MessageUtils.format("god.max_gods_reached", Map.of("max", String.valueOf(maxGods))));
            return true;
        }

        String godName = args[1];
        String domain = args[2];
        StringBuilder descBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            descBuilder.append(args[i]).append(" ");
        }
        String description = descBuilder.toString().trim();

        // Check if god already exists
        if (societiesManager.getGod(godName) != null) {
            player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "God (already exists)"));
            return true;
        }

        int creationCost = plugin.getConfigurationManager().getReligionConfig()
                .getInt("religion-system.gods.god-creation-cost", 2000);

        if (data.getMoney() < creationCost) {
            player.sendMessage(MessageUtils.format("town.not_enough_money", Map.of("needed", String.valueOf(creationCost), "have", String.valueOf(data.getMoney()))));
            return true;
        }

        if (religionService.createGod(godName, description, domain, player.getUniqueId(), data.getReligion())) {
             data.removeMoney(creationCost);

             // Persist player money immediately
             try { plugin.getDataManager().savePlayerMoney(player.getUniqueId()); } catch (Throwable ignored) {}

            player.sendMessage(MessageUtils.format("god.create_success", Map.of("name", godName)));
            player.sendMessage(MessageUtils.format("god.info_domain", Map.of("domain", domain)));
            player.sendMessage(MessageUtils.format("god.info_description", Map.of("description", description)));
            player.sendMessage(MessageUtils.format("god.info_religion", Map.of("religion", religion.getName())));
            player.sendMessage(MessageUtils.format("god.info_cost", Map.of("cost", String.valueOf(creationCost))));

            // Notify all followers
            for (UUID followerId : religion.getFollowers()) {
                Player follower = plugin.getServer().getPlayer(followerId);
                if (follower != null && !follower.equals(player)) {
                    follower.sendMessage(MessageUtils.format("god.notify_new_god_broadcast", Map.of("prefix", MessageUtils.get("general.prefix"), "religion", religion.getName(), "god", godName)));
                    follower.sendMessage(MessageUtils.format("god.info_domain", Map.of("domain", domain)));
                }
            }
         } else {
            player.sendMessage(MessageUtils.get("errors.generic").replace("{error}", "Failed to create god"));
         }

         return true;
     }

    private boolean handleWorship(Player player, String[] args) {
        // /god worship <name>
        if (args.length < 2) {
            player.sendMessage(MessageUtils.format("commands.usage", Map.of("usage","/god worship <name>")));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() == null) {
            player.sendMessage(MessageUtils.get("god.must_be_in_religion"));
            return true;
        }

        String godName = args[1];
        God god = societiesManager.getGod(godName);

        if (god == null) {
            player.sendMessage(MessageUtils.format("commands.not_found", Map.of("entity","God")));
            return true;
        }

        Religion religion = societiesManager.getReligion(data.getReligion());
        if (!god.getRelatedReligion().equals(religion)) {
            player.sendMessage(MessageUtils.get("god.not_part_of_religion"));
            return true;
        }

        if (god.getFollowers().contains(player.getUniqueId())) {
            player.sendMessage(MessageUtils.format("god.already_worship", Map.of("god", godName)));
            return true;
        }

        god.addFollower(player.getUniqueId());

        int affinityGain = plugin.getConfigurationManager().getReligionConfig()
                .getInt("religion-system.faith.affinity-gain-per-action", 5);
        int currentAffinity = data.getGodAffinity(godName);
        data.setGodAffinity(godName, currentAffinity + affinityGain);

        player.sendMessage(MessageUtils.format("god.worship_success", Map.of("god", godName)));
        player.sendMessage(MessageUtils.format("god.info_domain", Map.of("domain", god.getDomain())));
        player.sendMessage(MessageUtils.format("god.info_affinity", Map.of("affinity", String.valueOf(data.getGodAffinity(godName)))));

         return true;
     }

    private boolean handleAltar(Player player, String[] args) {
        // /god altar <create|remove> <godName>
        if (args.length < 3) {
            player.sendMessage(MessageUtils.format("commands.usage", Map.of("usage","/god altar <create|remove> <godName>")));
            return true;
        }

        String action = args[1].toLowerCase();
        String godName = args[2];

        God god = societiesManager.getGod(godName);
        if (god == null) {
            player.sendMessage(MessageUtils.get("gods.not_found"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        Religion religion = societiesManager.getReligion(data.getReligion());

        if (religion == null || !god.getRelatedReligion().equals(religion)) {
            player.sendMessage(MessageUtils.get("god.not_part_of_religion"));
            return true;
        }

        String location = player.getLocation().getWorld().getName() + "," +
                player.getLocation().getBlockX() + "," +
                player.getLocation().getBlockY() + "," +
                player.getLocation().getBlockZ();

        if (action.equals("create")) {
            int altarCost = plugin.getConfigurationManager().getReligionConfig()
                    .getInt("religion-system.religious-buildings.altar-construction-cost", 1000);

            if (data.getMoney() < altarCost) {
                player.sendMessage(MessageUtils.format("town.not_enough_money", Map.of("needed", String.valueOf(altarCost), "have", String.valueOf(data.getMoney()))));
                return true;
            }

            if (religionService.addAltarToGod(godName, location)) {
                data.removeMoney(altarCost);

                // Persist player money immediately
                try { plugin.getDataManager().savePlayerMoney(player.getUniqueId()); } catch (Throwable ignored) {}

                int powerGain = plugin.getConfigurationManager().getReligionConfig()
                        .getInt("religion-system.gods.power-gain-per-action", 1);
                religionService.addGodPower(godName, powerGain * 10); // Altars give more power

                player.sendMessage(MessageUtils.format("god.altar_built", Map.of("name", godName)));
                player.sendMessage(MessageUtils.format("god.altar_location", Map.of("location", location)));
                player.sendMessage(MessageUtils.format("god.altar_cost", Map.of("cost", String.valueOf(altarCost))));
                player.sendMessage(MessageUtils.format("god.altar_power", Map.of("power", String.valueOf(powerGain * 10))));
             } else {
                player.sendMessage(MessageUtils.get("errors.generic").replace("{error}", "Failed to create altar"));
             }

        } else if (action.equals("remove")) {
            if (god.hasAltar(location)) {
                god.removeAltar(location);
                player.sendMessage(MessageUtils.get("god.altar_removed"));
            } else {
                player.sendMessage(MessageUtils.get("god.no_altar_found"));
            }
        } else {
            player.sendMessage(MessageUtils.format("commands.usage", Map.of("usage","/god altar <create|remove> <godName>")));
        }

        return true;
    }

    private boolean handleSacrifice(Player player, String[] args) {
        // /god sacrifice <godName> <amount>
        if (args.length < 3) {
            player.sendMessage(MessageUtils.format("commands.usage", Map.of("usage","/god sacrifice <godName> <amount>")));
            return true;
        }

        String godName = args[1];
        God god = societiesManager.getGod(godName);

        if (god == null) {
            player.sendMessage(MessageUtils.format("commands.not_found", Map.of("entity","God")));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (!god.getFollowers().contains(player.getUniqueId())) {
            player.sendMessage(MessageUtils.format("god.not_follower", Map.of("god", godName)));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                player.sendMessage(MessageUtils.get("commands.invalid_number"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtils.get("commands.invalid_number"));
            return true;
        }

        if (data.getMoney() < amount) {
            player.sendMessage(MessageUtils.get("town.not_enough_money").replace("{needed}", String.valueOf(amount)).replace("{have}", String.valueOf(data.getMoney())));
            return true;
        }

        data.removeMoney(amount);

        // Persist player money immediately
        try { plugin.getDataManager().savePlayerMoney(player.getUniqueId()); } catch (Throwable ignored) {}

        // Calculate power gain (1 power per 10 coins)
        int powerGain = amount / 10;
        religionService.addGodPower(godName, powerGain);

        // Increase affinity
        int affinityGain = amount / 100;
        int currentAffinity = data.getGodAffinity(godName);
        data.setGodAffinity(godName, currentAffinity + affinityGain);

        player.sendMessage(MessageUtils.format("god.sacrifice_success", Map.of("god", godName)));
        player.sendMessage(MessageUtils.format("god.sacrifice_offering", Map.of("amount", String.valueOf(amount))));
        player.sendMessage(MessageUtils.format("god.sacrifice_power", Map.of("power", String.valueOf(powerGain))));
        player.sendMessage(MessageUtils.format("god.sacrifice_affinity", Map.of("affinity", String.valueOf(affinityGain), "total", String.valueOf(data.getGodAffinity(godName)))));

         return true;
     }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.format("commands.usage", Map.of("usage","/god info <name>")));
            return true;
        }

        String godName = args[1];
        God god = societiesManager.getGod(godName);

        if (god == null) {
            player.sendMessage(MessageUtils.format("commands.not_found", Map.of("entity","God")));
            return true;
        }

        player.sendMessage(MessageUtils.format("god.info_header", Map.of("name", god.getName())));
        player.sendMessage(MessageUtils.format("god.info_domain", Map.of("domain", god.getDomain())));
        player.sendMessage(MessageUtils.format("god.info_description", Map.of("description", god.getDescription())));
        player.sendMessage(MessageUtils.format("god.info_religion", Map.of("religion", god.getRelatedReligion().getName())));
        player.sendMessage(MessageUtils.format("god.info_power", Map.of("power", String.valueOf(god.getPower()))));
        player.sendMessage(MessageUtils.format("god.info_followers", Map.of("count", String.valueOf(god.getFollowerCount()))));
        player.sendMessage(MessageUtils.format("god.info_altars", Map.of("count", String.valueOf(god.getAltars().size()))));

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (god.getFollowers().contains(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("god.you_worship"));
            player.sendMessage(MessageUtils.format("god.info_affinity", Map.of("affinity", String.valueOf(data.getGodAffinity(godName)))));
        }

        player.sendMessage(MessageUtils.get("god.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<God> gods = societiesManager.getAllGods();

        if (gods.isEmpty()) {
            player.sendMessage(MessageUtils.get("god.list_empty"));
            return true;
        }

        player.sendMessage(MessageUtils.format("god.list_header", Map.of("count", String.valueOf(gods.size()))));

        for (God god : gods) {
            player.sendMessage(MessageUtils.format("god.list_item", Map.of("name", god.getName(), "domain", god.getDomain(), "religion", god.getRelatedReligion().getName(), "power", String.valueOf(god.getPower()), "followers", String.valueOf(god.getFollowerCount()))));
        }

        player.sendMessage(MessageUtils.get("god.list_footer"));

        return true;
    }

    private boolean handleFollowers(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.format("commands.usage", Map.of("usage","/god followers <name>")));
            return true;
        }

         String godName = args[1];
         God god = societiesManager.getGod(godName);

         if (god == null) {
            player.sendMessage(MessageUtils.format("commands.not_found", Map.of("entity","God")));
             return true;
         }

        player.sendMessage(MessageUtils.format("god.followers_header", Map.of("name", god.getName())));
        player.sendMessage(MessageUtils.format("god.followers_total", Map.of("count", String.valueOf(god.getFollowerCount()))));
        player.sendMessage(MessageUtils.get("general.empty"));

         for (UUID followerId : god.getFollowers()) {
            String followerName = Optional.ofNullable(plugin.getServer().getOfflinePlayer(followerId).getName()).orElse(followerId.toString());
            player.sendMessage(MessageUtils.format("god.followers_item", Map.of("name", followerName)));
         }

        player.sendMessage(MessageUtils.get("god.followers_footer"));

         return true;
     }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("god.help.header"));
        player.sendMessage(MessageUtils.get("god.help.create"));
        player.sendMessage(MessageUtils.get("god.help.worship"));
        player.sendMessage(MessageUtils.get("god.help.altar_create"));
        player.sendMessage(MessageUtils.get("god.help.altar_remove"));
        player.sendMessage(MessageUtils.get("god.help.sacrifice"));
        player.sendMessage(MessageUtils.get("god.help.info"));
        player.sendMessage(MessageUtils.get("god.help.list"));
        player.sendMessage(MessageUtils.get("god.help.followers"));
        player.sendMessage(MessageUtils.get("god.help.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "worship", "altar", "sacrifice", "info", "list", "followers")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("altar")) {
                return Arrays.asList("create", "remove")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }

            if (args[0].equalsIgnoreCase("worship") || args[0].equalsIgnoreCase("info") ||
                    args[0].equalsIgnoreCase("sacrifice") || args[0].equalsIgnoreCase("followers")) {
                return societiesManager.getAllGods().stream()
                        .map(God::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("altar")) {
            return societiesManager.getAllGods().stream()
                    .map(God::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return new ArrayList<>();
    }
}
