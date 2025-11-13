package me.marcdoesntexists.nations.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.law.Criminal;
import me.marcdoesntexists.nations.law.JusticeService;
import me.marcdoesntexists.nations.managers.*;
import me.marcdoesntexists.nations.societies.Alliance;
import me.marcdoesntexists.nations.societies.Empire;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.stream.Collectors;

public class NationsPlaceholderExpansion extends PlaceholderExpansion {

    private final Nations plugin = Nations.getInstance();

    @Override
    public boolean canRegister() {
        return plugin != null;
    }

    @Override
    public boolean register() {
        return super.register();
    }

    @Override
    public String getIdentifier() {
        return "nations";
    }

    @Override
    public String getAuthor() {
        return "MarcDoesntExists";
    }

    @Override
    public String getVersion() {
        return plugin != null ? plugin.getDescription().getVersion() : "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || identifier == null) return "";

        DataManager dataManager = plugin.getDataManager();
        SocietiesManager societies = plugin.getSocietiesManager();
        PvPManager pvp = PvPManager.getInstance();
        JusticeService justice = plugin.getJusticeService();
        EconomyManager econ = plugin.getEconomyManager();
        LeaderboardManager lbm = LeaderboardManager.getInstance(plugin);

        String id = identifier.toLowerCase(Locale.ROOT);

        // --- PLAYER / BASIC ---
        if (id.equals("player_money") || id.equals("player_balance") || id.equals("player_money_internal")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            return pd != null ? String.valueOf(pd.getMoney()) : "0";
        }

        if (id.equals("player_town") || id.equals("town")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            return pd != null && pd.getTown() != null ? pd.getTown() : "";
        }

        if (id.equals("player_job") || id.equals("job")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            return pd != null && pd.getJob() != null ? pd.getJob() : "";
        }

        if (id.equals("player_social_class")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            return pd != null ? pd.getSocialClass() : "Commoner";
        }

        if (id.equals("player_noble_tier") || id.equals("noble_tier")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            return pd != null && pd.getNobleTier() != null ? pd.getNobleTier().name() : "COMMONER";
        }

        if (id.equals("player_chat_channel")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            return pd != null ? pd.getChatChannel() : "GLOBAL";
        }

        // --- TOWN PLACEHOLDERS ---
        if (id.startsWith("town_")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            if (pd == null || pd.getTown() == null) return "";
            Town t = societies.getTown(pd.getTown());
            if (t == null) return "";

            return switch (id) {
                case "town_balance" -> String.valueOf(t.getBalance());
                case "town_claims" -> String.valueOf(t.getClaims().size());
                case "town_members" -> String.valueOf(t.getMembers().size());
                case "town_members_list" -> t.getMembers().stream().map(u -> {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(u);
                    return op != null && op.getName() != null ? op.getName() : u.toString();
                }).collect(Collectors.joining(", "));
                case "town_mayor" -> {
                    OfflinePlayer mayor = Bukkit.getOfflinePlayer(t.getMayor());
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
            // resolve kingdom for player (via player's town)
            String kName = null;
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            if (pd != null && pd.getTown() != null) {
                Town t = societies.getTown(pd.getTown());
                if (t != null) kName = t.getKingdom();
            }
            if (kName == null) return "";
            Kingdom k = societies.getKingdom(kName);
            if (k == null) return "";

            switch (id) {
                case "kingdom_name":
                    return k.getName();
                case "kingdom_capital":
                    return k.getCapital();
                case "kingdom_towns_count":
                    return String.valueOf(k.getTowns().size());
                case "kingdom_balance":
                    return String.valueOf(k.getBalance());
                case "kingdom_level":
                    return String.valueOf(k.getProgressionLevel());
                case "kingdom_allies_count":
                    return String.valueOf(k.getAllies().size());
                case "kingdom_enemies_count":
                    return String.valueOf(k.getEnemies().size());
                case "kingdom_vassals_count":
                    return String.valueOf(k.getVassals().size());
                case "kingdom_wars_count":
                    return String.valueOf(k.getWars().size());
                case "kingdom_treaties_count":
                    return String.valueOf(k.getTreaties().size());
                case "kingdom_functional_areas_count":
                    return String.valueOf(k.getFunctionalAreas().size());
                case "kingdom_suzerain":
                    return k.getSuzerain() != null ? k.getSuzerain() : "";
                case "kingdom_tribute_amount":
                    return String.valueOf(k.getTributeAmount());
                case "kingdom_prestige":
                    return String.valueOf(k.getPrestige());
                case "kingdom_infamy":
                    return String.valueOf(k.getInfamy());
                case "kingdom_loyalty":
                    return String.valueOf(k.getLoyalty());
                case "kingdom_rebellion_chance":
                    return String.format("%.1f%%", k.getRebellionChance() * 100);
                default:
                    return "";
            }
        }

        // --- EMPIRE PLACEHOLDERS ---
        if (id.startsWith("empire_")) {
            // resolve empire via player's kingdom -> empire
            String empireName = null;
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
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

            switch (id) {
                case "empire_name":
                    return e.getName();
                case "empire_kingdoms_count":
                    return String.valueOf(e.getKingdoms().size());
                case "empire_level":
                    return String.valueOf(e.getProgressionLevel());
                case "empire_functional_areas_count":
                    return String.valueOf(e.getFunctionalAreas().size());
                default:
                    return "";
            }
        }

        // --- ALLIANCE PLACEHOLDERS (resolve by player's kingdom)
        if (id.startsWith("alliance_")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
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
                var claim = cm.getClaimAt(player.getLocation());
                return claim != null ? claim.getTownName() : "";
            } catch (Throwable ignored) {
                return "";
            }
        }

        if (id.equals("visualizer_active_for_player")) {
            try {
                var vis = plugin.getClaimVisualizer();
                if (vis == null) return "false";
                return vis.isVisualizing(player) ? "true" : "false";
            } catch (Throwable ignored) {
                return "false";
            }
        }

        // --- PLAYER ROLE IN TOWN ---
        if (id.equals("player_role_in_town")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            if (pd == null || pd.getTown() == null) return "NONE";
            Town t = societies.getTown(pd.getTown());
            if (t == null) return "NONE";
            if (t.isMayor(player.getUniqueId())) return "MAYOR";
            if (t.getMembers().contains(player.getUniqueId())) return "MEMBER";
            return "NONE";
        }

        // --- CAPITAL / INFO PLACEHOLDERS ---
        if (id.equals("kingdom_capital_town")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            if (pd == null || pd.getTown() == null) return "";
            Town t = societies.getTown(pd.getTown());
            if (t == null || t.getKingdom() == null) return "";
            Kingdom k = societies.getKingdom(t.getKingdom());
            return k != null ? (k.getCapital() != null ? k.getCapital() : "") : "";
        }

        if (id.equals("empire_capital_kingdom")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
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
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
            if (pd == null || pd.getTown() == null) return "0";
            Town t = societies.getTown(pd.getTown());
            if (t == null || t.getKingdom() == null) return "0";
            Kingdom k = societies.getKingdom(t.getKingdom());
            return k != null ? String.valueOf(k.getWars().size()) : "0";
        }

        if (id.equals("wars_count_empire")) {
            PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
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

        // --- LAW / CRIMINALS / CRIMES ---
        try {
            LawManager lm = LawManager.getInstance();
            if (lm != null) {
                if (id.equals("crimes_total")) return String.valueOf(lm.getAllCrimes().size());
                if (id.equals("criminals_total")) return String.valueOf(lm.getAllCriminals().size());

                // per-player criminal info
                if (id.equals("criminal_wanted_level") || id.equals("player_wanted_level")) {
                    Criminal c = lm.getCriminal(player.getUniqueId());
                    return c != null ? String.valueOf(c.getWantedLevel()) : "0";
                }
                if (id.equals("criminal_is_arrested")) {
                    Criminal c = lm.getCriminal(player.getUniqueId());
                    return c != null ? (c.isArrested() ? "true" : "false") : "false";
                }
                if (id.equals("criminal_total_fines")) {
                    Criminal c = lm.getCriminal(player.getUniqueId());
                    return c != null ? String.valueOf(c.getTotalFines()) : "0";
                }
                if (id.equals("criminal_last_crime_type")) {
                    Criminal c = lm.getCriminal(player.getUniqueId());
                    if (c != null && !c.getCrimes().isEmpty()) {
                        var last = c.getCrimes().get(c.getCrimes().size() - 1);
                        return last != null && last.getCrimeType() != null ? last.getCrimeType().name() : "";
                    }
                    return "";
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
                    // player's kingdom at war?
                    PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
                    if (pd == null || pd.getTown() == null) return "false";
                    Town t = societies.getTown(pd.getTown());
                    if (t == null || t.getKingdom() == null) return "false";
                    Kingdom k = societies.getKingdom(t.getKingdom());
                    return (k != null && k.getWars().size() > 0) ? "true" : "false";
                }
                if (id.startsWith("is_at_war_with_")) {
                    String other = id.substring("is_at_war_with_".length());
                    PlayerData pd = dataManager.getPlayerData(player.getUniqueId());
                    if (pd == null || pd.getTown() == null) return "false";
                    Town t = societies.getTown(pd.getTown());
                    if (t == null || t.getKingdom() == null) return "false";
                    return String.valueOf(warfare.isAtWar(t.getKingdom(), other));
                }
            }
        } catch (Throwable ignored) {
        }

        // fallback: not recognized
        return "";
    }
}
