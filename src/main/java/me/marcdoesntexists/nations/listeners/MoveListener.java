package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.HybridClaimManager;
import me.marcdoesntexists.nations.managers.ClaimManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import me.marcdoesntexists.nations.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoveListener implements Listener {

    private final Nations plugin;
    private final HybridClaimManager hybridClaimManager;
    private final SocietiesManager societiesManager;
    private final Map<UUID, String> playerChunkCache = new HashMap<>();
    private final Map<UUID, String> playerLastTownCache = new HashMap<>();

    public MoveListener(Nations plugin) {
        this.plugin = plugin;
        this.hybridClaimManager = HybridClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
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

                boolean isMember = data != null && townName.equalsIgnoreCase(data.getTown());
                Component msg = Component.text(
                        MessageUtils.format(isMember ? "actionbar.your_territory" : "actionbar.foreign_territory", Map.of("town", town.getName()))
                );

                player.sendActionBar(msg); //

                if (player.hasPermission("nations.claim.verbose")) {
                    player.sendMessage(Component.text(MessageUtils.format("gui.town_verbose", Map.of("town", town.getName(), "member", isMember ? "true" : "false"))));
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
        player.sendActionBar(Component.text(MessageUtils.get("actionbar.wilderness")));
    }
}