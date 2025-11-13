package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.utils.Claim;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple particle-based fallback visualizer.
 */
public class ParticleClaimVisualizer implements ClaimVisualizer {
    private final Nations plugin;
    private final ClaimManager claimManager;
    private final Map<UUID, VisualizationData> active = new ConcurrentHashMap<>();

    // configurable parameters
    private final Particle particleType;
    private final int spacing;
    private final long refreshTicks;

    public ParticleClaimVisualizer(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);

        // Read configuration with safe defaults
        Particle p = Particle.END_ROD;
        try {
            String pname = plugin.getConfig().getString("visualizer.particle", "END_ROD");
            p = Particle.valueOf(pname.toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) {
        }
        this.particleType = p;

        int sp = 4;
        try {
            sp = Math.max(1, plugin.getConfig().getInt("visualizer.spacing", 4));
        } catch (Throwable ignored) {
        }
        this.spacing = sp;

        long rt = 40L;
        try {
            rt = Math.max(1L, plugin.getConfig().getLong("visualizer.refresh-ticks", 40L));
        } catch (Throwable ignored) {
        }
        this.refreshTicks = rt;

        // Register listener to stop visualizations when players disconnect/teleport/change world/kicked
        try {
            Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(), plugin);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isVisualizing(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    @Override
    public void toggleVisualization(Player player, String townName) {
        if (isVisualizing(player)) {
            stopVisualization(player);
            player.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.get("visualizer.disabled"));
        } else {
            startVisualization(player, townName);
            player.sendMessage(MessageUtils.format("visualizer.enabled", Map.of("town", townName)));
        }
    }

    @Override
    public void startVisualization(Player player, String townName) {
        if (isVisualizing(player)) return;
        VisualizationData d = new VisualizationData(townName);
        active.put(player.getUniqueId(), d);
        render(player, d);
        d.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopVisualization(player);
                    return;
                }
                render(player, d);
            }
        }.runTaskTimer(plugin, 40L, refreshTicks);
    }

    @Override
    public void stopVisualization(Player player) {
        VisualizationData d = active.remove(player.getUniqueId());
        if (d != null) {
            if (d.task != null) d.task.cancel();
        }
    }

    @Override
    public void stopAll() {
        // Cancel all running tasks and clear the map, even for offline players
        for (Map.Entry<UUID, VisualizationData> entry : new ArrayList<>(active.entrySet())) {
            VisualizationData d = active.remove(entry.getKey());
            if (d != null && d.task != null) {
                try {
                    d.task.cancel();
                } catch (Exception ignored) {
                }
            }
        }
        active.clear();
    }

    private void render(Player player, VisualizationData d) {
        Collection<Claim> claims = claimManager.getTownClaims(d.townName);
        if (claims == null || claims.isEmpty()) return;
        World w = player.getWorld();
        // Spawn particles at chunk borders of claims near the player using configured spacing and particle type
        claims.stream().filter(c -> Objects.equals(c.getWorldName(), w.getName())).forEach(c -> {
            int x = (c.getChunkX() * 16) + 8;
            int z = (c.getChunkZ() * 16) + 8;
            for (int dx = -8; dx <= 8; dx += Math.max(1, spacing)) {
                w.spawnParticle(particleType, x + dx, player.getLocation().getY(), z - 8, 1, 0, 0, 0, 0);
                w.spawnParticle(particleType, x + dx, player.getLocation().getY(), z + 8, 1, 0, 0, 0, 0);
            }
            for (int dz = -8; dz <= 8; dz += Math.max(1, spacing)) {
                w.spawnParticle(particleType, x - 8, player.getLocation().getY(), z + dz, 1, 0, 0, 0, 0);
                w.spawnParticle(particleType, x + 8, player.getLocation().getY(), z + dz, 1, 0, 0, 0, 0);
            }
        });
    }

    private static class VisualizationData {
        final String townName;
        BukkitTask task;

        VisualizationData(String townName) {
            this.townName = townName;
        }
    }

    // Listener that stops visualizations on relevant player lifecycle events to avoid orphaned tasks
    private class PlayerLifecycleListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent ev) {
            ParticleClaimVisualizer.this.stopVisualization(ev.getPlayer());
        }

        @EventHandler
        public void onKick(PlayerKickEvent ev) {
            ParticleClaimVisualizer.this.stopVisualization(ev.getPlayer());
        }

        @EventHandler
        public void onTeleport(PlayerTeleportEvent ev) {
            ParticleClaimVisualizer.this.stopVisualization(ev.getPlayer());
        }

        @EventHandler
        public void onChangeWorld(PlayerChangedWorldEvent ev) {
            ParticleClaimVisualizer.this.stopVisualization(ev.getPlayer());
        }
    }
}
