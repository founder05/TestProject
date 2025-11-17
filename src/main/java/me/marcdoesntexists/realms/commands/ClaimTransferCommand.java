package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.integrations.GriefPreventionMigrator;
import me.marcdoesntexists.realms.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ClaimTransferCommand implements CommandExecutor, TabCompleter {

    private final Realms plugin;

    public ClaimTransferCommand(Realms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }

        if (!player.isOp() && !player.hasPermission("realms.claimtransfer")) {
            player.sendMessage(MessageUtils.get("commands.no_permission"));
            return true;
        }

        player.sendMessage(MessageUtils.getOrDefault("migrate.starting", "§eAvvio migrazione claim da GriefPrevention..."));
        GriefPreventionMigrator migrator = new GriefPreventionMigrator(plugin);
        GriefPreventionMigrator.MigrationResult res = migrator.migrate();
        if (res.success()) {
            player.sendMessage(MessageUtils.getOrDefault("migrate.success", "§aMigrazione completata: {msg}").replace("{msg}", res.message()));
        } else {
            player.sendMessage(MessageUtils.getOrDefault("migrate.failed", "§cMigrazione fallita: {msg}").replace("{msg}", res.message()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return new ArrayList<>();
    }
}

