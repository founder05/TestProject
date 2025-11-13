package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.managers.PvPManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DuelProtectionListener implements Listener {
    private final PvPManager pvp = PvPManager.getInstance();

    @EventHandler
    public void onPreCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (!pvp.inDuel(p)) return;
        String cmd = event.getMessage().toLowerCase();
        // block common teleport commands
        if (cmd.startsWith("/tp") || cmd.startsWith("/tpa") || cmd.startsWith("/warp") || cmd.startsWith("/home") || cmd.startsWith("/spawn") || cmd.startsWith("/teleport") || cmd.startsWith("/tpmy"))  {
            p.sendMessage(MessageUtils.get("duel.cant_teleport"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player p = event.getPlayer();
        // Block any teleport attempts from players in duel (including chorus fruit)
        if (pvp.inDuel(p)) {
            p.sendMessage(MessageUtils.get("duel.cant_teleport"));
            event.setCancelled(true);
            return;
        }
        // additionally, if teleport cause is CHORUS_FRUIT and player is in duel, block (redundant but explicit)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT && pvp.inDuel(p)) {
            p.sendMessage(MessageUtils.get("duel.cant_teleport"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player p)) return;
        if (!pvp.inDuel(p)) return;
        p.sendMessage(MessageUtils.get("duel.cant_teleport"));
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        Entity ent = event.getEntity();
        // If a vehicle is teleporting and it has players aboard that are in duel, prevent teleport
        if (ent instanceof Vehicle v) {
            for (Entity passenger : v.getPassengers()) {
                if (passenger instanceof Player p) {
                    if (pvp.inDuel(p)) {
                        p.sendMessage(MessageUtils.get("duel.cant_teleport"));
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (!pvp.inDuel(p)) return;
        // inform PvPManager so it can finish session and apply penalties
        pvp.handlePlayerQuit(p.getUniqueId());
    }
}
