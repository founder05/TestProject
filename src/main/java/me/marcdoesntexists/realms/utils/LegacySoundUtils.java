package me.marcdoesntexists.realms.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import me.marcdoesntexists.realms.Realms;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class LegacySoundUtils {

    // Version-specific sound mappings
    private static final Map<String, String[]> SOUND_ALIASES = new HashMap<>();
    private static final Map<String, Sound> SOUND_CACHE = new HashMap<>();
    private static boolean initialized = false;

    static {
        initialize();
    }

    private LegacySoundUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initialize sound mappings and cache
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Build cache for all available sounds
            for (Sound sound : Sound.values()) {
                String name = sound.name();
                SOUND_CACHE.put(name, sound);
                SOUND_CACHE.put(name.toUpperCase(), sound);
                SOUND_CACHE.put(normalizeSound(name), sound);
            }

            // Common sound aliases across versions
            registerAliases("CLICK",
                    "CLICK", "UI_BUTTON_CLICK", "minecraft:ui.button.click");

            registerAliases("ORB_PICKUP",
                    "ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP", "minecraft:entity.experience_orb.pickup");

            registerAliases("LEVEL_UP",
                    "LEVEL_UP", "ENTITY_PLAYER_LEVELUP", "minecraft:entity.player.levelup");

            registerAliases("VILLAGER_YES",
                    "VILLAGER_YES", "ENTITY_VILLAGER_YES", "minecraft:entity.villager.yes");

            registerAliases("VILLAGER_NO",
                    "VILLAGER_NO", "ENTITY_VILLAGER_NO", "minecraft:entity.villager.no");

            registerAliases("ANVIL_USE",
                    "ANVIL_USE", "BLOCK_ANVIL_USE", "minecraft:block.anvil.use");

            registerAliases("CHEST_OPEN",
                    "CHEST_OPEN", "BLOCK_CHEST_OPEN", "minecraft:block.chest.open");

            registerAliases("CHEST_CLOSE",
                    "CHEST_CLOSE", "BLOCK_CHEST_CLOSE", "minecraft:block.chest.close");

            registerAliases("ENDERCHEST_OPEN",
                    "ENDERCHEST_OPEN", "BLOCK_ENDER_CHEST_OPEN", "minecraft:block.ender_chest.open");

            registerAliases("ENDERCHEST_CLOSE",
                    "ENDERCHEST_CLOSE", "BLOCK_ENDER_CHEST_CLOSE", "minecraft:block.ender_chest.close");

            registerAliases("ITEM_PICKUP",
                    "ITEM_PICKUP", "ENTITY_ITEM_PICKUP", "minecraft:entity.item.pickup");

            registerAliases("ITEM_BREAK",
                    "ITEM_BREAK", "ENTITY_ITEM_BREAK", "minecraft:entity.item.break");

            registerAliases("PLAYER_HURT",
                    "HURT", "ENTITY_PLAYER_HURT", "minecraft:entity.player.hurt");

            registerAliases("PLAYER_DEATH",
                    "HURT", "ENTITY_PLAYER_DEATH", "minecraft:entity.player.death");

            registerAliases("SUCCESSFUL_HIT",
                    "SUCCESSFUL_HIT", "ENTITY_PLAYER_ATTACK_STRONG", "minecraft:entity.player.attack.strong");

            registerAliases("NOTE_PLING",
                    "NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING", "minecraft:block.note_block.pling");

            registerAliases("NOTE_BASS",
                    "NOTE_BASS", "BLOCK_NOTE_BLOCK_BASS", "minecraft:block.note_block.bass");

            registerAliases("FIREWORK_BLAST",
                    "FIREWORK_BLAST", "ENTITY_FIREWORK_ROCKET_BLAST", "minecraft:entity.firework_rocket.blast");

            registerAliases("EXPLODE",
                    "EXPLODE", "ENTITY_GENERIC_EXPLODE", "minecraft:entity.generic.explode");

            registerAliases("DOOR_OPEN",
                    "DOOR_OPEN", "BLOCK_WOODEN_DOOR_OPEN", "minecraft:block.wooden_door.open");

            registerAliases("DOOR_CLOSE",
                    "DOOR_CLOSE", "BLOCK_WOODEN_DOOR_CLOSE", "minecraft:block.wooden_door.close");

            initialized = true;
        } catch (Exception e) {
            logError("Failed to initialize legacy sound mappings", e);
        }
    }

    /**
     * Register aliases for a sound across different versions
     */
    private static void registerAliases(String key, String... aliases) {
        SOUND_ALIASES.put(key.toUpperCase(), aliases);
    }

    /**
     * Parse a sound name with legacy support
     * Tries multiple naming conventions
     *
     * @param soundName the sound name
     * @return Sound enum or null if not found
     */
    public static Sound parseSound(String soundName) {
        if (soundName == null || soundName.trim().isEmpty()) {
            return null;
        }

        String candidate = soundName.trim();

        // Try direct cache lookup
        Sound sound = SOUND_CACHE.get(candidate);
        if (sound != null) return sound;

        sound = SOUND_CACHE.get(candidate.toUpperCase());
        if (sound != null) return sound;

        // Try normalized
        String normalized = normalizeSound(candidate);
        sound = SOUND_CACHE.get(normalized);
        if (sound != null) return sound;

        // Try with namespace removal
        if (candidate.contains(":")) {
            String[] parts = candidate.split(":", 2);
            if (parts.length == 2) {
                sound = parseSound(parts[1]);
                if (sound != null) return sound;
            }
        }

        // Try converting dots to underscores
        if (candidate.contains(".")) {
            String dotConverted = candidate.replace(".", "_");
            sound = parseSound(dotConverted);
            if (sound != null) return sound;
        }

        // Try alias lookup
        for (Map.Entry<String, String[]> entry : SOUND_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (alias.equalsIgnoreCase(candidate) ||
                        normalizeSound(alias).equals(normalized)) {
                    // Found in aliases, try all other aliases
                    for (String tryAlias : entry.getValue()) {
                        sound = SOUND_CACHE.get(tryAlias.toUpperCase());
                        if (sound != null) return sound;
                        sound = SOUND_CACHE.get(normalizeSound(tryAlias));
                        if (sound != null) return sound;
                    }
                }
            }
        }

        // Last resort: try direct enum lookup
        try {
            return Sound.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Not found
        }

        logWarning("Unknown sound (legacy): '" + soundName + "' - tried all version variants");
        return null;
    }

    /**
     * Parse sound with fallback support
     * If primary sound not found, tries fallback
     *
     * @param soundName primary sound name
     * @param fallback fallback sound name
     * @return Sound enum or null
     */
    public static Sound parseSoundWithFallback(String soundName, String fallback) {
        Sound sound = parseSound(soundName);
        if (sound != null) return sound;

        if (fallback != null) {
            return parseSound(fallback);
        }

        return null;
    }

    /**
     * Normalize sound name
     */
    private static String normalizeSound(String soundName) {
        if (soundName == null) return "";
        return soundName.toUpperCase()
                .replace(".", "_")
                .replace("-", "_")
                .replace(" ", "_")
                .replace(":", "_")
                .replaceAll("[^A-Z0-9_]", "");
    }

    /**
     * Play sound for player with legacy support
     */
    public static boolean playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null) return false;

        Sound sound = parseSound(soundName);
        if (sound == null) return false;

        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
            return true;
        } catch (Exception e) {
            logError("Failed to play legacy sound '" + soundName + "'", e);
            return false;
        }
    }

    /**
     * Play sound with default volume and pitch
     */
    public static boolean playSound(Player player, String soundName) {
        return playSound(player, soundName, 1.0f, 1.0f);
    }

    /**
     * Play sound at location
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
            logError("Failed to play legacy sound at location", e);
        }
        return false;
    }

    /**
     * Check if sound is valid
     */
    public static boolean isValidSound(String soundName) {
        return parseSound(soundName) != null;
    }

    /**
     * Get the modern equivalent of a legacy sound name
     *
     * @param legacyName legacy sound name (e.g., "CLICK")
     * @return modern sound name or original if no mapping exists
     */
    public static String getModernName(String legacyName) {
        if (legacyName == null) return null;

        String normalized = normalizeSound(legacyName);
        String[] aliases = SOUND_ALIASES.get(normalized);

        if (aliases != null && aliases.length > 0) {
            // Return the most modern variant (usually the last one)
            for (int i = aliases.length - 1; i >= 0; i--) {
                if (SOUND_CACHE.containsKey(normalizeSound(aliases[i]))) {
                    return aliases[i];
                }
            }
        }

        return legacyName;
    }

    /**
     * Convert a modern sound name to its legacy equivalent (1.8 style)
     *
     * @param modernName modern sound name
     * @return legacy sound name or original if no mapping
     */
    public static String getLegacyName(String modernName) {
        if (modernName == null) return null;

        String normalized = normalizeSound(modernName);

        for (Map.Entry<String, String[]> entry : SOUND_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (normalizeSound(alias).equals(normalized)) {
                    // Return first alias (usually the oldest/legacy one)
                    return entry.getValue()[0];
                }
            }
        }

        return modernName;
    }

    /**
     * Reload cache
     */
    public static void reloadCache() {
        SOUND_CACHE.clear();
        SOUND_ALIASES.clear();
        initialized = false;
        initialize();
    }

    // Logging
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

    /**
     * Legacy sound constants for backwards compatibility
     */
    public static final class LegacySounds {
        // 1.8 - 1.12 style names
        public static final String CLICK = "CLICK";
        public static final String ORB_PICKUP = "ORB_PICKUP";
        public static final String LEVEL_UP = "LEVEL_UP";
        public static final String ANVIL_USE = "ANVIL_USE";
        public static final String CHEST_OPEN = "CHEST_OPEN";
        public static final String CHEST_CLOSE = "CHEST_CLOSE";
        public static final String ITEM_PICKUP = "ITEM_PICKUP";
        public static final String VILLAGER_YES = "VILLAGER_YES";
        public static final String VILLAGER_NO = "VILLAGER_NO";
        public static final String NOTE_PLING = "NOTE_PLING";
        public static final String EXPLODE = "EXPLODE";

        private LegacySounds() {}
    }

    /**
     * Version detector utility
     */
    public static final class VersionDetector {
        private static String serverVersion = null;
        private static int majorVersion = -1;
        private static int minorVersion = -1;

        /**
         * Get server version string (e.g., "1.21.1")
         */
        public static String getServerVersion() {
            if (serverVersion == null) {
                try {
                    String version = org.bukkit.Bukkit.getVersion();
                    // Extract version like "1.21.1" from "(MC: 1.21.1)"
                    if (version.contains("MC:")) {
                        version = version.substring(version.indexOf("MC:") + 3);
                        version = version.substring(0, version.indexOf(")")).trim();
                        serverVersion = version;
                    }
                } catch (Exception e) {
                    serverVersion = "unknown";
                }
            }
            return serverVersion;
        }

        /**
         * Get major version (e.g., 21 for 1.21.1)
         */
        public static int getMajorVersion() {
            if (majorVersion == -1) {
                try {
                    String version = getServerVersion();
                    String[] parts = version.split("\\.");
                    if (parts.length >= 2) {
                        majorVersion = Integer.parseInt(parts[1]);
                    }
                } catch (Exception e) {
                    majorVersion = 0;
                }
            }
            return majorVersion;
        }

        /**
         * Get minor version (e.g., 1 for 1.21.1)
         */
        public static int getMinorVersion() {
            if (minorVersion == -1) {
                try {
                    String version = getServerVersion();
                    String[] parts = version.split("\\.");
                    if (parts.length >= 3) {
                        minorVersion = Integer.parseInt(parts[2]);
                    }
                } catch (Exception e) {
                    minorVersion = 0;
                }
            }
            return minorVersion;
        }

        /**
         * Check if server is 1.13 or newer (when sounds changed)
         */
        public static boolean is1_13Plus() {
            return getMajorVersion() >= 13;
        }

        /**
         * Check if server is 1.9 or newer
         */
        public static boolean is1_9Plus() {
            return getMajorVersion() >= 9;
        }

        /**
         * Check if server is legacy (1.8 or older)
         */
        public static boolean isLegacy() {
            return getMajorVersion() <= 8;
        }

        private VersionDetector() {}
    }
}