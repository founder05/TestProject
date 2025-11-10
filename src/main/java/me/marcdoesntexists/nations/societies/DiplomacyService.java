package me.marcdoesntexists.nations.societies;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.UUID;

public class DiplomacyService {
    private static DiplomacyService instance;
    private final Nations plugin;
    private final SocietiesManager societiesManager;
    private final ConfigurationManager configManager;

    private DiplomacyService(Nations plugin) {
        this.plugin = plugin;
        this.societiesManager = SocietiesManager.getInstance();
        this.configManager = ConfigurationManager.getInstance();
    }

    public static DiplomacyService getInstance(Nations plugin) {
        if (instance == null) {
            instance = new DiplomacyService(plugin);
        }
        return instance;
    }

    public static DiplomacyService getInstance() {
        return instance;
    }

    public boolean createAlliance(String allianceName, String founderKingdom, UUID founderId) {
        try {
            Alliance alliance = new Alliance(allianceName, founderKingdom);
            societiesManager.registerAlliance(alliance);
            
            Kingdom founder = societiesManager.getKingdom(founderKingdom);
            if (founder != null) {
                founder.joinAlliance(allianceName);
            }

            plugin.getLogger().info("Alliance " + allianceName + " created by " + founderKingdom);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create alliance: " + e.getMessage());
            return false;
        }
    }

    public boolean joinAlliance(String allianceName, String kingdom) {
        try {
            for (Alliance alliance : societiesManager.getAlliances()) {
                if (alliance.getName().equalsIgnoreCase(allianceName)) {
                    if (alliance.addMember(kingdom)) {
                        Kingdom k = societiesManager.getKingdom(kingdom);
                        if (k != null) {
                            k.joinAlliance(allianceName);
                        }
                        plugin.getLogger().info(kingdom + " joined " + allianceName);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to join alliance: " + e.getMessage());
            return false;
        }
    }

    public boolean leaveAlliance(String allianceName, String kingdom) {
        try {
            for (Alliance alliance : societiesManager.getAlliances()) {
                if (alliance.getName().equalsIgnoreCase(allianceName)) {
                    if (alliance.removeMember(kingdom)) {
                        Kingdom k = societiesManager.getKingdom(kingdom);
                        if (k != null) {
                            k.leaveAlliance(allianceName);
                        }
                        plugin.getLogger().info(kingdom + " left " + allianceName);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to leave alliance: " + e.getMessage());
            return false;
        }
    }

    public Alliance getAlliance(String allianceName) {
        for (Alliance alliance : societiesManager.getAlliances()) {
            if (alliance.getName().equalsIgnoreCase(allianceName)) {
                return alliance;
            }
        }
        return null;
    }

    public boolean createTreaty(String treatyName, String kingdom1, String kingdom2, Treaty.TreatyType type, long durationDays) {
        try {
            Treaty treaty = new Treaty(treatyName, kingdom1, kingdom2, type, durationDays);
            societiesManager.registerTreaty(treaty);

            Kingdom k1 = societiesManager.getKingdom(kingdom1);
            Kingdom k2 = societiesManager.getKingdom(kingdom2);

            if (k1 != null && k2 != null) {
                k1.addTreaty(treatyName);
                k2.addTreaty(treatyName);
            }

            plugin.getLogger().info("Treaty " + treatyName + " signed between " + kingdom1 + " and " + kingdom2);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create treaty: " + e.getMessage());
            return false;
        }
    }

    public void enforceTreaties() {
        for (Treaty treaty : societiesManager.getAllTreaties()) {
            if (treaty.isExpired() && treaty.getStatus() == Treaty.TreatyStatus.ACTIVE) {
                treaty.setStatus(Treaty.TreatyStatus.EXPIRED);
                plugin.getLogger().info("Treaty " + treaty.getName() + " has expired");
            }
        }
    }

    public boolean violateTreaty(String treatyName, String violatingKingdom) {
        for (Treaty treaty : societiesManager.getAllTreaties()) {
            if (treaty.getName().equalsIgnoreCase(treatyName)) {
                if (treaty.getStatus() == Treaty.TreatyStatus.ACTIVE) {
                    treaty.setStatus(Treaty.TreatyStatus.VIOLATED);
                    plugin.getLogger().info(violatingKingdom + " violated treaty " + treatyName);
                    return true;
                }
            }
        }
        return false;
    }

    public int getAllianceMemberCount(String allianceName) {
        Alliance alliance = getAlliance(allianceName);
        if (alliance != null) {
            return alliance.getMemberCount();
        }
        return 0;
    }

    public boolean isAllianceMember(String allianceName, String kingdom) {
        Alliance alliance = getAlliance(allianceName);
        if (alliance != null) {
            return alliance.isMember(kingdom);
        }
        return false;
    }
}
