package me.marcdoesntexists.nations.managers;

import me.clip.placeholderapi.libs.kyori.adventure.platform.bukkit.BukkitAudiences;
import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.utils.Claim;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimVisualizer {

    private final Nations plugin;
    private final ClaimManager claimManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, BukkitTask> activeVisualizers = new ConcurrentHashMap<>();

    private final int particleStep = 3;      // block spacing between particles
    private final int maxViewDistance = 8;   // max chunks radius around player to render

    public ClaimVisualizer(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
        // Kyori Adventure wrapper
        BukkitAudiences adventure = BukkitAudiences.create(plugin);
    }

    public boolean isVisualizing(Player player) {
        return activeVisualizers.containsKey(player.getUniqueId());
    }

    public void toggleVisualization(Player player, String townName) {
        if (isVisualizing(player)) {
            stopVisualization(player);
            // send plain colored message instead of shaded Component
            player.sendMessage("§aClaim visualization disabled.");
        } else {
            startVisualization(player, townName);
            player.sendMessage("§aClaim visualization enabled for town: §6" + townName);
        }
    }

    public void startVisualization(Player player, String townName) {
        if (isVisualizing(player)) return;
        if (claimManager == null) {
            // send plain colored message instead of shaded Component
            player.sendMessage("§cClaimManager unavailable.");
            return;
        }

        long tickInterval = 10L; // 0.5s refresh
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopVisualization(player);
                    return;
                }

                Collection<Claim> claims = claimManager.getTownClaims(townName);
                if (claims == null || claims.isEmpty()) return;

                World playerWorld = player.getWorld();
                int pxChunk = player.getLocation().getBlockX() >> 4;
                int pzChunk = player.getLocation().getBlockZ() >> 4;

                // Limit claims to visible range and same world
                Map<String, Set<ChunkCoord>> byWorld = new HashMap<>();
                for (Claim c : claims) {
                    if (c == null) continue;
                    if (!Objects.equals(c.getWorldName(), playerWorld.getName())) continue;

                    int dx = Math.abs(c.getChunkX() - pxChunk);
                    int dz = Math.abs(c.getChunkZ() - pzChunk);
                    if (dx > maxViewDistance || dz > maxViewDistance) continue; // too far away

                    byWorld.computeIfAbsent(c.getWorldName(), k -> new HashSet<>())
                            .add(new ChunkCoord(c.getChunkX(), c.getChunkZ()));
                }

                if (byWorld.isEmpty()) return;

                double eyeY = player.getEyeLocation().getY();

                for (Map.Entry<String, Set<ChunkCoord>> entry : byWorld.entrySet()) {
                    World world = Bukkit.getWorld(entry.getKey());
                    if (world == null) continue;

                    Set<ChunkCoord> all = entry.getValue();
                    if (all.isEmpty()) continue;

                    Set<ChunkCoord> remaining = new HashSet<>(all);

                    while (!remaining.isEmpty()) {
                        ChunkCoord start = remaining.iterator().next();
                        Set<ChunkCoord> component = floodFillComponent(start, remaining);
                        remaining.removeAll(component);

                        // compute edges
                        Map<Integer, List<IntSegment>> horizontalEdges = new HashMap<>();
                        Map<Integer, List<IntSegment>> verticalEdges = new HashMap<>();

                        for (ChunkCoord cc : component) {
                            int cx = cc.x;
                            int cz = cc.z;

                            int minX = cx * 16;
                            int maxX = (cx + 1) * 16;
                            int minZ = cz * 16;
                            int maxZ = (cz + 1) * 16;

                            if (!component.contains(new ChunkCoord(cx, cz - 1))) {
                                horizontalEdges.computeIfAbsent(minZ, k -> new ArrayList<>())
                                        .add(new IntSegment(minX, maxX));
                            }
                            if (!component.contains(new ChunkCoord(cx, cz + 1))) {
                                horizontalEdges.computeIfAbsent(maxZ, k -> new ArrayList<>())
                                        .add(new IntSegment(minX, maxX));
                            }
                            if (!component.contains(new ChunkCoord(cx - 1, cz))) {
                                verticalEdges.computeIfAbsent(minX, k -> new ArrayList<>())
                                        .add(new IntSegment(minZ, maxZ));
                            }
                            if (!component.contains(new ChunkCoord(cx + 1, cz))) {
                                verticalEdges.computeIfAbsent(maxX, k -> new ArrayList<>())
                                        .add(new IntSegment(minZ, maxZ));
                            }
                        }

                        // Draw horizontal edges (constant Z)
                        for (Map.Entry<Integer, List<IntSegment>> he : horizontalEdges.entrySet()) {
                            int zEdge = he.getKey();
                            List<IntSegment> segs = mergeSegments(he.getValue());
                            for (IntSegment seg : segs) {
                                for (int x = seg.start; x <= seg.end; x += particleStep) {
                                    spawnParticle(world, new Location(world, x + 0.5, eyeY, zEdge + 0.5));
                                }
                            }
                        }

                        // Draw vertical edges (constant X)
                        for (Map.Entry<Integer, List<IntSegment>> ve : verticalEdges.entrySet()) {
                            int xEdge = ve.getKey();
                            List<IntSegment> segs = mergeSegments(ve.getValue());
                            for (IntSegment seg : segs) {
                                for (int z = seg.start; z <= seg.end; z += particleStep) {
                                    spawnParticle(world, new Location(world, xEdge + 0.5, eyeY, z + 0.5));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, tickInterval);

        activeVisualizers.put(player.getUniqueId(), task);
    }

    public void stopVisualization(Player player) {
        BukkitTask task = activeVisualizers.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public void stopAll() {
        for (UUID u : new ArrayList<>(activeVisualizers.keySet())) {
            BukkitTask t = activeVisualizers.remove(u);
            if (t != null) t.cancel();
        }
    }

    // Flood-fill connected claim area
    private Set<ChunkCoord> floodFillComponent(ChunkCoord start, Set<ChunkCoord> pool) {
        Set<ChunkCoord> comp = new HashSet<>();
        ArrayDeque<ChunkCoord> dq = new ArrayDeque<>();
        if (!pool.contains(start)) return comp;
        dq.add(start);
        comp.add(start);

        while (!dq.isEmpty()) {
            ChunkCoord cur = dq.poll();
            for (ChunkCoord n : List.of(
                    new ChunkCoord(cur.x + 1, cur.z),
                    new ChunkCoord(cur.x - 1, cur.z),
                    new ChunkCoord(cur.x, cur.z + 1),
                    new ChunkCoord(cur.x, cur.z - 1))) {
                if (pool.contains(n) && comp.add(n)) dq.add(n);
            }
        }
        return comp;
    }

    // Merge continuous integer ranges
    private List<IntSegment> mergeSegments(List<IntSegment> segments) {
        if (segments == null || segments.isEmpty()) return Collections.emptyList();
        segments.sort(Comparator.comparingInt(s -> s.start));
        List<IntSegment> merged = new ArrayList<>();
        IntSegment cur = segments.getFirst();
        for (int i = 1; i < segments.size(); i++) {
            IntSegment next = segments.get(i);
            if (next.start <= cur.end) cur.end = Math.max(cur.end, next.end);
            else {
                merged.add(cur);
                cur = next;
            }
        }
        merged.add(cur);
        return merged;
    }

    private void spawnParticle(World world, Location loc) {
        world.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
    }

    private record ChunkCoord(int x, int z) {}
    private static class IntSegment {
        int start, end;
        IntSegment(int s, int e) {
            this.start = Math.min(s, e);
            this.end = Math.max(s, e);
        }
    }
}
