package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.CrimeType;
import me.marcdoesntexists.nations.law.Crime;
import me.marcdoesntexists.nations.law.JusticeService;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.LawManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
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
            player.sendMessage(MessageUtils.get("law.usage_report"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("law.must_be_in_town"));
            return true;
        }

        Player criminal = plugin.getServer().getPlayer(args[1]);
        if (criminal == null) {
            player.sendMessage(MessageUtils.get("law.player_not_found"));
            return true;
        }

        CrimeType crimeType;
        try {
            crimeType = CrimeType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("law.invalid_crime_type"));
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
            player.sendMessage(MessageUtils.format("law.crime_recorded", Map.of("crime", crimeType.getDisplayName())));
            player.sendMessage(MessageUtils.format("law.criminal_line", Map.of("criminal", criminal.getName())));
            player.sendMessage(MessageUtils.format("law.crime_line", Map.of("crime", crimeType.getDisplayName())));
            player.sendMessage(MessageUtils.format("law.fine_line", Map.of("fine", String.valueOf(justiceService.getCrimeBaseFine(crimeType)))));
        } else {
            player.sendMessage(MessageUtils.get("law.failed_record"));
        }

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("law.must_be_in_town"));
            return true;
        }

        List<Crime> townCrimes = lawManager.getAllCrimes().stream()
                .filter(c -> c.getTownId().equals(data.getTown()))
                .filter(c -> !c.isSolved())
                .collect(Collectors.toList());

        if (townCrimes.isEmpty()) {
            player.sendMessage(MessageUtils.get("law.list_none"));
            return true;
        }

        player.sendMessage(MessageUtils.format("law.list_header", Map.of("count", String.valueOf(townCrimes.size()))));

        for (Crime crime : townCrimes) {
            String criminalName = plugin.getServer().getOfflinePlayer(crime.getCriminalId()).getName();
            player.sendMessage(MessageUtils.format("law.list_item_crime", Map.of("crime", crime.getCrimeType().getDisplayName())));
            player.sendMessage(MessageUtils.format("law.list_item_criminal", Map.of("criminal", criminalName)));
            player.sendMessage(MessageUtils.format("law.list_item_date", Map.of("date", dateFormat.format(new Date(crime.getTimestamp())))));
            player.sendMessage(MessageUtils.format("law.list_item_id", Map.of("id", crime.getCrimeId().toString().substring(0, 8))));
        }

        player.sendMessage(MessageUtils.get("law.list_footer"));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("law.usage_info"));
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
                    player.sendMessage(MessageUtils.get("law.not_found"));
                    return true;
                }
                crimeId = foundCrime.getCrimeId();
            } else {
                crimeId = UUID.fromString(fullId);
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("law.invalid_id"));
            return true;
        }

        Crime crime = lawManager.getCrime(crimeId);
        if (crime == null) {
            player.sendMessage(MessageUtils.get("law.not_found"));
            return true;
        }

        String criminalName = plugin.getServer().getOfflinePlayer(crime.getCriminalId()).getName();

        player.sendMessage(MessageUtils.get("law.info_header"));
        player.sendMessage(MessageUtils.format("law.info_criminal", Map.of("criminal", criminalName)));
        player.sendMessage(MessageUtils.format("law.info_crime", Map.of("crime", crime.getCrimeType().getDisplayName())));
        player.sendMessage(MessageUtils.format("law.info_town", Map.of("town", crime.getTownId())));
        player.sendMessage(MessageUtils.format("law.info_date", Map.of("date", dateFormat.format(new Date(crime.getTimestamp())))));
        player.sendMessage(MessageUtils.format("law.info_location", Map.of("location", crime.getLocation())));
        player.sendMessage(MessageUtils.format("law.info_solved", Map.of("solved", crime.isSolved() ? MessageUtils.get("yes") : MessageUtils.get("no"))));
        player.sendMessage(MessageUtils.format("law.info_fine", Map.of("fine", String.valueOf(crime.getCrimeType().getBaseFine()))));

        if (crime.getEvidence() != null) {
            player.sendMessage(MessageUtils.format("law.info_evidence", Map.of("evidence", crime.getEvidence())));
        }

        player.sendMessage(MessageUtils.get("law.info_footer"));

        return true;
    }

    private boolean handleCrimes(Player player, String[] args) {
        player.sendMessage(MessageUtils.get("law.crimes_header"));

        for (CrimeType crimeType : CrimeType.values()) {
            player.sendMessage(MessageUtils.format("law.crime_type_item", Map.of("display", crimeType.getDisplayName(), "type", crimeType.name().toLowerCase(), "fine", String.valueOf(crimeType.getBaseFine()))));
        }

        player.sendMessage(MessageUtils.get("law.crimes_footer"));
        player.sendMessage(MessageUtils.get("law.crimes_help"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("law.help_header"));
        player.sendMessage(MessageUtils.get("law.help_report"));
        player.sendMessage(MessageUtils.get("law.help_list"));
        player.sendMessage(MessageUtils.get("law.help_info"));
        player.sendMessage(MessageUtils.get("law.help_crimes"));
        player.sendMessage(MessageUtils.get("law.help_footer"));
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
