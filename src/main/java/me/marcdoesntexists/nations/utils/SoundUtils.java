package me.marcdoesntexists.nations.utils;

import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;

public final class SoundUtils {
    // Build a single lookup map to avoid repeated calls to Sound.values()
    private static final Map<String, Sound> LOOKUP = new HashMap<>();

    static {
        for (Sound s : Sound.values()) {
            String name = s.name();
            LOOKUP.put(name, s);
            LOOKUP.put(name.toUpperCase().replace('-', '_').replace(' ', '_').replace('.', '_'), s);
            LOOKUP.put(name.replaceAll("[^A-Z0-9_]", ""), s);
        }
    }

    private SoundUtils() {
    }

    public static Sound parseSound(String name) {
        if (name == null) return null;
        String candidate = name.trim();
        if (candidate.isEmpty()) return null;

        // Try direct valueOf first (fast path)
        try {
            return Sound.valueOf(candidate.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        // Normalized forms
        String normalized = candidate.toUpperCase().replace('-', '_').replace(' ', '_').replace('.', '_');
        Sound s = LOOKUP.get(normalized);
        if (s != null) return s;

        String compact = normalized.replaceAll("[^A-Z0-9_]", "");
        return LOOKUP.get(compact);
    }
}
