package me.marcdoesntexists.nations.utils;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

/**
 * Utility class for managing localized messages from messages.yml
 */
public class MessageUtils {

    private static FileConfiguration messagesConfig;
    private static String prefix = "§6[Nations] §r";

    /**
     * Initialize the MessageUtils with the plugin instance
     */
    public static void init(Nations plugin) {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        if (configManager != null) {
            messagesConfig = configManager.getMessagesConfig();
            if (messagesConfig != null) {
                prefix = ChatColor.translateAlternateColorCodes('&',
                    messagesConfig.getString("general.prefix", "§6[Nations] §r"));
            }
        }
    }

    /**
     * Get a message from messages.yml by key path
     *
     * @param key The dot-separated path to the message (e.g., "town.created")
     * @return The formatted message string
     */
    public static String get(String key) {
        if (messagesConfig == null) {
            Nations.getInstance().getLogger().warning("Messages config not loaded! Key: " + key);
            return "§c[Message Error: " + key + "]";
        }

        String message = messagesConfig.getString(key);

        if (message == null) {
            Nations.getInstance().getLogger().warning("Missing messages.yml key: " + key + " — using placeholder");
            return "§c[Missing: " + key + "]";
        }

        // Replace %prefix% placeholder
        message = message.replace("%prefix%", prefix);

        // Translate color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Get a formatted message with placeholder replacements
     *
     * @param key The dot-separated path to the message
     * @param replacements Map of placeholder keys to replacement values
     * @return The formatted message with all placeholders replaced
     */
    public static String format(String key, Map<String, String> replacements) {
        String message = get(key);

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                message = message.replace(placeholder, entry.getValue());
            }
        }

        return message;
    }

    /**
     * Get the configured prefix
     *
     * @return The message prefix
     */
    public static String getPrefix() {
        return prefix;
    }

    /**
     * Check if a message key exists in the config
     *
     * @param key The message key to check
     * @return true if the key exists
     */
    public static boolean hasKey(String key) {
        return messagesConfig != null && messagesConfig.contains(key);
    }

    /**
     * Reload messages from config
     */
    public static void reload() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        if (configManager != null) {
            messagesConfig = configManager.getMessagesConfig();
            if (messagesConfig != null) {
                prefix = ChatColor.translateAlternateColorCodes('&',
                    messagesConfig.getString("general.prefix", "§6[Nations] §r"));
            }
        }
    }
}

