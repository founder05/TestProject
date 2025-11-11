package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.CrimeType;
import me.marcdoesntexists.nations.law.Crime;
import me.marcdoesntexists.nations.law.JusticeService;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.LawManager;
import me.marcdoesntexists.nations.utils.CustomMessages;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LawCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final LawManager lawManager;
    private final JusticeService justiceService;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public LawCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.lawManager = plugin.getLawManager();
        this.justiceService = JusticeService.getInstance();
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
            case "report":
                return handleReport(player, args);
            case "list":
                return handleList(player, args);
            case "info":
                return handleInfo(player, args);
            case "crimes":
                return handleCrimes(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleReport(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /law report <player> <crime>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to report crimes!");
            return true;
        }

        Player criminal = plugin.getServer().getPlayer(args[1]);
        if (criminal == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        CrimeType crimeType;
        try {
            crimeType = CrimeType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid crime type! Use /law crimes to see valid types.");
            return true;
        }

        String location = player.getLocation().getWorld().getName() + "," +
                player.getLocation().getBlockX() + "," +
                player.getLocation().getBlockY() + "," +
                player.getLocation().getBlockZ();

        // Use JusticeService to record the crime
        boolean recorded = justiceService != null &&
                justiceService.recordCrime(criminal.getUniqueId(), crimeType, data.getTown(), location);

        if (recorded) {
            player.sendMessage(String.format("§a✔ " + CustomMessages.Law.CRIME_RECORDED, crimeType.getDisplayName()));
            player.sendMessage("§7Criminal: §e" + criminal.getName());
            player.sendMessage("§7Crime: §c" + crimeType.getDisplayName());
            player.sendMessage("§7Fine: §6$" + justiceService.getCrimeBaseFine(crimeType));
        } else {
            player.sendMessage("§cFailed to record crime. Please try again later.");
        }

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to view crimes!");
            return true;
        }

        List<Crime> townCrimes = lawManager.getAllCrimes().stream()
                .filter(c -> c.getTownId().equals(data.getTown()))
                .filter(c -> !c.isSolved())
                .collect(Collectors.toList());

        if (townCrimes.isEmpty()) {
            player.sendMessage("§aNo unsolved crimes in your town!");
            return true;
        }

        player.sendMessage("§7§m----------§r §cUnsolved Crimes§7 (" + townCrimes.size() + ")§m----------");

        for (Crime crime : townCrimes) {
            String criminalName = plugin.getServer().getOfflinePlayer(crime.getCriminalId()).getName();
            player.sendMessage("§c• §6" + crime.getCrimeType().getDisplayName());
            player.sendMessage("§7  Criminal: §e" + criminalName);
            player.sendMessage("§7  Date: §e" + dateFormat.format(new Date(crime.getTimestamp())));
            player.sendMessage("§7  ID: §e" + crime.getCrimeId().toString().substring(0, 8));
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /law info <crimeId>");
            return true;
        }

        UUID crimeId;
        try {
            String fullId = args[1];
            if (fullId.length() == 8) {
                Crime foundCrime = null;
                for (Crime c : lawManager.getAllCrimes()) {
                    if (c.getCrimeId().toString().startsWith(fullId)) {
                        foundCrime = c;
                        break;
                    }
                }
                if (foundCrime == null) {
                    player.sendMessage("§cCrime not found!");
                    return true;
                }
                crimeId = foundCrime.getCrimeId();
            } else {
                crimeId = UUID.fromString(fullId);
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid crime ID!");
            return true;
        }

        Crime crime = lawManager.getCrime(crimeId);
        if (crime == null) {
            player.sendMessage("§cCrime not found!");
            return true;
        }

        String criminalName = plugin.getServer().getOfflinePlayer(crime.getCriminalId()).getName();

        player.sendMessage("§7§m----------§r §cCrime Info§7 §m----------");
        player.sendMessage("§eCriminal: §6" + criminalName);
        player.sendMessage("§eCrime: §c" + crime.getCrimeType().getDisplayName());
        player.sendMessage("§eTown: §6" + crime.getTownId());
        player.sendMessage("§eDate: §6" + dateFormat.format(new Date(crime.getTimestamp())));
        player.sendMessage("§eLocation: §6" + crime.getLocation());
        player.sendMessage("§eSolved: §6" + (crime.isSolved() ? "Yes" : "No"));
        player.sendMessage("§eFine: §6$" + crime.getCrimeType().getBaseFine());

        if (crime.getEvidence() != null) {
            player.sendMessage("§eEvidence: §6" + crime.getEvidence());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleCrimes(Player player, String[] args) {
        player.sendMessage("§7§m----------§r §cCrime Types§7 §m----------");

        for (CrimeType crimeType : CrimeType.values()) {
            player.sendMessage("§c• §6" + crimeType.getDisplayName());
            player.sendMessage("§7  Type: §e" + crimeType.name().toLowerCase());
            player.sendMessage("§7  Base Fine: §6$" + crimeType.getBaseFine());
        }

        player.sendMessage("§7§m--------------------------------");
        player.sendMessage("§7Use §e/law report <player> <type>§7 to report a crime!");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §cLaw Commands§7 §m----------");
        player.sendMessage("§e/law report <player> <crime>§7 - Report a crime");
        player.sendMessage("§e/law list§7 - List unsolved crimes");
        player.sendMessage("§e/law info <crimeId>§7 - View crime details");
        player.sendMessage("§e/law crimes§7 - List crime types");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("report", "list", "info", "crimes")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("report")) {
            return Arrays.stream(CrimeType.values())
                    .map(ct -> ct.name().toLowerCase())
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
