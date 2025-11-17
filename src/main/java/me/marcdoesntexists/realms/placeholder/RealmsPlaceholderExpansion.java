package me.marcdoesntexists.realms.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.*;
import me.marcdoesntexists.realms.societies.Alliance;
import me.marcdoesntexists.realms.societies.Empire;
import me.marcdoesntexists.realms.societies.Kingdom;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.stream.Collectors;

public class RealmsPlaceholderExpansion extends PlaceholderExpansion {

    private final Realms plugin = Realms.getInstance();

    @Override
    public boolean canRegister() {
        return plugin != null;
    }

    @Override
    public boolean register() {
        return super.register();
    }

    @Override
    public boolean persist() {
        // keep the expansion across reloads
        return true;
    }

    @Override
    public String getIdentifier() {
        return "Realms";
    }

    @Override
    public String getAuthor() {
        return "MarcDoesntExists";
    }

    @Override
    public String getVersion() {
        return plugin != null ? plugin.getDescription().getVersion() : "1.0";
    }

    // Compatibility: PAPI may call this (older versions)
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        return handleRequest(player, player, identifier);
    }

    // Modern PAPI method for offline player support
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String identifier) {
        Player online = offlinePlayer != null ? plugin.getServer().getPlayer(offlinePlayer.getUniqueId()) : null;
        return handleRequest(offlinePlayer, online, identifier);
    }

    // Common handler for both online and offline
    private String handleRequest(OfflinePlayer offline, Player online, String identifier) {
        if (identifier == null || identifier.isEmpty()) return "";
        String id = identifier.toLowerCase(Locale.ROOT);

        // Resolve managers/services
        DataManager dataManager = plugin.getDataManager();
        SocietiesManager societies = plugin.getSocietiesManager();
        PvPManager pvp = PvPManager.getInstance();
        EconomyManager econ = plugin.getEconomyManager();
        LeaderboardManager lbm = LeaderboardManager.getInstance(plugin);

        OfflinePlayer player = offline;

        // Helper to get player data by OfflinePlayer
        PlayerData pd = null;
        try {
            if (player != null) pd = dataManager.getPlayerData(player.getUniqueId());
        } catch (Throwable ignored) {}

        // --- PLAYER / BASIC ---
        if (id.equals("player_money") || id.equals("player_balance") || id.equals("player_money_internal")) {
            return pd != null ? String.valueOf(pd.getMoney()) : "0";
        }

        if (id.equals("player_town") || id.equals("town")) {
            return pd != null && pd.getTown() != null ? pd.getTown() : "";
        }

        if (id.equals("player_job") || id.equals("job")) {
            return pd != null && pd.getJob() != null ? pd.getJob() : "";
        }

        if (id.equals("player_social_class")) {
            return pd != null ? pd.getSocialClass() : "Commoner";
        }

        if (id.equals("player_noble_tier") || id.equals("noble_tier")) {
            return pd != null && pd.getNobleTier() != null ? pd.getNobleTier().name() : "COMMONER";
        }

        if (id.equals("player_chat_channel")) {
            return pd != null ? pd.getChatChannel() : "GLOBAL";
        }

        // --- TOWN PLACEHOLDERS ---
        if (id.startsWith("town_")) {
            if (pd == null || pd.getTown() == null) return "";
            Town t = societies.getTown(pd.getTown());
            if (t == null) return "";

            return switch (id) {
                case "town_balance" -> String.valueOf(t.getBalance());
                case "town_claims" -> String.valueOf(t.getClaims().size());
                case "town_members" -> String.valueOf(t.getMembers().size());
                case "town_members_list" -> t.getMembers().stream().map(u -> {
                    var op = Bukkit.getOfflinePlayer(u);
                    return op != null && op.getName() != null ? op.getName() : u.toString();
                }).collect(Collectors.joining(", "));
                case "town_mayor" -> {
                    var mayor = Bukkit.getOfflinePlayer(t.getMayor());
                    yield mayor != null && mayor.getName() != null ? mayor.getName() : t.getMayor().toString();
                }
                case "town_level" -> String.valueOf(t.getProgressionLevel());
                case "town_progression_xp" -> String.valueOf(t.getProgressionExperience());
                case "town_functional_areas_count" -> String.valueOf(t.getFunctionalAreas().size());
                default -> "";
            };
        }

        // --- KINGDOM PLACEHOLDERS ---
        if (id.startsWith("kingdom_")) {
            String kName = null;
            if (pd != null && pd.getTown() != null) {
                Town t = societies.getTown(pd.getTown());
                if (t != null) kName = t.getKingdom();
            }
            if (kName == null) return "";
            Kingdom k = societies.getKingdom(kName);
            if (k == null) return "";

            return switch (id) {
                case "kingdom_name" -> k.getName();
                case "kingdom_capital" -> k.getCapital();
                case "kingdom_towns_count" -> String.valueOf(k.getTowns().size());
                case "kingdom_balance" -> String.valueOf(k.getBalance());
                case "kingdom_level" -> String.valueOf(k.getProgressionLevel());
                case "kingdom_allies_count" -> String.valueOf(k.getAllies().size());
                case "kingdom_enemies_count" -> String.valueOf(k.getEnemies().size());
                case "kingdom_vassals_count" -> String.valueOf(k.getVassals().size());
                case "kingdom_wars_count" -> String.valueOf(k.getWars().size());
                case "kingdom_treaties_count" -> String.valueOf(k.getTreaties().size());
                case "kingdom_functional_areas_count" -> String.valueOf(k.getFunctionalAreas().size());
                case "kingdom_suzerain" -> (k.getSuzerain() != null ? k.getSuzerain() : "");
                case "kingdom_tribute_amount" -> String.valueOf(k.getTributeAmount());
                case "kingdom_prestige" -> String.valueOf(k.getPrestige());
                case "kingdom_infamy" -> String.valueOf(k.getInfamy());
                case "kingdom_loyalty" -> String.valueOf(k.getLoyalty());
                case "kingdom_rebellion_chance" -> String.format("%.1f%%", k.getRebellionChance() * 100);
                default -> "";
            };
        }

        // --- EMPIRE PLACEHOLDERS ---
        if (id.startsWith("empire_")) {
            String empireName = null;
            if (pd != null && pd.getTown() != null) {
                Town t = societies.getTown(pd.getTown());
                if (t != null && t.getKingdom() != null) {
                    Kingdom k = societies.getKingdom(t.getKingdom());
                    if (k != null && k.getEmpire() != null) empireName = k.getEmpire();
                }
            }
            if (empireName == null) return "";
            Empire e = societies.getEmpire(empireName);
            if (e == null) return "";

            return switch (id) {
                case "empire_name" -> e.getName();
                case "empire_kingdoms_count" -> String.valueOf(e.getKingdoms().size());
                case "empire_level" -> String.valueOf(e.getProgressionLevel());
                case "empire_functional_areas_count" -> String.valueOf(e.getFunctionalAreas().size());
                default -> "";
            };
        }

        // --- ALLIANCE PLACEHOLDERS ---
        if (id.startsWith("alliance_")) {
            if (pd == null || pd.getTown() == null) return "";
            Town t = societies.getTown(pd.getTown());
            if (t == null || t.getKingdom() == null) return "";
            String playerKingdom = t.getKingdom();
            Alliance found = null;
            for (Alliance a : societies.getAlliances()) {
                if (a.isMember(playerKingdom) || a.getLeader().equals(playerKingdom)) {
                    found = a;
                    break;
                }
            }
            if (found == null) return "";
            switch (id) {
                case "alliance_name":
                    return found.getName();
                case "alliance_leader":
                    return found.getLeader();
                case "alliance_members_count":
                    return String.valueOf(found.getMemberCount());
                case "alliance_description":
                    return found.getDescription();
                case "alliance_tier":
                    return found.getAllianceTier().name();
                case "alliance_total_contributions":
                    return String.valueOf(found.getTotalContributions());
                default:
                    return "";
            }
        }

        // --- GLOBAL COUNTS ---
        if (id.equals("total_towns")) return String.valueOf(societies.getAllTowns().size());
        if (id.equals("total_kingdoms")) return String.valueOf(societies.getAllKingdoms().size());
        if (id.equals("total_empires")) return String.valueOf(societies.getAllEmpires().size());

        // --- PLUGIN / MISC ---
        if (id.equals("plugin_version")) {
            return plugin != null ? (plugin.getDescription() != null ? plugin.getDescription().getVersion() : "") : "";
        }

        // --- CLAIM / VISUALIZER ---
        if (id.equals("claim_owner_at_player")) {
            try {
                ClaimManager cm = plugin.getClaimManager();
                if (cm == null) return "";
                var claim = cm.getClaimAt(online != null ? online.getLocation() : (player != null ? Bukkit.getWorlds().get(0).getSpawnLocation() : null));
                return claim != null ? claim.getTownName() : "";
            } catch (Throwable ignored) {
                return "";
            }
        }

        if (id.equals("claim_owner_uuid_at_player")) {
            try {
                ClaimManager cm = plugin.getClaimManager();
                if (cm == null) return "";
                var claim = cm.getClaimAt(online != null ? online.getLocation() : (player != null ? Bukkit.getWorlds().get(0).getSpawnLocation() : null));
                return claim != null ? (claim.getOwnerUuid() != null ? claim.getOwnerUuid() : "") : "";
            } catch (Throwable ignored) {
                return "";
            }
        }

        if (id.equals("visualizer_active_for_player")) {
            try {
                var vis = plugin.getClaimVisualizer();
                if (vis == null) return "false";
                return (online != null) ? (vis.isVisualizing(online) ? "true" : "false") : "false";
            } catch (Throwable ignored) {
                return "false";
            }
        }

        // --- PLAYER ROLE IN TOWN ---
        if (id.equals("player_role_in_town")) {
            if (pd == null || pd.getTown() == null) return "NONE";
            Town t = societies.getTown(pd.getTown());
            if (t == null) return "NONE";
            if (player != null && t.isMayor(player.getUniqueId())) return "MAYOR";
            if (player != null && t.getMembers().contains(player.getUniqueId())) return "MEMBER";
            return "NONE";
        }

        // --- CAPITAL / INFO PLACEHOLDERS ---
        if (id.equals("kingdom_capital_town")) {
            if (pd == null || pd.getTown() == null) return "";
            Town t = societies.getTown(pd.getTown());
            if (t == null || t.getKingdom() == null) return "";
            Kingdom k = societies.getKingdom(t.getKingdom());
            return k != null ? (k.getCapital() != null ? k.getCapital() : "") : "";
        }

        if (id.equals("empire_capital_kingdom")) {
            if (pd == null || pd.getTown() == null) return "";
            Town t = societies.getTown(pd.getTown());
            if (t == null || t.getKingdom() == null) return "";
            Kingdom k = societies.getKingdom(t.getKingdom());
            if (k == null || k.getEmpire() == null) return "";
            Empire e = societies.getEmpire(k.getEmpire());
            return e != null ? (e.getCapital() != null ? e.getCapital() : "") : "";
        }

        // --- WARS COUNT PLACEHOLDERS ---
        if (id.equals("wars_count_kingdom")) {
            if (pd == null || pd.getTown() == null) return "0";
            Town t = societies.getTown(pd.getTown());
            if (t == null || t.getKingdom() == null) return "0";
            Kingdom k = societies.getKingdom(t.getKingdom());
            return k != null ? String.valueOf(k.getWars().size()) : "0";
        }

        if (id.equals("wars_count_empire")) {
            if (pd == null || pd.getTown() == null) return "0";
            Town t = societies.getTown(pd.getTown());
            if (t == null || t.getKingdom() == null) return "0";
            Kingdom k = societies.getKingdom(t.getKingdom());
            if (k == null || k.getEmpire() == null) return "0";
            Empire e = societies.getEmpire(k.getEmpire());
            if (e == null) return "0";
            int sum = 0;
            for (String kn : e.getKingdoms()) {
                Kingdom kk = societies.getKingdom(kn);
                if (kk != null) sum += kk.getWars().size();
            }
            return String.valueOf(sum);
        }

        // --- CLAIMS / CLAIMMANAGER ---
        try {
            ClaimManager cm = plugin.getClaimManager();
            if (cm != null) {
                if (id.equals("total_claims")) {
                    return String.valueOf(cm.getStatistics().getOrDefault("total_claims", 0));
                }
                if (id.equals("total_towns_with_claims")) {
                    return String.valueOf(cm.getStatistics().getOrDefault("total_towns_with_claims", 0));
                }
            }
        } catch (Throwable ignored) {
        }

        // --- ECONOMY / TREASURIES ---
        try {
            if (id.startsWith("treasury_balance_")) {
                String key = id.substring("treasury_balance_".length()).toUpperCase(Locale.ROOT);
                double b = econ != null ? econ.getTreasuryBalance(key) : 0.0;
                return String.valueOf(b);
            }
            if (id.equals("transactions_total")) {
                return String.valueOf(econ != null ? econ.getTransactions().size() : 0);
            }
        } catch (Throwable ignored) {
        }

        // --- WARFARE / WARS ---
        try {
            var warfare = plugin.getWarfareService();
            if (warfare != null) {
                if (id.equals("kingdom_at_war")) {
                    if (pd == null || pd.getTown() == null) return "false";
                    Town t = societies.getTown(pd.getTown());
                    if (t == null || t.getKingdom() == null) return "false";
                    Kingdom k = societies.getKingdom(t.getKingdom());
                    return (k != null && k.getWars().size() > 0) ? "true" : "false";
                }
                if (id.startsWith("is_at_war_with_")) {
                    String other = id.substring("is_at_war_with_".length());
                    if (pd == null || pd.getTown() == null) return "false";
                    Town t = societies.getTown(pd.getTown());
                    if (t == null || t.getKingdom() == null) return "false";
                    return String.valueOf(warfare.isAtWar(t.getKingdom(), other));
                }
            }
        } catch (Throwable ignored) {
        }

        // --- LEADERBOARDS (pattern: leaderboard_top_<category>_<rank> and _value) ---
        try {
            if (id.startsWith("leaderboard_top_")) {
                String rest = id.substring("leaderboard_top_".length());
                boolean wantValue = false;
                if (rest.endsWith("_value")) {
                    wantValue = true;
                    rest = rest.substring(0, rest.length() - "_value".length());
                }
                String[] parts = rest.split("_");
                if (parts.length >= 2) {
                    // last part should be rank
                    int rank = 1;
                    try { rank = Integer.parseInt(parts[parts.length - 1]); } catch (Throwable ignored) {}
                    // category is everything before rank
                    String category = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));

                    java.util.List<LeaderboardManager.LeaderboardEntry> entries = java.util.List.of();
                    int limit = Math.max(1, rank);
                    switch (category) {
                        case "towns_population" -> entries = lbm.getTopTownsByPopulation(limit);
                        case "towns_balance" -> entries = lbm.getTopTownsByWealth(limit);
                        case "towns_claims" -> entries = lbm.getTopTownsByClaims(limit);
                        case "towns_level" -> entries = lbm.getTopTownsByLevel(limit);
                        case "kingdoms_population" -> entries = lbm.getTopKingdomsByPopulation(limit);
                        case "kingdoms_towns" -> entries = lbm.getTopKingdomsByTowns(limit);
                        case "kingdoms_balance" -> entries = lbm.getTopKingdomsByWealth(limit);
                        case "kingdoms_vassals" -> entries = lbm.getTopKingdomsByVassals(limit);
                        case "kingdoms_level" -> entries = lbm.getTopKingdomsByLevel(limit);
                        case "empires_population" -> entries = lbm.getTopEmpiresByPopulation(limit);
                        case "empires_kingdoms" -> entries = lbm.getTopEmpiresByKingdoms(limit);
                        case "empires_territory" -> entries = lbm.getTopEmpiresByTerritory(limit);
                        case "empires_level" -> entries = lbm.getTopEmpiresByLevel(limit);
                        case "players_money" -> entries = lbm.getTopPlayersByWealth(limit);
                        case "players_noble_tier" -> entries = lbm.getTopPlayersByNobleTier(limit);
                        default -> entries = java.util.List.of();
                    }

                    if (entries.isEmpty() || rank <= 0 || rank > entries.size()) return "";
                    var entry = entries.get(rank - 1);
                    return wantValue ? entry.getFormattedValue() : entry.getName();
                }
            }
        } catch (Throwable ignored) {
        }

        // fallback: not recognized
        return "";
    }
}
