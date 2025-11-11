package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.DiplomacyService;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.societies.Treaty;
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

public class TreatyCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final DiplomacyService diplomacyService;

    public TreatyCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.diplomacyService = plugin.getDiplomacyService();
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
            case "accept":
                return handleAccept(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player, args);
            case "break":
                return handleBreak(player, args);
            case "renew":
                return handleRenew(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        // /treaty create <kingdom> <type> <days>
        if (args.length < 4) {
            player.sendMessage("§cUsage: /treaty create <kingdom> <type> <days>");
            player.sendMessage("§7Types: PEACE, TRADE, NON_AGGRESSION, MUTUAL_DEFENSE, NEUTRALITY");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to create treaties!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage("§cOnly the capital's mayor can create treaties!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor!");
            return true;
        }

        String targetKingdomName = args[1];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);

        if (targetKingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return true;
        }

        if (targetKingdom.getName().equals(kingdom.getName())) {
            player.sendMessage("§cYou cannot create a treaty with your own kingdom!");
            return true;
        }

        Treaty.TreatyType type;
        try {
            type = Treaty.TreatyType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid treaty type!");
            player.sendMessage("§7Valid types: PEACE, TRADE, NON_AGGRESSION, MUTUAL_DEFENSE, NEUTRALITY");
            return true;
        }

        long days;
        try {
            days = Long.parseLong(args[3]);
            if (days < 1 || days > 365) {
                player.sendMessage("§cDuration must be between 1 and 365 days!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number of days!");
            return true;
        }

        String treatyName = kingdom.getName() + "-" + targetKingdom.getName() + "-" + type.name();

        if (diplomacyService.createTreaty(treatyName, kingdom.getName(), targetKingdom.getName(), type, days)) {
            player.sendMessage("§a✔ Treaty §6" + treatyName + "§a created!");
            player.sendMessage("§7Type: §e" + type.name());
            player.sendMessage("§7Duration: §e" + days + " days");
            player.sendMessage("§7Between: §6" + kingdom.getName() + " §7and §6" + targetKingdom.getName());

            // Notify the other kingdom
            Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
            if (targetCapital != null) {
                Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
                if (targetKing != null) {
                    targetKing.sendMessage("§7[§6Treaty§7] §eA treaty has been proposed by §6" + kingdom.getName());
                    targetKing.sendMessage("§7Type: §e" + type.name() + " §7Duration: §e" + days + " days");
                    targetKing.sendMessage("§7Use §e/treaty accept " + treatyName + "§7 to accept!");
                }
            }
        } else {
            player.sendMessage("§cFailed to create treaty!");
        }

        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /treaty accept <treatyName>");
            return true;
        }

        String treatyName = args[1];
        Treaty treaty = null;

        for (Treaty t : societiesManager.getAllTreaties()) {
            if (t.getName().equalsIgnoreCase(treatyName)) {
                treaty = t;
                break;
            }
        }

        if (treaty == null) {
            player.sendMessage("§cTreaty not found!");
            return true;
        }

        if (treaty.getStatus() != Treaty.TreatyStatus.PENDING) {
            player.sendMessage("§cThis treaty is not pending acceptance!");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!treaty.getKingdom1().equals(kingdom.getName()) && !treaty.getKingdom2().equals(kingdom.getName())) {
            player.sendMessage("§cThis treaty doesn't involve your kingdom!");
            return true;
        }

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the capital's mayor can accept treaties!");
            return true;
        }

        treaty.setStatus(Treaty.TreatyStatus.ACTIVE);

        player.sendMessage("§a✔ Treaty §6" + treaty.getName() + "§a accepted!");
        player.sendMessage("§7The treaty is now active for §e" + treaty.getDaysRemaining() + "§7 days");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /treaty info <treatyName>");
            return true;
        }

        String treatyName = args[1];
        Treaty treaty = null;

        for (Treaty t : societiesManager.getAllTreaties()) {
            if (t.getName().equalsIgnoreCase(treatyName)) {
                treaty = t;
                break;
            }
        }

        if (treaty == null) {
            player.sendMessage("§cTreaty not found!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Treaty Info§7 §m----------");
        player.sendMessage("§eName: §6" + treaty.getName());
        player.sendMessage("§eType: §6" + treaty.getType().name());
        player.sendMessage("§eKingdoms: §6" + treaty.getKingdom1() + " §7& §6" + treaty.getKingdom2());
        player.sendMessage("§eStatus: §6" + treaty.getStatus().name());
        player.sendMessage("§eDays Remaining: §6" + treaty.getDaysRemaining());

        if (treaty.isActive()) {
            player.sendMessage("§a✔ Treaty is currently active");
        } else if (treaty.isExpired()) {
            player.sendMessage("§c✘ Treaty has expired");
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Treaty> treaties = societiesManager.getAllTreaties();

        if (treaties.isEmpty()) {
            player.sendMessage("§cNo treaties exist!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Treaties §7(" + treaties.size() + ")§m----------");

        for (Treaty treaty : treaties) {
            String status = treaty.isActive() ? "§aActive" :
                    treaty.isExpired() ? "§cExpired" :
                            treaty.getStatus() == Treaty.TreatyStatus.PENDING ? "§ePending" : "§7" + treaty.getStatus();

            player.sendMessage("§e• §6" + treaty.getName());
            player.sendMessage("§7  Type: §e" + treaty.getType().name() + " §7| Status: " + status);
            player.sendMessage("§7  Between: §6" + treaty.getKingdom1() + " §7& §6" + treaty.getKingdom2());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleBreak(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /treaty break <treatyName>");
            return true;
        }

        String treatyName = args[1];
        Treaty treaty = null;

        for (Treaty t : societiesManager.getAllTreaties()) {
            if (t.getName().equalsIgnoreCase(treatyName)) {
                treaty = t;
                break;
            }
        }

        if (treaty == null) {
            player.sendMessage("§cTreaty not found!");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!treaty.getKingdom1().equals(kingdom.getName()) && !treaty.getKingdom2().equals(kingdom.getName())) {
            player.sendMessage("§cThis treaty doesn't involve your kingdom!");
            return true;
        }

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the capital's mayor can break treaties!");
            return true;
        }

        if (diplomacyService.violateTreaty(treaty.getName(), kingdom.getName())) {
            player.sendMessage("§c✘ Treaty §6" + treaty.getName() + "§c broken!");
            player.sendMessage("§7Your kingdom will suffer diplomatic penalties!");

            // Notify the other kingdom
            String otherKingdomName = treaty.getOtherKingdom(kingdom.getName());
            Kingdom otherKingdom = societiesManager.getKingdom(otherKingdomName);
            if (otherKingdom != null) {
                Town otherCapital = societiesManager.getTown(otherKingdom.getCapital());
                if (otherCapital != null) {
                    Player otherKing = plugin.getServer().getPlayer(otherCapital.getMayor());
                    if (otherKing != null) {
                        otherKing.sendMessage("§c✘ §6" + kingdom.getName() + "§c has broken the treaty §6" + treaty.getName() + "§c!");
                    }
                }
            }
        } else {
            player.sendMessage("§cFailed to break treaty!");
        }

        return true;
    }

    private boolean handleRenew(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /treaty renew <treatyName> <days>");
            return true;
        }

        String treatyName = args[1];
        Treaty treaty = null;

        for (Treaty t : societiesManager.getAllTreaties()) {
            if (t.getName().equalsIgnoreCase(treatyName)) {
                treaty = t;
                break;
            }
        }

        if (treaty == null) {
            player.sendMessage("§cTreaty not found!");
            return true;
        }

        long days;
        try {
            days = Long.parseLong(args[2]);
            if (days < 1 || days > 365) {
                player.sendMessage("§cDuration must be between 1 and 365 days!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number of days!");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom!");
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the capital's mayor can renew treaties!");
            return true;
        }

        long newExpiry = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000);
        treaty.setExpiresAt(newExpiry);
        treaty.setStatus(Treaty.TreatyStatus.ACTIVE);

        player.sendMessage("§a✔ Treaty §6" + treaty.getName() + "§a renewed!");
        player.sendMessage("§7New expiration: §e" + days + "§7 days from now");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Treaty Commands§7 §m----------");
        player.sendMessage("§e/treaty create <kingdom> <type> <days>§7 - Create treaty");
        player.sendMessage("§e/treaty accept <name>§7 - Accept treaty");
        player.sendMessage("§e/treaty info <name>§7 - View treaty info");
        player.sendMessage("§e/treaty list§7 - List all treaties");
        player.sendMessage("§e/treaty break <name>§7 - Break a treaty");
        player.sendMessage("§e/treaty renew <name> <days>§7 - Renew treaty");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "accept", "info", "list", "break", "renew")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                return societiesManager.getAllKingdoms().stream()
                        .map(Kingdom::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("info") ||
                    args[0].equalsIgnoreCase("break") || args[0].equalsIgnoreCase("renew")) {
                return societiesManager.getAllTreaties().stream()
                        .map(Treaty::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.asList("PEACE", "TRADE", "NON_AGGRESSION", "MUTUAL_DEFENSE", "NEUTRALITY")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}