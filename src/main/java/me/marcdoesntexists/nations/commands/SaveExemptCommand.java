package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ExemptionManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SaveExemptCommand implements CommandExecutor {
    private final Nations plugin;

    public SaveExemptCommand(Nations plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.save.exempt") && !sender.isOp()) {
            sender.sendMessage(MessageUtils.get("saveexempt.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtils.get("saveexempt.usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtils.get("saveexempt.player_not_found"));
            return true;
        }

        ExemptionManager exemptionManager = Nations.getInstance() != null ? Nations.getInstance().getExemptionManager() : null;
        if (exemptionManager == null) {
            sender.sendMessage(MessageUtils.get("saveexempt.manager_unavailable"));
            return true;
        }

        UUID id = target.getUniqueId();
        boolean now = exemptionManager.toggleExempt(id);
        sender.sendMessage(MessageUtils.format("saveexempt.changed", java.util.Map.of("player", target.getName(), "status", now ? MessageUtils.get("yes") : MessageUtils.get("no"))));
        return true;
    }
}
