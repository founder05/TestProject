package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.PunishmentType;
import me.marcdoesntexists.nations.law.*;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.LawManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("unused")
public class TrialCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final LawManager lawManager;
    private final SocietiesManager societiesManager;
    private final JusticeService justiceService;

    @SuppressWarnings("unused")
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public TrialCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.lawManager = plugin.getLawManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.justiceService = plugin.getJusticeService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
            case "start":
                if (!player.hasPermission("nations.trial.start")) {
                    player.sendMessage("§cYou do not have permission to start trials.");
                    return true;
                }
                return handleStart(player, args);
            case "verdict":
            case "sentence":
            case "arrest":
            case "release":
                if (!player.hasPermission("nations.trial.judge")) {
                    player.sendMessage("§cYou do not have permission to judge or sentence.");
                    return true;
                }
                if (subCommand.equals("verdict")) return handleVerdict(player, args);
                if (subCommand.equals("sentence")) return handleSentence(player, args);
                if (subCommand.equals("arrest")) return handleArrest(player, args);
                return handleRelease(player, args);
            case "info":
            case "list":
            case "criminals":
                // viewing trial-related info allowed for all players (or could be limited)
                return switch (subCommand) {
                    case "info" -> handleInfo(player, args);
                    case "list" -> handleList(player, args);
                    default -> handleCriminals(player, args);
                };
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleStart(Player player, String[] args) {
        // /trial start <crimeId> <defendant>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /trial start <crimeId> <defendant>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to start trials!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can start trials!");
            return true;
        }

        String crimeIdStr = args[1];
        Crime crime = null;

        // Try to find crime by short ID
        for (Crime c : lawManager.getAllCrimes()) {
            if (c.getCrimeId().toString().startsWith(crimeIdStr)) {
                crime = c;
                break;
            }
        }

        if (crime == null) {
            player.sendMessage("§cCrime not found!");
            return true;
        }

        Player defendant = plugin.getServer().getPlayer(args[2]);
        if (defendant == null) {
            player.sendMessage("§cDefendant not online!");
            return true;
        }

        if (justiceService.startTrial(crime, player.getUniqueId(), defendant.getUniqueId())) {
            player.sendMessage("§a✔ Trial started!");
            player.sendMessage("§7Judge: §e" + player.getName());
            player.sendMessage("§7Defendant: §e" + defendant.getName());
            player.sendMessage("§7Crime: §c" + crime.getCrimeType().getDisplayName());

            defendant.sendMessage("§c⚖ You are on trial!");
            defendant.sendMessage("§7Judge: §e" + player.getName());
            defendant.sendMessage("§7Charge: §c" + crime.getCrimeType().getDisplayName());
            defendant.sendMessage("§7Maximum Fine: §6$" + crime.getCrimeType().getBaseFine());
        } else {
            player.sendMessage("§cFailed to start trial!");
        }

        return true;
    }

    private boolean handleVerdict(Player player, String[] args) {
        // /trial verdict <trialId> <guilty|not_guilty>
        if (args.length < 3) {
            player.sendMessage("§cUsage: /trial verdict <trialId> <guilty|not_guilty>");
            return true;
        }

        String trialIdStr = args[1];
        Trial trial = null;

        for (Trial t : lawManager.getAllTrials()) {
            if (t.getTrialId().toString().startsWith(trialIdStr)) {
                trial = t;
                break;
            }
        }

        if (trial == null) {
            player.sendMessage("§cTrial not found!");
            return true;
        }

        if (!trial.getJudgeId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou are not the judge of this trial!");
            return true;
        }

        if (trial.getStatus() != Trial.TrialStatus.IN_PROGRESS) {
            trial.setStatus(Trial.TrialStatus.IN_PROGRESS);
        }

        String verdictStr = args[2].toLowerCase();
        PunishmentType verdict;

        if (verdictStr.equals("guilty")) {
            verdict = PunishmentType.GUILTY;
        } else if (verdictStr.equals("not_guilty")) {
            verdict = PunishmentType.NOT_GUILTY;
        } else {
            player.sendMessage("§cInvalid verdict! Use 'guilty' or 'not_guilty'");
            return true;
        }

        player.sendMessage("§a✔ Verdict recorded: " + (verdict == PunishmentType.GUILTY ? "§cGUILTY" : "§aNOT GUILTY"));
        player.sendMessage("§7Use §e/trial sentence <trialId> <punishment> <amount>§7 to apply sentence");

        Player defendant = plugin.getServer().getPlayer(trial.getDefendantId());
        if (defendant != null) {
            if (verdict == PunishmentType.GUILTY) {
                defendant.sendMessage("§c⚖ You have been found GUILTY!");
                defendant.sendMessage("§7Awaiting sentence...");
            } else {
                defendant.sendMessage("§a⚖ You have been found NOT GUILTY!");
                defendant.sendMessage("§7You are free to go!");
            }
        }

        return true;
    }

    private boolean handleSentence(Player player, String[] args) {
        // /trial sentence <trialId> <punishment> <amount>
        if (args.length < 4) {
            player.sendMessage("§cUsage: /trial sentence <trialId> <punishment> <amount>");
            player.sendMessage("§7Punishments: FINE, IMPRISONMENT, BANISHMENT");
            return true;
        }

        String trialIdStr = args[1];
        Trial trial = null;

        for (Trial t : lawManager.getAllTrials()) {
            if (t.getTrialId().toString().startsWith(trialIdStr)) {
                trial = t;
                break;
            }
        }

        if (trial == null) {
            player.sendMessage("§cTrial not found!");
            return true;
        }

        if (!trial.getJudgeId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou are not the judge of this trial!");
            return true;
        }

        PunishmentType punishment;
        try {
            punishment = PunishmentType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid punishment type!");
            player.sendMessage("§7Valid types: FINE, IMPRISONMENT, BANISHMENT");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return true;
        }

        String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "No reason provided";

        if (justiceService.concludeTrial(trial, punishment, amount, reason)) {
            player.sendMessage("§a✔ Sentence applied!");
            player.sendMessage("§7Punishment: §c" + punishment.getDescription());
            player.sendMessage("§7Amount: §6" + (int)amount);
            player.sendMessage("§7Reason: §e" + reason);

            Player defendant = plugin.getServer().getPlayer(trial.getDefendantId());
            if (defendant != null) {
                defendant.sendMessage("§c⚖ SENTENCE!");
                defendant.sendMessage("§7Punishment: §c" + punishment.getDescription());

                PlayerData defendantData = dataManager.getPlayerData(defendant.getUniqueId());

                switch (punishment) {
                    case FINE:
                        defendant.sendMessage("§7Fine: §6$" + (int)amount);
                        if (defendantData.removeMoney((int)amount)) {
                            defendant.sendMessage("§c✘ Fine paid automatically!");
                        } else {
                            defendant.sendMessage("§c✘ Insufficient funds! You now have a debt!");
                        }
                        break;

                    case IMPRISONMENT:
                        defendant.sendMessage("§7Duration: §c" + (int)amount + " hours");
                        defendant.sendMessage("§c✘ You are now imprisoned!");
                        break;

                    case BANISHMENT:
                        defendant.sendMessage("§7Duration: §c" + (int)amount + " days");
                        defendant.sendMessage("§c✘ You are banished from this town!");
                        break;
                }

                defendant.sendMessage("§7Reason: §e" + reason);
            }

            // Update criminal record
            Criminal criminal = lawManager.getCriminal(trial.getDefendantId());
            if (criminal != null) {
                criminal.addFine(amount);
            }
        } else {
            player.sendMessage("§cFailed to apply sentence!");
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /trial info <trialId>");
            return true;
        }

        String trialIdStr = args[1];
        Trial trial = null;

        for (Trial t : lawManager.getAllTrials()) {
            if (t.getTrialId().toString().startsWith(trialIdStr)) {
                trial = t;
                break;
            }
        }

        if (trial == null) {
            player.sendMessage("§cTrial not found!");
            return true;
        }

        String judgeName = plugin.getServer().getOfflinePlayer(trial.getJudgeId()).getName();
        String defendantName = plugin.getServer().getOfflinePlayer(trial.getDefendantId()).getName();

        player.sendMessage("§7§m----------§r §c⚖ Trial Info§7 §m----------");
        player.sendMessage("§eID: §6" + trial.getTrialId().toString().substring(0, 8));
        player.sendMessage("§eJudge: §6" + judgeName);
        player.sendMessage("§eDefendant: §6" + defendantName);
        player.sendMessage("§eCrime: §c" + trial.getCrime().getCrimeType().getDisplayName());
        player.sendMessage("§eStatus: §6" + trial.getStatus().name());

        if (trial.getVerdict() != null) {
            player.sendMessage("§eVerdict: §6" + trial.getVerdict().getDescription());
            if (trial.getPunishment() > 0) {
                player.sendMessage("§ePunishment Amount: §6" + (int)trial.getPunishment());
            }
            if (trial.getReason() != null) {
                player.sendMessage("§eReason: §e" + trial.getReason());
            }
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        List<Trial> townTrials = lawManager.getAllTrials().stream()
                .filter(t -> {
                    Crime crime = t.getCrime();
                    return crime.getTownId().equals(data.getTown());
                })
                .filter(t -> t.getStatus() != Trial.TrialStatus.CONCLUDED)
                .toList();

        if (townTrials.isEmpty()) {
            player.sendMessage("§aNo active trials!");
            return true;
        }

        player.sendMessage("§7§m----------§r §c⚖ Active Trials§7 (" + townTrials.size() + ")§m----------");

        for (Trial trial : townTrials) {
            String defendantName = plugin.getServer().getOfflinePlayer(trial.getDefendantId()).getName();
            player.sendMessage("§c• §e" + defendantName + " §7- §c" + trial.getCrime().getCrimeType().getDisplayName());
            player.sendMessage("§7  Status: §6" + trial.getStatus().name() + " §7| ID: §e" + trial.getTrialId().toString().substring(0, 8));
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleCriminals(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Collection<Criminal> criminals = lawManager.getAllCriminals();
        List<Criminal> townCriminals = criminals.stream()
                .filter(c -> c.getTownId().equals(data.getTown()))
                .filter(c -> c.getWantedLevel() > 0)
                .toList();

        if (townCriminals.isEmpty()) {
            player.sendMessage("§aNo active criminals in your town!");
            return true;
        }

        player.sendMessage("§7§m----------§r §cCriminals§7 (" + townCriminals.size() + ")§m----------");

        for (Criminal criminal : townCriminals) {
            String criminalName = plugin.getServer().getOfflinePlayer(criminal.getCriminalId()).getName();
            player.sendMessage("§c• §e" + criminalName);
            player.sendMessage("§7  Wanted Level: §c" + criminal.getWantedLevel() +
                    " §7| Total Fines: §6$" + (int)criminal.getTotalFines() +
                    " §7| Arrested: " + (criminal.isArrested() ? "§aYes" : "§cNo"));
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleArrest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /trial arrest <player>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        justiceService.arrestCriminal(target.getUniqueId());

        player.sendMessage("§a✔ §e" + target.getName() + "§a arrested!");
        target.sendMessage("§c✘ You have been arrested by §e" + player.getName() + "§c!");

        return true;
    }

    private boolean handleRelease(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /trial release <player>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can release criminals!");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        justiceService.releaseCriminal(target.getUniqueId());

        player.sendMessage("§a✔ §e" + target.getName() + "§a released!");
        target.sendMessage("§a✔ You have been released by §e" + player.getName() + "§a!");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §c⚖ Trial Commands§7 §m----------");
        player.sendMessage("§e/trial start <crimeId> <defendant>§7 - Start trial");
        player.sendMessage("§e/trial verdict <id> <guilty|not_guilty>§7 - Verdict");
        player.sendMessage("§e/trial sentence <id> <type> <amt>§7 - Sentence");
        player.sendMessage("§e/trial info <id>§7 - View trial info");
        player.sendMessage("§e/trial list§7 - List active trials");
        player.sendMessage("§e/trial criminals§7 - List criminals");
        player.sendMessage("§e/trial arrest <player>§7 - Arrest player");
        player.sendMessage("§e/trial release <player>§7 - Release player");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "verdict", "sentence", "info", "list", "criminals", "arrest", "release")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("verdict")) {
            return Arrays.asList("guilty", "not_guilty")
                    .stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("sentence")) {
            return Arrays.asList("FINE", "IMPRISONMENT", "BANISHMENT")
                    .stream()
                    .filter(s -> s.startsWith(args[2].toUpperCase()))
                    .toList();
        }

        return new ArrayList<>();
    }
}