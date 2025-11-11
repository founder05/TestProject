package me.marcdoesntexists.nations.listeners;// inside imports: add HybridClaimManager
import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.HybridClaimManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

// bungee imports for action bar compatibility
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class MoveListener implements Listener {
    private final Nations plugin;
    // private final ClaimManager claimManager;   <-- remove
    private final HybridClaimManager hybridClaimManager;
    private final SocietiesManager societiesManager;

    private final Map<UUID, String> playerChunkCache = new HashMap<>();

    public MoveListener(Nations plugin) {
        this.plugin = plugin;
        this.hybridClaimManager = HybridClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) return;
        if (Objects.equals(event.getFrom().getChunk(), event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        Chunk toChunk = event.getTo().getChunk();
        if (toChunk == null) return;

        // compute chunk key safely even if ClaimManager is not yet available
        String toChunkKey;
        if (plugin.getClaimManager() != null) {
            toChunkKey = plugin.getClaimManager().getChunkKey(toChunk);
        } else {
            toChunkKey = toChunk.getWorld().getName() + "," + toChunk.getX() + "," + toChunk.getZ();
        }

        String cachedChunk = playerChunkCache.get(player.getUniqueId());
        if (toChunkKey.equals(cachedChunk)) {
            return;
        }

        playerChunkCache.put(player.getUniqueId(), toChunkKey);

        // Use HybridClaimManager so GP-backed claims are detected
        String townName = hybridClaimManager != null ? hybridClaimManager.getTownAtLocation(event.getTo()) : null;
        if (townName != null) {
            Town town = societiesManager != null ? societiesManager.getTown(townName) : null;
            if (town != null) {
                PlayerData playerData = plugin.getDataManager() != null ? plugin.getDataManager().getPlayerData(player.getUniqueId()) : null;

                boolean isMemberTown = playerData != null && townName.equals(playerData.getTown());

                String msg = (isMemberTown ? "§a⚑ §6" : "§e⚑ §6") + town.getName() + (isMemberTown ? " §7- §aYour Territory" : " §7- §eForeign Territory");
                // use bungee action bar for compatibility
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));

                if (player.hasPermission("nations.claim.verbose")) {
                    player.sendMessage("§7[§6" + town.getName() + "§7] " +
                            (isMemberTown ? "§aYour territory" : "§eForeign territory"));
                }
                return;
            }
        }

        // no town found -> wilderness
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§8⚐ §7Wilderness §8- §7Unclaimed Land"));
    }
}