package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.managers.HybridClaimManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MoveListener implements Listener {

    private final Realms plugin;
    private final HybridClaimManager hybridClaimManager;
    private final SocietiesManager societiesManager;
    private final Map<UUID, String> playerChunkCache = new HashMap<>();
    private final Map<UUID, String> playerLastTownCache = new HashMap<>();

    public MoveListener(Realms plugin) {
        this.plugin = plugin;
        this.hybridClaimManager = HybridClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        Player p = ev.getPlayer();
        // initialize caches so players don't get an actionbar for being "entered" into a town on their first movement
        try {
            String chunkKey = p.getWorld().getName() + "," + p.getLocation().getChunk().getX() + "," + p.getLocation().getChunk().getZ();
            playerChunkCache.put(p.getUniqueId(), chunkKey);
            String town = null;
            if (hybridClaimManager != null) town = hybridClaimManager.getTownAtLocation(p.getLocation());
            else if (plugin.getClaimManager() != null) town = plugin.getClaimManager().getTownAtLocation(p.getLocation());
            playerLastTownCache.put(p.getUniqueId(), town);
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent ev) {
        Player p = ev.getPlayer();
        // Update chunk cache to destination chunk to avoid duplicate actionbars
        try {
            String chunkKey = ev.getTo().getWorld().getName() + "," + ev.getTo().getChunk().getX() + "," + ev.getTo().getChunk().getZ();
            String prevTown = playerLastTownCache.get(p.getUniqueId());

            String town = null;
            try {
                if (hybridClaimManager != null) town = hybridClaimManager.getTownAtLocation(ev.getTo());
                else if (plugin.getClaimManager() != null) town = plugin.getClaimManager().getTownAtLocation(ev.getTo());
            } catch (Exception ignored) {}

            // If unchanged, just update chunk and town caches and return
            if (Objects.equals(town, prevTown)) {
                playerChunkCache.put(p.getUniqueId(), chunkKey);
                playerLastTownCache.put(p.getUniqueId(), town);
                return;
            }

            // different: behave like entering/exiting
            playerChunkCache.put(p.getUniqueId(), chunkKey);
            playerLastTownCache.put(p.getUniqueId(), town);

            if (town != null && societiesManager != null) {
                Town t = societiesManager.getTown(town);
                if (t != null) {
                    PlayerData data = plugin.getDataManager() != null
                            ? plugin.getDataManager().getPlayerData(p.getUniqueId())
                            : null;
                    boolean isMember = false;
                    if (t.getMembers().contains(p.getUniqueId())) isMember = true;
                    else if (t.isTrusted(p.getUniqueId())) isMember = true;
                    else if (data != null && data.getTown() != null && town.equalsIgnoreCase(data.getTown())) isMember = true;

                    p.sendActionBar(MessageUtils.toComponent(MessageUtils.format(isMember ? "actionbar.your_territory" : "actionbar.foreign_territory", Map.of("town", t.getName()))));
                    if (p.hasPermission("realms.claim.verbose")) {
                        p.sendMessage(MessageUtils.toComponent(MessageUtils.format("gui.town_verbose", Map.of("town", t.getName(), "member", isMember ? "true" : "false"))));
                    }
                    return;
                }
            }

            // wilderness case
            if (prevTown == null) {
                // was already wilderness, nothing to do
                return;
            }

            p.sendActionBar(MessageUtils.toComponent(MessageUtils.get("actionbar.wilderness")));
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {


        Chunk from = event.getFrom().getChunk();
        Chunk to = event.getTo().getChunk();
        if (from.getX() == to.getX() && from.getZ() == to.getZ() && from.getWorld().equals(to.getWorld())) return;

        Player player = event.getPlayer();
        String toChunkKey = to.getWorld().getName() + "," + to.getX() + "," + to.getZ();
        if (toChunkKey.equals(playerChunkCache.get(player.getUniqueId()))) return;
        playerChunkCache.put(player.getUniqueId(), toChunkKey);
        String townName = null;
        try {
            if (hybridClaimManager != null) {
                townName = hybridClaimManager.getTownAtLocation(event.getTo());
            } else if (plugin.getClaimManager() != null) {
                ClaimManager claimManager = plugin.getClaimManager();
                townName = claimManager.getTownAtLocation(event.getTo());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error into Land Search: " + e.getMessage());
        }

        String lastTown = playerLastTownCache.get(player.getUniqueId());
        if (townName != null && societiesManager != null) {
            if (townName.equals(lastTown)) {
                // still in same town claim, don't resend actionbar
                return;
            }

            playerLastTownCache.put(player.getUniqueId(), townName);

            Town town = societiesManager.getTown(townName);
            if (town != null) {
                PlayerData data = plugin.getDataManager() != null
                        ? plugin.getDataManager().getPlayerData(player.getUniqueId())
                        : null;

                // Robust membership check: prefer live town membership/trust lists, fallback to stored PlayerData town
                boolean isMember = false;
                if (town.getMembers().contains(player.getUniqueId())) {
                    isMember = true;
                } else if (town.isTrusted(player.getUniqueId())) {
                    isMember = true;
                } else if (data != null && data.getTown() != null && townName.equalsIgnoreCase(data.getTown())) {
                    isMember = true;
                }

                Component msg = MessageUtils.toComponent(MessageUtils.format(isMember ? "actionbar.your_territory" : "actionbar.foreign_territory", Map.of("town", town.getName())));

                player.sendActionBar(msg); //

                if (player.hasPermission("realms.claim.verbose")) {
                    player.sendMessage(MessageUtils.toComponent(MessageUtils.format("gui.town_verbose", Map.of("town", town.getName(), "member", isMember ? "true" : "false"))));
                }
                return;
            }
        }

        // wilderness case
        if (lastTown == null) {
            // was already wilderness, don't resend
            return;
        }

        playerLastTownCache.put(player.getUniqueId(), null);
        player.sendActionBar(MessageUtils.toComponent(MessageUtils.get("actionbar.wilderness")));
    }
}