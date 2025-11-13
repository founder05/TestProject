package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.gui.NationsGUI;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NationsGUICommand implements CommandExecutor, TabCompleter {

    public NationsGUICommand(Nations plugin) {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }

        if (args.length == 0) {
            // Open main GUI (Towns)
            NationsGUI.openMainGUI(player, "TOWNS");
            return true;
        }

        String category = args[0].toUpperCase();

        switch (category) {
            case "TOWNS":
            case "KINGDOMS":
            case "EMPIRES":
                NationsGUI.openMainGUI(player, category);
                break;
            default:
                player.sendMessage(MessageUtils.get("commands.usage") + " §cUsage: /nations [towns|kingdoms|empires]");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("towns", "kingdoms", "empires")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
