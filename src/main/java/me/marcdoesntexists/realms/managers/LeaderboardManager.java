package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.societies.Empire;
import me.marcdoesntexists.realms.societies.Kingdom;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private static LeaderboardManager instance;
    private final Realms plugin;
    private final SocietiesManager societiesManager;
    private final DataManager dataManager;

    private LeaderboardManager(Realms plugin) {
        this.plugin = plugin;
        this.societiesManager = SocietiesManager.getInstance();
        this.dataManager = DataManager.getInstance();
    }

    public static LeaderboardManager getInstance(Realms plugin) {
        if (instance == null) {
            instance = new LeaderboardManager(plugin);
        }
        return instance;
    }

    public static LeaderboardManager getInstance() {
        return instance;
    }

    // ========== TOWN LEADERBOARDS ==========

    public List<LeaderboardEntry> getTopTownsByPopulation(int limit) {
        return societiesManager.getAllTowns().stream()
                .sorted((t1, t2) -> Integer.compare(t2.getMembers().size(), t1.getMembers().size()))
                .limit(limit)
                .map(town -> new LeaderboardEntry(
                        town.getName(),
                        town.getMembers().size(),
                        "members"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopTownsByWealth(int limit) {
        return societiesManager.getAllTowns().stream()
                .sorted((t1, t2) -> Integer.compare(t2.getBalance(), t1.getBalance()))
                .limit(limit)
                .map(town -> new LeaderboardEntry(
                        town.getName(),
                        town.getBalance(),
                        "balance"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopTownsByClaims(int limit) {
        return societiesManager.getAllTowns().stream()
                .sorted((t1, t2) -> Integer.compare(t2.getClaims().size(), t1.getClaims().size()))
                .limit(limit)
                .map(town -> new LeaderboardEntry(
                        town.getName(),
                        town.getClaims().size(),
                        "claims"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopTownsByLevel(int limit) {
        return societiesManager.getAllTowns().stream()
                .sorted((t1, t2) -> Integer.compare(t2.getProgressionLevel(), t1.getProgressionLevel()))
                .limit(limit)
                .map(town -> new LeaderboardEntry(
                        town.getName(),
                        town.getProgressionLevel(),
                        "level"
                ))
                .collect(Collectors.toList());
    }

    // ========== KINGDOM LEADERBOARDS ==========

    public List<LeaderboardEntry> getTopKingdomsByPopulation(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(
                        getTotalKingdomPopulation(k2),
                        getTotalKingdomPopulation(k1)
                ))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        getTotalKingdomPopulation(kingdom),
                        "population"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByTowns(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getTowns().size(), k1.getTowns().size()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getTowns().size(),
                        "towns"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByWealth(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getBalance(), k1.getBalance()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getBalance(),
                        "balance"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByVassals(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getVassals().size(), k1.getVassals().size()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getVassals().size(),
                        "vassals"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByAllies(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getAllies().size(), k1.getAllies().size()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getAllies().size(),
                        "allies"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByEnemies(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getEnemies().size(), k1.getEnemies().size()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getEnemies().size(),
                        "enemies"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByWars(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getWars().size(), k1.getWars().size()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getWars().size(),
                        "wars"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByTreaties(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getTreaties().size(), k1.getTreaties().size()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getTreaties().size(),
                        "treaties"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopKingdomsByLevel(int limit) {
        return societiesManager.getAllKingdoms().stream()
                .sorted((k1, k2) -> Integer.compare(k2.getProgressionLevel(), k1.getProgressionLevel()))
                .limit(limit)
                .map(kingdom -> new LeaderboardEntry(
                        kingdom.getName(),
                        kingdom.getProgressionLevel(),
                        "level"
                ))
                .collect(Collectors.toList());
    }

    // ========== EMPIRE LEADERBOARDS ==========

    public List<LeaderboardEntry> getTopEmpiresByPopulation(int limit) {
        return societiesManager.getAllEmpires().stream()
                .sorted((e1, e2) -> Integer.compare(
                        getTotalEmpirePopulation(e2),
                        getTotalEmpirePopulation(e1)
                ))
                .limit(limit)
                .map(empire -> new LeaderboardEntry(
                        empire.getName(),
                        getTotalEmpirePopulation(empire),
                        "population"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopEmpiresByKingdoms(int limit) {
        return societiesManager.getAllEmpires().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getKingdoms().size(), e1.getKingdoms().size()))
                .limit(limit)
                .map(empire -> new LeaderboardEntry(
                        empire.getName(),
                        empire.getKingdoms().size(),
                        "kingdoms"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopEmpiresByTerritory(int limit) {
        return societiesManager.getAllEmpires().stream()
                .sorted((e1, e2) -> Integer.compare(
                        getTotalEmpireClaims(e2),
                        getTotalEmpireClaims(e1)
                ))
                .limit(limit)
                .map(empire -> new LeaderboardEntry(
                        empire.getName(),
                        getTotalEmpireClaims(empire),
                        "claims"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopEmpiresByLevel(int limit) {
        return societiesManager.getAllEmpires().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getProgressionLevel(), e1.getProgressionLevel()))
                .limit(limit)
                .map(empire -> new LeaderboardEntry(
                        empire.getName(),
                        empire.getProgressionLevel(),
                        "level"
                ))
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopEmpiresByWealth(int limit) {
        return societiesManager.getAllEmpires().stream()
                .sorted((e1, e2) -> Integer.compare(getTotalEmpireWealth(e2), getTotalEmpireWealth(e1)))
                .limit(limit)
                .map(empire -> new LeaderboardEntry(
                        empire.getName(),
                        getTotalEmpireWealth(empire),
                        "balance"
                ))
                .collect(Collectors.toList());
    }

    // ========== PLAYER LEADERBOARDS ==========

    public List<LeaderboardEntry> getTopPlayersByWealth(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data != null) {
                entries.add(new LeaderboardEntry(
                        player.getName() != null ? player.getName() : "Unknown",
                        data.getMoney(),
                        "money"
                ));
            }
        }

        return entries.stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopPlayersByNobleTier(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data != null && data.getNobleTier() != null) {
                entries.add(new LeaderboardEntry(
                        player.getName() != null ? player.getName() : "Unknown",
                        data.getNobleTier().getLevel(),
                        "noble_tier",
                        data.getNobleTier().name()
                ));
            }
        }

        return entries.stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopPlayersByJobExp(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data != null) {
                entries.add(new LeaderboardEntry(
                        player.getName() != null ? player.getName() : "Unknown",
                        data.getJobExperience(),
                        "job_exp"
                ));
            }
        }
        return entries.stream().sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())).limit(limit).collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopPlayersByClassExp(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data != null) {
                entries.add(new LeaderboardEntry(
                        player.getName() != null ? player.getName() : "Unknown",
                        data.getClassExperience(),
                        "class_exp"
                ));
            }
        }
        return entries.stream().sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())).limit(limit).collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getTopPlayersByNobleExp(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data != null) {
                entries.add(new LeaderboardEntry(
                        player.getName() != null ? player.getName() : "Unknown",
                        data.getNobleTierExperience(),
                        "noble_exp"
                ));
            }
        }
        return entries.stream().sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())).limit(limit).collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    private int getTotalKingdomPopulation(Kingdom kingdom) {
        int total = 0;
        for (String townName : kingdom.getTowns()) {
            Town town = societiesManager.getTown(townName);
            if (town != null) {
                total += town.getMembers().size();
            }
        }
        return total;
    }

    private int getTotalEmpirePopulation(Empire empire) {
        int total = 0;
        for (String kingdomName : empire.getKingdoms()) {
            Kingdom kingdom = societiesManager.getKingdom(kingdomName);
            if (kingdom != null) {
                total += getTotalKingdomPopulation(kingdom);
            }
        }
        return total;
    }

    private int getTotalEmpireClaims(Empire empire) {
        int total = 0;
        for (String kingdomName : empire.getKingdoms()) {
            Kingdom kingdom = societiesManager.getKingdom(kingdomName);
            if (kingdom != null) {
                for (String townName : kingdom.getTowns()) {
                    Town town = societiesManager.getTown(townName);
                    if (town != null) {
                        total += town.getClaims().size();
                    }
                }
            }
        }
        return total;
    }

    private int getTotalEmpireWealth(Empire empire) {
        int total = 0;
        for (String kingdomName : empire.getKingdoms()) {
            Kingdom kingdom = societiesManager.getKingdom(kingdomName);
            if (kingdom != null) {
                total += kingdom.getBalance();
            }
        }
        return total;
    }

    // ========== LEADERBOARD ENTRY CLASS ==========

    public static class LeaderboardEntry {
        private final String name;
        private final long value;
        private final String metric;
        private final String extra;

        public LeaderboardEntry(String name, long value, String metric) {
            this(name, value, metric, null);
        }

        public LeaderboardEntry(String name, long value, String metric, String extra) {
            this.name = name;
            this.value = value;
            this.metric = metric;
            this.extra = extra;
        }

        public String getName() {
            return name;
        }

        public long getValue() {
            return value;
        }

        public String getMetric() {
            return metric;
        }

        public String getExtra() {
            return extra;
        }

        public String getFormattedValue() {
            return switch (metric) {
                case "money", "balance" -> "$" + value;
                case "members", "population", "towns", "kingdoms", "vassals", "claims", "followers", "power" ->
                        String.valueOf(value);
                case "level" -> "Level " + value;
                case "noble_tier" -> extra != null ? extra : String.valueOf(value);
                default -> String.valueOf(value);
            };
        }
    }
}
