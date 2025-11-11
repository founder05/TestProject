package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.utils.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Particle.DustOptions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimVisualizer {

    private final Nations plugin;
    private final ClaimManager claimManager;

    private final Map<UUID, BukkitTask> activeVisualizers = new ConcurrentHashMap<>();

    private final int particleStep = 3; // spacing in blocks between spawned particles
    private final DustOptions dust = new DustOptions(Color.fromRGB(80, 200, 255), 1.0f);

    public ClaimVisualizer(Nations plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
    }

    public boolean isVisualizing(Player player) {
        return activeVisualizers.containsKey(player.getUniqueId());
    }

    public void toggleVisualization(Player player, String townName) {
        if (isVisualizing(player)) {
            stopVisualization(player);
            player.sendMessage("§aClaim visualization disabled.");
        } else {
            startVisualization(player, townName);
            player.sendMessage("§aClaim visualization enabled for town: §6" + townName);
        }
    }

    public void startVisualization(Player player, String townName) {
        if (isVisualizing(player)) return;

        // every 10 ticks (~0.5s)
        long tickInterval = 10L;
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopVisualization(player);
                    return;
                }

                Set<Claim> claims = claimManager.getTownClaims(townName);
                if (claims == null || claims.isEmpty()) {
                    // nothing to show
                    return;
                }

                // Group claims by world
                Map<String, Set<ChunkCoord>> byWorld = new HashMap<>();
                for (Claim c : claims) {
                    String w = c.getWorldName();
                    byWorld.computeIfAbsent(w, k -> new HashSet<>())
                            .add(new ChunkCoord(c.getChunkX(), c.getChunkZ()));
                }

                double eyeY = player.getLocation().getY();

                for (Map.Entry<String, Set<ChunkCoord>> entry : byWorld.entrySet()) {
                    World world = Bukkit.getWorld(entry.getKey());
                    if (world == null) continue;

                    Set<ChunkCoord> all = entry.getValue();

                    // Discover connected components and draw each
                    Set<ChunkCoord> remaining = new HashSet<>(all);
                    while (!remaining.isEmpty()) {
                        ChunkCoord start = remaining.iterator().next();
                        Set<ChunkCoord> component = floodFillComponent(start, remaining);
                        // component now contains one connected region; remove from remaining
                        remaining.removeAll(component);

                        // compute exposed edges for component
                        Map<Integer, List<IntSegment>> horizontalEdges = new HashMap<>(); // zEdge -> list of x segments
                        Map<Integer, List<IntSegment>> verticalEdges = new HashMap<>();   // xEdge -> list of z segments

                        for (ChunkCoord cc : component) {
                            int cx = cc.x;
                            int cz = cc.z;

                            int minX = cx * 16;
                            int maxX = (cx + 1) * 16; // edge coordinate at (x+1)*16
                            int minZ = cz * 16;
                            int maxZ = (cz + 1) * 16;

                            // North (cz-1)
                            if (!component.contains(new ChunkCoord(cx, cz - 1))) {
                                horizontalEdges.computeIfAbsent(minZ, k -> new ArrayList<>())
                                        .add(new IntSegment(minX, maxX));
                            }
                            // South (cz+1)
                            if (!component.contains(new ChunkCoord(cx, cz + 1))) {
                                horizontalEdges.computeIfAbsent(maxZ, k -> new ArrayList<>())
                                        .add(new IntSegment(minX, maxX));
                            }
                            // West (cx-1)
                            if (!component.contains(new ChunkCoord(cx - 1, cz))) {
                                verticalEdges.computeIfAbsent(minX, k -> new ArrayList<>())
                                        .add(new IntSegment(minZ, maxZ));
                            }
                            // East (cx+1)
                            if (!component.contains(new ChunkCoord(cx + 1, cz))) {
                                verticalEdges.computeIfAbsent(maxX, k -> new ArrayList<>())
                                        .add(new IntSegment(minZ, maxZ));
                            }
                        }

                        // Merge segments per edge and draw
                        // Horizontal edges (constant Z): segments along X
                        for (Map.Entry<Integer, List<IntSegment>> he : horizontalEdges.entrySet()) {
                            int zEdge = he.getKey();
                            List<IntSegment> segs = mergeSegments(he.getValue());
                            for (IntSegment seg : segs) {
                                // draw particle line from seg.start -> seg.end at zEdge
                                for (int x = seg.start; x <= seg.end; x += particleStep) {
                                    spawnParticle(world, new Location(world, x + 0.5, eyeY, zEdge + 0.5));
                                }
                                // draw corner markers
                                spawnParticle(world, new Location(world, seg.start + 0.5, eyeY, zEdge + 0.5));
                                spawnParticle(world, new Location(world, seg.end + 0.5, eyeY, zEdge + 0.5));
                            }
                        }

                        // Vertical edges (constant X): segments along Z
                        for (Map.Entry<Integer, List<IntSegment>> ve : verticalEdges.entrySet()) {
                            int xEdge = ve.getKey();
                            List<IntSegment> segs = mergeSegments(ve.getValue());
                            for (IntSegment seg : segs) {
                                for (int z = seg.start; z <= seg.end; z += particleStep) {
                                    spawnParticle(world, new Location(world, xEdge + 0.5, eyeY, z + 0.5));
                                }
                                spawnParticle(world, new Location(world, xEdge + 0.5, eyeY, seg.start + 0.5));
                                spawnParticle(world, new Location(world, xEdge + 0.5, eyeY, seg.end + 0.5));
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

    // Flood-fill to get 4-neighbor connected component and remove visited from remaining set
    private Set<ChunkCoord> floodFillComponent(ChunkCoord start, Set<ChunkCoord> pool) {
        Set<ChunkCoord> comp = new HashSet<>();
        ArrayDeque<ChunkCoord> dq = new ArrayDeque<>();
        if (!pool.contains(start)) return comp;
        dq.add(start);
        comp.add(start);

        while (!dq.isEmpty()) {
            ChunkCoord cur = dq.poll();
            ChunkCoord[] neigh = {
                    new ChunkCoord(cur.x + 1, cur.z),
                    new ChunkCoord(cur.x - 1, cur.z),
                    new ChunkCoord(cur.x, cur.z + 1),
                    new ChunkCoord(cur.x, cur.z - 1)
            };
            for (ChunkCoord n : neigh) {
                if (pool.contains(n) && !comp.contains(n)) {
                    comp.add(n);
                    dq.add(n);
                }
            }
        }
        return comp;
    }

    // Merge integer segments [start,end] that are collinear and contiguous/overlapping
    private List<IntSegment> mergeSegments(List<IntSegment> segments) {
        if (segments == null || segments.isEmpty()) return Collections.emptyList();
        segments.sort(Comparator.comparingInt(s -> s.start));
        List<IntSegment> merged = new ArrayList<>();
        IntSegment cur = segments.get(0);
        for (int i = 1; i < segments.size(); i++) {
            IntSegment next = segments.get(i);
            if (next.start <= cur.end) {
                // overlap or contiguous
                cur.end = Math.max(cur.end, next.end);
            } else {
                merged.add(cur);
                cur = next;
            }
        }
        merged.add(cur);
        return merged;
    }

    private void spawnParticle(World world, Location loc) {
        // Use FLAME particle to avoid API compatibility issues with DustOptions on some server builds
        world.spawnParticle(Particle.FLAME, loc, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // Helper classes
    private static class ChunkCoord {
        final int x, z;
        ChunkCoord(int x, int z) { this.x = x; this.z = z; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkCoord c)) return false;
            return x == c.x && z == c.z;
        }
        @Override public int hashCode() { return Objects.hash(x, z); }
    }

    private static class IntSegment {
        int start, end;
        IntSegment(int s, int e) {
            this.start = Math.min(s, e);
            this.end = Math.max(s, e);
        }
    }
}