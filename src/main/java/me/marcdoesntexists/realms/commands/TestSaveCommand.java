package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.SaveAtomicTester;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestSaveCommand implements CommandExecutor {
    private final Realms plugin;

    public TestSaveCommand(Realms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("realms.admin")) {
            sender.sendMessage("§cYou don't have permission to run this command.");
            return true;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            SaveAtomicTester.runTest(plugin);
        });

        sender.sendMessage("§aRunning save diagnostic test in background. See console for results.");
        return true;
    }
}

