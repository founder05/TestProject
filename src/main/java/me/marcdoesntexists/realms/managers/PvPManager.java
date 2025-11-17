package me.marcdoesntexists.realms.managers;

import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.SoundUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
/**
 * PvPManager - Manages player versus player duels
 */
public class PvPManager {

    // Singleton instance
    private static PvPManager instance;

    // Active data structures
    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, DuelSession> activeSessions = new ConcurrentHashMap<>();

    // Configuration
    private Plugin plugin;
    private final DuelConfig config;

    // Monitoring task
    private BukkitTask monitorTask;

    // Constants
    private static final long REQUEST_TIMEOUT_MS = 30_000L; // 30 seconds
    private static final long MONITOR_INTERVAL_TICKS = 20L; // 1 second

    private PvPManager() {
        this.config = new DuelConfig();
    }

    /**
     * Get singleton instance
     */
    public static PvPManager getInstance() {
        if (instance == null) {
            instance = new PvPManager();
        }
        return instance;
    }

    /**
     * Initialize the PvP manager with plugin instance and configuration
     *
     * @param plugin the plugin instance
     */
    public void init(Plugin plugin) {
        if (this.plugin != null) {
            throw new IllegalStateException("PvPManager already initialized");
        }

        this.plugin = plugin;
        loadConfiguration();
        startMonitorTask();

        plugin.getLogger().info("PvPManager initialized successfully");
    }

    /**
     * Load configuration from plugin config
     */
    private void loadConfiguration() {
        try {
            var cfg = plugin.getConfig();

            config.radius = cfg.getDouble("pvp.radius", 50.0);
            config.timeoutSeconds = cfg.getLong("pvp.timeout-seconds", 120);
            config.penalty = PenaltyType.fromString(
                    cfg.getString("pvp.penalty", "KILL")
            );

            // Boss bar configuration
            config.bossBarEnabled = cfg.getBoolean("pvp.bossbar.enabled", true);
            config.bossBarColor = parseBossBarColor(
                    cfg.getString("pvp.bossbar.color", "RED")
            );
            config.bossBarOverlay = parseBossBarOverlay(
                    cfg.getString("pvp.bossbar.style", "PROGRESS")
            );

            // Sound configuration
            String startSoundName = cfg.getString("pvp.sound.start", "ENTITY_ENDER_DRAGON_GROWL");
            String endSoundName = cfg.getString("pvp.sound.end", "ENTITY_PLAYER_LEVELUP");
            String errorSoundName = cfg.getString("pvp.sound.error", "ENTITY_VILLAGER_NO");

            config.startSound = startSoundName;
            config.endSound = endSoundName;
            config.errorSound = errorSoundName;

            plugin.getLogger().info("PvP configuration loaded: " +
                    "radius=" + config.radius + ", " +
                    "timeout=" + config.timeoutSeconds + "s, " +
                    "penalty=" + config.penalty);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load PvP configuration", e);
            // Use defaults on error
        }
    }

    /**
     * Send a duel request from one player to another
     *
     * @param requester the player sending the request
     * @param target the player receiving the request
     * @return true if request was sent successfully
     */
    public boolean sendRequest(Player requester, Player target) {
        if (requester == null || target == null) {
            return false;
        }

        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(MessageUtils.get("duel.cant_self"));
            return false;
        }

        if (isInDuel(requester)) {
            requester.sendMessage(MessageUtils.get("duel.already_in_duel"));
            return false;
        }

        if (isInDuel(target)) {
            requester.sendMessage(MessageUtils.format("duel.target_in_duel",
                    Map.of("target", target.getName())));
            return false;
        }

        if (hasPendingRequest(target)) {
            requester.sendMessage(MessageUtils.get("duel.target_has_pending"));
            return false;
        }

        DuelRequest request = new DuelRequest(
                requester.getUniqueId(),
                target.getUniqueId(),
                System.currentTimeMillis()
        );

        pendingRequests.put(target.getUniqueId(), request);

        // Notify both players
        requester.sendMessage(MessageUtils.format("duel.sent",
                Map.of("player", target.getName())));
        target.sendMessage(MessageUtils.format("duel.request_received",
                Map.of("player", requester.getName())));

        // Play sound
        SoundUtils.playSound(requester, config.startSound, 0.5f, 1.0f);
        SoundUtils.playSound(target, config.startSound, 0.5f, 1.0f);

        return true;
    }

    public boolean acceptRequest(Player target, Location origin) {
        DuelRequest request = pendingRequests.remove(target.getUniqueId());

        if (request == null) {
            target.sendMessage(MessageUtils.get("duel.no_request"));
            return false;
        }

        if (System.currentTimeMillis() - request.timestamp > REQUEST_TIMEOUT_MS) {
            target.sendMessage(MessageUtils.get("duel.request_expired"));
            return false;
        }

        Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(MessageUtils.get("duel.requester_offline"));
            return false;
        }

        // Verify both players can still duel
        if (isInDuel(requester) || isInDuel(target)) {
            target.sendMessage(MessageUtils.get("duel.already_in_duel"));
            SoundUtils.playSound(target, config.errorSound);
            return false;
        }

        // Start the duel
        Location duelOrigin = origin != null ? origin : requester.getLocation();
        startDuel(
                requester.getUniqueId(),
                target.getUniqueId(),
                duelOrigin,
                config.radius,
                config.timeoutSeconds * 1000L
        );

        // Notify players
        requester.sendMessage(MessageUtils.format("duel.accepted",
                Map.of("opponent", target.getName())));
        target.sendMessage(MessageUtils.format("duel.accepted",
                Map.of("opponent", requester.getName())));

        requester.sendMessage(MessageUtils.format("duel.start",
                Map.of("opponent", target.getName())));
        target.sendMessage(MessageUtils.format("duel.start",
                Map.of("opponent", requester.getName())));

        // Play start sound
        SoundUtils.playSound(requester, config.startSound, 1.0f, 1.0f);
        SoundUtils.playSound(target, config.startSound, 1.0f, 1.0f);

        return false;
    }

    /**
     * Cancel a pending duel request
     *
     * @param target the player whose request to cancel
     * @return true if request was cancelled
     */
    public boolean cancelRequest(Player target) {
        DuelRequest removed = pendingRequests.remove(target.getUniqueId());
        return removed != null;
    }

    /**
     * Check if a player has a pending duel request
     *
     * @param target the player to check
     * @return true if player has a pending request
     */
    public boolean hasPendingRequest(Player target) {
        DuelRequest request = pendingRequests.get(target.getUniqueId());
        if (request == null) return false;

        if (System.currentTimeMillis() - request.timestamp > REQUEST_TIMEOUT_MS) {
            pendingRequests.remove(target.getUniqueId());
            return false;
        }

        return true;
    }

    /**
     * Get the UUID of the player who sent a duel request
     *
     * @param target the target player
     * @return optional containing requester UUID
     */
    public Optional<UUID> getRequesterUUID(Player target) {
        DuelRequest request = pendingRequests.get(target.getUniqueId());
        if (request == null) return Optional.empty();

        if (System.currentTimeMillis() - request.timestamp > REQUEST_TIMEOUT_MS) {
            pendingRequests.remove(target.getUniqueId());
            return Optional.empty();
        }

        return Optional.of(request.requester);
    }

    /**
     * Check if a player is in an active duel
     *
     * @param player the player to check
     * @return true if player is in a duel
     */
    public boolean isInDuel(Player player) {
        return player != null && activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Check if a UUID is in an active duel
     *
     * @param uuid the UUID to check
     * @return true if UUID is in a duel
     */
    public boolean isInDuel(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    /**
     * Get the opponent of a player in a duel
     *
     * @param uuid the player's UUID
     * @return optional containing opponent's UUID
     */
    public Optional<UUID> getOpponentUUID(UUID uuid) {
        DuelSession session = activeSessions.get(uuid);
        if (session == null) return Optional.empty();

        return Optional.of(
                session.playerA.equals(uuid) ? session.playerB : session.playerA
        );
    }

    /**
     * Get the opponent player object
     *
     * @param player the player
     * @return opponent player or null
     */
    public Player getOpponent(Player player) {
        return getOpponentUUID(player.getUniqueId())
                .map(Bukkit::getPlayer)
                .orElse(null);
    }

    /**
     * Get remaining time in a duel
     *
     * @param uuid the player's UUID
     * @return remaining seconds, or 0 if not in duel
     */
    public long getRemainingSeconds(UUID uuid) {
        DuelSession session = activeSessions.get(uuid);
        if (session == null) return 0L;

        long elapsed = System.currentTimeMillis() - session.startedAt;
        long remaining = session.timeoutMs - elapsed;
        return Math.max(0L, remaining / 1000L);
    }

    /**
     * Check if two UUIDs are in a duel together
     *
     * @param a first UUID
     * @param b second UUID
     * @return true if they are dueling each other
     */
    public boolean isDuelAcceptedBetween(UUID a, UUID b) {
        DuelSession session = activeSessions.get(a);
        if (session == null) return false;

        return (session.playerA.equals(a) && session.playerB.equals(b)) ||
                (session.playerA.equals(b) && session.playerB.equals(a));
    }

    /**
     * End a duel for a specific player
     *
     * @param uuid the player's UUID
     */
    public void endDuel(UUID uuid) {
        DuelSession session = activeSessions.remove(uuid);
        if (session == null) return;

        UUID other = session.playerA.equals(uuid) ? session.playerB : session.playerA;
        activeSessions.remove(other);

        cleanupSession(session);
    }

    /**
     * End a duel for a player
     *
     * @param player the player
     */
    public void endDuel(Player player) {
        if (player != null) {
            endDuel(player.getUniqueId());
        }
    }

    /**
     * Handle a player quitting/disconnecting during a duel
     *
     * @param quitter the UUID of the player who quit
     */
    public void handlePlayerQuit(UUID quitter) {
        DuelSession session = activeSessions.get(quitter);
        if (session == null) return;

        UUID winner = session.playerA.equals(quitter) ? session.playerB : session.playerA;
        finishDuel(session, winner, quitter, EndReason.QUIT);
    }

    /**
     * Start a new duel session
     */
    private void startDuel(UUID playerA, UUID playerB, Location origin,
                           double radius, long timeoutMs) {
        DuelSession session = new DuelSession(
                playerA,
                playerB,
                System.currentTimeMillis(),
                origin.clone(),
                radius,
                timeoutMs
        );

        // Create boss bar for Paper 1.21+
        if (config.bossBarEnabled) {
            try {
                Player pA = Bukkit.getPlayer(playerA);
                Player pB = Bukkit.getPlayer(playerB);

                String nameA = pA != null ? pA.getName() : "Player A";
                String nameB = pB != null ? pB.getName() : "Player B";

                Component title = MessageUtils.toComponent(
                        MessageUtils.format("duel.bossbar_title",
                                Map.of("player1", nameA, "player2", nameB))
                );

                BossBar bossBar = BossBar.bossBar(
                        title,
                        1.0f,
                        config.bossBarColor,
                        config.bossBarOverlay
                );

                session.bossBar = bossBar;

                if (pA != null) pA.showBossBar(bossBar);
                if (pB != null) pB.showBossBar(bossBar);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to create boss bar for duel", e);
            }
        }

        activeSessions.put(playerA, session);
        activeSessions.put(playerB, session);
    }

    /**
     * Finish a duel with a winner
     */
    private void finishDuel(DuelSession session, UUID winnerUuid,
                            UUID loserUuid, EndReason reason) {
        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = Bukkit.getPlayer(loserUuid);

        // Send messages
        if (winner != null) {
            String opponentName = loser != null ? loser.getName() : "Unknown";
            winner.sendMessage(MessageUtils.format("duel.winner",
                    Map.of("opponent", opponentName)));
        }

        if (loser != null) {
            String opponentName = winner != null ? winner.getName() : "Unknown";
            loser.sendMessage(MessageUtils.format("duel.loser",
                    Map.of("opponent", opponentName)));
        }

        // Apply penalty
        applyPenalty(loser, reason);

        // Play end sounds
        if (winner != null) {
            SoundUtils.playSound(winner, config.endSound, 1.0f, 1.0f);
        }
        if (loser != null) {
            SoundUtils.playSound(loser, config.endSound, 1.0f, 0.8f);
        }

        // Cleanup
        cleanupSession(session);
        activeSessions.remove(session.playerA);
        activeSessions.remove(session.playerB);
    }

    /**
     * Apply penalty to losing player
     */
    private void applyPenalty(Player loser, EndReason reason) {
        if (loser == null || !loser.isOnline()) return;

        try {
            if (config.penalty == PenaltyType.KILL) {
                // Only kill if player is alive
                if (!loser.isDead() && loser.getHealth() > 0) {
                    loser.setHealth(0.0);
                    loser.sendMessage(MessageUtils.get("duel.penalty_kill"));
                }
            } else if (config.penalty == PenaltyType.DAMAGE) {
                // Apply damage without killing
                double currentHealth = loser.getHealth();
                double newHealth = Math.max(1.0, currentHealth - 10.0);
                loser.setHealth(newHealth);
            }
            // NONE penalty type does nothing
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to apply duel penalty to " + loser.getName(), e);
        }
    }

    /**
     * Cleanup a session's resources (boss bars, etc)
     */
    private void cleanupSession(DuelSession session) {
        if (session.bossBar != null) {
            try {
                Player pA = Bukkit.getPlayer(session.playerA);
                Player pB = Bukkit.getPlayer(session.playerB);

                if (pA != null) pA.hideBossBar(session.bossBar);
                if (pB != null) pB.hideBossBar(session.bossBar);

                session.bossBar = null;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to cleanup boss bar", e);
            }
        }
    }

    /**
     * Start the monitoring task
     */
    private void startMonitorTask() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }

        monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                monitorDuels();
                cleanupExpiredRequests();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Error in PvP monitor task", e);
            }
        }, MONITOR_INTERVAL_TICKS, MONITOR_INTERVAL_TICKS);
    }

    /**
     * Monitor active duels for timeouts, disconnects, and escapes
     */
    private void monitorDuels() {
        // Create snapshot to avoid ConcurrentModificationException
        Set<DuelSession> sessions = new HashSet<>(activeSessions.values());
        Set<DuelSession> processed = new HashSet<>();

        for (DuelSession session : sessions) {
            // Skip if already processed (both players point to same session)
            if (processed.contains(session)) continue;
            processed.add(session);

            Player playerA = Bukkit.getPlayer(session.playerA);
            Player playerB = Bukkit.getPlayer(session.playerB);

            // Check if either player is offline
            if (playerA == null || !playerA.isOnline() ||
                    playerB == null || !playerB.isOnline()) {
                handleOfflinePlayer(session, playerA, playerB);
                continue;
            }

            long elapsed = System.currentTimeMillis() - session.startedAt;

            // Check timeout
            if (elapsed > session.timeoutMs) {
                handleTimeout(session, playerA, playerB);
                continue;
            }

            // Update boss bar
            updateBossBar(session, playerA, playerB, elapsed);

            // Check radius (escape)
            checkRadius(session, playerA, playerB);
        }
    }

    /**
     * Handle offline player during duel
     */
    private void handleOfflinePlayer(DuelSession session, Player playerA, Player playerB) {
        if (playerA != null) {
            playerA.sendMessage(MessageUtils.get("duel.opponent_offline"));
        }
        if (playerB != null) {
            playerB.sendMessage(MessageUtils.get("duel.opponent_offline"));
        }

        cleanupSession(session);
        activeSessions.remove(session.playerA);
        activeSessions.remove(session.playerB);
    }

    /**
     * Handle duel timeout
     */
    private void handleTimeout(DuelSession session, Player playerA, Player playerB) {
        // Determine winner by health
        UUID winner, loser;
        if (playerA.getHealth() > playerB.getHealth()) {
            winner = session.playerA;
            loser = session.playerB;
        } else {
            winner = session.playerB;
            loser = session.playerA;
        }

        finishDuel(session, winner, loser, EndReason.TIMEOUT);
    }

    /**
     * Update boss bar progress and title
     */
    private void updateBossBar(DuelSession session, Player playerA,
                               Player playerB, long elapsed) {
        if (session.bossBar == null) return;

        try {
            // Update progress
            float progress = Math.max(0.0f, Math.min(1.0f,
                    (float) (session.timeoutMs - elapsed) / (float) session.timeoutMs));

            BossBar updated = session.bossBar.progress(progress);

            // Update title with remaining time
            long remainingSec = Math.max(0L, (session.timeoutMs - elapsed) / 1000L);
            Component title = MessageUtils.toComponent(
                    MessageUtils.format("duel.bossbar_title", Map.of(
                            "player1", playerA.getName(),
                            "player2", playerB.getName(),
                            "time", String.valueOf(remainingSec)
                    ))
            );

            session.bossBar = updated.name(title);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to update boss bar", e);
        }
    }

    /**
     * Check if players are within radius
     */
    private void checkRadius(DuelSession session, Player playerA, Player playerB) {
        if (session.origin == null) return;

        try {
            double distanceA = session.origin.distance(playerA.getLocation());
            double distanceB = session.origin.distance(playerB.getLocation());

            if (distanceA > session.radius && distanceB <= session.radius) {
                // Player A escaped
                finishDuel(session, session.playerB, session.playerA, EndReason.ESCAPE);
            } else if (distanceB > session.radius && distanceA <= session.radius) {
                // Player B escaped
                finishDuel(session, session.playerA, session.playerB, EndReason.ESCAPE);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to check duel radius", e);
        }
    }

    /**
     * Cleanup expired duel requests
     */
    private void cleanupExpiredRequests() {
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(entry ->
                now - entry.getValue().timestamp > REQUEST_TIMEOUT_MS
        );
    }

    /**
     * Shutdown the manager
     */
    public void shutdown() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }

        // Cleanup all active sessions
        for (DuelSession session : new HashSet<>(activeSessions.values())) {
            cleanupSession(session);
        }

        activeSessions.clear();
        pendingRequests.clear();

        plugin.getLogger().info("PvPManager shutdown complete");
    }

    // ===== Helper Methods =====

    /**
     * Parse boss bar color from string
     */
    private BossBar.Color parseBossBarColor(String color) {
        try {
            return BossBar.Color.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
        }
    }

    /**
     * Parse boss bar overlay from string
     */
    private BossBar.Overlay parseBossBarOverlay(String overlay) {
        try {
            return BossBar.Overlay.valueOf(overlay.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    // ===== Inner Classes =====

    /**
     * Duel request data
     */
    private static class DuelRequest {
        final UUID requester;
        final UUID target;
        final long timestamp;

        DuelRequest(UUID requester, UUID target, long timestamp) {
            this.requester = requester;
            this.target = target;
            this.timestamp = timestamp;
        }
    }

    /**
     * Active duel session data
     */
    private static class DuelSession {
        final UUID playerA;
        final UUID playerB;
        final long startedAt;
        final Location origin;
        final double radius;
        final long timeoutMs;
        BossBar bossBar;

        DuelSession(UUID playerA, UUID playerB, long startedAt,
                    Location origin, double radius, long timeoutMs) {
            this.playerA = playerA;
            this.playerB = playerB;
            this.startedAt = startedAt;
            this.origin = origin;
            this.radius = radius;
            this.timeoutMs = timeoutMs;
            this.bossBar = null;
        }
    }

    /**
     * Duel configuration
     */
    private static class DuelConfig {
        double radius = 50.0;
        long timeoutSeconds = 120;
        PenaltyType penalty = PenaltyType.KILL;
        boolean bossBarEnabled = true;
        BossBar.Color bossBarColor = BossBar.Color.RED;
        BossBar.Overlay bossBarOverlay = BossBar.Overlay.PROGRESS;
        String startSound = "ENTITY_ENDER_DRAGON_GROWL";
        String endSound = "ENTITY_PLAYER_LEVELUP";
        String errorSound = "ENTITY_VILLAGER_NO";
    }

    /**
     * Penalty types for losing a duel
     */
    private enum PenaltyType {
        NONE,
        DAMAGE,
        KILL;

        static PenaltyType fromString(String s) {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return KILL;
            }
        }
    }

    /**
     * Reasons for duel ending
     */
    private enum EndReason {
        QUIT,
        TIMEOUT,
        ESCAPE,
        DEATH
    }
}