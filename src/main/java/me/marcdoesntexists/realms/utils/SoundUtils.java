package me.marcdoesntexists.realms.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import me.marcdoesntexists.realms.Realms;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class for handling sounds in Paper 1.21.1+
 * Provides robust sound parsing with namespace support and fallbacks
 */
public final class SoundUtils {

    private static final Map<String, Sound> SOUND_CACHE = new HashMap<>();
    private static final Map<String, Sound> NAMESPACE_CACHE = new HashMap<>();
    private static boolean initialized = false;

    static {
        initialize();
    }

    private SoundUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initialize sound caches for fast lookup
     */
    private static void initialize() {
        if (initialized) return;

        try {
            for (Sound sound : Sound.values()) {
                String name = sound.name();

                // Cache original name
                SOUND_CACHE.put(name, sound);

                // Cache uppercase
                SOUND_CACHE.put(name.toUpperCase(), sound);

                // Cache normalized (remove special chars)
                String normalized = normalizeSound(name);
                SOUND_CACHE.put(normalized, sound);

                // Cache with minecraft namespace
                NAMESPACE_CACHE.put("minecraft:" + name.toLowerCase(), sound);
                NAMESPACE_CACHE.put("minecraft:" + normalized.toLowerCase(), sound);
            }
            initialized = true;
        } catch (Exception e) {
            logError("Failed to initialize sound cache", e);
        }
    }

    /**
     * Parse a sound name to a Sound enum value
     * Supports multiple formats:
     * - ENTITY_PLAYER_HURT
     * - entity.player.hurt
     * - minecraft:entity.player.hurt
     * - Entity Player Hurt
     *
     * @param soundName the sound name to parse
     * @return Sound enum or null if not found
     */
    public static Sound parseSound(String soundName) {
        if (soundName == null || soundName.trim().isEmpty()) {
            return null;
        }

        String candidate = soundName.trim();

        // Try direct cache hit first (fastest)
        Sound sound = SOUND_CACHE.get(candidate);
        if (sound != null) return sound;

        // Try uppercase
        sound = SOUND_CACHE.get(candidate.toUpperCase());
        if (sound != null) return sound;

        // Try normalized
        String normalized = normalizeSound(candidate);
        sound = SOUND_CACHE.get(normalized);
        if (sound != null) return sound;

        // Try with namespace
        if (candidate.contains(":")) {
            sound = NAMESPACE_CACHE.get(candidate.toLowerCase());
            if (sound != null) return sound;

            // Try after removing namespace
            String[] parts = candidate.split(":", 2);
            if (parts.length == 2) {
                String afterNamespace = parts[1];
                sound = parseSound(afterNamespace);
                if (sound != null) return sound;
            }
        }

        // Try converting dots to underscores (minecraft:entity.player.hurt -> ENTITY_PLAYER_HURT)
        if (candidate.contains(".")) {
            String dotConverted = candidate.replace(".", "_").toUpperCase();
            sound = SOUND_CACHE.get(dotConverted);
            if (sound != null) return sound;
        }

        // Try converting spaces to underscores
        if (candidate.contains(" ")) {
            String spaceConverted = candidate.replace(" ", "_").toUpperCase();
            sound = SOUND_CACHE.get(spaceConverted);
            if (sound != null) return sound;
        }

        // Last resort: try direct enum lookup
        try {
            return Sound.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Sound not found
        }

        logWarning("Unknown sound name: '" + soundName + "' - no match found in Sound enum");
        return null;
    }

    /**
     * Normalize a sound name by removing special characters and converting to uppercase
     *
     * @param soundName the sound name to normalize
     * @return normalized sound name
     */
    private static String normalizeSound(String soundName) {
        if (soundName == null) return "";
        return soundName.toUpperCase()
                .replace(".", "_")
                .replace("-", "_")
                .replace(" ", "_")
                .replaceAll("[^A-Z0-9_]", "");
    }

    /**
     * Play a sound for a player at their location
     *
     * @param player the player to play the sound for
     * @param soundName the sound name
     * @param volume the volume (1.0 = normal)
     * @param pitch the pitch (1.0 = normal)
     * @return true if sound was played successfully
     */
    public static boolean playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null) return false;

        Sound sound = parseSound(soundName);
        if (sound == null) return false;

        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
            return true;
        } catch (Exception e) {
            logError("Failed to play sound '" + soundName + "' for player " + player.getName(), e);
            return false;
        }
    }

    /**
     * Play a sound for a player at their location with default volume and pitch
     *
     * @param player the player to play the sound for
     * @param soundName the sound name
     * @return true if sound was played successfully
     */
    public static boolean playSound(Player player, String soundName) {
        return playSound(player, soundName, 1.0f, 1.0f);
    }

    /**
     * Play a sound at a specific location
     *
     * @param location the location to play the sound at
     * @param soundName the sound name
     * @param volume the volume
     * @param pitch the pitch
     * @return true if sound was played successfully
     */
    public static boolean playSound(Location location, String soundName, float volume, float pitch) {
        if (location == null || soundName == null) return false;

        Sound sound = parseSound(soundName);
        if (sound == null) return false;

        try {
            if (location.getWorld() != null) {
                location.getWorld().playSound(location, sound, volume, pitch);
                return true;
            }
        } catch (Exception e) {
            logError("Failed to play sound '" + soundName + "' at location", e);
        }
        return false;
    }

    /**
     * Check if a sound name is valid
     *
     * @param soundName the sound name to check
     * @return true if the sound exists
     */
    public static boolean isValidSound(String soundName) {
        return parseSound(soundName) != null;
    }

    /**
     * Get all available sound names
     *
     * @return array of all sound names
     */
    public static String[] getAllSoundNames() {
        Sound[] sounds = Sound.values();
        String[] names = new String[sounds.length];
        for (int i = 0; i < sounds.length; i++) {
            names[i] = sounds[i].name();
        }
        return names;
    }

    /**
     * Reload the sound cache (useful after plugin reload)
     */
    public static void reloadCache() {
        SOUND_CACHE.clear();
        NAMESPACE_CACHE.clear();
        initialized = false;
        initialize();
    }

    // Logging helpers
    private static void logWarning(String message) {
        Realms plugin = Realms.getInstance();
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }

    private static void logError(String message, Throwable throwable) {
        Realms plugin = Realms.getInstance();
        if (plugin != null) {
            plugin.getLogger().log(Level.SEVERE, message, throwable);
        }
    }


}