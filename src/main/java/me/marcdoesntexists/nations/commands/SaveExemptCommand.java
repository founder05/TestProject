package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ExemptionManager;
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
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /saveexempt <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        ExemptionManager exemptionManager = Nations.getInstance() != null ? Nations.getInstance().getExemptionManager() : null;
        if (exemptionManager == null) {
            sender.sendMessage("§cExemption manager not available.");
            return true;
        }

        UUID id = target.getUniqueId();
        boolean now = exemptionManager.toggleExempt(id);
        sender.sendMessage("§aPlayer " + target.getName() + " is now " + (now ? "exempt" : "not exempt") + " from partial saves.");
        return true;
    }
}
