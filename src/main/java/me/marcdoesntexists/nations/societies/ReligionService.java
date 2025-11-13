package me.marcdoesntexists.nations.societies;

import me.marcdoesntexists.nations.Nations;

/**
 * ReligionService stub - religion support removed.
 */
@Deprecated
public class ReligionService {
    private static ReligionService instance;
    private final Nations plugin;

    private ReligionService(Nations plugin) {
        this.plugin = plugin;
    }

    public static ReligionService getInstance(Nations plugin) {
        if (instance == null) instance = new ReligionService(plugin);
        return instance;
    }

    public static ReligionService getInstance() {
        return instance;
    }
}
