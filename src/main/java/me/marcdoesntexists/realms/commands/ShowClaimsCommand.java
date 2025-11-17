package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ClaimVisualizer;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShowClaimsCommand implements CommandExecutor, TabCompleter {

    private final Realms plugin;

    public ShowClaimsCommand(Realms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }

        DataManager dataManager = plugin.getDataManager();
        SocietiesManager societiesManager = plugin.getSocietiesManager();

        String townName = dataManager.getPlayerData(player.getUniqueId()).getTown();
        if (townName == null) {
            player.sendMessage(MessageUtils.get("claims.not_in_town"));
            return true;
        }

        Town town = societiesManager.getTown(townName);
        if (town == null) {
            player.sendMessage(MessageUtils.get("claims.town_not_found"));
            return true;
        }

        if (!player.hasPermission("realms.claim.show")) {
            player.sendMessage(MessageUtils.get("claims.no_permission"));
            return true;
        }

        ClaimVisualizer visualizer = plugin.getClaimVisualizer();
        if (visualizer == null) {
            player.sendMessage(MessageUtils.get("claims.visualizer_unavailable"));
            return true;
        }

        visualizer.toggleVisualization(player, town.getName());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // suggest town names (player's town first if applicable)
        List<String> suggestions = new ArrayList<>();
        if (sender instanceof Player) {
            Player p = (Player) sender;
            DataManager dm = plugin.getDataManager();
            SocietiesManager sm = plugin.getSocietiesManager();
            PlayerData pd = dm.getPlayerData(p.getUniqueId());
            if (pd != null && pd.getTown() != null) {
                suggestions.add(pd.getTown());
            }
            sm.getAllTowns().stream().map(Town::getName).forEach(suggestions::add);
        }
        if (args.length == 1) {
            // use TabCompletionUtils to match and sort
            List<String> merged = new ArrayList<>(suggestions);
            return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(merged, args[0]);
        }
        return List.of();
    }
}