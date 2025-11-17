package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class SaveAtomicTester {
    /**
     * Run a simple save test that creates a small YAML and attempts to save it using DataManager's atomic save.
     * Call this from plugin enable or via a command to validate behavior on the target system.
     */
    public static void runTest(Realms plugin) {
        try {
            plugin.getLogger().info("Starting SaveAtomicTester test...");
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("test.timestamp", System.currentTimeMillis());
            cfg.set("test.note", "This is a diagnostic save from SaveAtomicTester");

            File out = new File(plugin.getDataFolder(), "tmp/test-save.yml");
            // Ensure parent
            if (out.getParentFile() != null) out.getParentFile().mkdirs();

            DataManager.getInstance().saveConfigAtomicPublic(cfg, out);
            plugin.getLogger().info("SaveAtomicTester completed successfully: " + out.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().severe("SaveAtomicTester failed: " + e.toString());
            try {
                for (StackTraceElement ste : e.getStackTrace()) plugin.getLogger().severe(ste.toString());
            } catch (Exception ignored) {
            }
        }
    }
}

