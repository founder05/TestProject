package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Gestisce la migrazione automatica dei file di configurazione preservando
 * le personalizzazioni dell'utente quando vengono rilasciate nuove versioni.
 */
public class ConfigMigrationManager {

    private static final String CONFIG_VERSION_KEY = "config-version";
    private static final int CURRENT_CONFIG_VERSION = 1;
    private static final int CURRENT_MESSAGES_VERSION = 1;
    private static final int CURRENT_GUI_VERSION = 1;

    private final Realms plugin;
    private final File backupFolder;

    // Nomi dei file di configurazione da gestire
    private static final String[] CONFIG_FILES = {
        "config.yml",
        "messages.yml",
        "gui.yml",
        "settlements.yml",
        "economy.yml",
        "feudal.yml",
        "military.yml",
        "war.yml",
        "diplomacy.yml",
        "social.yml"
    };

    public ConfigMigrationManager(Realms plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "config-backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    /**
     * Esegue la migrazione di tutti i file di configurazione
     */
    public void migrateAllConfigs() {
        plugin.getLogger().info("=== Inizio migrazione configurazioni ===");

        boolean anyMigrated = false;

        for (String configFileName : CONFIG_FILES) {
            try {
                if (migrateConfigFile(configFileName)) {
                    anyMigrated = true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                    "Errore durante la migrazione di " + configFileName, e);
            }
        }

        if (anyMigrated) {
            plugin.getLogger().info("=== Migrazione completata! I backup sono in: " +
                backupFolder.getAbsolutePath() + " ===");
        } else {
            plugin.getLogger().info("=== Nessuna migrazione necessaria ===");
        }
    }

    /**
     * Migra un singolo file di configurazione
     */
    private boolean migrateConfigFile(String fileName) throws IOException {
        File userFile = new File(plugin.getDataFolder(), fileName);

        // Se il file non esiste, crea quello di default
        if (!userFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Creato file di default: " + fileName);
            return false;
        }

        // Carica il file dell'utente
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);

        // Carica il file di default dalla risorsa
        FileConfiguration defaultConfig = new YamlConfiguration();
        try (java.io.InputStream resourceStream = plugin.getResource(fileName)) {
            if (resourceStream == null) {
                plugin.getLogger().warning("Risorsa di default non trovata per " + fileName +
                    " - skip migrazione");
                return false;
            }
            try (java.io.InputStreamReader reader = new java.io.InputStreamReader(
                    resourceStream, java.nio.charset.StandardCharsets.UTF_8)) {
                defaultConfig = YamlConfiguration.loadConfiguration(reader);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Errore caricamento risorsa di default per " + fileName +
                ": " + e.getMessage());
            return false;
        }

        // Determina la versione corrente del file
        int currentVersion = getExpectedVersion(fileName);
        int userVersion = userConfig.getInt(CONFIG_VERSION_KEY, 0);

        // Se la versione dell'utente è già aggiornata, non fare nulla
        if (userVersion >= currentVersion) {
            return false;
        }

        plugin.getLogger().info("Migrazione " + fileName + " da versione " +
            userVersion + " a versione " + currentVersion);

        // Crea backup del file esistente
        backupConfigFile(userFile, userVersion);

        // Esegui il merge intelligente
        FileConfiguration mergedConfig = mergeConfigs(userConfig, defaultConfig, fileName);

        // Imposta la nuova versione
        mergedConfig.set(CONFIG_VERSION_KEY, currentVersion);

        // Salva il file migrato
        mergedConfig.save(userFile);

        plugin.getLogger().info("✓ Migrazione completata per " + fileName);
        return true;
    }

    /**
     * Determina la versione attesa per un file di configurazione
     */
    private int getExpectedVersion(String fileName) {
        return switch (fileName) {
            case "config.yml" -> CURRENT_CONFIG_VERSION;
            case "messages.yml" -> CURRENT_MESSAGES_VERSION;
            case "gui.yml" -> CURRENT_GUI_VERSION;
            default -> 1;
        };
    }

    /**
     * Crea un backup del file di configurazione con timestamp
     */
    private void backupConfigFile(File file, int version) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String backupName = file.getName().replace(".yml",
            "_v" + version + "_" + timestamp + ".yml");

        File backupFile = new File(backupFolder, backupName);
        Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        plugin.getLogger().info("Backup creato: " + backupName);
    }

    /**
     * Esegue un merge intelligente tra config utente e config di default.
     * Preserva i valori personalizzati dall'utente e aggiunge nuove chiavi.
     */
    private FileConfiguration mergeConfigs(FileConfiguration userConfig,
                                           FileConfiguration defaultConfig,
                                           String fileName) {

        FileConfiguration merged = new YamlConfiguration();

        // Primo passo: copia TUTTE le chiavi dal default (struttura completa)
        copyAllKeys(defaultConfig, merged, "");

        // Secondo passo: sovrascrivi con i valori personalizzati dall'utente
        overlayUserValues(userConfig, merged, "", fileName);

        return merged;
    }

    /**
     * Copia ricorsivamente tutte le chiavi e valori da source a destination
     */
    private void copyAllKeys(ConfigurationSection source,
                            ConfigurationSection dest,
                            String path) {

        for (String key : source.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (source.isConfigurationSection(key)) {
                // Ricorsione per sezioni nidificate
                dest.createSection(key);
                copyAllKeys(Objects.requireNonNull(source.getConfigurationSection(key)),
                           dest.getConfigurationSection(key),
                           fullPath);
            } else {
                // Copia il valore
                dest.set(key, source.get(key));
            }
        }
    }

    /**
     * Sovrappone i valori personalizzati dall'utente sul config merged.
     * Usa logica speciale per messages.yml per preservare traduzioni custom.
     */
    private void overlayUserValues(ConfigurationSection userConfig,
                                   ConfigurationSection merged,
                                   String path,
                                   String fileName) {

        for (String key : userConfig.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            // Salta la chiave config-version (viene gestita separatamente)
            if (key.equals(CONFIG_VERSION_KEY)) {
                continue;
            }

            if (userConfig.isConfigurationSection(key)) {
                // Ricorsione per sezioni nidificate
                if (!merged.isConfigurationSection(key)) {
                    merged.createSection(key);
                }
                overlayUserValues(userConfig.getConfigurationSection(key),
                                 merged.getConfigurationSection(key),
                                 fullPath,
                                 fileName);
            } else {
                Object userValue = userConfig.get(key);
                Object defaultValue = merged.get(key);

                // Logica speciale per messages.yml
                if (fileName.equals("messages.yml")) {
                    // Preserva SEMPRE le traduzioni custom dell'utente
                    if (userValue instanceof String) {
                        String userStr = (String) userValue;
                        // Se l'utente ha modificato il messaggio (non è vuoto), preservalo
                        if (!userStr.isEmpty()) {
                            merged.set(key, userValue);
                        }
                    } else {
                        merged.set(key, userValue);
                    }
                } else {
                    // Per altri config, preserva valori custom se diversi dal default
                    if (shouldPreserveValue(userValue, defaultValue, fullPath)) {
                        merged.set(key, userValue);
                    }
                }
            }
        }
    }

    /**
     * Determina se un valore custom dell'utente dovrebbe essere preservato
     */
    private boolean shouldPreserveValue(Object userValue, Object defaultValue, String path) {
        // Preserva sempre se il default non esiste (chiave rimossa)
        if (defaultValue == null) {
            return true;
        }

        // Preserva se i valori sono diversi
        if (userValue == null) {
            return false;
        }

        // Confronta i valori
        if (userValue instanceof Number && defaultValue instanceof Number) {
            return !userValue.equals(defaultValue);
        }

        if (userValue instanceof Boolean && defaultValue instanceof Boolean) {
            return !userValue.equals(defaultValue);
        }

        if (userValue instanceof String && defaultValue instanceof String) {
            return !userValue.equals(defaultValue);
        }

        if (userValue instanceof List && defaultValue instanceof List) {
            return !userValue.equals(defaultValue);
        }

        // Default: preserva
        return true;
    }

    /**
     * Migra manualmente un config specifico (comando admin)
     */
    public boolean forceMigration(String fileName) {
        try {
            return migrateConfigFile(fileName);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                "Errore durante la migrazione forzata di " + fileName, e);
            return false;
        }
    }

    /**
     * Ripristina un backup specifico
     */
    public boolean restoreBackup(String backupFileName) {
        try {
            File backupFile = new File(backupFolder, backupFileName);
            if (!backupFile.exists()) {
                plugin.getLogger().warning("Backup non trovato: " + backupFileName);
                return false;
            }

            // Estrai il nome del file originale dal nome del backup
            String originalName = backupFileName.replaceAll("_v\\d+_.*\\.yml", ".yml");
            File targetFile = new File(plugin.getDataFolder(), originalName);

            // Crea backup del file corrente prima di ripristinare
            if (targetFile.exists()) {
                backupConfigFile(targetFile, -1); // -1 = ripristino
            }

            // Ripristina il backup
            Files.copy(backupFile.toPath(), targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

            plugin.getLogger().info("Backup ripristinato: " + backupFileName + " -> " + originalName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                "Errore durante il ripristino del backup", e);
            return false;
        }
    }

    /**
     * Lista tutti i backup disponibili
     */
    public List<String> listBackups() {
        File[] backups = backupFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (backups == null || backups.length == 0) {
            return Collections.emptyList();
        }

        List<String> backupNames = new ArrayList<>();
        for (File backup : backups) {
            backupNames.add(backup.getName());
        }

        // Ordina per data (più recenti prima)
        backupNames.sort(Collections.reverseOrder());
        return backupNames;
    }

    /**
     * Elimina backup vecchi (mantiene solo gli ultimi N per file)
     */
    public void cleanOldBackups(int keepPerFile) {
        Map<String, List<File>> backupsByFile = new HashMap<>();

        File[] backups = backupFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (backups == null) return;

        // Raggruppa backup per file originale
        for (File backup : backups) {
            String fileName = backup.getName().replaceAll("_v\\d+_.*\\.yml", ".yml");
            backupsByFile.computeIfAbsent(fileName, k -> new ArrayList<>()).add(backup);
        }

        // Per ogni file, mantieni solo i più recenti
        for (List<File> fileBackups : backupsByFile.values()) {
            fileBackups.sort(Comparator.comparing(File::getName).reversed());

            // Elimina quelli in eccesso
            for (int i = keepPerFile; i < fileBackups.size(); i++) {
                File toDelete = fileBackups.get(i);
                if (toDelete.delete()) {
                    plugin.getLogger().info("Backup vecchio eliminato: " + toDelete.getName());
                }
            }
        }
    }

    /**
     * Verifica se ci sono migrazioni pendenti
     */
    public Map<String, Integer> checkPendingMigrations() {
        Map<String, Integer> pending = new HashMap<>();

        for (String fileName : CONFIG_FILES) {
            File userFile = new File(plugin.getDataFolder(), fileName);
            if (!userFile.exists()) continue;

            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
            int userVersion = userConfig.getInt(CONFIG_VERSION_KEY, 0);
            int expectedVersion = getExpectedVersion(fileName);

            if (userVersion < expectedVersion) {
                pending.put(fileName, expectedVersion - userVersion);
            }
        }

        return pending;
    }
}

