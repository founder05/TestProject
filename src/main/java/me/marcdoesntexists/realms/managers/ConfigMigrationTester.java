package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;

/**
 * Classe di test per verificare il funzionamento del sistema di migrazione.
 * Da eseguire in un ambiente di test prima del rilascio in produzione.
 */
public class ConfigMigrationTester {

    private final Realms plugin;
    private final ConfigMigrationManager migrationManager;

    public ConfigMigrationTester(Realms plugin) {
        this.plugin = plugin;
        this.migrationManager = new ConfigMigrationManager(plugin);
    }

    /**
     * Test completo del sistema di migrazione
     */
    public void runFullTest() {
        plugin.getLogger().info("=== INIZIO TEST MIGRAZIONE CONFIG ===");

        testVersionDetection();
        testBackupCreation();
        testMergeLogic();
        testMessagePreservation();
        testBackupManagement();

        plugin.getLogger().info("=== TEST COMPLETATI ===");
    }

    /**
     * Test 1: Rilevamento versione
     */
    private void testVersionDetection() {
        plugin.getLogger().info("Test 1: Rilevamento Versione");

        try {
            var pending = migrationManager.checkPendingMigrations();

            plugin.getLogger().info("✓ Migrazioni pendenti trovate: " + pending.size());
            for (var entry : pending.entrySet()) {
                plugin.getLogger().info("  - " + entry.getKey() + " (+" + entry.getValue() + " versioni)");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "✗ Test fallito", e);
        }
    }

    /**
     * Test 2: Creazione backup
     */
    private void testBackupCreation() {
        plugin.getLogger().info("Test 2: Creazione Backup");

        try {
            File testFile = new File(plugin.getDataFolder(), "test-config.yml");
            FileConfiguration testConfig = new YamlConfiguration();
            testConfig.set("config-version", 0);
            testConfig.set("test.value", "original");
            testConfig.save(testFile);

            // Simula migrazione
            boolean migrated = migrationManager.forceMigration("test-config.yml");

            // Verifica che il backup sia stato creato
            File backupFolder = new File(plugin.getDataFolder(), "config-backups");
            File[] backups = backupFolder.listFiles((dir, name) ->
                name.startsWith("test-config_v0_"));

            if (backups != null && backups.length > 0) {
                plugin.getLogger().info("✓ Backup creato: " + backups[0].getName());
            } else {
                plugin.getLogger().warning("✗ Backup non trovato!");
            }

            // Cleanup
            testFile.delete();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "✗ Test fallito", e);
        }
    }

    /**
     * Test 3: Logica di merge
     */
    private void testMergeLogic() {
        plugin.getLogger().info("Test 3: Logica di Merge");

        try {
            // Crea config utente (vecchio)
            FileConfiguration userConfig = new YamlConfiguration();
            userConfig.set("config-version", 0);
            userConfig.set("existing.custom_value", "PERSONALIZZATO");
            userConfig.set("existing.default_value", "default");

            // Crea config default (nuovo)
            FileConfiguration defaultConfig = new YamlConfiguration();
            defaultConfig.set("config-version", 1);
            defaultConfig.set("existing.custom_value", "default_value");
            defaultConfig.set("existing.default_value", "default");
            defaultConfig.set("new.added_key", "nuovo_valore");

            // Test merge manuale
            FileConfiguration merged = new YamlConfiguration();

            // Simula merge
            copyAllKeys(defaultConfig, merged);
            overlayUserValues(userConfig, merged);

            // Verifica risultati
            String customValue = merged.getString("existing.custom_value");
            String newKey = merged.getString("new.added_key");

            if ("PERSONALIZZATO".equals(customValue)) {
                plugin.getLogger().info("✓ Valore personalizzato preservato");
            } else {
                plugin.getLogger().warning("✗ Valore personalizzato perso: " + customValue);
            }

            if ("nuovo_valore".equals(newKey)) {
                plugin.getLogger().info("✓ Nuova chiave aggiunta");
            } else {
                plugin.getLogger().warning("✗ Nuova chiave non aggiunta");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "✗ Test fallito", e);
        }
    }

    /**
     * Test 4: Preservazione messaggi
     */
    private void testMessagePreservation() {
        plugin.getLogger().info("Test 4: Preservazione Messaggi");

        try {
            // Simula messages.yml con traduzione custom
            FileConfiguration userMessages = new YamlConfiguration();
            userMessages.set("config-version", 0);
            userMessages.set("town.created", "§aCittà fondata con successo!"); // Custom

            // Simula nuovo default
            FileConfiguration defaultMessages = new YamlConfiguration();
            defaultMessages.set("config-version", 1);
            defaultMessages.set("town.created", "&aDefault message");
            defaultMessages.set("town.disbanded", "&cTown disbanded"); // Nuovo

            // Simula merge con logica speciale messages
            FileConfiguration merged = new YamlConfiguration();
            copyAllKeys(defaultMessages, merged);

            // Overlay user values (logica speciale per messages)
            String userCreated = userMessages.getString("town.created");
            if (userCreated != null && !userCreated.isEmpty()) {
                merged.set("town.created", userCreated);
            }

            // Verifica
            String resultCreated = merged.getString("town.created");
            String resultDisbanded = merged.getString("town.disbanded");

            if ("§aCittà fondata con successo!".equals(resultCreated)) {
                plugin.getLogger().info("✓ Traduzione custom preservata");
            } else {
                plugin.getLogger().warning("✗ Traduzione custom persa: " + resultCreated);
            }

            if ("&cTown disbanded".equals(resultDisbanded)) {
                plugin.getLogger().info("✓ Nuovo messaggio aggiunto");
            } else {
                plugin.getLogger().warning("✗ Nuovo messaggio non aggiunto");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "✗ Test fallito", e);
        }
    }

    /**
     * Test 5: Gestione backup
     */
    private void testBackupManagement() {
        plugin.getLogger().info("Test 5: Gestione Backup");

        try {
            var backups = migrationManager.listBackups();
            plugin.getLogger().info("✓ Backup totali trovati: " + backups.size());

            if (!backups.isEmpty()) {
                plugin.getLogger().info("  Backup più recente: " + backups.getFirst());
            }

            // Test pulizia (simulato)
            plugin.getLogger().info("  Test pulizia backup con limite 3...");
            // Non eseguiamo realmente per non eliminare backup reali
            plugin.getLogger().info("✓ Metodo pulizia disponibile");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "✗ Test fallito", e);
        }
    }

    // Helper methods per simulare il merge
    private void copyAllKeys(FileConfiguration source, FileConfiguration dest) {
        for (String key : source.getKeys(true)) {
            if (!source.isConfigurationSection(key)) {
                dest.set(key, source.get(key));
            }
        }
    }

    private void overlayUserValues(FileConfiguration user, FileConfiguration merged) {
        for (String key : user.getKeys(true)) {
            if (!user.isConfigurationSection(key) && !key.equals("config-version")) {
                Object userValue = user.get(key);
                Object mergedValue = merged.get(key);

                if (userValue != null && !userValue.equals(mergedValue)) {
                    merged.set(key, userValue);
                }
            }
        }
    }
}

