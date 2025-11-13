package me.marcdoesntexists.nations.managers;

import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PvPManager {
    private static PvPManager instance;
    private final Map<UUID, PvPRequest> pending = new ConcurrentHashMap<>(); // key: target uuid -> request
    private final Map<UUID, PvPSession> sessions = new ConcurrentHashMap<>(); // key: player uuid -> session
    private final long requestTimeoutMs = 30_000L; // 30s
    private Plugin plugin;
    // default duel settings (overridable from config)
    private double defaultRadius = 50.0;
    private long defaultTimeoutMs = 120_000L; // 120s
    private String penalty = "kill"; // options: none, kill
    private boolean bossbarEnabled = true;
    private BarColor bossbarColor = BarColor.RED;
    private BarStyle bossbarStyle = BarStyle.SOLID;
    private Sound startSound = null;
    private Sound endSound = null;
    // Monitor task id
    private int monitorTaskId = -1;

    private PvPManager() {
    }

    public static PvPManager getInstance() {
        if (instance == null) instance = new PvPManager();
        return instance;
    }

    public boolean sendRequest(Player from, Player to) {
        if (from == null || to == null) return false;
        if (from.getUniqueId().equals(to.getUniqueId())) return false;
        if (inDuel(from) || inDuel(to)) return false;

        PvPRequest req = new PvPRequest(from.getUniqueId(), to.getUniqueId(), System.currentTimeMillis());
        pending.put(to.getUniqueId(), req);
        return true;
    }

    public void init(Plugin plugin) {
        this.plugin = plugin;
        // load pvp config
        try {
            var cfg = plugin.getConfig();
            this.defaultRadius = cfg.getDouble("pvp.radius", this.defaultRadius);
            this.defaultTimeoutMs = cfg.getLong("pvp.timeout-seconds", this.defaultTimeoutMs / 1000L) * 1000L;
            this.penalty = cfg.getString("pvp.penalty", this.penalty);
            this.bossbarEnabled = cfg.getBoolean("pvp.bossbar.enabled", true);
            try {
                String color = cfg.getString("pvp.bossbar.color", "RED");
                this.bossbarColor = BarColor.valueOf(color.toUpperCase());
            } catch (Throwable ignored) {
            }
            try {
                String style = cfg.getString("pvp.bossbar.style", "SOLID");
                this.bossbarStyle = BarStyle.valueOf(style.toUpperCase());
            } catch (Throwable ignored) {
            }
            try {
                String sstart = cfg.getString("pvp.sound.start", null);
                if (sstart != null) this.startSound = parseSound(sstart);
            } catch (Throwable ignored) {
            }
            try {
                String send = cfg.getString("pvp.sound.end", null);
                if (send != null) this.endSound = parseSound(send);
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load pvp config: " + t.getMessage());
        }

        startMonitor();
    }

    public boolean hasPendingRequest(Player target) {
        PvPRequest r = pending.get(target.getUniqueId());
        if (r == null) return false;
        if (System.currentTimeMillis() - r.timestamp > requestTimeoutMs) {
            pending.remove(target.getUniqueId());
            return false;
        }
        return true;
    }

    public Optional<UUID> getRequesterUUID(Player target) {
        PvPRequest r = pending.get(target.getUniqueId());
        if (r == null) return Optional.empty();
        if (System.currentTimeMillis() - r.timestamp > requestTimeoutMs) {
            pending.remove(target.getUniqueId());
            return Optional.empty();
        }
        return Optional.of(r.requester);
    }

    public boolean acceptRequest(Player target, Location origin) {
        PvPRequest r = pending.remove(target.getUniqueId());
        if (r == null) return false;
        if (System.currentTimeMillis() - r.timestamp > requestTimeoutMs) return false;

        UUID a = r.requester;
        UUID b = r.target;
        startDuel(a, b, origin != null ? origin : Bukkit.getPlayer(a).getLocation(), defaultRadius, defaultTimeoutMs);

        // play start sound and send start messages
        Player pa = Bukkit.getPlayer(a);
        Player pb = Bukkit.getPlayer(b);
        if (pa != null)
            pa.sendMessage(MessageUtils.format("duel.start", Map.of("opponent", pb != null ? pb.getName() : "Unknown")));
        if (pb != null)
            pb.sendMessage(MessageUtils.format("duel.start", Map.of("opponent", pa != null ? pa.getName() : "Unknown")));
        if (startSound != null) {
            if (pa != null) pa.playSound(pa.getLocation(), startSound, 1f, 1f);
            if (pb != null) pb.playSound(pb.getLocation(), startSound, 1f, 1f);
        }
        return true;
    }

    public boolean cancelRequest(Player target) {
        return pending.remove(target.getUniqueId()) != null;
    }

    public boolean inDuel(Player p) {
        return sessions.containsKey(p.getUniqueId());
    }

    public boolean inDuel(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public Optional<UUID> getOpponentUUID(UUID uuid) {
        PvPSession s = sessions.get(uuid);
        if (s == null) return Optional.empty();
        if (s.playerA.equals(uuid)) return Optional.of(s.playerB);
        return Optional.of(s.playerA);
    }

    public Player getOpponent(Player p) {
        Optional<UUID> opp = getOpponentUUID(p.getUniqueId());
        return opp.map(Bukkit::getPlayer).orElse(null);
    }

    public void endDuel(UUID uuid) {
        PvPSession s = sessions.remove(uuid);
        if (s == null) return;
        // remove other entry and cancel session tracking
        UUID other = s.playerA.equals(uuid) ? s.playerB : s.playerA;
        sessions.remove(other);
    }

    public void endDuel(Player p) {
        if (p != null) endDuel(p.getUniqueId());
    }

    public boolean isDuelAcceptedBetween(UUID a, UUID b) {
        PvPSession s = sessions.get(a);
        if (s == null) return false;
        return (s.playerA.equals(a) && s.playerB.equals(b)) || (s.playerA.equals(b) && s.playerB.equals(a));
    }

    // Return remaining seconds for a player's duel, or 0 if not in duel
    public long getRemainingSeconds(UUID uuid) {
        PvPSession s = sessions.get(uuid);
        if (s == null) return 0L;
        long elapsed = System.currentTimeMillis() - s.startedAt;
        long remaining = s.timeoutMs - elapsed;
        return Math.max(0L, remaining / 1000L);
    }

    // Expose session (package-private) for internal use if needed
    public PvPSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    /**
     * Start a duel session between two players with origin, radius and timeout.
     */
    public void startDuel(UUID a, UUID b, Location origin, double radius, long timeoutMs) {
        PvPSession s = new PvPSession(a, b, System.currentTimeMillis(), true, origin.clone(), radius, timeoutMs);
        // create bossbar if enabled
        if (bossbarEnabled && plugin != null) {
            try {
                BossBar bb = Bukkit.createBossBar(MessageUtils.format("duel.bossbar_title", Map.of("a", Bukkit.getPlayer(a) != null ? Bukkit.getPlayer(a).getName() : "A", "b", Bukkit.getPlayer(b) != null ? Bukkit.getPlayer(b).getName() : "B")), bossbarColor, bossbarStyle);
                s.bossBar = bb;
                Player pa = Bukkit.getPlayer(a);
                Player pb = Bukkit.getPlayer(b);
                if (pa != null) bb.addPlayer(pa);
                if (pb != null) bb.addPlayer(pb);
                // initial progress
                bb.setProgress(1.0);
            } catch (Throwable ignored) {
            }
        }
        sessions.put(a, s);
        sessions.put(b, s);
    }

    private void finishSession(PvPSession s, UUID winnerUuid, UUID loserUuid, String reason) {
        // remove bossbar
        try {
            if (s.bossBar != null) {
                s.bossBar.removeAll();
                s.bossBar = null;
            }
        } catch (Throwable ignored) {
        }

        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = Bukkit.getPlayer(loserUuid);

        if (winner != null)
            winner.sendMessage(MessageUtils.format("duel.winner", Map.of("opponent", loser != null ? loser.getName() : "Unknown")));
        if (loser != null)
            loser.sendMessage(MessageUtils.format("duel.loser", Map.of("opponent", winner != null ? winner.getName() : "Unknown")));

        // apply penalty
        try {
            if ("kill".equalsIgnoreCase(penalty) && loser != null) {
                // force death
                try {
                    loser.setHealth(0.0);
                } catch (Throwable t) {
                    try {
                        loser.damage(1000.0);
                    } catch (Throwable ignored) {
                    }
                }
                if (loser != null) loser.sendMessage(MessageUtils.get("duel.penalty_kill"));
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to apply duel penalty: " + t.getMessage());
        }

        // play end sound
        if (endSound != null) {
            if (winner != null) winner.playSound(winner.getLocation(), endSound, 1f, 1f);
            if (loser != null) loser.playSound(loser.getLocation(), endSound, 1f, 1f);
        }

        // remove session entries
        sessions.remove(s.playerA);
        sessions.remove(s.playerB);
    }

    /**
     * Handle a quitting player: if they are in a duel, award the win to the other and finish the session.
     */
    public void handlePlayerQuit(UUID quitter) {
        PvPSession s = sessions.get(quitter);
        if (s == null) return;
        UUID other = s.playerA.equals(quitter) ? s.playerB : s.playerA;
        finishSession(s, other, quitter, "quit");
    }

    private void startMonitor() {
        if (plugin == null) return;
        if (monitorTaskId != -1) return;
        monitorTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                var iterator = sessions.values().iterator();
                // Use a snapshot of sessions
                Set<PvPSession> handled = new HashSet<>(new HashSet<>(sessions.values()));
                for (PvPSession s : handled) {
                    Player a = Bukkit.getPlayer(s.playerA);
                    Player b = Bukkit.getPlayer(s.playerB);
                    // if one is offline, end duel and notify
                    if (a == null || b == null) {
                        if (a != null) a.sendMessage(MessageUtils.get("duel.opponent_offline"));
                        if (b != null) b.sendMessage(MessageUtils.get("duel.opponent_offline"));
                        sessions.remove(s.playerA);
                        sessions.remove(s.playerB);
                        continue;
                    }
                    // timeout
                    long elapsed = System.currentTimeMillis() - s.startedAt;
                    if (elapsed > s.timeoutMs) {
                        // timeout: pick winner arbitrarily (playerB) or decide by health
                        finishSession(s, s.playerB, s.playerA, "timeout");
                        continue;
                    }
                    // update bossbar progress and title
                    if (s.bossBar != null) {
                        double progress = Math.max(0.0, Math.min(1.0, (double) (s.timeoutMs - elapsed) / (double) s.timeoutMs));
                        try {
                            s.bossBar.setProgress(progress);
                        } catch (Throwable ignored) {
                        }
                        try {
                            long remainingSec = Math.max(0L, (s.timeoutMs - elapsed) / 1000L);
                            s.bossBar.setTitle(MessageUtils.format("duel.bossbar_title", Map.of("a", Bukkit.getPlayer(s.playerA) != null ? Bukkit.getPlayer(s.playerA).getName() : "A", "b", Bukkit.getPlayer(s.playerB) != null ? Bukkit.getPlayer(s.playerB).getName() : "B", "time", String.valueOf(remainingSec))));
                        } catch (Throwable ignored) {
                        }
                    }
                    // check radius: if one is outside origin radius, they lose
                    if (s.origin != null) {
                        double da = s.origin.distance(a.getLocation());
                        double db = s.origin.distance(b.getLocation());
                        if (da > s.radius && db <= s.radius) {
                            // a fled -> b wins
                            finishSession(s, s.playerB, s.playerA, "escape");
                            continue;
                        } else if (db > s.radius && da <= s.radius) {
                            finishSession(s, s.playerA, s.playerB, "escape");
                            continue;
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("PvPManager monitor task error: " + t.getMessage());
            }
        }, 20L, 20L).getTaskId();
    }

    // Internal records

    private Sound parseSound(String s) {
        if (s == null) return null;
        try {
            String norm = s.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace('.', '_').replace(':', '_');
            return Sound.valueOf(norm);
        } catch (Throwable t) {
            try {
                return Sound.valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (Throwable ignored) {
                plugin.getLogger().warning("Unknown sound name for PvP config: '" + s + "'");
                return null;
            }
        }
    }

    private static class PvPRequest {
        final UUID requester;
        final UUID target;
        final long timestamp;

        PvPRequest(UUID requester, UUID target, long timestamp) {
            this.requester = requester;
            this.target = target;
            this.timestamp = timestamp;
        }
    }

    private static class PvPSession {
        final UUID playerA;
        final UUID playerB;
        final long startedAt;
        final boolean accepted;
        final Location origin;
        final double radius;
        final long timeoutMs;
        // transient runtime-only data
        transient BossBar bossBar;

        PvPSession(UUID a, UUID b, long startedAt, boolean accepted, Location origin, double radius, long timeoutMs) {
            this.playerA = a;
            this.playerB = b;
            this.startedAt = startedAt;
            this.accepted = accepted;
            this.origin = origin;
            this.radius = radius;
            this.timeoutMs = timeoutMs;
            this.bossBar = null;
        }
    }
}
