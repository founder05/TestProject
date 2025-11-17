package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SaveCheckCommand implements CommandExecutor {
    private final Realms plugin;

    public SaveCheckCommand(Realms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("realms.admin")) {
            sender.sendMessage(me.marcdoesntexists.realms.utils.MessageUtils.get("general.no_permission"));
            return true;
        }

        sender.sendMessage(me.marcdoesntexists.realms.utils.MessageUtils.get("commands.savecheck_started"));
        plugin.getLogger().info("Admin triggered savecheck by: " + sender.getName());
        try {
            DataManager.getInstance().saveAllData();
            sender.sendMessage(me.marcdoesntexists.realms.utils.MessageUtils.get("commands.savecheck_success"));
            plugin.getLogger().info("Savecheck completed successfully.");
        } catch (Exception e) {
            sender.sendMessage(me.marcdoesntexists.realms.utils.MessageUtils.format("commands.savecheck_failed", java.util.Map.of("error", e.getMessage())));
            plugin.getLogger().severe("Savecheck failed: " + e.getMessage());
        }

        return true;
    }
}
