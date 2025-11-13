package me.marcdoesntexists.nations.utils;

import me.marcdoesntexists.nations.Nations;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessageUtils {
    // keep track of missing keys we've already warned about to avoid log spam
    private static final Set<String> warnedMissing = ConcurrentHashMap.newKeySet();

    // sensible defaults for critical messages to avoid empty chat when messages.yml is missing/bugged
    private static final Map<String, String> DEFAULTS = new HashMap<>();
    static {
        DEFAULTS.put("general.prefix", "§8[§6Nations§8]");

        DEFAULTS.put("chat.format", "§7[{channel}] {player}: {message}");

        DEFAULTS.put("errors.generic", "{prefix} §cErrore: {error}");

        DEFAULTS.put("visualizer.enabled", "{prefix} §aVisualizer attivato per la città {town}.");
        DEFAULTS.put("visualizer.disabled", "{prefix} §cVisualizer disattivato.");
        DEFAULTS.put("visualizer.unavailable", "{prefix} §cVisualizer non disponibile su questo server.");

        DEFAULTS.put("actionbar.wilderness", "§7Sei in una zona selvaggia");
        DEFAULTS.put("actionbar.your_territory", "§aSei nel territorio di §6{town}");
        DEFAULTS.put("actionbar.foreign_territory", "§cSei nel territorio di §6{town}");

        DEFAULTS.put("gui.leaderboard.click_message", "{prefix} §6{board} §7- {name}: {value}");
        DEFAULTS.put("gui.town_verbose", "{prefix} §6{town} §7(Membro: {member})");

        // some common fallbacks used across the plugin
        DEFAULTS.put("commands.player_only", "{prefix} §cQuesto comando può essere usato solo da un giocatore.");
        DEFAULTS.put("misc.unknown", "§7Sconosciuto");
    }

    // Fetch raw message by key (dot-separated) and replace placeholders
    public static String get(String key) {
        FileConfiguration cfg = Nations.getInstance().getConfigurationManager().getMessagesConfig();
        if (cfg != null) {
            String val = cfg.getString(key, null);
            if (val != null && !val.isEmpty()) return val;
        }
        // fallback to defaults
        String def = DEFAULTS.get(key);
        if (def != null) return def;

        // warn once per missing key and return a visible placeholder so chat doesn't go blank
        if (warnedMissing.add(key)) {
            try { Nations.getInstance().getLogger().warning("Missing messages.yml key: " + key + " — using placeholder"); } catch (Throwable ignored) {}
        }
        return "{" + key + "}" ;
    }

    public static String format(String key, Map<String, String> replacements) {
        String raw = get(key);
        if (raw == null) raw = "";
        if (replacements != null) {
            for (Map.Entry<String, String> e : replacements.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        // support {prefix} replacement from messages.yml or defaults
        if (raw.contains("{prefix}")) {
            String prefix = get("general.prefix");
            raw = raw.replace("{prefix}", prefix != null ? prefix : "");
        }
        return raw;
    }
}
