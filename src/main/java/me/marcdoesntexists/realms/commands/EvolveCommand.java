package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.managers.SettlementEvolutionManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Kingdom;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class EvolveCommand implements CommandExecutor, TabCompleter {

    private final Realms plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final SettlementEvolutionManager evolutionManager;

    public EvolveCommand(Realms plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.evolutionManager = plugin.getEvolutionManager();
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
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("evolution.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("evolution.only_mayor"));
            return true;
        }

        player.sendMessage(MessageUtils.get("evolution.evolution_check_header"));

        // Check Town -> Kingdom
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("evolution.town_to_kingdom_header"));

            int minClaims = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.town-to-kingdom.minimum-claims", 100);
            int minPop = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.town-to-kingdom.minimum-population", 10);
            int minTreasury = plugin.getConfigurationManager().getSettlementsConfig()
                    .getInt("settlement-evolution.town-to-kingdom.minimum-treasury", 50000);

            boolean hasClaims = town.getClaims().size() >= minClaims;
            boolean hasPop = town.getMembers().size() >= minPop;
            boolean hasFunds = town.getBalance() >= minTreasury;

            player.sendMessage((hasClaims ? MessageUtils.get("evolution.requirement_met") : MessageUtils.get("evolution.requirement_unmet")).replace("{label}", "Claims: §e" + town.getClaims().size() + "§7/§6" + minClaims));
            player.sendMessage((hasPop ? MessageUtils.get("evolution.requirement_met") : MessageUtils.get("evolution.requirement_unmet")).replace("{label}", "Population: §e" + town.getMembers().size() + "§7/§6" + minPop));
            player.sendMessage((hasFunds ? MessageUtils.get("evolution.requirement_met") : MessageUtils.get("evolution.requirement_unmet")).replace("{label}", "Treasury: §e$" + town.getBalance() + "§7/§6$" + minTreasury));

            boolean canEvolve = evolutionManager.canEvolveToKingdom(town);

            if (canEvolve) {
                player.sendMessage(MessageUtils.get("general.empty"));
                player.sendMessage(MessageUtils.get("evolution.can_evolve_town"));
                player.sendMessage(MessageUtils.get("evolution.evolve_use_kingdom"));
            } else {
                player.sendMessage(MessageUtils.get("general.empty"));
                player.sendMessage(MessageUtils.get("evolution.requirement_unmet").replace("{label}", "Requirements not met yet!"));
            }
        } else {
            // Check Kingdom -> Empire
            Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());

            if (!kingdom.isKing(town.getName())) {
                player.sendMessage("§cOnly the capital can check kingdom evolution!");
                return true;
            }

            player.sendMessage(MessageUtils.get("evolution.kingdom_to_empire_header"));

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

            player.sendMessage((hasClaims ? MessageUtils.get("evolution.requirement_met") : MessageUtils.get("evolution.requirement_unmet")).replace("{label}", "Total Claims: §e" + totalClaims + "§7/§6" + minClaims));
            player.sendMessage((hasPop ? MessageUtils.get("evolution.requirement_met") : MessageUtils.get("evolution.requirement_unmet")).replace("{label}", "Total Population: §e" + totalPop + "§7/§6" + minPop));
            player.sendMessage((hasVassals ? MessageUtils.get("evolution.requirement_met") : MessageUtils.get("evolution.requirement_unmet")).replace("{label}", "Vassal Kingdoms: §e" + kingdom.getVassals().size() + "§7/§6" + minVassals));

            boolean canEvolve = evolutionManager.canEvolveToEmpire(kingdom);

            if (canEvolve) {
                player.sendMessage(MessageUtils.get("general.empty"));
                player.sendMessage(MessageUtils.get("evolution.can_evolve_kingdom"));
                player.sendMessage(MessageUtils.get("evolution.evolve_use_empire"));
            } else {
                player.sendMessage(MessageUtils.get("general.empty"));
                player.sendMessage(MessageUtils.get("evolution.requirement_unmet").replace("{label}", "Requirements not met yet!"));
            }
        }

        player.sendMessage(MessageUtils.get("general.prefix") + " §7§m--------------------------------");

        return true;
    }

    private boolean handleKingdom(Player player, String[] args) {
        // /evolve kingdom <name>
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("evolution.usage_kingdom"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("evolution.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("evolution.only_mayor"));
            return true;
        }

        if (town.getKingdom() != null) {
            player.sendMessage(MessageUtils.get("evolution.town_already_in_kingdom"));
            return true;
        }

        if (!evolutionManager.canEvolveToKingdom(town)) {
            player.sendMessage(MessageUtils.get("evolution.town_not_meet_requirements"));
            player.sendMessage(MessageUtils.get("evolution.use_check_hint"));
            return true;
        }

        String kingdomName = args[1];

        if (societiesManager.getKingdom(kingdomName) != null) {
            player.sendMessage(MessageUtils.get("evolution.kingdom_already_exists"));
            return true;
        }

        if (evolutionManager.evolveToKingdom(town, kingdomName)) {
            player.sendMessage(MessageUtils.get("evolution.evolved_kingdom_success"));
            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.format("evolution.evolved_kingdom_announcement", Map.of("town", town.getName(), "kingdom", kingdomName)));
            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.get("evolution.evolved_kingdom_member_title"));
            player.sendMessage(MessageUtils.format("evolution.evolved_kingdom_member_message", Map.of("kingdom", kingdomName)));
            player.sendMessage(MessageUtils.get("general.empty"));
            // Capabilities - keep concise
            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", "", "label", "Invite other towns: /kingdom invite <town>")));
            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", "", "label", "Declare wars: /war declare <kingdom>")));
            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", "", "label", "Form alliances: /alliance create <name>")));
            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", "", "label", "Take vassals: /feudal vassal <kingdom>")));

            // Broadcast to all town members
            for (UUID memberId : town.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null && !member.equals(player)) {
                    member.sendMessage(MessageUtils.get("evolution.evolved_kingdom_member_title"));
                    member.sendMessage(MessageUtils.format("evolution.evolved_kingdom_member_message", Map.of("kingdom", kingdomName)));
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("evolution.failed_evolve_kingdom"));
        }

        return true;
    }

    private boolean handleEmpire(Player player, String[] args) {
        // /evolve empire <name>
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("evolution.usage_empire"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getTown() == null) {
            player.sendMessage(MessageUtils.get("evolution.must_be_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage(MessageUtils.get("war.must_be_in_kingdom"));
            return true;
        }

        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage(MessageUtils.get("evolution.only_mayor"));
            return true;
        }

        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("evolution.only_mayor"));
            return true;
        }

        if (kingdom.getEmpire() != null) {
            player.sendMessage(MessageUtils.get("evolution.kingdom_already_in_empire"));
            return true;
        }

        if (!evolutionManager.canEvolveToEmpire(kingdom)) {
            player.sendMessage(MessageUtils.get("evolution.kingdom_not_meet_requirements"));
            player.sendMessage(MessageUtils.get("evolution.use_check_hint"));
            return true;
        }

        String empireName = args[1];

        if (societiesManager.getEmpire(empireName) != null) {
            player.sendMessage(MessageUtils.get("evolution.empire_already_exists"));
            return true;
        }

        if (evolutionManager.evolveToEmpire(kingdom, empireName)) {
            player.sendMessage(MessageUtils.get("evolution.evolved_empire_success"));
            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.format("evolution.evolved_empire_announcement", Map.of("kingdom", kingdom.getName(), "empire", empireName)));
            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.get("evolution.evolved_empire_member_title"));
            player.sendMessage(MessageUtils.format("evolution.evolved_empire_member_message", Map.of("empire", empireName)));
            player.sendMessage(MessageUtils.get("general.empty"));
            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", "0", "label", "Towns/Pop/Vassals below")));
            player.sendMessage(MessageUtils.get("general.empty"));

            int totalTowns = 0;
            int totalPop = 0;
            for (String townName : kingdom.getTowns()) {
                Town t = societiesManager.getTown(townName);
                if (t != null) {
                    totalTowns++;
                    totalPop += t.getMembers().size();
                }
            }

            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", String.valueOf(totalTowns), "label", "towns")));
            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", String.valueOf(totalPop), "label", "citizens")));
            player.sendMessage(MessageUtils.format("evolution.evolved_info_list", Map.of("count", String.valueOf(kingdom.getVassals().size()), "label", "vassal kingdoms")));

            // Broadcast to all kingdom
            for (String townName : kingdom.getTowns()) {
                Town t = societiesManager.getTown(townName);
                if (t != null) {
                    for (UUID memberId : t.getMembers()) {
                        Player member = plugin.getServer().getPlayer(memberId);
                        if (member != null && !member.equals(player)) {
                            member.sendMessage(MessageUtils.get("evolution.evolved_empire_member_title"));
                            member.sendMessage(MessageUtils.format("evolution.evolved_empire_member_message", Map.of("empire", empireName)));
                        }
                    }
                }
            }
        } else {
            player.sendMessage(MessageUtils.get("evolution.failed_evolve_empire"));
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        player.sendMessage(MessageUtils.get("evolution.info_header"));
        player.sendMessage(MessageUtils.get("general.empty"));
        player.sendMessage(MessageUtils.get("evolution.info_hierarchy_title"));
        player.sendMessage(MessageUtils.get("evolution.info_hierarchy_1"));
        player.sendMessage(MessageUtils.get("evolution.info_hierarchy_2"));
        player.sendMessage(MessageUtils.get("evolution.info_hierarchy_3"));
        player.sendMessage(MessageUtils.get("general.empty"));
        player.sendMessage(MessageUtils.get("evolution.info_benefits_title"));
        player.sendMessage(MessageUtils.get("evolution.info_benefit_1"));
        player.sendMessage(MessageUtils.get("evolution.info_benefit_2"));
        player.sendMessage(MessageUtils.get("evolution.info_benefit_3"));
        player.sendMessage(MessageUtils.get("evolution.info_benefit_4"));
        player.sendMessage(MessageUtils.get("evolution.info_benefit_5"));
        player.sendMessage(MessageUtils.get("general.empty"));
        player.sendMessage(MessageUtils.get("evolution.info_check_hint"));
        player.sendMessage(MessageUtils.get("evolution.info_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("evolution.help_header"));
        player.sendMessage(MessageUtils.get("evolution.help_check"));
        player.sendMessage(MessageUtils.get("evolution.help_kingdom"));
        player.sendMessage(MessageUtils.get("evolution.help_empire"));
        player.sendMessage(MessageUtils.get("evolution.help_info"));
        player.sendMessage(MessageUtils.get("evolution.help_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.realms.utils.TabCompletionUtils.match(Arrays.asList("check", "kingdom", "empire", "info"), args[0]);
        }

        return new ArrayList<>();
    }
}