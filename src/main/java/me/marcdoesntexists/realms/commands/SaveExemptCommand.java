package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ExemptionManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SaveExemptCommand implements CommandExecutor {
    private final Realms plugin;

    public SaveExemptCommand(Realms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("realms.save.exempt") && !sender.isOp()) {
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

        ExemptionManager exemptionManager = Realms.getInstance() != null ? Realms.getInstance().getExemptionManager() : null;
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
