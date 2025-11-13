package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SaveCheckCommand implements CommandExecutor {
    private final Nations plugin;

    public SaveCheckCommand(Nations plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) {
            sender.sendMessage(me.marcdoesntexists.nations.utils.MessageUtils.get("general.no_permission"));
            return true;
        }

        sender.sendMessage(me.marcdoesntexists.nations.utils.MessageUtils.get("commands.savecheck_started"));
        plugin.getLogger().info("Admin triggered savecheck by: " + sender.getName());
        try {
            DataManager.getInstance().saveAllData();
            sender.sendMessage(me.marcdoesntexists.nations.utils.MessageUtils.get("commands.savecheck_success"));
            plugin.getLogger().info("Savecheck completed successfully.");
        } catch (Exception e) {
            sender.sendMessage(me.marcdoesntexists.nations.utils.MessageUtils.format("commands.savecheck_failed", java.util.Map.of("error", e.getMessage())));
            plugin.getLogger().severe("Savecheck failed: " + e.getMessage());
        }

        return true;
    }
}
