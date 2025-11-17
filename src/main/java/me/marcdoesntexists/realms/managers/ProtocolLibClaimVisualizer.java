package me.marcdoesntexists.realms.managers;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * ProtocolLib-based claim visualizer fully client-sided using block-change packets.
 * This avoids server-side entities and reduces server load and lag.
 */
public class ProtocolLibClaimVisualizer implements ClaimVisualizer {
    private final Realms plugin;
    private final ClaimManager claimManager;
    private final ProtocolManager protocolManager;

    private final Map<UUID, VisualizationData> activeVisualizers = new ConcurrentHashMap<>();

    public ProtocolLibClaimVisualizer(Realms plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);

        ProtocolManager pm = null;
        try {
            pm = ProtocolLibrary.getProtocolManager();
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().log(Level.WARNING, "ProtocolLib non trovato: visualizer userà fallback sendBlockChange.", e);
        }
        this.protocolManager = pm;

        // Register listeners to stop visualizations when players disconnect/teleport/change world/kicked
        try {
            Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(), plugin);
            // protection listener prevents players from standing on client-side marker blocks
            Bukkit.getPluginManager().registerEvents(new PlayerProtectionListener(), plugin);
            // intercept teleport commands if configured
            Bukkit.getPluginManager().registerEvents(new CommandInterceptorListener(), plugin);
            // protect mounts (vehicle enter)
            Bukkit.getPluginManager().registerEvents(new MountProtectionListener(), plugin);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isVisualizing(Player player) {
        return activeVisualizers.containsKey(player.getUniqueId());
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
                if (!player.isOnline()) {
                    stopVisualization(player);
                    return;
                }
                renderBorders(player, data);
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    @Override
    public void stopVisualization(Player player) {
        VisualizationData data = activeVisualizers.remove(player.getUniqueId());
        if (data != null) {
            if (data.updateTask != null) data.updateTask.cancel();
            removeAllDisplays(player, data);
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
                // can't send packets to offline player, but clear tracking
                data.edgeToLocations.clear();
                data.activeEdges.clear();
                data.originalBlocks.clear();
            }
        }
    }

    private void renderBorders(Player player, VisualizationData data) {
        Collection<Claim> claims = claimManager.getTownClaims(data.townName);
        if (claims == null || claims.isEmpty()) return;
        World playerWorld = player.getWorld();
        Set<ChunkCoord> visibleChunks = getChunkCoords(player, claims, playerWorld);
        if (visibleChunks.isEmpty()) {
            removeAllDisplays(player, data);
            return;
        }
        Set<BorderEdge> edges = findBorderEdges(visibleChunks);
        // remove edges no longer present
        Set<BorderEdge> toRemove = new HashSet<>(data.activeEdges);
        toRemove.removeAll(edges);
        for (BorderEdge e : toRemove) {
            List<Location> locs = data.edgeToLocations.remove(e);
            if (locs != null) {
                for (Location loc : locs) restoreBlockForPlayer(player, loc, data);
            }
        }
        data.activeEdges.removeAll(toRemove);
        // add new edges
        Set<BorderEdge> toAdd = new HashSet<>(edges);
        toAdd.removeAll(data.activeEdges);
        for (BorderEdge e : toAdd) {
            List<Location> locs = spawnClientBlockMarkers(player, e, data);
            data.edgeToLocations.put(e, locs);
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
            int minX = chunk.x * 16;
            int maxX = (chunk.x + 1) * 16;
            int minZ = chunk.z * 16;
            int maxZ = (chunk.z + 1) * 16;
            if (!chunks.contains(new ChunkCoord(chunk.x, chunk.z - 1)))
                edges.add(new BorderEdge(minX, minZ, maxX, minZ, EdgeDirection.HORIZONTAL));
            if (!chunks.contains(new ChunkCoord(chunk.x, chunk.z + 1)))
                edges.add(new BorderEdge(minX, maxZ, maxX, maxZ, EdgeDirection.HORIZONTAL));
            if (!chunks.contains(new ChunkCoord(chunk.x - 1, chunk.z)))
                edges.add(new BorderEdge(minX, minZ, minX, maxZ, EdgeDirection.VERTICAL));
            if (!chunks.contains(new ChunkCoord(chunk.x + 1, chunk.z)))
                edges.add(new BorderEdge(maxX, minZ, maxX, maxZ, EdgeDirection.VERTICAL));
        }
        return edges;
    }

    /**
     * Spawna marker client-sided inviando pacchetti BLOCK_CHANGE (ProtocolLib).
     * Se ProtocolLib non disponibile effettua fallback a player.sendBlockChange.
     */
    private List<Location> spawnClientBlockMarkers(Player player, BorderEdge edge, VisualizationData data) {
        List<Location> spawned = new ArrayList<>();
        try {
            Material markerBlock = Material.GLOWSTONE;
            int spacing = 4; // blocks between displays
            if (edge.direction == EdgeDirection.HORIZONTAL) {
                int start = edge.x1;
                int end = edge.x2;
                for (int x = start; x <= end; x += spacing) {
                    World w = player.getWorld();
                    int by = getSurfaceY(w, x, edge.z1); // place marker just above ground
                    Location blockLoc = new Location(w, x, by, edge.z1);
                    // Save original block data if not already saved
                    if (!data.originalBlocks.containsKey(blockLoc)) {
                        WrappedBlockData orig = WrappedBlockData.createData(blockLoc.getBlock().getBlockData());
                        data.originalBlocks.put(blockLoc.clone(), orig);
                    }
                    // Send client-side block change
                    boolean ok = sendBlockChangePacket(player, blockLoc, markerBlock);
                    if (!ok) {
                        // fallback
                        player.sendBlockChange(blockLoc, Bukkit.createBlockData(markerBlock));
                    }
                    spawned.add(blockLoc.clone());
                }
            } else {
                int start = edge.z1;
                int end = edge.z2;
                for (int z = start; z <= end; z += spacing) {
                    World w = player.getWorld();
                    int by = getSurfaceY(w, edge.x1, z); // place marker just above ground
                    Location blockLoc = new Location(w, edge.x1, by, z);
                    if (!data.originalBlocks.containsKey(blockLoc)) {
                        WrappedBlockData orig = WrappedBlockData.createData(blockLoc.getBlock().getBlockData());
                        data.originalBlocks.put(blockLoc.clone(), orig);
                    }
                    boolean ok = sendBlockChangePacket(player, blockLoc, markerBlock);
                    if (!ok) {
                        player.sendBlockChange(blockLoc, Bukkit.createBlockData(markerBlock));
                    }
                    spawned.add(blockLoc.clone());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn client markers: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Stacktrace:", e);
        }
        return spawned;
    }

    // Return the Y coordinate to place a marker at given x,z: highest solid block Y + 1 (air above terrain), clamped
    private int getSurfaceY(World world, int x, int z) {
        try {
            int top = world.getHighestBlockYAt(x, z) + 1;
            return Math.max(1, Math.min(255, top));
        } catch (Throwable t) {
            return Math.min(255, 64);
        }
    }

    /**
     * Invia un pacchetto BLOCK_CHANGE al solo giocatore tramite ProtocolLib.
     * Restituisce true se il pacchetto è stato inviato, false per fallback.
     */
    private boolean sendBlockChangePacket(Player player, Location loc, Material material) {
        if (protocolManager == null) return false;
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            // BlockPosition is x,y,z ints
            BlockPosition pos = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            packet.getBlockPositionModifier().write(0, pos);

            WrappedBlockData wrapped = WrappedBlockData.createData(material);
            // ProtocolLib expects the internal block data handle on some versions, but write(Object) usually works
            packet.getBlockData().write(0, (WrappedBlockData) wrapped.getHandle());

            protocolManager.sendServerPacket(player, packet);
            return true;
        } catch (Exception e) {
            // log at fine level to avoid spam but keep info for debugging
            plugin.getLogger().finer("ProtocolLib packet failed for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void restoreBlockForPlayer(Player player, Location loc, VisualizationData data) {
        try {
            WrappedBlockData orig = data.originalBlocks.remove(loc);
            if (orig != null) {
                boolean ok = sendWrappedBlockDataPacket(player, loc, orig);
                if (!ok) {
                    // fallback to server block state
                    BlockData serverData = loc.getBlock().getBlockData();
                    player.sendBlockChange(loc, serverData);
                }
            } else {
                // if we don't have original, request server block data
                BlockData serverData = loc.getBlock().getBlockData();
                player.sendBlockChange(loc, serverData);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean sendWrappedBlockDataPacket(Player player, Location loc, WrappedBlockData wrapped) {
        if (protocolManager == null) return false;
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            BlockPosition pos = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            packet.getBlockPositionModifier().write(0, pos);
            packet.getBlockData().write(0, (WrappedBlockData) wrapped.getHandle());
            protocolManager.sendServerPacket(player, packet);
            return true;
        } catch (Exception e) {
            plugin.getLogger().finer("ProtocolLib restore packet failed for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void removeAllDisplays(Player player, VisualizationData data) {
        if (data == null || data.edgeToLocations.isEmpty()) return;

        for (List<Location> lst : data.edgeToLocations.values()) {
            for (Location loc : lst) {
                restoreBlockForPlayer(player, loc, data);
            }
        }

        data.edgeToLocations.clear();
        data.activeEdges.clear();
        data.originalBlocks.clear();
    }

    private enum EdgeDirection {HORIZONTAL, VERTICAL}

    private record ChunkCoord(int x, int z) {}

    private record BorderEdge(int x1, int z1, int x2, int z2, EdgeDirection direction) {}

    private static class VisualizationData {
        final String townName;
        final Map<BorderEdge, List<Location>> edgeToLocations = new HashMap<>();
        final Set<BorderEdge> activeEdges = new HashSet<>();
        final Map<Location, WrappedBlockData> originalBlocks = new ConcurrentHashMap<>();
        BukkitTask updateTask;

        VisualizationData(String townName) {
            this.townName = townName;
        }
    }

    // Listener that stops visualizations on relevant player lifecycle events to avoid orphaned tasks
    private class PlayerLifecycleListener implements Listener {
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent ev) {
            ProtocolLibClaimVisualizer.this.stopVisualization(ev.getPlayer());
        }

        @EventHandler
        public void onPlayerKick(PlayerKickEvent ev) {
            ProtocolLibClaimVisualizer.this.stopVisualization(ev.getPlayer());
        }

        @EventHandler
        public void onPlayerTeleport(PlayerTeleportEvent ev) {
            Player p = ev.getPlayer();
            // stop visualization on teleport to avoid visual artifacts if player changes location or dimension
            ProtocolLibClaimVisualizer.this.stopVisualization(p);
        }

        // Note: intentionally not stopping on world-change/move to keep visualizer active while walking.
        // Plugin onDisable() will call stopAll() to cleanup on server stop/crash.
    }

    // Prevent players from standing on client-side marker blocks by teleporting them back or safe-teleporting.
    private class PlayerProtectionListener implements Listener {
        @EventHandler
        public void onPlayerMove(PlayerMoveEvent ev) {
            if (!plugin.getConfig().getBoolean("visualizer.protection.enabled", true)) return;

            Player p = ev.getPlayer();
            if (!ProtocolLibClaimVisualizer.this.isVisualizing(p)) return;
            // allow bypass for admins/staff
            if (p.hasPermission("realms.visualizer.bypass")) return;

            VisualizationData data = activeVisualizers.get(p.getUniqueId());
            if (data == null || data.edgeToLocations.isEmpty()) return;

            // check destination block
            org.bukkit.Location toBlock = ev.getTo().getBlock().getLocation();
            for (List<org.bukkit.Location> lst : data.edgeToLocations.values()) {
                for (org.bukkit.Location marker : lst) {
                    if (marker.getWorld().equals(toBlock.getWorld()) &&
                            marker.getBlockX() == toBlock.getBlockX() &&
                            marker.getBlockY() == toBlock.getBlockY() &&
                            marker.getBlockZ() == toBlock.getBlockZ()) {

                        if (plugin.getConfig().getBoolean("visualizer.protection.log-attempts", true)) {
                            plugin.getLogger().info("Visualizer block move attempt by " + p.getName() + " at " + toBlock);
                        }

                        String mode = plugin.getConfig().getString("visualizer.protection.mode", "safe-teleport");
                        if ("safe-teleport".equalsIgnoreCase(mode)) {
                            org.bukkit.Location safe = findSafeLocationNear(ev.getFrom());
                            if (safe != null) {
                                p.teleport(safe);
                            } else {
                                ev.setTo(ev.getFrom());
                            }
                        } else {
                            // bounce
                            ev.setTo(ev.getFrom());
                        }
                        return;
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerTeleport(PlayerTeleportEvent ev) {
            if (!plugin.getConfig().getBoolean("visualizer.protection.enabled", true)) return;

            Player p = ev.getPlayer();
            if (!ProtocolLibClaimVisualizer.this.isVisualizing(p)) return;
            if (p.hasPermission("realms.visualizer.bypass")) return;

            VisualizationData data = activeVisualizers.get(p.getUniqueId());
            if (data == null || data.edgeToLocations.isEmpty()) return;

            org.bukkit.Location toBlock = ev.getTo().getBlock().getLocation();
            for (List<org.bukkit.Location> lst : data.edgeToLocations.values()) {
                for (org.bukkit.Location marker : lst) {
                    if (marker.getWorld().equals(toBlock.getWorld()) &&
                            marker.getBlockX() == toBlock.getBlockX() &&
                            marker.getBlockY() == toBlock.getBlockY() &&
                            marker.getBlockZ() == toBlock.getBlockZ()) {

                        if (plugin.getConfig().getBoolean("visualizer.protection.log-attempts", true)) {
                            plugin.getLogger().info("Visualizer block teleport attempt by " + p.getName() + " to " + toBlock);
                        }

                        String mode = plugin.getConfig().getString("visualizer.protection.mode", "safe-teleport");
                        if ("safe-teleport".equalsIgnoreCase(mode)) {
                            org.bukkit.Location safe = findSafeLocationNear(ev.getFrom());
                            if (safe != null) ev.setTo(safe);
                            else ev.setCancelled(true);
                        } else {
                            ev.setTo(ev.getFrom());
                        }
                        return;
                    }
                }
            }
        }
    }

    // Intercept common teleport commands to prevent players from setting homes or warping onto markers (best-effort)
    private class CommandInterceptorListener implements Listener {
        @EventHandler
        public void onPlayerCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent ev) {
            if (!plugin.getConfig().getBoolean("visualizer.protection.enabled", true)) return;
            if (!plugin.getConfig().getBoolean("visualizer.protection.protect-teleport-commands", true)) return;

            Player p = ev.getPlayer();
            if (!ProtocolLibClaimVisualizer.this.isVisualizing(p)) return;
            if (p.hasPermission("realms.visualizer.bypass")) return;

            String msg = ev.getMessage().toLowerCase(Locale.ROOT);
            // block simple known teleport commands
            if (msg.startsWith("/sethome") || msg.startsWith("/home") || msg.startsWith("/spawn") || msg.startsWith("/tp ")) {
                // best-effort: if player's current block or look target is a marker, cancel
                org.bukkit.Location loc = p.getLocation().getBlock().getLocation();
                VisualizationData data = activeVisualizers.get(p.getUniqueId());
                if (data != null && isMarkerAt(loc, data)) {
                    if (plugin.getConfig().getBoolean("visualizer.protection.log-attempts", true)) {
                        plugin.getLogger().info("Blocked teleport command by " + p.getName() + " while marker active at " + loc);
                    }
                    p.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.get("visualizer.unavailable"));
                    ev.setCancelled(true);
                }
            }
        }
    }

    // Prevent mounts from being placed on markers
    private class MountProtectionListener implements Listener {
        @EventHandler
        public void onVehicleEnter(org.bukkit.event.vehicle.VehicleEnterEvent ev) {
            if (!plugin.getConfig().getBoolean("visualizer.protection.enabled", true)) return;
            if (!plugin.getConfig().getBoolean("visualizer.protection.protect-mounts", true)) return;
            if (!(ev.getEntered() instanceof Player p)) return;
            if (!ProtocolLibClaimVisualizer.this.isVisualizing(p)) return;
            if (p.hasPermission("realms.visualizer.bypass")) return;

            VisualizationData data = activeVisualizers.get(p.getUniqueId());
            if (data == null || data.edgeToLocations.isEmpty()) return;

            org.bukkit.Location loc = ev.getVehicle().getLocation().getBlock().getLocation();
            if (isMarkerAt(loc, data)) {
                if (plugin.getConfig().getBoolean("visualizer.protection.log-attempts", true)) {
                    plugin.getLogger().info("Blocked mount enter by " + p.getName() + " at " + loc);
                }
                ev.setCancelled(true);
            }
        }
    }

    // helper: checks whether a marker exists at this block location for the player's active visualization
    private boolean isMarkerAt(org.bukkit.Location loc, VisualizationData data) {
        for (List<org.bukkit.Location> lst : data.edgeToLocations.values()) {
            for (org.bukkit.Location marker : lst) {
                if (marker.getWorld().equals(loc.getWorld()) && marker.getBlockX() == loc.getBlockX()
                        && marker.getBlockY() == loc.getBlockY() && marker.getBlockZ() == loc.getBlockZ()) return true;
            }
        }
        return false;
    }

    // helper: search for safe location around 'from' within look-radius
    private org.bukkit.Location findSafeLocationNear(org.bukkit.Location from) {
        int radius = plugin.getConfig().getInt("visualizer.protection.look-radius", 2);
        int yOffset = plugin.getConfig().getInt("visualizer.protection.safe-teleport-vertical-offset", 1);
        World w = from.getWorld();
        int baseX = from.getBlockX();
        int baseY = from.getBlockY();
        int baseZ = from.getBlockZ();

        // first try same x,z with y + offset
        org.bukkit.Location tryLoc = new org.bukkit.Location(w, baseX + 0.5, baseY + yOffset, baseZ + 0.5);
        if (isLocationSafe(tryLoc)) return tryLoc;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    int tx = baseX + dx;
                    int ty = baseY + dy + yOffset;
                    int tz = baseZ + dz;
                    org.bukkit.Location candidate = new org.bukkit.Location(w, tx + 0.5, ty, tz + 0.5);
                    if (isLocationSafe(candidate)) return candidate;
                }
            }
        }
        return null;
    }

    private boolean isLocationSafe(org.bukkit.Location loc) {
        if (loc == null) return false;
        World w = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        // Check block at feet and head
        org.bukkit.block.Block feet = w.getBlockAt(x, y, z);
        org.bukkit.block.Block head = w.getBlockAt(x, y + 1, z);
        // safe if feet is solid ground and head and feet are not solid (no suffocation)
        boolean feetSolid = feet.getType().isSolid();
        boolean headSolid = head.getType().isSolid();
        return feetSolid && !headSolid;
    }

}
