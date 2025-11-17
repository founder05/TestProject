package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.gui.RealmsGUI;
import me.marcdoesntexists.realms.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RealmsGUICommand implements CommandExecutor, TabCompleter {

    public RealmsGUICommand(Realms plugin) {
        // No plugin field required here; RealmsGUI operations use static access where needed.
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }

        if (args.length == 0) {
            // Open main GUI (Towns)
            RealmsGUI.openMainGUI(player, "TOWNS");
            return true;
        }

        String category = args[0].toUpperCase();

        switch (category) {
            case "TOWNS":
            case "KINGDOMS":
            case "EMPIRES":
                RealmsGUI.openMainGUI(player, category);
                break;
            case "CLAIMTRANSFER":
            case "CLAIM_TRANSFER":
            case "CLAIM-TRANSFER":
                player.sendMessage(MessageUtils.get("commands.usage") + " §ePer migrare i claim usa: /claimtransfer (admin) oppure /realms claimtransfer dal console/admin");
                break;
            default:
                player.sendMessage(MessageUtils.get("commands.usage") + " §cUsage: /realms [towns|kingdoms|empires|claimtransfer]");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.realms.utils.TabCompletionUtils.match(Arrays.asList("towns", "kingdoms", "empires", "claimtransfer"), args[0]);
        }
        return new ArrayList<>();
    }
}
