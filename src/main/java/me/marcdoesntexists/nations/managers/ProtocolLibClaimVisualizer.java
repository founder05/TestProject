package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.utils.Claim;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtocolLib-based claim visualizer using BlockDisplay spawn + metadata.
 */
public class ProtocolLibClaimVisualizer implements ClaimVisualizer {
    private final Nations plugin;
    private final ClaimManager claimManager;

    private final Map<UUID, VisualizationData> activeVisualizers = new ConcurrentHashMap<>();

    public ProtocolLibClaimVisualizer(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
        // Force fallback: many ProtocolLib / Paper versions do not provide a serializer for
        // org.bukkit.util.Transformation which leads to ClassCastException when building
        // ENTITY_METADATA packets. Use the multi-entity fallback rendering which is safer
        // and compatible across server versions.
        // Register listener to block interactions with our marker armor stands (prevent any player/admin interaction)
        try {
            // Accessing the ProtocolManager can be done if needed later. For now we avoid storing an unused reference.
            Bukkit.getPluginManager().registerEvents(new MarkerEntityListener(), plugin);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isVisualizing(Player player) { return activeVisualizers.containsKey(player.getUniqueId()); }

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
        if (claimManager == null) {
            player.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.get("visualizer.unavailable"));
            return;
        }
        VisualizationData data = new VisualizationData(townName);
        activeVisualizers.put(player.getUniqueId(), data);
        renderBorders(player, data);
        data.updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { stopVisualization(player); return; }
                renderBorders(player, data);
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    @Override
    public void stopVisualization(Player player) {
        VisualizationData data = activeVisualizers.remove(player.getUniqueId());
        if (data != null) {
            if (data.updateTask != null) data.updateTask.cancel();
            removeAllDisplays(data);
        }
    }

    @Override
    public void stopAll() {
        // Attempt to stop all visualizations; remove displays even if player is offline
        for (Map.Entry<UUID, VisualizationData> entry : new ArrayList<>(activeVisualizers.entrySet())) {
            UUID playerId = entry.getKey();
            VisualizationData data = activeVisualizers.remove(playerId);
            if (data != null) {
                if (data.updateTask != null) data.updateTask.cancel();
                // remove entities tracked by this visualization regardless of player state
                removeAllDisplays(data);
            }
        }
    }

    private void renderBorders(Player player, VisualizationData data) {
        Collection<Claim> claims = claimManager.getTownClaims(data.townName);
        if (claims == null || claims.isEmpty()) return;
        World playerWorld = player.getWorld();
        Set<ChunkCoord> visibleChunks = getChunkCoords(player, claims, playerWorld);
        if (visibleChunks.isEmpty()) { removeAllDisplays(data); return; }
        Set<BorderEdge> edges = findBorderEdges(visibleChunks);
        Set<BorderEdge> toRemove = new HashSet<>(data.activeEdges);
        toRemove.removeAll(edges);
        for (BorderEdge e : toRemove) {
            List<UUID> ids = data.edgeToEntityId.remove(e);
            if (ids != null) {
                for (UUID id : ids) removeDisplay(id);
            }
        }
        data.activeEdges.removeAll(toRemove);
        Set<BorderEdge> toAdd = new HashSet<>(edges); toAdd.removeAll(data.activeEdges);
        for (BorderEdge e : toAdd) {
            List<UUID> ids = spawnBlockDisplays(player, e);
            data.edgeToEntityId.put(e, ids);
            data.activeEdges.add(e);
        }
    }

    private @NotNull Set<ChunkCoord> getChunkCoords(Player player, Collection<Claim> claims, World playerWorld) {
        int pxChunk = player.getLocation().getBlockX() >> 4;
        int pzChunk = player.getLocation().getBlockZ() >> 4;
        Set<ChunkCoord> visibleChunks = new HashSet<>();
        for (Claim c : claims) {
            if (c == null) continue;
            if (!Objects.equals(c.getWorldName(), playerWorld.getName())) continue;
            int dx = Math.abs(c.getChunkX() - pxChunk);
            int dz = Math.abs(c.getChunkZ() - pzChunk);
            int maxViewDistance = 8;
            if (dx > maxViewDistance || dz > maxViewDistance) continue;
            visibleChunks.add(new ChunkCoord(c.getChunkX(), c.getChunkZ()));
        }
        return visibleChunks;
    }

    private Set<BorderEdge> findBorderEdges(Set<ChunkCoord> chunks) {
        Set<BorderEdge> edges = new HashSet<>();
        for (ChunkCoord chunk : chunks) {
            int minX = chunk.x * 16; int maxX = (chunk.x + 1) * 16; int minZ = chunk.z * 16; int maxZ = (chunk.z + 1) * 16;
            if (!chunks.contains(new ChunkCoord(chunk.x, chunk.z - 1))) edges.add(new BorderEdge(minX, minZ, maxX, minZ, EdgeDirection.HORIZONTAL));
            if (!chunks.contains(new ChunkCoord(chunk.x, chunk.z + 1))) edges.add(new BorderEdge(minX, maxZ, maxX, maxZ, EdgeDirection.HORIZONTAL));
            if (!chunks.contains(new ChunkCoord(chunk.x - 1, chunk.z))) edges.add(new BorderEdge(minX, minZ, minX, maxZ, EdgeDirection.VERTICAL));
            if (!chunks.contains(new ChunkCoord(chunk.x + 1, chunk.z))) edges.add(new BorderEdge(maxX, minZ, maxX, maxZ, EdgeDirection.VERTICAL));
        }
        return edges;
    }

    private List<UUID> spawnBlockDisplays(Player player, BorderEdge edge) {
        List<UUID> spawned = new ArrayList<>();
        // We'll spawn server-side ArmorStands as markers with a glass block as helmet.
        // This avoids ProtocolLib serialization issues and is stable across Paper versions.
        try {
            int spacing = 4; // blocks between displays
            if (edge.direction == EdgeDirection.HORIZONTAL) {
                int start = edge.x1; int end = edge.x2;
                for (int x = start; x <= end; x += spacing) {
                    double px = x + 0.5; double py = player.getEyeLocation().getY(); double pz = edge.z1 + 0.5;
                    UUID id = spawnArmorMarker(player.getWorld(), px, py, pz);
                    if (id != null) spawned.add(id);
                }
            } else {
                int start = edge.z1; int end = edge.z2;
                for (int z = start; z <= end; z += spacing) {
                    double px = edge.x1 + 0.5; double py = player.getEyeLocation().getY(); double pz = z + 0.5;
                    UUID id = spawnArmorMarker(player.getWorld(), px, py, pz);
                    if (id != null) spawned.add(id);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn marker entities: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Stacktrace:", e);
        }
        return spawned;
    }

    private void removeDisplay(UUID entityUuid) {
        try {
            Entity ent = Bukkit.getEntity(entityUuid);
            if (ent != null) ent.remove();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove marker entity: " + e.getMessage());
        }
    }

    // Remove all marker entities tracked by a visualization (player may be offline)
    private void removeAllDisplays(VisualizationData data) {
        if (data == null || data.edgeToEntityId.isEmpty()) return;

        for (List<UUID> lst : data.edgeToEntityId.values()) {
            for (UUID id : lst) {
                try {
                    Entity ent = Bukkit.getEntity(id);
                    if (ent != null) ent.remove();
                } catch (Exception ignored) {}
            }
        }

        data.edgeToEntityId.clear();
        data.activeEdges.clear();
    }


    // Spawn a small invisible armor stand with a glass block helmet to act as a visual marker.
    private UUID spawnArmorMarker(World world, double x, double y, double z) {
        try {
            Entity e = world.spawnEntity(new org.bukkit.Location(world, x, y, z), EntityType.ARMOR_STAND);
            if (!(e instanceof ArmorStand as)) return null;
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(false); // keep hitbox minimal but show helmet
            as.setInvulnerable(true);
            as.setArms(false);
            as.setSmall(true);
            as.getEquipment().setHelmet(new ItemStack(Material.GLOWSTONE));
            // Mark this entity as a Nations visualizer marker so we can block interactions
            try {
                NamespacedKey key = new NamespacedKey(plugin, "nations_visual_marker");
                as.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
            } catch (Throwable ignored) {}
            return as.getUniqueId();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to spawn armor marker: " + ex.getMessage());
            return null;
        }
    }

    private record ChunkCoord(int x, int z) {}
    private enum EdgeDirection { HORIZONTAL, VERTICAL }
    private record BorderEdge(int x1, int z1, int x2, int z2, EdgeDirection direction) {}

    private static class VisualizationData {
        final String townName; final Map<BorderEdge, List<UUID>> edgeToEntityId = new HashMap<>(); final Set<BorderEdge> activeEdges = new HashSet<>(); BukkitTask updateTask;
        VisualizationData(String townName) { this.townName = townName; }
    }

    // Internal listener to block ANY interaction with marker ArmorStands created by this visualizer
    private class MarkerEntityListener implements Listener {
        private final NamespacedKey key = new NamespacedKey(plugin, "nations_visual_marker");

        private boolean isMarker(Entity e) {
            try {
                if (e == null) return false;
                if (!(e instanceof ArmorStand)) return false;
                Byte val = e.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
                return val != null && val == (byte)1;
            } catch (Throwable t) {
                return false;
            }
        }

        @EventHandler
        public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent ev) {
            if (isMarker(ev.getRightClicked())) ev.setCancelled(true);
        }

        @EventHandler
        public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent ev) {
            if (isMarker(ev.getRightClicked())) ev.setCancelled(true);
        }

        @EventHandler
        public void onPlayerInteractEntity(PlayerInteractEntityEvent ev) {
            if (isMarker(ev.getRightClicked())) ev.setCancelled(true);
        }

        @EventHandler
        public void onEntityDamage(EntityDamageEvent ev) {
            if (isMarker(ev.getEntity())) ev.setCancelled(true);
        }

        // Stop visualization when player quits/kicked/teleports/changes world to avoid orphaned displays and related crashes
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent ev) {
            Player p = ev.getPlayer();
            ProtocolLibClaimVisualizer.this.stopVisualization(p);
        }

        @EventHandler
        public void onPlayerKick(PlayerKickEvent ev) {
            Player p = ev.getPlayer();
            ProtocolLibClaimVisualizer.this.stopVisualization(p);
        }

        @EventHandler
        public void onPlayerTeleport(PlayerTeleportEvent ev) {
            Player p = ev.getPlayer();
            // stop visualization on teleport to avoid visual artifacts if player changes location or dimension
            ProtocolLibClaimVisualizer.this.stopVisualization(p);
        }

        @EventHandler
        public void onPlayerChangedWorld(PlayerChangedWorldEvent ev) {
            Player p = ev.getPlayer();
            ProtocolLibClaimVisualizer.this.stopVisualization(p);
        }
    }
}
