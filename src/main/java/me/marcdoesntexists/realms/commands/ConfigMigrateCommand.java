package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ConfigMigrationManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Comando per gestire le migrazioni dei file di configurazione
 */
public class ConfigMigrateCommand implements CommandExecutor, TabCompleter {

    private final Realms plugin;
    private final ConfigMigrationManager migrationManager;

    public ConfigMigrateCommand(Realms plugin) {
        this.plugin = plugin;
        this.migrationManager = new ConfigMigrationManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("realms.admin.config")) {
            sender.sendMessage(MessageUtils.get("general.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "check" -> handleCheck(sender);
            case "migrate" -> handleMigrate(sender, args);
            case "backup" -> handleBackup(sender, args);
            case "restore" -> handleRestore(sender, args);
            case "clean" -> handleClean(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCheck(CommandSender sender) {
        sender.sendMessage("§6⚙ Controllo migrazioni pendenti...");

        Map<String, Integer> pending = migrationManager.checkPendingMigrations();

        if (pending.isEmpty()) {
            sender.sendMessage("§a✓ Tutti i file di configurazione sono aggiornati!");
            return;
        }

        sender.sendMessage("§e⚠ Trovate " + pending.size() + " migrazioni pendenti:");
        for (Map.Entry<String, Integer> entry : pending.entrySet()) {
            sender.sendMessage("  §7- §e" + entry.getKey() + " §7(+" + entry.getValue() + " versioni)");
        }
        sender.sendMessage("§eUsa §6/configmigrate migrate §eper eseguire la migrazione.");
    }

    private void handleMigrate(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            // Migrazione di un file specifico
            String fileName = args[1];
            if (!fileName.endsWith(".yml")) {
                fileName += ".yml";
            }

            sender.sendMessage("§6⚙ Migrazione di " + fileName + "...");

            boolean success = migrationManager.forceMigration(fileName);
            if (success) {
                sender.sendMessage("§a✓ Migrazione completata per " + fileName);
                sender.sendMessage("§7Il backup è stato salvato in plugins/Realms/config-backups/");
            } else {
                sender.sendMessage("§c✗ Nessuna migrazione necessaria o errore durante la migrazione.");
            }
        } else {
            // Migrazione di tutti i file
            sender.sendMessage("§6⚙ Migrazione di tutti i file di configurazione...");
            sender.sendMessage("§7I tuoi valori personalizzati saranno preservati.");

            migrationManager.migrateAllConfigs();

            sender.sendMessage("§a✓ Migrazione completata!");
            sender.sendMessage("§7Ricarica il plugin con §e/realms reload §7per applicare le modifiche.");
        }
    }

    private void handleBackup(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            // Lista backup
            List<String> backups = migrationManager.listBackups();

            if (backups.isEmpty()) {
                sender.sendMessage("§e⚠ Nessun backup disponibile.");
                return;
            }

            sender.sendMessage("§6⚙ Backup disponibili (" + backups.size() + "):");
            for (int i = 0; i < Math.min(backups.size(), 20); i++) {
                sender.sendMessage("  §7" + (i + 1) + ". §f" + backups.get(i));
            }

            if (backups.size() > 20) {
                sender.sendMessage("  §7... e altri " + (backups.size() - 20) + " backup");
            }

            sender.sendMessage("§eUsa §6/configmigrate restore <nome_backup> §eper ripristinare.");
        }
    }

    private void handleRestore(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUtilizzo: /configmigrate restore <nome_backup>");
            sender.sendMessage("§7Usa §e/configmigrate backup list §7per vedere i backup disponibili.");
            return;
        }

        String backupName = args[1];
        if (!backupName.endsWith(".yml")) {
            backupName += ".yml";
        }

        sender.sendMessage("§6⚙ Ripristino backup: " + backupName);
        sender.sendMessage("§c⚠ ATTENZIONE: Il file corrente sarà sostituito!");

        boolean success = migrationManager.restoreBackup(backupName);

        if (success) {
            sender.sendMessage("§a✓ Backup ripristinato con successo!");
            sender.sendMessage("§7Ricarica il plugin con §e/realms reload §7per applicare le modifiche.");
        } else {
            sender.sendMessage("§c✗ Errore durante il ripristino del backup.");
        }
    }

    private void handleClean(CommandSender sender, String[] args) {
        int keepPerFile = 5; // Default: mantieni 5 backup per file

        if (args.length >= 2) {
            try {
                keepPerFile = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNumero non valido. Uso default: 5");
            }
        }

        sender.sendMessage("§6⚙ Pulizia backup vecchi (mantiene i " + keepPerFile + " più recenti per file)...");

        migrationManager.cleanOldBackups(keepPerFile);

        sender.sendMessage("§a✓ Pulizia completata!");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6⚙ ===== Config Migration Manager ===== ⚙");
        sender.sendMessage("§e/configmigrate check §7- Controlla migrazioni pendenti");
        sender.sendMessage("§e/configmigrate migrate [file] §7- Migra tutti i config o un file specifico");
        sender.sendMessage("§e/configmigrate backup [list] §7- Lista backup disponibili");
        sender.sendMessage("§e/configmigrate restore <backup> §7- Ripristina un backup");
        sender.sendMessage("§e/configmigrate clean [n] §7- Pulisci backup vecchi (mantiene N per file)");
        sender.sendMessage("§6⚙ ================================== ⚙");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("realms.admin.config")) {
            return List.of();
        }

        if (args.length == 1) {
            return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(
                Arrays.asList("check", "migrate", "backup", "restore", "clean"),
                args[0]
            );
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "migrate" -> {
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(
                        Arrays.asList("config.yml", "messages.yml", "gui.yml", "settlements.yml",
                            "economy.yml", "feudal.yml", "legal.yml", "military.yml",
                            "war.yml", "diplomacy.yml", "social.yml"),
                        args[1]
                    );
                }
                case "backup" -> {
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(
                        List.of("list"),
                        args[1]
                    );
                }
                case "restore" -> {
                    List<String> backups = migrationManager.listBackups();
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(
                        backups,
                        args[1]
                    );
                }
                case "clean" -> {
                    return me.marcdoesntexists.realms.utils.TabCompletionUtils.matchDistinct(
                        Arrays.asList("1", "3", "5", "10"),
                        args[1]
                    );
                }
            }
        }

        return List.of();
    }
}

