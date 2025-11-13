package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.PunishmentType;
import me.marcdoesntexists.nations.law.*;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.LawManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.MessageUtils;
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
            case "start":
                if (!player.hasPermission("nations.trial.start")) {
                    player.sendMessage(MessageUtils.get("trial.no_permission_start"));
                    return true;
                }
                return handleStart(player, args);
            case "verdict":
            case "sentence":
            case "arrest":
            case "release":
                if (!player.hasPermission("nations.trial.judge")) {
                    player.sendMessage(MessageUtils.get("trial.no_permission_judge"));
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
            player.sendMessage(MessageUtils.get("trial.usage_start"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("trial.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("trial.only_mayor"));
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
            player.sendMessage(MessageUtils.get("trial.crime_not_found"));
            return true;
        }

        Player defendant = plugin.getServer().getPlayer(args[2]);
        if (defendant == null) {
            player.sendMessage(MessageUtils.get("trial.defendant_offline"));
            return true;
        }

        if (justiceService.startTrial(crime, player.getUniqueId(), defendant.getUniqueId())) {
            player.sendMessage(MessageUtils.get("trial.started_player"));
            player.sendMessage(MessageUtils.format("trial.started_judge", Map.of("judge", player.getName())));
            player.sendMessage(MessageUtils.format("trial.started_defendant", Map.of("defendant", defendant.getName())));
            player.sendMessage(MessageUtils.format("trial.started_charge", Map.of("crime", crime.getCrimeType().getDisplayName())));

            defendant.sendMessage(MessageUtils.get("trial.started_notify_defendant"));
            defendant.sendMessage(MessageUtils.format("trial.notify_judge", Map.of("judge", player.getName())));
            defendant.sendMessage(MessageUtils.format("trial.notify_charge", Map.of("charge", crime.getCrimeType().getDisplayName())));
            defendant.sendMessage(MessageUtils.format("trial.notify_max_fine", Map.of("fine", String.valueOf((int)crime.getCrimeType().getBaseFine()))));
        } else {
            player.sendMessage(MessageUtils.get("trial.failed_start"));
        }

        return true;
    }

    private boolean handleVerdict(Player player, String[] args) {
        // /trial verdict <trialId> <guilty|not_guilty>
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("trial.usage_verdict"));
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
            player.sendMessage(MessageUtils.get("trial.not_found"));
            return true;
        }

        if (!trial.getJudgeId().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("trial.not_judge"));
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
            player.sendMessage(MessageUtils.get("trial.invalid_verdict"));
            return true;
        }

        player.sendMessage(MessageUtils.format("trial.verdict_recorded", Map.of("verdict", verdict == PunishmentType.GUILTY ? MessageUtils.get("trial.guilty") : MessageUtils.get("trial.not_guilty"))));
        player.sendMessage(MessageUtils.get("trial.verdict_next_steps"));

        Player defendant = plugin.getServer().getPlayer(trial.getDefendantId());
        if (defendant != null) {
            if (verdict == PunishmentType.GUILTY) {
                defendant.sendMessage(MessageUtils.get("trial.notify_guilty"));
                defendant.sendMessage(MessageUtils.get("trial.notify_awaiting_sentence"));
            } else {
                defendant.sendMessage(MessageUtils.get("trial.notify_not_guilty"));
                defendant.sendMessage(MessageUtils.get("trial.notify_free"));
            }
        }

        return true;
    }

    private boolean handleSentence(Player player, String[] args) {
        // /trial sentence <trialId> <punishment> <amount>
        if (args.length < 4) {
            player.sendMessage(MessageUtils.get("trial.usage_sentence"));
            player.sendMessage(MessageUtils.get("trial.punishments_list"));
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
            player.sendMessage(MessageUtils.get("trial.not_found"));
            return true;
        }

        if (!trial.getJudgeId().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("trial.not_judge"));
            return true;
        }

        PunishmentType punishment;
        try {
            punishment = PunishmentType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("trial.invalid_punishment"));
            player.sendMessage(MessageUtils.get("trial.punishments_list"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtils.get("trial.invalid_amount"));
            return true;
        }

        String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : MessageUtils.get("trial.no_reason_provided");

        if (justiceService.concludeTrial(trial, punishment, amount, reason)) {
            player.sendMessage(MessageUtils.get("trial.sentence_applied"));
            player.sendMessage(MessageUtils.format("trial.sentence_info", Map.of("punishment", punishment.getDescription(), "amount", String.valueOf((int)amount))));
            player.sendMessage(MessageUtils.format("trial.sentence_reason", Map.of("reason", reason)));

            Player defendant = plugin.getServer().getPlayer(trial.getDefendantId());
            if (defendant != null) {
                defendant.sendMessage(MessageUtils.get("trial.notify_sentence"));
                defendant.sendMessage(MessageUtils.format("trial.notify_punishment", Map.of("punishment", punishment.getDescription())));

                PlayerData defendantData = dataManager.getPlayerData(defendant.getUniqueId());

                switch (punishment) {
                    case FINE:
                        defendant.sendMessage(MessageUtils.format("trial.notify_fine_amount", Map.of("amount", String.valueOf((int)amount))));
                        if (defendantData.removeMoney((int)amount)) {
                            defendant.sendMessage(MessageUtils.get("trial.fine_paid"));
                            try { plugin.getDataManager().savePlayerMoney(defendant.getUniqueId()); } catch (Throwable ignored) {}
                        } else {
                            defendant.sendMessage(MessageUtils.get("trial.fine_unpaid"));
                        }
                        break;

                    case IMPRISONMENT:
                        defendant.sendMessage(MessageUtils.format("trial.notify_imprisonment", Map.of("hours", String.valueOf((int)amount))));
                        defendant.sendMessage(MessageUtils.get("trial.notify_imprisoned"));
                        break;

                    case BANISHMENT:
                        defendant.sendMessage(MessageUtils.format("trial.notify_banishment", Map.of("days", String.valueOf((int)amount))));
                        defendant.sendMessage(MessageUtils.get("trial.notify_banished"));
                        break;
                }

                defendant.sendMessage(MessageUtils.format("trial.notify_reason", Map.of("reason", reason)));
            }

            // Update criminal record
            Criminal criminal = lawManager.getCriminal(trial.getDefendantId());
            if (criminal != null) {
                criminal.addFine(amount);
            }
        } else {
            player.sendMessage(MessageUtils.get("trial.failed_sentence"));
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("trial.usage_info"));
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
            player.sendMessage(MessageUtils.get("trial.not_found"));
            return true;
        }

        String judgeName = plugin.getServer().getOfflinePlayer(trial.getJudgeId()).getName();
        String defendantName = plugin.getServer().getOfflinePlayer(trial.getDefendantId()).getName();

        player.sendMessage(MessageUtils.get("trial.info_header"));
        player.sendMessage(MessageUtils.format("trial.info_id", Map.of("id", trial.getTrialId().toString().substring(0, 8))));
        player.sendMessage(MessageUtils.format("trial.info_judge", Map.of("judge", judgeName)));
        player.sendMessage(MessageUtils.format("trial.info_defendant", Map.of("defendant", defendantName)));
        player.sendMessage(MessageUtils.format("trial.info_crime", Map.of("crime", trial.getCrime().getCrimeType().getDisplayName())));
        player.sendMessage(MessageUtils.format("trial.info_status", Map.of("status", trial.getStatus().name())));

        if (trial.getVerdict() != null) {
            player.sendMessage(MessageUtils.format("trial.info_verdict", Map.of("verdict", trial.getVerdict().getDescription())));
            if (trial.getPunishment() > 0) {
                player.sendMessage(MessageUtils.format("trial.info_punishment_amount", Map.of("amount", String.valueOf((int)trial.getPunishment()))));
            }
            if (trial.getReason() != null) {
                player.sendMessage(MessageUtils.format("trial.info_reason", Map.of("reason", trial.getReason())));
            }
        }

        player.sendMessage(MessageUtils.get("trial.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("trial.must_be_in_town"));
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
            player.sendMessage(MessageUtils.get("trial.list_none"));
            return true;
        }

        player.sendMessage(MessageUtils.format("trial.list_header", Map.of("count", String.valueOf(townTrials.size()))));

        for (Trial trial : townTrials) {
            String defendantName = plugin.getServer().getOfflinePlayer(trial.getDefendantId()).getName();
            player.sendMessage(MessageUtils.format("trial.list_item", Map.of("defendant", defendantName, "charge", trial.getCrime().getCrimeType().getDisplayName(), "status", trial.getStatus().name(), "id", trial.getTrialId().toString().substring(0, 8))));
            player.sendMessage(MessageUtils.format("trial.list_item_status", Map.of("status", trial.getStatus().name(), "id", trial.getTrialId().toString().substring(0, 8))));
        }

        player.sendMessage(MessageUtils.get("trial.list_footer"));

        return true;
    }

    private boolean handleCriminals(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("trial.must_be_in_town"));
            return true;
        }

        Collection<Criminal> criminals = lawManager.getAllCriminals();
        List<Criminal> townCriminals = criminals.stream()
                .filter(c -> c.getTownId().equals(data.getTown()))
                .filter(c -> c.getWantedLevel() > 0)
                .toList();

        if (townCriminals.isEmpty()) {
            player.sendMessage(MessageUtils.get("trial.criminals_none"));
            return true;
        }

        player.sendMessage(MessageUtils.format("trial.criminals_header", Map.of("count", String.valueOf(townCriminals.size()))));

        for (Criminal criminal : townCriminals) {
            String criminalName = plugin.getServer().getOfflinePlayer(criminal.getCriminalId()).getName();
            player.sendMessage(MessageUtils.format("trial.criminal_item", Map.of("criminal", criminalName)));
            player.sendMessage(MessageUtils.format("trial.criminal_stats", Map.of("wanted", String.valueOf(criminal.getWantedLevel()), "fines", String.valueOf((int)criminal.getTotalFines()), "arrested", criminal.isArrested() ? MessageUtils.get("yes") : MessageUtils.get("no"))));
        }

        player.sendMessage(MessageUtils.get("trial.criminals_footer"));

        return true;
    }

    private boolean handleArrest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("trial.usage_arrest"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("trial.must_be_in_town"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("trial.player_not_found"));
            return true;
        }

        justiceService.arrestCriminal(target.getUniqueId());

        player.sendMessage(MessageUtils.format("trial.arrested_player", Map.of("player", target.getName())));
        target.sendMessage(MessageUtils.format("trial.arrested_notify", Map.of("by", player.getName())));

        return true;
    }

    private boolean handleRelease(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("trial.usage_release"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("trial.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("trial.only_mayor_release"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(MessageUtils.get("trial.player_not_found"));
            return true;
        }

        justiceService.releaseCriminal(target.getUniqueId());

        player.sendMessage(MessageUtils.format("trial.released_player", Map.of("player", target.getName())));
        target.sendMessage(MessageUtils.format("trial.released_notify", Map.of("by", player.getName())));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("trial.help_header"));
        player.sendMessage(MessageUtils.get("trial.help_start"));
        player.sendMessage(MessageUtils.get("trial.help_verdict"));
        player.sendMessage(MessageUtils.get("trial.help_sentence"));
        player.sendMessage(MessageUtils.get("trial.help_info"));
        player.sendMessage(MessageUtils.get("trial.help_list"));
        player.sendMessage(MessageUtils.get("trial.help_criminals"));
        player.sendMessage(MessageUtils.get("trial.help_arrest"));
        player.sendMessage(MessageUtils.get("trial.help_release"));
        player.sendMessage(MessageUtils.get("trial.help_footer"));
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