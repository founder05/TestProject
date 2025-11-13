package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ClaimManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.Claim;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TpMyCommand implements CommandExecutor, TabCompleter {
    private final Nations plugin;

    public TpMyCommand(Nations plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("tpmy.only_players"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length != 1) {
            player.sendMessage(MessageUtils.get("tpmy.usage"));
            return true;
        }
        String type = args[0].toLowerCase(Locale.ROOT);
        PlayerData pd = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (pd == null) { player.sendMessage(MessageUtils.get("tpmy.player_data_not_found")); return true; }

        ClaimManager claimManager = ClaimManager.getInstance();
        SocietiesManager societies = SocietiesManager.getInstance();

        switch (type) {
            case "town": {
                String townName = pd.getTown();
                if (townName == null) { player.sendMessage(MessageUtils.get("tpmy.not_in_town")); return true; }
                Set<Claim> claims = claimManager.getTownClaims(townName);
                if (claims == null || claims.isEmpty()) { player.sendMessage(MessageUtils.get("tpmy.no_claims_town")); return true; }
                Claim c = claims.iterator().next();
                World w = Bukkit.getWorld(c.getWorldName());
                if (w == null) { player.sendMessage(MessageUtils.format("tpmy.world_not_found", Map.of("world", c.getWorldName()))); return true; }
                int cx = c.getChunkX(); int cz = c.getChunkZ();
                int x = cx * 16 + 8; int z = cz * 16 + 8; int y = w.getHighestBlockYAt(x, z);
                Location loc = new Location(w, x + 0.5, y + 1, z + 0.5);
                player.teleport(loc);
                player.sendMessage(MessageUtils.format("tpmy.teleported_town", Map.of("town", townName)));
                return true;
            }
            case "kingdom": {
                String townName = pd.getTown();
                if (townName == null) { player.sendMessage(MessageUtils.get("tpmy.not_in_town")); return true; }
                Town town = societies.getTown(townName);
                if (town == null) { player.sendMessage(MessageUtils.get("tpmy.town_data_not_found")); return true; }
                String kingdomName = town.getKingdom();
                if (kingdomName == null) { player.sendMessage(MessageUtils.get("chat.not_in_kingdom")); return true; }
                Kingdom kingdom = societies.getKingdom(kingdomName);
                if (kingdom == null) { player.sendMessage(MessageUtils.get("chat.kingdom_data_not_found")); return true; }
                // search claims across towns
                for (String tName : kingdom.getTowns()) {
                    Set<Claim> claims = claimManager.getTownClaims(tName);
                    if (claims != null && !claims.isEmpty()) {
                        Claim c = claims.iterator().next();
                        World w = Bukkit.getWorld(c.getWorldName());
                        if (w == null) continue;
                        int cx = c.getChunkX(); int cz = c.getChunkZ();
                        int x = cx * 16 + 8; int z = cz * 16 + 8; int y = w.getHighestBlockYAt(x, z);
                        Location loc = new Location(w, x + 0.5, y + 1, z + 0.5);
                        player.teleport(loc);
                        player.sendMessage(MessageUtils.format("tpmy.teleported_kingdom", Map.of("kingdom", kingdomName)));
                        return true;
                    }
                }
                player.sendMessage(MessageUtils.get("tpmy.no_claims_kingdom"));
                return true;
            }
            case "empire": {
                String townName = pd.getTown();
                if (townName == null) { player.sendMessage(MessageUtils.get("tpmy.not_in_town")); return true; }
                Town town = societies.getTown(townName);
                if (town == null) { player.sendMessage(MessageUtils.get("tpmy.town_data_not_found")); return true; }
                String kingdomName = town.getKingdom();
                if (kingdomName == null) { player.sendMessage(MessageUtils.get("chat.not_in_kingdom")); return true; }
                Kingdom kingdom = societies.getKingdom(kingdomName);
                if (kingdom == null) { player.sendMessage(MessageUtils.get("chat.kingdom_data_not_found")); return true; }
                String empireName = kingdom.getEmpire();
                if (empireName == null) { player.sendMessage(MessageUtils.get("chat.not_in_empire")); return true; }
                me.marcdoesntexists.nations.societies.Empire empire = societies.getEmpire(empireName);
                if (empire == null) { player.sendMessage(MessageUtils.get("chat.empire_data_not_found")); return true; }
                for (String k : empire.getKingdoms()) {
                    Kingdom kObj = societies.getKingdom(k);
                    if (kObj == null) continue;
                    for (String tName : kObj.getTowns()) {
                        Set<Claim> claims = claimManager.getTownClaims(tName);
                        if (claims != null && !claims.isEmpty()) {
                            Claim c = claims.iterator().next();
                            World w = Bukkit.getWorld(c.getWorldName());
                            if (w == null) continue;
                            int cx = c.getChunkX(); int cz = c.getChunkZ();
                            int x = cx * 16 + 8; int z = cz * 16 + 8; int y = w.getHighestBlockYAt(x, z);
                            Location loc = new Location(w, x + 0.5, y + 1, z + 0.5);
                            player.teleport(loc);
                            player.sendMessage(MessageUtils.format("tpmy.teleported_empire", Map.of("empire", empireName)));
                            return true;
                        }
                    }
                }
                player.sendMessage(MessageUtils.get("tpmy.no_claims_empire"));
                return true;
            }
            default:
                player.sendMessage(MessageUtils.get("tpmy.unknown_type"));
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("town","kingdom","empire");
        return Collections.emptyList();
    }
}
