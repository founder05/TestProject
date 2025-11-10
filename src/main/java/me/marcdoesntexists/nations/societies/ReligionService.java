package me.marcdoesntexists.nations.societies;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.UUID;

public class ReligionService {
    private static ReligionService instance;
    private final Nations plugin;
    private final SocietiesManager societiesManager;
    private final ConfigurationManager configManager;

    private ReligionService(Nations plugin) {
        this.plugin = plugin;
        this.societiesManager = SocietiesManager.getInstance();
        this.configManager = ConfigurationManager.getInstance();
    }

    public static ReligionService getInstance(Nations plugin) {
        if (instance == null) {
            instance = new ReligionService(plugin);
        }
        return instance;
    }

    public static ReligionService getInstance() {
        return instance;
    }

    public boolean createReligion(String religionName, UUID founderID) {
        try {
            Religion religion = new Religion(religionName, founderID);
            societiesManager.registerReligion(religion);

            plugin.getLogger().info("Religion " + religionName + " created by " + founderID);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create religion: " + e.getMessage());
            return false;
        }
    }

    public boolean createGod(String godName, String description, String domain, UUID creatorId, String religionName) {
        try {
            Religion religion = societiesManager.getReligion(religionName);
            if (religion == null) {
                return false;
            }

            God god = new God(godName, description, domain, creatorId, religion);
            societiesManager.registerGod(god);

            plugin.getLogger().info("God " + godName + " created in religion " + religionName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create god: " + e.getMessage());
            return false;
        }
    }

    public boolean addFollower(String religionName, UUID playerId) {
        try {
            Religion religion = societiesManager.getReligion(religionName);
            if (religion == null) {
                return false;
            }

            religion.addFollower(playerId);
            plugin.getLogger().info(playerId + " is now a follower of " + religionName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add follower: " + e.getMessage());
            return false;
        }
    }

    public boolean addAltarToGod(String godName, String location) {
        try {
            God god = societiesManager.getGod(godName);
            if (god == null) {
                return false;
            }

            god.addAltar(location);
            plugin.getLogger().info("Altar built for " + godName + " at " + location);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add altar: " + e.getMessage());
            return false;
        }
    }

    public Religion getReligion(String religionName) {
        return societiesManager.getReligion(religionName);
    }

    public God getGod(String godName) {
        return societiesManager.getGod(godName);
    }

    public int getFollowerCount(String religionName) {
        Religion religion = getReligion(religionName);
        if (religion != null) {
            return religion.getFollowers().size();
        }
        return 0;
    }

    public int getGodFollowerCount(String godName) {
        God god = getGod(godName);
        if (god != null) {
            return god.getFollowerCount();
        }
        return 0;
    }

    public void addGodPower(String godName, int amount) {
        God god = getGod(godName);
        if (god != null) {
            god.addPower(amount);
        }
    }

    public int getGodPower(String godName) {
        God god = getGod(godName);
        if (god != null) {
            return god.getPower();
        }
        return 0;
    }
}
