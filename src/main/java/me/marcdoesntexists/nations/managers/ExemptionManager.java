package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExemptionManager {
    private final Nations plugin;
    private final File file;
    private FileConfiguration config;
    private final Set<UUID> exemptSet = new HashSet<>();

    public ExemptionManager(Nations plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "exemptions.yml");
        load();
    }

    private void load() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            this.config = YamlConfiguration.loadConfiguration(file);
            for (String s : config.getStringList("exemptions")) {
                try {
                    exemptSet.add(UUID.fromString(s));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load exemptions: " + e.getMessage());
        }
    }

    private void save() {
        try {
            config.set("exemptions", exemptSet.stream().map(UUID::toString).toList());
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save exemptions: " + e.getMessage());
        }
    }

    public boolean isExempt(UUID id) {
        return exemptSet.contains(id);
    }

    public void setExempt(UUID id, boolean exempt) {
        if (exempt) exemptSet.add(id);
        else exemptSet.remove(id);
        save();
    }

    public boolean toggleExempt(UUID id) {
        boolean now = !exemptSet.contains(id);
        setExempt(id, now);
        return now;
    }
}

