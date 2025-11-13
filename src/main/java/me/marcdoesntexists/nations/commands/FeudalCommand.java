package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.economy.EconomyService;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.FeudalService;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class FeudalCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final FeudalService feudalService;
    private final EconomyService economyService;

    public FeudalCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.feudalService = plugin.getFeudalService();
        this.economyService = EconomyService.getInstance();
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
            case "vassal":
                return handleVassal(player, args);
            case "tribute":
                return handleTribute(player, args);
            case "rebel":
                return handleRebel(player, args);
            case "independence":
                return handleIndependence(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleVassal(Player player, String[] args) {
        // /feudal vassal <kingdom>
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("feudal.usage_vassal"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("kingdom.only_capital_mayor"));
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
            player.sendMessage(MessageUtils.get("feudal.already_vassal").replace("{suzerain}", targetKingdom.getSuzerain()));
            return true;
        }

        // Calculate tribute amount (10% of target kingdom balance)
        double tributeAmount = targetKingdom.getBalance() * 0.10;
        String obligations = "Military support in wars, tribute payment";

        if (feudalService.createFeudalRelationship(kingdom.getName(), targetKingdom.getName(), tributeAmount, obligations)) {
            player.sendMessage(MessageUtils.format("feudal.vassal_success", Map.of("target", targetKingdomName)));
            player.sendMessage(MessageUtils.format("feudal.vassal_tribute", Map.of("amount", String.valueOf((int) tributeAmount))));
            player.sendMessage(MessageUtils.format("feudal.vassal_obligations", Map.of("obligations", obligations)));

            // Notify vassal
            Town vassalCapital = societiesManager.getTown(targetKingdom.getCapital());
            if (vassalCapital != null) {
                Player vassalKing = plugin.getServer().getPlayer(vassalCapital.getMayor());
                if (vassalKing != null) {
                    vassalKing.sendMessage(MessageUtils.format("feudal.vassal_notify", Map.of("kingdom", kingdom.getName())));
                    vassalKing.sendMessage(MessageUtils.format("feudal.vassal_tribute", Map.of("amount", String.valueOf((int) tributeAmount))));
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("feudal.create_failed"));
        }

        return true;
    }

    private boolean handleTribute(Player player, String[] args) {
        // /feudal tribute <pay|set> [amount]
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("feudal.usage_tribute"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("feudal.only_capital_mayor"));
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("pay")) {
            if (kingdom.getSuzerain() == null) {
                player.sendMessage(MessageUtils.get("feudal.not_vassal"));
                return true;
            }

            double amount = kingdom.getTributeAmount();
            if (amount <= 0) {
                player.sendMessage(MessageUtils.get("feudal.no_tribute_set"));
                return true;
            }

            if (feudalService.payTribute(kingdom.getName(), kingdom.getSuzerain(), amount)) {
                player.sendMessage(MessageUtils.format("feudal.paid_tribute", Map.of("amount", String.valueOf((int) amount), "to", kingdom.getSuzerain())));

                // Persist kingdom (its treasury changed)
                try {
                    plugin.getDataManager().saveKingdom(kingdom);
                } catch (Throwable ignored) {
                }

                // Notify suzerain
                Kingdom suzerain = societiesManager.getKingdom(kingdom.getSuzerain());
                if (suzerain != null) {
                    Town suzCapital = societiesManager.getTown(suzerain.getCapital());
                    if (suzCapital != null) {
                        Player suzKing = plugin.getServer().getPlayer(suzCapital.getMayor());
                        if (suzKing != null) {
                            suzKing.sendMessage(MessageUtils.format("feudal.suzerain_notify", Map.of("kingdom", kingdom.getName(), "amount", String.valueOf((int) amount))));
                        }
                    }
                }
            } else {
                player.sendMessage(MessageUtils.get("feudal.pay_failed_insufficient"));
            }

        } else if (action.equals("set")) {
            if (args.length < 3) {
                player.sendMessage(MessageUtils.get("feudal.usage_tribute_set"));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 0) {
                    player.sendMessage(MessageUtils.get("feudal.amount_positive"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(MessageUtils.get("commands.invalid_number"));
                return true;
            }

            kingdom.setTributeAmount(amount);
            player.sendMessage(MessageUtils.format("feudal.tribute_set_success", Map.of("amount", String.valueOf(amount))));

        } else {
            player.sendMessage(MessageUtils.get("feudal.usage_tribute"));
        }

        return true;
    }

    private boolean handleRebel(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("feudal.only_capital_mayor"));
            return true;
        }

        if (kingdom.getSuzerain() == null) {
            player.sendMessage(MessageUtils.get("feudal.not_vassal"));
            return true;
        }

        String suzerainName = kingdom.getSuzerain();

        player.sendMessage(MessageUtils.format("feudal.rebel_attempt", Map.of("suzerain", suzerainName)));

        if (feudalService.betrayVassal(kingdom.getName(), suzerainName)) {
            player.sendMessage(MessageUtils.get("feudal.rebel_success"));
            player.sendMessage(MessageUtils.format("feudal.rebel_no_longer_vassal", Map.of("suzerain", suzerainName)));

            // Notify suzerain
            Kingdom suzerain = societiesManager.getKingdom(suzerainName);
            if (suzerain != null) {
                Town suzCapital = societiesManager.getTown(suzerain.getCapital());
                if (suzCapital != null) {
                    Player suzKing = plugin.getServer().getPlayer(suzCapital.getMayor());
                    if (suzKing != null) {
                        suzKing.sendMessage(MessageUtils.format("feudal.rebel_notify", Map.of("kingdom", kingdom.getName())));
                        suzKing.sendMessage(MessageUtils.get("feudal.rebel_notify_removed"));
                    }
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("feudal.rebel_failed"));
            player.sendMessage(MessageUtils.format("feudal.rebel_failed_status", Map.of("suzerain", suzerainName)));
            player.sendMessage(MessageUtils.get("feudal.rebel_failed_penalties"));
        }

        return true;
    }

    private boolean handleIndependence(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        if (!kingdom.isKing(town.getName()) || !town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("feudal.only_capital_mayor"));
            return true;
        }

        if (kingdom.getSuzerain() == null) {
            player.sendMessage(MessageUtils.get("feudal.already_independent"));
            return true;
        }

        String suzerainName = kingdom.getSuzerain();

        // Independence costs money (peaceful breakaway)
        int cost = 50000;

        // First try to charge the player directly (external economy)
        boolean chargedPlayer = false;
        if (economyService != null) {
            chargedPlayer = economyService.withdrawFromPlayer(player.getUniqueId(), cost);
        }

        if (!chargedPlayer) {
            // fallback to kingdom treasury
            if (kingdom.getBalance() < cost) {
                player.sendMessage(MessageUtils.format("feudal.insufficient_funds", Map.of("cost", String.valueOf(cost), "balance", String.valueOf(kingdom.getBalance()))));
                player.sendMessage(MessageUtils.get("feudal.rebel_tip"));
                return true;
            }

            kingdom.removeMoney(cost);
            // persist kingdom immediately
            try {
                plugin.getDataManager().saveKingdom(kingdom);
            } catch (Throwable ignored) {
            }
        }

        boolean endOk = feudalService.endFeudalRelationship(kingdom.getName(), suzerainName);

        if (endOk) {
            player.sendMessage(MessageUtils.get("feudal.independence_success"));
            player.sendMessage(MessageUtils.format("feudal.independence_cost", Map.of("cost", String.valueOf(cost))));

            // Persist kingdom after successful independence (state changed)
            try {
                plugin.getDataManager().saveKingdom(kingdom);
            } catch (Throwable ignored) {
            }

            // Notify suzerain
            Kingdom suzerain = societiesManager.getKingdom(suzerainName);
            if (suzerain != null) {
                Town suzCapital = societiesManager.getTown(suzerain.getCapital());
                if (suzCapital != null) {
                    Player suzKing = plugin.getServer().getPlayer(suzCapital.getMayor());
                    if (suzKing != null) {
                        suzKing.sendMessage(MessageUtils.format("feudal.suzerain_independence_notify", Map.of("kingdom", kingdom.getName(), "cost", String.valueOf(cost))));
                    }
                }
            }
        } else {
            // rollback if we charged player directly
            if (chargedPlayer) {
                economyService.depositToPlayer(player.getUniqueId(), cost);
                try {
                    plugin.getDataManager().savePlayerMoney(player.getUniqueId());
                } catch (Throwable ignored) {
                }
            } else {
                kingdom.addMoney(cost); // Refund
                try {
                    plugin.getDataManager().saveKingdom(kingdom);
                } catch (Throwable ignored) {
                }
            }
            player.sendMessage(MessageUtils.get("feudal.independence_failed"));
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("military.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("kingdom.must_be_in_town"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

        player.sendMessage(MessageUtils.get("feudal.info_header"));
        player.sendMessage(MessageUtils.format("feudal.info_kingdom", Map.of("kingdom", kingdom.getName())));

        if (kingdom.getSuzerain() != null) {
            player.sendMessage(MessageUtils.get("feudal.info_status_vassal"));
            player.sendMessage(MessageUtils.format("feudal.info_suzerain", Map.of("suzerain", kingdom.getSuzerain())));
            player.sendMessage(MessageUtils.format("feudal.info_tribute", Map.of("amount", String.valueOf(kingdom.getTributeAmount()))));
        } else {
            player.sendMessage(MessageUtils.get("feudal.info_status_independent"));
        }

        if (!kingdom.getVassals().isEmpty()) {
            player.sendMessage(MessageUtils.format("feudal.info_vassals", Map.of("count", String.valueOf(kingdom.getVassals().size()))));
            for (String vassal : kingdom.getVassals()) {
                Kingdom vassalKingdom = societiesManager.getKingdom(vassal);
                if (vassalKingdom != null) {
                    player.sendMessage(MessageUtils.format("feudal.info_vassal_item", Map.of("vassal", vassal, "tribute", String.valueOf(vassalKingdom.getTributeAmount()))));
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("feudal.info_vassals_none"));
        }

        player.sendMessage(MessageUtils.get("feudal.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Kingdom> kingdoms = societiesManager.getAllKingdoms();

        int vassalCount = 0;
        int independentCount = 0;

        for (Kingdom k : kingdoms) {
            if (k.getSuzerain() != null) {
                vassalCount++;
            } else {
                independentCount++;
            }
        }

        player.sendMessage(MessageUtils.get("feudal.list_header"));
        player.sendMessage(MessageUtils.format("feudal.list_total", Map.of("total", String.valueOf(kingdoms.size()))));
        player.sendMessage(MessageUtils.format("feudal.list_independent", Map.of("count", String.valueOf(independentCount))));
        player.sendMessage(MessageUtils.format("feudal.list_vassals", Map.of("count", String.valueOf(vassalCount))));
        player.sendMessage(MessageUtils.get("general.empty"));

        player.sendMessage(MessageUtils.get("feudal.list_major_powers_header"));
        for (Kingdom k : kingdoms) {
            if (k.getSuzerain() == null && !k.getVassals().isEmpty()) {
                player.sendMessage(MessageUtils.format("feudal.list_major_power_item", Map.of("name", k.getName(), "count", String.valueOf(k.getVassals().size()))));
            }
        }

        player.sendMessage(MessageUtils.get("feudal.list_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("feudal.help_header"));
        player.sendMessage(MessageUtils.get("feudal.help_vassal"));
        player.sendMessage(MessageUtils.get("feudal.help_tribute_pay"));
        player.sendMessage(MessageUtils.get("feudal.help_tribute_set"));
        player.sendMessage(MessageUtils.get("feudal.help_rebel"));
        player.sendMessage(MessageUtils.get("feudal.help_independence"));
        player.sendMessage(MessageUtils.get("feudal.help_info"));
        player.sendMessage(MessageUtils.get("feudal.help_list"));
        player.sendMessage(MessageUtils.get("feudal.help_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(
                    Arrays.asList("vassal", "tribute", "rebel", "independence", "info", "list"),
                    args[0]
            );
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("vassal")) {
                return me.marcdoesntexists.nations.utils.TabCompletionUtils.kingdoms(societiesManager, args[1]);
            }

            if (args[0].equalsIgnoreCase("tribute")) {
                return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(Arrays.asList("pay", "set"), args[1]);
            }
        }

        return new ArrayList<>();
    }
}