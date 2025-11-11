package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FeudalCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final FeudalService feudalService;

    public FeudalCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.feudalService = plugin.getFeudalService();
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
            player.sendMessage("§cUsage: /feudal vassal <kingdom>");
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
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage("§cOnly the capital's mayor can manage vassals!");
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
            player.sendMessage("§cYou cannot vassalize your own kingdom!");
            return true;
        }

        if (targetKingdom.getSuzerain() != null) {
            player.sendMessage("§cThat kingdom is already a vassal of §6" + targetKingdom.getSuzerain() + "§c!");
            return true;
        }

        // Calculate tribute amount (10% of target kingdom balance)
        double tributeAmount = targetKingdom.getBalance() * 0.10;
        String obligations = "Military support in wars, tribute payment";

        if (feudalService.createFeudalRelationship(kingdom.getName(), targetKingdom.getName(), tributeAmount, obligations)) {
            player.sendMessage("§a✔ §6" + targetKingdomName + "§a is now your vassal!");
            player.sendMessage("§7Tribute: §6$" + (int) tributeAmount + "§7/week");
            player.sendMessage("§7Obligations: §e" + obligations);

            // Notify vassal
            Town vassalCapital = societiesManager.getTown(targetKingdom.getCapital());
            if (vassalCapital != null) {
                Player vassalKing = plugin.getServer().getPlayer(vassalCapital.getMayor());
                if (vassalKing != null) {
                    vassalKing.sendMessage("§7[§6Feudal§7] §eYour kingdom is now a vassal of §6" + kingdom.getName());
                    vassalKing.sendMessage("§7Tribute: §6$" + (int) tributeAmount + "§7/week");
                }
            }
        } else {
            player.sendMessage("§cFailed to create feudal relationship!");
        }

        return true;
    }

    private boolean handleTribute(Player player, String[] args) {
        // /feudal tribute <pay|set> [amount]
        if (args.length < 2) {
            player.sendMessage("§cUsage: /feudal tribute <pay|set> [amount]");
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
            player.sendMessage("§cOnly the capital's mayor can manage tribute!");
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("pay")) {
            if (kingdom.getSuzerain() == null) {
                player.sendMessage("§cYour kingdom is not a vassal!");
                return true;
            }

            double amount = kingdom.getTributeAmount();
            if (amount <= 0) {
                player.sendMessage("§cNo tribute amount set!");
                return true;
            }

            if (feudalService.payTribute(kingdom.getName(), kingdom.getSuzerain(), amount)) {
                player.sendMessage("§a✔ Paid tribute of §6$" + (int) amount + "§a to §6" + kingdom.getSuzerain());

                // Notify suzerain
                Kingdom suzerain = societiesManager.getKingdom(kingdom.getSuzerain());
                if (suzerain != null) {
                    Town suzCapital = societiesManager.getTown(suzerain.getCapital());
                    if (suzCapital != null) {
                        Player suzKing = plugin.getServer().getPlayer(suzCapital.getMayor());
                        if (suzKing != null) {
                            suzKing.sendMessage("§a✔ §6" + kingdom.getName() + "§a paid tribute: §6$" + (int) amount);
                        }
                    }
                }
            } else {
                player.sendMessage("§cFailed to pay tribute! Insufficient funds!");
            }

        } else if (action.equals("set")) {
            if (args.length < 3) {
                player.sendMessage("§cUsage: /feudal tribute set <amount>");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 0) {
                    player.sendMessage("§cAmount must be positive!");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid amount!");
                return true;
            }

            kingdom.setTributeAmount(amount);
            player.sendMessage("§a✔ Tribute amount set to §6$" + amount);

        } else {
            player.sendMessage("§cUsage: /feudal tribute <pay|set> [amount]");
        }

        return true;
    }

    private boolean handleRebel(Player player, String[] args) {
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
            player.sendMessage("§cOnly the capital's mayor can rebel!");
            return true;
        }

        if (kingdom.getSuzerain() == null) {
            player.sendMessage("§cYour kingdom is not a vassal!");
            return true;
        }

        String suzerainName = kingdom.getSuzerain();

        player.sendMessage("§c⚔ Attempting rebellion against §6" + suzerainName + "§c...");

        if (feudalService.betrayVassal(kingdom.getName(), suzerainName)) {
            player.sendMessage("§a✔ Rebellion successful! Your kingdom is now independent!");
            player.sendMessage("§7You are no longer a vassal of §6" + suzerainName);

            // Notify suzerain
            Kingdom suzerain = societiesManager.getKingdom(suzerainName);
            if (suzerain != null) {
                Town suzCapital = societiesManager.getTown(suzerain.getCapital());
                if (suzCapital != null) {
                    Player suzKing = plugin.getServer().getPlayer(suzCapital.getMayor());
                    if (suzKing != null) {
                        suzKing.sendMessage("§c⚔ §6" + kingdom.getName() + "§c has successfully rebelled!");
                        suzKing.sendMessage("§7They are no longer your vassal!");
                    }
                }
            }
        } else {
            player.sendMessage("§c✘ Rebellion failed!");
            player.sendMessage("§7Your kingdom remains a vassal of §6" + suzerainName);
            player.sendMessage("§7Diplomatic penalties may apply!");
        }

        return true;
    }

    private boolean handleIndependence(Player player, String[] args) {
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
            player.sendMessage("§cOnly the capital's mayor can declare independence!");
            return true;
        }

        if (kingdom.getSuzerain() == null) {
            player.sendMessage("§cYour kingdom is already independent!");
            return true;
        }

        String suzerainName = kingdom.getSuzerain();

        // Independence costs money (peaceful breakaway)
        int cost = 50000;
        if (kingdom.getBalance() < cost) {
            player.sendMessage("§cInsufficient funds! Need §6$" + cost + "§c for peaceful independence!");
            player.sendMessage("§7Current balance: §6$" + kingdom.getBalance());
            player.sendMessage("§7Tip: Use §e/feudal rebel§7 for a risky free alternative!");
            return true;
        }

        kingdom.removeMoney(cost);

        if (feudalService.endFeudalRelationship(kingdom.getName(), suzerainName)) {
            player.sendMessage("§a✔ Your kingdom is now independent!");
            player.sendMessage("§7Cost: §6$" + cost);

            // Notify suzerain
            Kingdom suzerain = societiesManager.getKingdom(suzerainName);
            if (suzerain != null) {
                Town suzCapital = societiesManager.getTown(suzerain.getCapital());
                if (suzCapital != null) {
                    Player suzKing = plugin.getServer().getPlayer(suzCapital.getMayor());
                    if (suzKing != null) {
                        suzKing.sendMessage("§7[§6Feudal§7] §6" + kingdom.getName() + "§7 has peacefully gained independence");
                        suzKing.sendMessage("§7They paid §6$" + cost + "§7 for their freedom");
                    }
                }
            }
        } else {
            kingdom.addMoney(cost); // Refund
            player.sendMessage("§cFailed to gain independence!");
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
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

        player.sendMessage("§7§m----------§r §6Feudal Info§7 §m----------");
        player.sendMessage("§eKingdom: §6" + kingdom.getName());

        if (kingdom.getSuzerain() != null) {
            player.sendMessage("§eStatus: §cVassal");
            player.sendMessage("§eSuzerain: §6" + kingdom.getSuzerain());
            player.sendMessage("§eTribute: §6$" + kingdom.getTributeAmount() + "§7/week");
        } else {
            player.sendMessage("§eStatus: §aIndependent");
        }

        if (!kingdom.getVassals().isEmpty()) {
            player.sendMessage("§eVassals: §6" + kingdom.getVassals().size());
            for (String vassal : kingdom.getVassals()) {
                Kingdom vassalKingdom = societiesManager.getKingdom(vassal);
                if (vassalKingdom != null) {
                    player.sendMessage("§7  • §e" + vassal + " §7(Tribute: §6$" + vassalKingdom.getTributeAmount() + "§7/week)");
                }
            }
        } else {
            player.sendMessage("§eVassals: §7None");
        }

        player.sendMessage("§7§m--------------------------------");

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

        player.sendMessage("§7§m----------§r §6Feudal System§7 §m----------");
        player.sendMessage("§eTotal Kingdoms: §6" + kingdoms.size());
        player.sendMessage("§eIndependent: §a" + independentCount);
        player.sendMessage("§eVassals: §c" + vassalCount);
        player.sendMessage("");

        // Show independent kingdoms with vassals
        player.sendMessage("§6Major Powers:");
        for (Kingdom k : kingdoms) {
            if (k.getSuzerain() == null && !k.getVassals().isEmpty()) {
                player.sendMessage("§e• §6" + k.getName() + " §7- Vassals: §e" + k.getVassals().size());
            }
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Feudal Commands§7 §m----------");
        player.sendMessage("§e/feudal vassal <kingdom>§7 - Make kingdom vassal");
        player.sendMessage("§e/feudal tribute pay§7 - Pay tribute to suzerain");
        player.sendMessage("§e/feudal tribute set <amount>§7 - Set tribute amount");
        player.sendMessage("§e/feudal rebel§7 - Attempt rebellion (risky)");
        player.sendMessage("§e/feudal independence§7 - Buy independence (costly)");
        player.sendMessage("§e/feudal info§7 - View feudal status");
        player.sendMessage("§e/feudal list§7 - List feudal system");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("vassal", "tribute", "rebel", "independence", "info", "list")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("vassal")) {
                return societiesManager.getAllKingdoms().stream()
                        .map(Kingdom::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("tribute")) {
                return Arrays.asList("pay", "set")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}