package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.MessageUtils;
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
import java.util.Map;
import java.util.stream.Collectors;

public class KingdomCommand implements CommandExecutor, TabCompleter {

    private static final int MIN_TOWN_MEMBERS = 3;
    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;

    public KingdomCommand(Nations plugin) {
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
            case "info":
                return handleInfo(player, args);
            case "invite":
                return handleInvite(player, args);
            case "vassalize":
                return handleVassalize(player, args);
            case "list":
                return handleList(player, args);
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
        if (data.getTown() == null) {
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
            player.sendMessage(MessageUtils.get("kingdom.not_enough_members").replace("{min}", String.valueOf(MIN_TOWN_MEMBERS)));
            return true;
        }

        String kingdomName = args[1];

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
            if (data.getTown() == null) {
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
        if (data.getTown() == null) {
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
        if (data.getTown() == null) {
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
            player.sendMessage(MessageUtils.format("kingdom.kings_list_item", Map.of("name", kingdom.getName(), "capital", kingdom.getCapital(), "count", String.valueOf(kingdom.getTowns().size()))));
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
        player.sendMessage(MessageUtils.get("kingdom.help_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "info", "invite", "vassalize", "list")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("vassalize")) {
                return societiesManager.getAllKingdoms().stream()
                        .map(Kingdom::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("invite")) {
                return societiesManager.getAllTowns().stream()
                        .filter(t -> t.getKingdom() == null)
                        .map(Town::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
