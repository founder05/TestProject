package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.DiplomacyService;
import me.marcdoesntexists.realms.societies.Kingdom;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.societies.Treaty;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TreatyCommand implements CommandExecutor, TabCompleter {

    private final Realms plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final DiplomacyService diplomacyService;

    public TreatyCommand(Realms plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.diplomacyService = plugin.getDiplomacyService();
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
            player.sendMessage(MessageUtils.get("treaty.usage_create"));
            player.sendMessage(MessageUtils.get("treaty.usage_types"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("treaty.only_capital_can_create"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("treaty.must_be_mayor"));
            return true;
        }

        String targetKingdomName = args[1];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);

        if (targetKingdom == null) {
            player.sendMessage(MessageUtils.get("treaty.kingdom_not_found"));
            return true;
        }

        if (targetKingdom.getName().equals(kingdom.getName())) {
            player.sendMessage(MessageUtils.get("treaty.cannot_self"));
            return true;
        }

        Treaty.TreatyType type;
        try {
            type = Treaty.TreatyType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("treaty.invalid_type"));
            player.sendMessage(MessageUtils.get("treaty.usage_types"));
            return true;
        }

        long days;
        try {
            days = Long.parseLong(args[3]);
            if (days < 1 || days > 365) {
                player.sendMessage(MessageUtils.get("treaty.duration_bounds"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtils.get("treaty.invalid_days"));
            return true;
        }

        String treatyName = kingdom.getName() + "-" + targetKingdom.getName() + "-" + type.name();

        if (diplomacyService.createTreaty(treatyName, kingdom.getName(), targetKingdom.getName(), type, days)) {
            player.sendMessage(MessageUtils.format("treaty.created", java.util.Map.of("name", treatyName)));
            player.sendMessage(MessageUtils.format("treaty.created_type", java.util.Map.of("type", type.name())));
            player.sendMessage(MessageUtils.format("treaty.created_duration", java.util.Map.of("days", String.valueOf(days))));
            player.sendMessage(MessageUtils.format("treaty.created_between", java.util.Map.of("a", kingdom.getName(), "b", targetKingdom.getName())));

            // Notify the other kingdom
            Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
            if (targetCapital != null) {
                Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
                if (targetKing != null) {
                    targetKing.sendMessage(MessageUtils.format("treaty.notify_proposed", java.util.Map.of("from", kingdom.getName())));
                    targetKing.sendMessage(MessageUtils.format("treaty.notify_proposed_details", java.util.Map.of("type", type.name(), "days", String.valueOf(days))));
                    targetKing.sendMessage(MessageUtils.format("treaty.notify_proposed_accept", java.util.Map.of("name", treatyName)));
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("treaty.create_failed"));
        }

        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("treaty.accept_usage"));
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
            player.sendMessage(MessageUtils.get("treaty.not_found"));
            return true;
        }

        if (treaty.getStatus() != Treaty.TreatyStatus.PENDING) {
            player.sendMessage(MessageUtils.get("treaty.not_pending"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!treaty.getKingdom1().equals(kingdom.getName()) && !treaty.getKingdom2().equals(kingdom.getName())) {
            player.sendMessage(MessageUtils.get("treaty.not_involved"));
            return true;
        }

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("treaty.only_capital_accept"));
            return true;
        }

        treaty.setStatus(Treaty.TreatyStatus.ACTIVE);

        player.sendMessage(MessageUtils.format("treaty.accept_success", java.util.Map.of("name", treaty.getName())));
        player.sendMessage(MessageUtils.format("treaty.accept_active_days", java.util.Map.of("days", String.valueOf(treaty.getDaysRemaining()))));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("treaty.accept_usage"));
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
            player.sendMessage(MessageUtils.get("treaty.not_found"));
            return true;
        }

        player.sendMessage(MessageUtils.get("treaty.info_header"));
        player.sendMessage(MessageUtils.format("treaty.info_line_name", java.util.Map.of("name", treaty.getName())));
        player.sendMessage(MessageUtils.format("treaty.info_line_type", java.util.Map.of("type", treaty.getType().name())));
        player.sendMessage(MessageUtils.format("treaty.info_line_kingdoms", java.util.Map.of("a", treaty.getKingdom1(), "b", treaty.getKingdom2())));
        player.sendMessage(MessageUtils.format("treaty.info_line_status", java.util.Map.of("status", treaty.getStatus().name())));
        player.sendMessage(MessageUtils.format("treaty.info_line_days", java.util.Map.of("days", String.valueOf(treaty.getDaysRemaining()))));

        if (treaty.isActive()) {
            player.sendMessage(MessageUtils.get("treaty.accept_success").replace("{name}", treaty.getName()));
        } else if (treaty.isExpired()) {
            player.sendMessage(MessageUtils.get("treaty.break_failed"));
        }

        player.sendMessage(MessageUtils.get("treaty.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Treaty> treaties = societiesManager.getAllTreaties();

        if (treaties.isEmpty()) {
            player.sendMessage(MessageUtils.get("treaty.create_failed").replace("Failed to create treaty!", "No treaties exist!"));
            return true;
        }

        player.sendMessage(MessageUtils.format("treaty.list_header", java.util.Map.of("count", String.valueOf(treaties.size()))));

        for (Treaty treaty : treaties) {
            String statusKey = treaty.isActive() ? "treaty.status.active" :
                    treaty.isExpired() ? "treaty.status.expired" :
                            treaty.getStatus() == Treaty.TreatyStatus.PENDING ? "treaty.status.pending" : "treaty.status.unknown";
            String status = MessageUtils.get(statusKey).replace("{status}", treaty.getStatus().name());

            player.sendMessage(MessageUtils.format("treaty.list_item_name", java.util.Map.of("name", treaty.getName())));
            player.sendMessage(MessageUtils.format("treaty.list_item_details", java.util.Map.of("type", treaty.getType().name(), "status", status)));
            player.sendMessage(MessageUtils.format("treaty.list_item_between", java.util.Map.of("a", treaty.getKingdom1(), "b", treaty.getKingdom2())));
        }

        player.sendMessage(MessageUtils.get("treaty.list_footer"));

        return true;
    }

    private boolean handleBreak(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("treaty.break_usage"));
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
            player.sendMessage(MessageUtils.get("treaty.not_found"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!treaty.getKingdom1().equals(kingdom.getName()) && !treaty.getKingdom2().equals(kingdom.getName())) {
            player.sendMessage(MessageUtils.get("treaty.not_involved"));
            return true;
        }

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("treaty.only_capital_accept"));
            return true;
        }

        if (diplomacyService.violateTreaty(treaty.getName(), kingdom.getName())) {
            player.sendMessage(MessageUtils.format("treaty.break_success", java.util.Map.of("name", treaty.getName())));
            player.sendMessage(MessageUtils.get("treaty.break_penalty"));

            // Notify the other kingdom
            String otherKingdomName = treaty.getOtherKingdom(kingdom.getName());
            Kingdom otherKingdom = societiesManager.getKingdom(otherKingdomName);
            if (otherKingdom != null) {
                Town otherCapital = societiesManager.getTown(otherKingdom.getCapital());
                if (otherCapital != null) {
                    Player otherKing = plugin.getServer().getPlayer(otherCapital.getMayor());
                    if (otherKing != null) {
                        otherKing.sendMessage(MessageUtils.format("treaty.notify_proposed", java.util.Map.of("from", kingdom.getName())).replace("{from}", kingdom.getName()) + " " + MessageUtils.format("treaty.break_success", java.util.Map.of("name", treaty.getName())));
                    }
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("treaty.break_failed"));
        }

        return true;
    }

    private boolean handleRenew(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtils.get("treaty.renew_usage"));
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
            player.sendMessage(MessageUtils.get("treaty.not_found"));
            return true;
        }

        long days;
        try {
            days = Long.parseLong(args[2]);
            if (days < 1 || days > 365) {
                player.sendMessage(MessageUtils.get("treaty.duration_invalid"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtils.get("treaty.invalid_days"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("treaty.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("treaty.only_capital_accept"));
            return true;
        }

        long newExpiry = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000);
        treaty.setExpiresAt(newExpiry);
        treaty.setStatus(Treaty.TreatyStatus.ACTIVE);

        player.sendMessage(MessageUtils.format("treaty.renew_success", java.util.Map.of("name", treaty.getName())));
        player.sendMessage(MessageUtils.format("treaty.renew_new_expiry", java.util.Map.of("days", String.valueOf(days))));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("treaty.info_header").replace("Treaty Info", "Treaty Commands"));
        player.sendMessage(MessageUtils.get("treaty.usage_create"));
        player.sendMessage(MessageUtils.get("treaty.accept_usage"));
        player.sendMessage(MessageUtils.get("treaty.info_header"));
        player.sendMessage(MessageUtils.get("treaty.list_header"));
        player.sendMessage(MessageUtils.get("treaty.break_usage"));
        player.sendMessage(MessageUtils.get("treaty.renew_usage"));
        player.sendMessage(MessageUtils.get("treaty.info_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.realms.utils.TabCompletionUtils.match(Arrays.asList("create", "accept", "info", "list", "break", "renew"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                return me.marcdoesntexists.realms.utils.TabCompletionUtils.kingdoms(societiesManager, args[1]);
            }

            if (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("info") ||
                    args[0].equalsIgnoreCase("break") || args[0].equalsIgnoreCase("renew")) {
                return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(
                        societiesManager.getAllTreaties().stream().map(Treaty::getName).collect(java.util.stream.Collectors.toList()), args[1]
                );
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return me.marcdoesntexists.realms.utils.TabCompletionUtils.match(Arrays.asList("PEACE", "TRADE", "NON_AGGRESSION", "MUTUAL_DEFENSE", "NEUTRALITY"), args[2]);
        }

        return new ArrayList<>();
    }
}