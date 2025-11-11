package me.marcdoesntexists.nations.societies;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ConfigurationManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import org.bukkit.configuration.file.FileConfiguration;

public class FeudalService {
    private static FeudalService instance;
    private final Nations plugin;
    private final SocietiesManager societiesManager;
    private final ConfigurationManager configManager;

    private FeudalService(Nations plugin) {
        this.plugin = plugin;
        this.societiesManager = SocietiesManager.getInstance();
        this.configManager = ConfigurationManager.getInstance();
    }

    public static FeudalService getInstance(Nations plugin) {
        if (instance == null) {
            instance = new FeudalService(plugin);
        }
        return instance;
    }

    public static FeudalService getInstance() {
        return instance;
    }

    public boolean createFeudalRelationship(String suzerain, String vassal, double tributeAmount, String obligations) {
        try {
            Kingdom suzRealm = societiesManager.getKingdom(suzerain);
            Kingdom vassalRealm = societiesManager.getKingdom(vassal);

            if (suzRealm == null || vassalRealm == null) {
                return false;
            }

            FeudalRelationship relationship = new FeudalRelationship(suzerain, vassal, tributeAmount, obligations);
            societiesManager.registerFeudalRelationship(relationship);

            suzRealm.addVassal(vassal);
            vassalRealm.setSuzerain(suzerain);

            plugin.getLogger().info(vassal + " became a vassal of " + suzerain);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create feudal relationship: " + e.getMessage());
            return false;
        }
    }

    public boolean payTribute(String vassal, String suzerain, double amount) {
        try {
            Kingdom vassalRealm = societiesManager.getKingdom(vassal);
            Kingdom suzRealm = societiesManager.getKingdom(suzerain);

            if (vassalRealm == null || suzRealm == null) {
                return false;
            }

            if (vassalRealm.getBalance() >= amount) {
                vassalRealm.setTributeAmount((int) amount);
                plugin.getLogger().info(vassal + " paid tribute of " + amount + " to " + suzerain);
                return true;
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to pay tribute: " + e.getMessage());
            return false;
        }
    }

    public boolean betrayVassal(String vassal, String suzerain) {
        try {
            Kingdom vassalRealm = societiesManager.getKingdom(vassal);
            Kingdom suzRealm = societiesManager.getKingdom(suzerain);

            if (vassalRealm == null || suzRealm == null) {
                return false;
            }

            FileConfiguration feudalConfig = configManager.getFeudalConfig();
            boolean rebellionSuccess = Math.random() < feudalConfig.getDouble("feudal-system.betrayal.rebellion-success-chance", 0.30);

            if (rebellionSuccess) {
                vassalRealm.setSuzerain(null);
                suzRealm.removeVassal(vassal);
                plugin.getLogger().info(vassal + " successfully rebelled against " + suzerain);
                return true;
            } else {
                plugin.getLogger().info(vassal + " failed to rebel against " + suzerain);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to attempt rebellion: " + e.getMessage());
            return false;
        }
    }

    public boolean endFeudalRelationship(String vassal, String suzerain) {
        try {
            Kingdom vassalRealm = societiesManager.getKingdom(vassal);
            Kingdom suzRealm = societiesManager.getKingdom(suzerain);

            if (vassalRealm == null || suzRealm == null) {
                return false;
            }

            vassalRealm.setSuzerain(null);
            suzRealm.removeVassal(vassal);

            plugin.getLogger().info("Feudal relationship between " + vassal + " and " + suzerain + " has ended");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to end feudal relationship: " + e.getMessage());
            return false;
        }
    }

    public boolean isVassal(String kingdom1, String kingdom2) {
        Kingdom vassal = societiesManager.getKingdom(kingdom1);
        Kingdom suzerain = societiesManager.getKingdom(kingdom2);

        if (vassal == null || suzerain == null) {
            return false;
        }

        return suzerain.getVassals().contains(kingdom1);
    }

    public String getSuzerain(String vassal) {
        Kingdom kingdom = societiesManager.getKingdom(vassal);
        if (kingdom != null) {
            return kingdom.getSuzerain();
        }
        return null;
    }

    public int getVassalCount(String suzerain) {
        Kingdom kingdom = societiesManager.getKingdom(suzerain);
        if (kingdom != null) {
            return kingdom.getVassals().size();
        }
        return 0;
    }

    public double getTributeAmount(String vassal) {
        Kingdom kingdom = societiesManager.getKingdom(vassal);
        if (kingdom != null) {
            return kingdom.getTributeAmount();
        }
        return 0;
    }
}
