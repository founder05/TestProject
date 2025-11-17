package me.marcdoesntexists.realms.utils;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ConfigurationManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility class for managing localized messages from messages.yml and gui.yml
 * Improved: supports fallbacks, cached missing-key warnings and flexible formatting.
 */
public class MessageUtils {

    private static FileConfiguration messagesConfig;
    // New: gui.yml configuration (fallback for GUI-specific keys)
    private static FileConfiguration guiConfig;
    private static String prefix = ChatColor.translateAlternateColorCodes('&', "&6[Realms] &r");

    // Cache of missing keys we've already warned about to avoid console spam
    private static final Set<String> warnedMissingKeys = Collections.synchronizedSet(new HashSet<>());

    // File where missing keys will be appended for debugging (one-time per key)
    private static final String MISSING_KEYS_FILENAME = "missing_message_keys.txt";

    /**
     * Initialize the MessageUtils with the plugin instance
     */
    public static void init(Realms plugin) {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        if (configManager != null) {
            messagesConfig = configManager.getMessagesConfig();
            guiConfig = configManager.getRealmsGuiconfig();
            if (messagesConfig != null) {
                prefix = ChatColor.translateAlternateColorCodes('&',
                    messagesConfig.getString("general.prefix", "&6[Realms] &r"));
            }
        }
    }

    /**
     * Reload messages from config
     */
    public static void reload() {
        init(null);
        warnedMissingKeys.clear();
    }

    /**
     * Get a message from messages.yml (or gui.yml fallback) by key path, with optional fallback keys.
     * If none found, returns a visible placeholder and logs a single warning per missing key.
     *
     * @param key primary key
     * @param fallbacks optional fallback keys to try
     * @return formatted message string (colors translated, %prefix% replaced)
     */
    public static String get(String key, String... fallbacks) {
        String raw = getRawStringWithFallbacks(key, fallbacks);
        if (raw == null) {
            // warn only once per missing key
            if (!warnedMissingKeys.contains(key)) {
                warnedMissingKeys.add(key);
                if (Realms.getInstance() != null) {
                    Realms.getInstance().getLogger().warning("Missing messages.yml key: " + key + " — using placeholder");
                }
                // append missing key to debug file if configured (default: true)
                try {
                    boolean dumpToFile = true;
                    if (messagesConfig != null) dumpToFile = messagesConfig.getBoolean("debug.write_missing_keys_file", true);
                    if (dumpToFile && Realms.getInstance() != null) {
                        try {
                            File pluginFolder = Realms.getInstance().getDataFolder();
                            if (!pluginFolder.exists()) pluginFolder.mkdirs();
                            Path file = new File(pluginFolder, MISSING_KEYS_FILENAME).toPath();
                            String line = java.time.Instant.now().toString() + " - " + key + System.lineSeparator();
                            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException ioe) {
                            // don't spam the log if file write fails
                            try { Realms.getInstance().getLogger().warning("Failed to write missing message key to file: " + ioe.getMessage()); } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }
            String missingTemplate = null;
            try {
                if (messagesConfig != null) missingTemplate = messagesConfig.getString("debug.missing_key", null);
            } catch (Exception ignored) {}
            if (missingTemplate != null) {
                return ChatColor.translateAlternateColorCodes('&', missingTemplate.replace("{key}", key).replace("%prefix%", prefix).replace("{prefix}", prefix));
            }
            return ChatColor.RED + "[Missing: " + key + "]";
        }

        // Replace prefix token then translate color codes
        raw = raw.replace("%prefix%", prefix).replace("{prefix}", prefix);
         return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Get a message with a provided default value if the key doesn't exist.
     */
    public static String getOrDefault(String key, String defaultValue, String... fallbacks) {
        String raw = getRawStringWithFallbacks(key, fallbacks);
        if (raw == null) return ChatColor.translateAlternateColorCodes('&', (defaultValue == null ? "" : defaultValue).replace("%prefix%", prefix).replace("{prefix}", prefix));
        raw = raw.replace("%prefix%", prefix).replace("{prefix}", prefix);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Return a copy of the missing keys cache (for diagnostics).
     */
    public static Set<String> getMissingKeys() {
        return new HashSet<>(warnedMissingKeys);
    }

    /**
     * Get raw string from config, trying messagesConfig then guiConfig, then provided fallbacks in order.
     */
    private static String getRawStringWithFallbacks(String key, String... fallbacks) {
        // Try messages.yml first
        try {
            if (messagesConfig != null && messagesConfig.contains(key)) {
                return messagesConfig.getString(key);
            }
            // Then try gui.yml (useful for keys moved to gui.yml)
            if (guiConfig != null) {
                // direct key present?
                if (guiConfig.contains(key)) return guiConfig.getString(key);
                // handle keys like 'gui.town_verbose' -> check gui.messages.town_verbose or gui.town_verbose
                if (key.startsWith("gui.")) {
                    String sub = key.substring(4);
                    if (guiConfig.contains("messages." + sub)) return guiConfig.getString("messages." + sub);
                    if (guiConfig.contains(sub)) return guiConfig.getString(sub);
                }
                // handle keys that might be stored under 'messages' section inside gui.yml (e.g., currently_viewing)
                if (guiConfig.contains("messages." + key)) return guiConfig.getString("messages." + key);
            }
            if (fallbacks != null) {
                for (String fb : fallbacks) {
                    if (fb == null) continue;
                    if (messagesConfig != null && messagesConfig.contains(fb)) return messagesConfig.getString(fb);
                    if (guiConfig != null) {
                        if (guiConfig.contains(fb)) return guiConfig.getString(fb);
                        if (guiConfig.contains("messages." + fb)) return guiConfig.getString("messages." + fb);
                        if (fb.startsWith("gui.")) {
                            String sub = fb.substring(4);
                            if (guiConfig.contains("messages." + sub)) return guiConfig.getString("messages." + sub);
                            if (guiConfig.contains(sub)) return guiConfig.getString(sub);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Format message with replacements (map values converted to String). Keeps existing signature.
     *
     * @param key message key
     * @param replacements map of placeholder name -> value (will replace {name})
     * @return formatted string
     */
    public static String format(String key, Map<String, ?> replacements) {
        String message = get(key);
        // get(...) never returns null: it returns a placeholder when missing; still guard
        if (replacements == null || replacements.isEmpty()) return message;
        // Create a copy to avoid modifying input
        Map<String, String> safe = new HashMap<>();
        for (Map.Entry<String, ?> e : replacements.entrySet()) {
            safe.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
        }
        String out = formatMessagePlaceholders(message, safe);
        // Warn once for missing placeholders inside the message to help debugging
        checkForUnreplacedPlaceholders(key, out);
        return out;
    }

    /**
     * Convenience overload that accepts varargs pairs: (key1, value1, key2, value2,...)
     */
    public static String format(String key, Object... kvPairs) {
        if (kvPairs == null || kvPairs.length == 0) return get(key);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < kvPairs.length - 1; i += 2) {
            Object k = kvPairs[i];
            Object v = kvPairs[i + 1];
            if (k != null) map.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
        }
        return format(key, map);
    }

    private static String formatMessagePlaceholders(String message, Map<String, String> replacements) {
        String out = message;
        for (Map.Entry<String, String> e : replacements.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    // Cache for placeholder warnings to avoid console spam
    private static final Set<String> warnedMissingPlaceholders = Collections.synchronizedSet(new HashSet<>());
    private static void checkForUnreplacedPlaceholders(String key, String message) {
        if (message == null) return;
        // naive scan for patterns like {something}
        int idx = message.indexOf('{');
        while (idx != -1) {
            int end = message.indexOf('}', idx + 1);
            if (end == -1) break;
            String ph = message.substring(idx + 1, end);
            if (!ph.isEmpty() && !warnedMissingPlaceholders.contains(key + ":" + ph)) {
                warnedMissingPlaceholders.add(key + ":" + ph);
                if (Realms.getInstance() != null) {
                    Realms.getInstance().getLogger().warning("Message '" + key + "' contains unreplaced placeholder: {" + ph + "}");
                }
            }
            idx = message.indexOf('{', end + 1);
        }
    }

    /**
     * Check if a message key exists in the config (messages or gui)
     */
    public static boolean hasKey(String key) {
        try {
            if (messagesConfig != null && messagesConfig.contains(key)) return true;
            if (guiConfig != null && guiConfig.contains(key)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Get the configured prefix (already color-translated)
     */
    public static String getPrefix() {
        return prefix;
    }

    /**
     * Convert a legacy or MiniMessage string into a Component.
     * Priority: if the string contains '<' and '>' we try MiniMessage, otherwise we use legacy '&' parser.
     */
    public static Component toComponent(String raw) {
        if (raw == null) return Component.empty();
        // replace prefix tokens first (prefix contains legacy color codes already)
        String replaced = raw.replace("%prefix%", prefix).replace("{prefix}", prefix);
        try {
            if (replaced.contains("<") && replaced.contains(">")) {
                return MiniMessage.miniMessage().deserialize(replaced);
            }
        } catch (Throwable ignored) {}
        // fallback to legacy '&' serializer
        return LegacyComponentSerializer.legacyAmpersand().deserialize(replaced);
    }

    public static List<Component> toComponentList(List<String> lines) {
        if (lines == null) return List.of();
        return lines.stream().map(MessageUtils::toComponent).collect(Collectors.toList());
    }

    /**
     * Validate a list of message keys at startup. For any missing key, append a placeholder
     * YAML entry to the missing keys file for the server admin to review and copy into messages.yml/gui.yml.
     * This will also log a warning for each missing key (once).
     */
    public static void validateKeysOnStartup(List<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        File pluginFolder = null;
        try {
            if (Realms.getInstance() != null) pluginFolder = Realms.getInstance().getDataFolder();
        } catch (Throwable ignored) {}
        Path file = null;
        if (pluginFolder != null) file = new File(pluginFolder, MISSING_KEYS_FILENAME).toPath();

        StringBuilder yamlPlaceholders = new StringBuilder();
        yamlPlaceholders.append("# Missing message keys detected at startup -- placeholder entries below\n");
        yamlPlaceholders.append("# Copy relevant lines into messages.yml or gui.yml as appropriate and edit the texts.\n\n");

        for (String key : keys) {
            try {
                if (!hasKey(key)) {
                    // warn
                    if (!warnedMissingKeys.contains(key)) {
                        warnedMissingKeys.add(key);
                        if (Realms.getInstance() != null) Realms.getInstance().getLogger().warning("Missing messages key at startup: " + key);
                    }
                    // append placeholder YAML style, convert dot-notation to nested YAML comment
                    String placeholder = "# " + key + ": \"&c[Missing: " + key + "]\"\n";
                    yamlPlaceholders.append(placeholder);
                }
            } catch (Throwable ignored) {}
        }

        // write file if we added placeholders
        try {
            if (!yamlPlaceholders.isEmpty() && file != null) {
                Files.writeString(file, yamlPlaceholders.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException ioe) {
            try { if (Realms.getInstance() != null) Realms.getInstance().getLogger().warning("Unable to write missing messages file: " + ioe.getMessage()); } catch (Throwable ignored) {}
        }
    }
}
