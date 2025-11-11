package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
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
            player.sendMessage("§cUsage: /kingdom create <name>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to create a kingdom!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can evolve a town into a kingdom!");
            return true;
        }

        if (town.getKingdom() != null) {
            player.sendMessage("§cYour town is already part of a kingdom!");
            return true;
        }

        if (town.getMembers().size() < MIN_TOWN_MEMBERS) {
            player.sendMessage("§cYour town needs at least " + MIN_TOWN_MEMBERS + " members to form a kingdom!");
            return true;
        }

        String kingdomName = args[1];

        if (societiesManager.getKingdom(kingdomName) != null) {
            player.sendMessage("§cA kingdom with this name already exists!");
            return true;
        }

        Kingdom newKingdom = new Kingdom(kingdomName, town.getName());
        societiesManager.registerKingdom(newKingdom);
        town.setKingdom(kingdomName);

        player.sendMessage("§a✔ Kingdom §6" + kingdomName + "§a created successfully!");
        player.sendMessage("§7Your town §e" + town.getName() + "§7 is now the capital!");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String kingdomName;

        if (args.length < 2) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getTown() == null) {
                player.sendMessage("§cYou are not in a town! Usage: /kingdom info <name>");
                return true;
            }

            Town town = societiesManager.getTown(data.getTown());
            if (town.getKingdom() == null) {
                player.sendMessage("§cYour town is not part of a kingdom! Usage: /kingdom info <name>");
                return true;
            }

            kingdomName = town.getKingdom();
        } else {
            kingdomName = args[1];
        }

        Kingdom kingdom = societiesManager.getKingdom(kingdomName);
        if (kingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6" + kingdom.getName() + "§7 §m----------");
        player.sendMessage("§eCapital: §6" + kingdom.getCapital());
        player.sendMessage("§eTowns: §6" + kingdom.getTowns().size());
        player.sendMessage("§eAllies: §6" + kingdom.getAllies().size());
        player.sendMessage("§eEnemies: §6" + kingdom.getEnemies().size());
        player.sendMessage("§eActive Wars: §6" + kingdom.getWars().size());
        player.sendMessage("§eVassals: §6" + kingdom.getVassals().size());

        if (kingdom.getSuzerain() != null) {
            player.sendMessage("§eSuzerain: §6" + kingdom.getSuzerain());
        }

        if (kingdom.getEmpire() != null) {
            player.sendMessage("§eEmpire: §6" + kingdom.getEmpire());
        }

        player.sendMessage("§eLevel: §6" + kingdom.getProgressionLevel());
        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /kingdom invite <town>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town senderTown = societiesManager.getTown(data.getTown());
        if (senderTown.getKingdom() == null) {
            player.sendMessage("§cYour town is not part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(senderTown.getKingdom());
        if (!kingdom.isKing(senderTown.getName())) {
            player.sendMessage("§cOnly the capital's mayor can invite towns!");
            return true;
        }

        if (!senderTown.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor to invite towns!");
            return true;
        }

        String targetTownName = args[1];
        Town targetTown = societiesManager.getTown(targetTownName);

        if (targetTown == null) {
            player.sendMessage("§cTown not found!");
            return true;
        }

        if (targetTown.getKingdom() != null) {
            player.sendMessage("§cThat town is already part of a kingdom!");
            return true;
        }

        targetTown.setKingdom(kingdom.getName());
        kingdom.addTown(targetTownName);

        player.sendMessage("§a✔ §6" + targetTownName + "§a has joined your kingdom!");

        Player targetMayor = plugin.getServer().getPlayer(targetTown.getMayor());
        if (targetMayor != null) {
            targetMayor.sendMessage("§7[§6" + kingdom.getName() + "§7] §aYour town has been invited and joined §6" + kingdom.getName() + "§a!");
        }

        return true;
    }

    private boolean handleVassalize(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /kingdom vassalize <kingdom>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou are not in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town is not part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage("§cOnly the capital's mayor can vassalize kingdoms!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor to vassalize kingdoms!");
            return true;
        }

        String targetKingdomName = args[1];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);

        if (targetKingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return true;
        }

        if (targetKingdom.getName().equals(kingdom.getName())) {
            player.sendMessage("§cYou cannot vassalize your own kingdom!");
            return true;
        }

        if (targetKingdom.getSuzerain() != null) {
            player.sendMessage("§cThat kingdom is already a vassal of another kingdom!");
            return true;
        }

        kingdom.addVassal(targetKingdomName);
        targetKingdom.setSuzerain(kingdom.getName());

        player.sendMessage("§a✔ §6" + targetKingdomName + "§a is now your vassal!");

        Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
        if (targetCapital != null) {
            Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
            if (targetKing != null) {
                targetKing.sendMessage("§7[§6" + kingdom.getName() + "§7] §eYour kingdom is now a vassal of §6" + kingdom.getName() + "§e!");
            }
        }

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Kingdom> kingdoms = societiesManager.getAllKingdoms();

        if (kingdoms.isEmpty()) {
            player.sendMessage("§cNo kingdoms exist yet!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Kingdoms §7(" + kingdoms.size() + ")§m----------");

        for (Kingdom kingdom : kingdoms) {
            player.sendMessage("§e• §6" + kingdom.getName() + " §7- Capital: §e" + kingdom.getCapital() + " §7- Towns: §e" + kingdom.getTowns().size());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Kingdom Commands§7 §m----------");
        player.sendMessage("§e/kingdom create <name>§7 - Create a kingdom");
        player.sendMessage("§e/kingdom info [name]§7 - View kingdom info");
        player.sendMessage("§e/kingdom invite <town>§7 - Invite a town");
        player.sendMessage("§e/kingdom vassalize <kingdom>§7 - Vassalize a kingdom");
        player.sendMessage("§e/kingdom list§7 - List all kingdoms");
        player.sendMessage("§7§m--------------------------------");
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
