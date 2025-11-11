package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SettlementEvolutionManager;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EvolveCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final SettlementEvolutionManager evolutionManager;

    public EvolveCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.evolutionManager = plugin.getEvolutionManager();
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
            case "check":
                return handleCheck(player, args);
            case "kingdom":
                return handleKingdom(player, args);
            case "empire":
                return handleEmpire(player, args);
            case "info":
                return handleInfo(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCheck(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can check evolution requirements!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Evolution Check§7 §m----------");

        // Check Town -> Kingdom
        if (town.getKingdom() == null) {
            player.sendMessage("§e§lTown → Kingdom Requirements:");

            int minClaims = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.town-to-kingdom.minimum-claims", 100);
            int minPop = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.town-to-kingdom.minimum-population", 10);
            int minTreasury = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.town-to-kingdom.minimum-treasury", 50000);

            boolean hasClaims = town.getClaims().size() >= minClaims;
            boolean hasPop = town.getMembers().size() >= minPop;
            boolean hasFunds = town.getBalance() >= minTreasury;

            player.sendMessage((hasClaims ? "§a✔" : "§c✘") + " §7Claims: §e" + town.getClaims().size() + "§7/§6" + minClaims);
            player.sendMessage((hasPop ? "§a✔" : "§c✘") + " §7Population: §e" + town.getMembers().size() + "§7/§6" + minPop);
            player.sendMessage((hasFunds ? "§a✔" : "§c✘") + " §7Treasury: §e$" + town.getBalance() + "§7/§6$" + minTreasury);

            boolean canEvolve = evolutionManager.canEvolveToKingdom(town);

            if (canEvolve) {
                player.sendMessage("");
                player.sendMessage("§a✔ Your town can evolve to a Kingdom!");
                player.sendMessage("§7Use §e/evolve kingdom <name>§7 to evolve!");
            } else {
                player.sendMessage("");
                player.sendMessage("§c✘ Requirements not met yet!");
            }
        } else {
            // Check Kingdom -> Empire
            Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

            if (!kingdom.isKing(town.getName())) {
                player.sendMessage("§cOnly the capital can check kingdom evolution!");
                return true;
            }

            player.sendMessage("§e§lKingdom → Empire Requirements:");

            int minClaims = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.kingdom-to-empire.minimum-claims", 500);
            int minPop = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.kingdom-to-empire.minimum-population", 50);
            int minTreasury = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.kingdom-to-empire.minimum-treasury", 250000);
            int minVassals = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.kingdom-to-empire.minimum-vassal-kingdoms", 2);

            int totalClaims = 0;
            int totalPop = 0;
            for (String townName : kingdom.getTowns()) {
                Town t = societiesManager.getTown(townName);
                if (t != null) {
                    totalClaims += t.getClaims().size();
                    totalPop += t.getMembers().size();
                }
            }

            boolean hasClaims = totalClaims >= minClaims;
            boolean hasPop = totalPop >= minPop;
            boolean hasFunds = true; // Placeholder
            boolean hasVassals = kingdom.getVassals().size() >= minVassals;

            player.sendMessage((hasClaims ? "§a✔" : "§c✘") + " §7Total Claims: §e" + totalClaims + "§7/§6" + minClaims);
            player.sendMessage((hasPop ? "§a✔" : "§c✘") + " §7Total Population: §e" + totalPop + "§7/§6" + minPop);
            player.sendMessage((hasVassals ? "§a✔" : "§c✘") + " §7Vassal Kingdoms: §e" + kingdom.getVassals().size() + "§7/§6" + minVassals);

            boolean canEvolve = evolutionManager.canEvolveToEmpire(kingdom);

            if (canEvolve) {
                player.sendMessage("");
                player.sendMessage("§a✔ Your kingdom can evolve to an Empire!");
                player.sendMessage("§7Use §e/evolve empire <name>§7 to evolve!");
            } else {
                player.sendMessage("");
                player.sendMessage("§c✘ Requirements not met yet!");
            }
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleKingdom(Player player, String[] args) {
        // /evolve kingdom <name>
        if (args.length < 2) {
            player.sendMessage("§cUsage: /evolve kingdom <name>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town!");
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cOnly the mayor can evolve the town!");
            return true;
        }

        if (town.getKingdom() != null) {
            player.sendMessage("§cYour town is already part of a kingdom!");
            return true;
        }

        if (!evolutionManager.canEvolveToKingdom(town)) {
            player.sendMessage("§cYour town doesn't meet the requirements!");
            player.sendMessage("§7Use §e/evolve check§7 to see requirements");
            return true;
        }

        String kingdomName = args[1];

        if (societiesManager.getKingdom(kingdomName) != null) {
            player.sendMessage("§cA kingdom with this name already exists!");
            return true;
        }

        if (evolutionManager.evolveToKingdom(town, kingdomName)) {
            player.sendMessage("§a§l✔ EVOLUTION SUCCESSFUL!");
            player.sendMessage("");
            player.sendMessage("§6" + town.getName() + " §7has evolved into the Kingdom of §6" + kingdomName + "§7!");
            player.sendMessage("");
            player.sendMessage("§eYour town is now the capital of the kingdom!");
            player.sendMessage("§7You can now:");
            player.sendMessage("§7 • Invite other towns to join: §e/kingdom invite <town>");
            player.sendMessage("§7 • Declare wars: §e/war declare <kingdom>");
            player.sendMessage("§7 • Form alliances: §e/alliance create <name>");
            player.sendMessage("§7 • Take vassals: §e/feudal vassal <kingdom>");

            // Broadcast to all town members
            for (UUID memberId : town.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null && !member.equals(player)) {
                    member.sendMessage("§a§l✔ EVOLUTION!");
                    member.sendMessage("§7Your town has evolved into the Kingdom of §6" + kingdomName + "§7!");
                }
            }
        } else {
            player.sendMessage("§cFailed to evolve to kingdom!");
        }

        return true;
    }

    private boolean handleEmpire(Player player, String[] args) {
        // /evolve empire <name>
        if (args.length < 2) {
            player.sendMessage("§cUsage: /evolve empire <name>");
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
            player.sendMessage("§cOnly the capital's mayor can evolve the kingdom!");
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor!");
            return true;
        }

        if (kingdom.getEmpire() != null) {
            player.sendMessage("§cYour kingdom is already part of an empire!");
            return true;
        }

        if (!evolutionManager.canEvolveToEmpire(kingdom)) {
            player.sendMessage("§cYour kingdom doesn't meet the requirements!");
            player.sendMessage("§7Use §e/evolve check§7 to see requirements");
            return true;
        }

        String empireName = args[1];

        if (societiesManager.getEmpire(empireName) != null) {
            player.sendMessage("§cAn empire with this name already exists!");
            return true;
        }

        if (evolutionManager.evolveToEmpire(kingdom, empireName)) {
            player.sendMessage("§d§l✔ ASCENSION!");
            player.sendMessage("");
            player.sendMessage("§6" + kingdom.getName() + " §7has ascended to become the §d" + empireName + " §7Empire!");
            player.sendMessage("");
            player.sendMessage("§eYour kingdom is now the ruling kingdom of the empire!");
            player.sendMessage("§7The empire now spans:");

            int totalTowns = 0;
            int totalPop = 0;
            for (String townName : kingdom.getTowns()) {
                Town t = societiesManager.getTown(townName);
                if (t != null) {
                    totalTowns++;
                    totalPop += t.getMembers().size();
                }
            }

            player.sendMessage("§7 • §6" + totalTowns + "§7 towns");
            player.sendMessage("§7 • §6" + totalPop + "§7 citizens");
            player.sendMessage("§7 • §6" + kingdom.getVassals().size() + "§7 vassal kingdoms");

            // Broadcast to all kingdom
            for (String townName : kingdom.getTowns()) {
                Town t = societiesManager.getTown(townName);
                if (t != null) {
                    for (UUID memberId : t.getMembers()) {
                        Player member = plugin.getServer().getPlayer(memberId);
                        if (member != null && !member.equals(player)) {
                            member.sendMessage("§d§l✔ ASCENSION!");
                            member.sendMessage("§7Your kingdom has become the §d" + empireName + " §7Empire!");
                        }
                    }
                }
            }
        } else {
            player.sendMessage("§cFailed to evolve to empire!");
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        player.sendMessage("§7§m----------§r §6Evolution Info§7 §m----------");
        player.sendMessage("");
        player.sendMessage("§e§lSettlement Hierarchy:");
        player.sendMessage("§71. §6Town §7→ Basic settlement");
        player.sendMessage("§72. §6Kingdom §7→ Town + vassals + warfare");
        player.sendMessage("§73. §dEmpire §7→ Kingdom + multiple kingdoms");
        player.sendMessage("");
        player.sendMessage("§e§lEvolution Benefits:");
        player.sendMessage("§7• §aIncreased claim limits");
        player.sendMessage("§7• §aNew diplomatic options");
        player.sendMessage("§7• §aVassal management");
        player.sendMessage("§7• §aWarfare capabilities");
        player.sendMessage("§7• §aEmpire-wide bonuses");
        player.sendMessage("");
        player.sendMessage("§7Use §e/evolve check§7 to see your requirements!");
        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Evolution Commands§7 §m----------");
        player.sendMessage("§e/evolve check§7 - Check evolution requirements");
        player.sendMessage("§e/evolve kingdom <name>§7 - Evolve to Kingdom");
        player.sendMessage("§e/evolve empire <name>§7 - Evolve to Empire");
        player.sendMessage("§e/evolve info§7 - View evolution system info");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("check", "kingdom", "empire", "info")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}