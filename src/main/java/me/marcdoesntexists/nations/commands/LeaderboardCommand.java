package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.gui.LeaderboardGUI;
import me.marcdoesntexists.nations.managers.LeaderboardGUIManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {
    private final LeaderboardGUI gui;

    public LeaderboardCommand(Nations plugin) {
        this.gui = new LeaderboardGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // open default leaderboard (towns_population)
            gui.open(player, "towns_population");
            return true;
        }

        String key = args[0].toLowerCase();
        // map possible aliases
        if (key.equals("towns") || key.equals("towns_population") || key.equals("townpop")) {
            gui.open(player, "towns_population");
            return true;
        }
        if (key.equals("players") || key.equals("players_money") || key.equals("playermoney") || key.equals("money")) {
            gui.open(player, "players_money");
            return true;
        }

        player.sendMessage(MessageUtils.get("commands.not_found").replace("{entity}", "Leaderboard"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> choices = Arrays.asList("towns_population", "players_money", "towns", "players", "money", "townpop", "playermoney");
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(choices, args[0]);
        }
        return new ArrayList<>();
    }
}
