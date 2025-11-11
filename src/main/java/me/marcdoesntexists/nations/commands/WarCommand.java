package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.MilitaryManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.military.War;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
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
            player.sendMessage("§cUsage: /war declare <kingdom> [reason]");
            return true;
        }
        
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to declare war!");
            return true;
        }
        
        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom to declare war!");
            return true;
        }
        
        Kingdom attackerKingdom = societiesManager.getKingdom(town.getKingdom());
        if (!attackerKingdom.isKing(town.getName())) {
            player.sendMessage("§cOnly the capital's mayor can declare war!");
            return true;
        }
        
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor to declare war!");
            return true;
        }
        
        String targetKingdomName = args[1];
        Kingdom defenderKingdom = societiesManager.getKingdom(targetKingdomName);
        
        if (defenderKingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return true;
        }
        
        if (defenderKingdom.getName().equals(attackerKingdom.getName())) {
            player.sendMessage("§cYou cannot declare war on your own kingdom!");
            return true;
        }
        
        if (attackerKingdom.getWars().contains(targetKingdomName)) {
            player.sendMessage("§cYou are already at war with this kingdom!");
            return true;
        }
        
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason provided";
        
        War war = new War(attackerKingdom.getName(), defenderKingdom.getName(), player.getUniqueId(), reason);
        militaryManager.declareWar(war);
        
        attackerKingdom.declareWar(defenderKingdom.getName());
        defenderKingdom.declareWar(attackerKingdom.getName());
        
        attackerKingdom.addEnemy(defenderKingdom.getName());
        defenderKingdom.addEnemy(attackerKingdom.getName());
        
        player.sendMessage("§c⚔ War declared against §4" + defenderKingdom.getName() + "§c!");
        player.sendMessage("§7Reason: §e" + reason);
        
        Town defenderCapital = societiesManager.getTown(defenderKingdom.getCapital());
        if (defenderCapital != null) {
            Player defenderKing = plugin.getServer().getPlayer(defenderCapital.getMayor());
            if (defenderKing != null) {
                defenderKing.sendMessage("§c⚔ §4" + attackerKingdom.getName() + "§c has declared war on your kingdom!");
                defenderKing.sendMessage("§7Reason: §e" + reason);
            }
        }
        
        return true;
    }
    
    private boolean handleEnd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /war end <kingdom>");
            return true;
        }
        
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to end a war!");
            return true;
        }
        
        Town town = societiesManager.getTown(data.getTown());
        if (town.getKingdom() == null) {
            player.sendMessage("§cYour town must be part of a kingdom to end a war!");
            return true;
        }
        
        Kingdom kingdom = societiesManager.getKingdom(town.getKingdom());
        if (!kingdom.isKing(town.getName())) {
            player.sendMessage("§cOnly the capital's mayor can end a war!");
            return true;
        }
        
        if (!town.isMayor(player.getUniqueId())) {
            player.sendMessage("§cYou must be the mayor to end a war!");
            return true;
        }
        
        String targetKingdomName = args[1];
        Kingdom targetKingdom = societiesManager.getKingdom(targetKingdomName);
        
        if (targetKingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return true;
        }
        
        if (!kingdom.getWars().contains(targetKingdomName)) {
            player.sendMessage("§cYou are not at war with this kingdom!");
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
        
        player.sendMessage("§a✔ War with §6" + targetKingdomName + "§a has ended!");
        
        Town targetCapital = societiesManager.getTown(targetKingdom.getCapital());
        if (targetCapital != null) {
            Player targetKing = plugin.getServer().getPlayer(targetCapital.getMayor());
            if (targetKing != null) {
                targetKing.sendMessage("§a✔ The war with §6" + kingdom.getName() + "§a has ended!");
            }
        }
        
        return true;
    }
    
    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /war info <warId>");
            return true;
        }
        
        UUID warId;
        try {
            warId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid war ID!");
            return true;
        }
        
        War war = militaryManager.getWar(warId);
        if (war == null) {
            player.sendMessage("§cWar not found!");
            return true;
        }
        
        player.sendMessage("§7§m----------§r §cWar Info§7 §m----------");
        player.sendMessage("§eAttacker: §6" + war.getAttackerKingdom());
        player.sendMessage("§eDefender: §6" + war.getDefenderKingdom());
        player.sendMessage("§eStatus: §6" + war.getStatus().name());
        player.sendMessage("§eStarted: §6" + dateFormat.format(new Date(war.getStartDate())));
        
        if (war.getStatus() == War.WarStatus.CONCLUDED && war.getEndDate() > 0) {
            player.sendMessage("§eEnded: §6" + dateFormat.format(new Date(war.getEndDate())));
        }
        
        player.sendMessage("§eReason: §6" + war.getReason());
        player.sendMessage("§eAttacker Casualties: §6" + war.getAttackerCasualties());
        player.sendMessage("§eDefender Casualties: §6" + war.getDefenderCasualties());
        player.sendMessage("§eWar Crimes: §6" + war.getWarCrimes().size());
        player.sendMessage("§7§m--------------------------------");
        
        return true;
    }
    
    private boolean handleList(Player player, String[] args) {
        Collection<War> wars = militaryManager.getAllWars();
        
        List<War> activeWars = wars.stream()
                .filter(w -> w.getStatus() != War.WarStatus.CONCLUDED)
                .collect(Collectors.toList());
        
        if (activeWars.isEmpty()) {
            player.sendMessage("§aThere are no active wars!");
            return true;
        }
        
        player.sendMessage("§7§m----------§r §cActive Wars §7(" + activeWars.size() + ")§m----------");
        
        for (War war : activeWars) {
            player.sendMessage("§c⚔ §6" + war.getAttackerKingdom() + " §7vs §6" + war.getDefenderKingdom());
            player.sendMessage("§7  Status: §e" + war.getStatus().name() + " §7| ID: §e" + war.getWarId().toString().substring(0, 8));
        }
        
        player.sendMessage("§7§m--------------------------------");
        
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
        player.sendMessage("§7§m----------§r §cWar Commands§7 §m----------");
        player.sendMessage("§e/war declare <kingdom> [reason]§7 - Declare war");
        player.sendMessage("§e/war end <kingdom>§7 - End a war");
        player.sendMessage("§e/war info <warId>§7 - View war info");
        player.sendMessage("§e/war list§7 - List active wars");
        player.sendMessage("§7§m--------------------------------");
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
