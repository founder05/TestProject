package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.MilitaryManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.military.War;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
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

public class WarCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final MilitaryManager militaryManager;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public WarCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.militaryManager = plugin.getMilitaryManager();
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
            case "declare":
                return handleDeclare(player, args);
            case "end":
                return handleEnd(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleDeclare(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("war.usage_declare"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("war.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("war.must_be_in_kingdom"));
            return true;
        }

        Kingdom attackerKingdom = societiesManager.getKingdom(town.getKingdom());
        if (!attackerKingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("war.only_capital_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("war.must_be_mayor"));
            return true;
        }

        String targetKingdomName = args[1];
        Kingdom defenderKingdom = societiesManager.getKingdom(targetKingdomName);

        if (defenderKingdom == null) {
            player.sendMessage(MessageUtils.get("war.kingdom_not_found"));
            return true;
        }

        if (defenderKingdom.getName().equals(attackerKingdom.getName())) {
            player.sendMessage(MessageUtils.get("war.cannot_attack_own"));
            return true;
        }

        if (attackerKingdom.getWars().contains(targetKingdomName)) {
            player.sendMessage(MessageUtils.get("war.already_at_war"));
            return true;
        }

        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : MessageUtils.get("war.no_reason_provided");

        War war = new War(attackerKingdom.getName(), defenderKingdom.getName(), player.getUniqueId(), reason);
        militaryManager.declareWar(war);

        attackerKingdom.declareWar(defenderKingdom.getName());
        defenderKingdom.declareWar(attackerKingdom.getName());

        attackerKingdom.addEnemy(defenderKingdom.getName());
        defenderKingdom.addEnemy(attackerKingdom.getName());

        player.sendMessage(MessageUtils.format("war.declared_player", Map.of("defender", defenderKingdom.getName(), "reason", reason)));

        Town defenderCapital = societiesManager.getTown(defenderKingdom.getCapital());
        if (defenderCapital != null) {
            Player defenderKing = plugin.getServer().getPlayer(defenderCapital.getMayor());
            if (defenderKing != null) {
                defenderKing.sendMessage(MessageUtils.format("war.declared_target", Map.of("attacker", attackerKingdom.getName(), "reason", reason)));
            }
        }

        return true;
    }

    private boolean handleEnd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("war.usage_end"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("war.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("war.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("war.only_capital_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("war.must_be_mayor"));
            return true;
        }

        String targetKingdomName = args[1];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);

        if (targetKingdom == null) {
            player.sendMessage(MessageUtils.get("war.kingdom_not_found"));
            return true;
        }

        if (!kingdom.getWars().contains(targetKingdomName)) {
            player.sendMessage(MessageUtils.get("war.not_at_war"));
            return true;
        }

        War activeWar = findWar(kingdom.getName(), targetKingdom.getName());
        if (activeWar != null) {
            activeWar.endWar();
        }

        kingdom.removeWar(targetKingdomName);
        targetKingdom.removeWar(kingdom.getName());

        kingdom.removeEnemy(targetKingdomName);
        targetKingdom.removeEnemy(kingdom.getName());

        player.sendMessage(MessageUtils.format("war.ended_player", Map.of("target", targetKingdomName)));

        Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
        if (targetCapital != null) {
            Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
            if (targetKing != null) {
                targetKing.sendMessage(MessageUtils.format("war.ended_target", Map.of("attacker", kingdom.getName())));
            }
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("war.usage_info"));
            return true;
        }

        UUID warId;
        try {
            warId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("war.invalid_id"));
            return true;
        }

        War war = militaryManager.getWar(warId);
        if (war == null) {
            player.sendMessage(MessageUtils.get("war.not_found"));
            return true;
        }

        player.sendMessage(MessageUtils.get("war.info_header"));
        player.sendMessage(MessageUtils.format("war.info_attacker", Map.of("attacker", war.getAttackerKingdom())));
        player.sendMessage(MessageUtils.format("war.info_defender", Map.of("defender", war.getDefenderKingdom())));
        player.sendMessage(MessageUtils.format("war.info_status", Map.of("status", war.getStatus().name())));
        player.sendMessage(MessageUtils.format("war.info_started", Map.of("started", dateFormat.format(new Date(war.getStartDate())))));

        if (war.getStatus() == War.WarStatus.CONCLUDED && war.getEndDate() > 0) {
            player.sendMessage(MessageUtils.format("war.info_ended", Map.of("ended", dateFormat.format(new Date(war.getEndDate())))));
        }

        player.sendMessage(MessageUtils.format("war.info_reason", Map.of("reason", war.getReason())));
        player.sendMessage(MessageUtils.format("war.info_attacker_casualties", Map.of("casualties", String.valueOf(war.getAttackerCasualties()))));
        player.sendMessage(MessageUtils.format("war.info_defender_casualties", Map.of("casualties", String.valueOf(war.getDefenderCasualties()))));
        player.sendMessage(MessageUtils.format("war.info_war_crimes", Map.of("crimes", String.valueOf(war.getWarCrimes().size()))));
        player.sendMessage(MessageUtils.get("war.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<War> wars = militaryManager.getAllWars();

        List<War> activeWars = wars.stream()
                .filter(w -> w.getStatus() != War.WarStatus.CONCLUDED)
                .collect(Collectors.toList());

        if (activeWars.isEmpty()) {
            player.sendMessage(MessageUtils.get("war.list_none"));
            return true;
        }

        player.sendMessage(MessageUtils.format("war.list_header", Map.of("count", String.valueOf(activeWars.size()))));

        for (War war : activeWars) {
            player.sendMessage(MessageUtils.format("war.list_item", Map.of("attacker", war.getAttackerKingdom(), "defender", war.getDefenderKingdom(), "status", war.getStatus().name(), "id", war.getWarId().toString().substring(0, 8))));
        }

        player.sendMessage(MessageUtils.get("war.list_footer"));

        return true;
    }

    private War findWar(String kingdom1, String kingdom2) {
        for (War war : militaryManager.getAllWars()) {
            if ((war.getAttackerKingdom().equals(kingdom1) && war.getDefenderKingdom().equals(kingdom2)) ||
                    (war.getAttackerKingdom().equals(kingdom2) && war.getDefenderKingdom().equals(kingdom1))) {
                if (war.getStatus() != War.WarStatus.CONCLUDED) {
                    return war;
                }
            }
        }
        return null;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("war.help_header"));
        player.sendMessage(MessageUtils.get("war.help_declare"));
        player.sendMessage(MessageUtils.get("war.help_end"));
        player.sendMessage(MessageUtils.get("war.help_info"));
        player.sendMessage(MessageUtils.get("war.help_list"));
        player.sendMessage(MessageUtils.get("war.help_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("declare", "end", "info", "list")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("declare") || args[0].equalsIgnoreCase("end")) {
                return societiesManager.getAllKingdoms().stream()
                        .map(Kingdom::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
