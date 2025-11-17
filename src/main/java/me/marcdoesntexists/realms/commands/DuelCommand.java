package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.PvPManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.SoundUtils;
import me.marcdoesntexists.realms.utils.TabCompletionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command handler for the /duel system
 *
 * Commands:
 * - /duel <player> - Send a duel request
 * - /duel accept - Accept a pending duel request
 * - /duel deny - Deny a pending duel request
 * - /duel info - View current duel information
 * - /duel forfeit - Forfeit current duel
 * - /duel help - Show help menu
 *
 * @author Realms
 * @version 2.0
 */
public class DuelCommand implements CommandExecutor, TabCompleter {

    private final Realms plugin;
    private final PvPManager pvpManager;

    // Cooldown system (prevent spam)
    private final Map<UUID, Long> requestCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 3000; // 3 seconds

    public DuelCommand(Realms plugin) {
        this.plugin = plugin;
        this.pvpManager = PvPManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Player-only command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }

        // No arguments - show usage
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Route to appropriate handler
        return switch (subCommand) {
            case "accept" -> handleAccept(player);
            case "deny", "decline", "reject" -> handleDeny(player);
            case "info", "status" -> handleInfo(player);
            case "forfeit", "surrender", "quit" -> handleForfeit(player);
            case "help", "?" -> {
                sendHelp(player);
                yield true;
            }
            default -> handleRequest(player, args[0]);
        };
    }

    /**
     * Handle sending a duel request
     */
    private boolean handleRequest(Player sender, String targetName) {
        // Check cooldown
        if (isOnCooldown(sender)) {
            long remaining = getRemainingCooldown(sender);
            sender.sendMessage(MessageUtils.format("commands.cooldown_remaining",
                    Map.of("time", String.valueOf(remaining / 1000))));
            SoundUtils.playSound(sender, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Find target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(MessageUtils.format("commands.not_found",
                    Map.of("entity", targetName)));
            SoundUtils.playSound(sender, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Validate request
        String error = validateDuelRequest(sender, target);
        if (error != null) {
            sender.sendMessage(error);
            SoundUtils.playSound(sender, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Send request
        boolean success = pvpManager.sendRequest(sender, target);
        if (!success) {
            sender.sendMessage(MessageUtils.get("duel.send_failed"));
            SoundUtils.playSound(sender, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Set cooldown
        setCooldown(sender);

        // Success messages are sent by PvPManager
        return true;
    }

    /**
     * Handle accepting a duel request
     */
    private boolean handleAccept(Player player) {
        // Check if player has a pending request
        if (!pvpManager.hasPendingRequest(player)) {
            player.sendMessage(MessageUtils.get("duel.no_request"));
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Get requester
        Optional<UUID> requesterUUID = pvpManager.getRequesterUUID(player);
        if (requesterUUID.isEmpty()) {
            player.sendMessage(MessageUtils.get("duel.no_request"));
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterUUID.get());
        if (requester == null || !requester.isOnline()) {
            player.sendMessage(MessageUtils.get("duel.requester_offline"));
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Validate both players can still duel
        String error = validateDuelAccept(requester, player);
        if (error != null) {
            player.sendMessage(error);
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Accept request - PvPManager handles messages and sounds
        boolean success = pvpManager.acceptRequest(player, player.getLocation());
        if (!success) {
            player.sendMessage(MessageUtils.get("duel.accept_failed"));
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
        }

        return true;
    }

    /**
     * Handle denying a duel request
     */
    private boolean handleDeny(Player player) {
        if (!pvpManager.hasPendingRequest(player)) {
            player.sendMessage(MessageUtils.get("duel.no_request"));
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Get requester to notify them
        Optional<UUID> requesterUUID = pvpManager.getRequesterUUID(player);
        Player requester = requesterUUID.map(Bukkit::getPlayer).orElse(null);

        // Cancel the request
        boolean cancelled = pvpManager.cancelRequest(player);
        if (cancelled) {
            player.sendMessage(MessageUtils.get("duel.denied"));
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);

            if (requester != null && requester.isOnline()) {
                requester.sendMessage(MessageUtils.format("duel.denied_notify",
                        Map.of("player", player.getName())));
                SoundUtils.playSound(requester, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            }
        }

        return true;
    }

    /**
     * Handle viewing duel info
     */
    private boolean handleInfo(Player player) {
        // Check if player is in a duel
        if (!pvpManager.isInDuel(player)) {
            player.sendMessage(MessageUtils.get("duel.not_in_duel"));
            return true;
        }

        // Get opponent
        Player opponent = pvpManager.getOpponent(player);
        if (opponent == null) {
            player.sendMessage(MessageUtils.get("duel.opponent_offline"));
            pvpManager.endDuel(player);
            return true;
        }

        // Get remaining time
        long remainingSeconds = pvpManager.getRemainingSeconds(player.getUniqueId());

        // Send info
        player.sendMessage(MessageUtils.get("duel.info_header"));
        player.sendMessage(MessageUtils.format("duel.info_opponent",
                Map.of("opponent", opponent.getName())));
        player.sendMessage(MessageUtils.format("duel.info_time",
                Map.of("time", formatTime(remainingSeconds))));
        player.sendMessage(MessageUtils.get("duel.info_footer"));

        return true;
    }

    /**
     * Handle forfeiting a duel
     */
    private boolean handleForfeit(Player player) {
        // Check if player is in a duel
        if (!pvpManager.isInDuel(player)) {
            player.sendMessage(MessageUtils.get("duel.not_in_duel"));
            SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            return true;
        }

        // Get opponent
        Player opponent = pvpManager.getOpponent(player);

        // Notify both players
        player.sendMessage(MessageUtils.get("duel.forfeit_self"));
        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(MessageUtils.format("duel.forfeit_opponent",
                    Map.of("player", player.getName())));
            SoundUtils.playSound(opponent, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
        }

        // End duel
        pvpManager.endDuel(player);
        SoundUtils.playSound(player, "ENTITY_VILLAGER_NO", 1.0f, 0.8f);

        return true;
    }

    /**
     * Send help menu
     */
    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("duel.help_header"));
        player.sendMessage(MessageUtils.get("duel.help_request"));
        player.sendMessage(MessageUtils.get("duel.help_accept"));
        player.sendMessage(MessageUtils.get("duel.help_deny"));
        player.sendMessage(MessageUtils.get("duel.help_info"));
        player.sendMessage(MessageUtils.get("duel.help_forfeit"));
        player.sendMessage(MessageUtils.get("duel.help_footer"));
    }

    /**
     * Validate a duel request
     */
    private String validateDuelRequest(Player sender, Player target) {
        // Can't duel yourself
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            return MessageUtils.get("duel.cant_self");
        }

        // Check if sender is already in duel
        if (pvpManager.isInDuel(sender)) {
            return MessageUtils.get("duel.already_in_duel");
        }

        // Check if target is already in duel
        if (pvpManager.isInDuel(target)) {
            return MessageUtils.format("duel.target_in_duel",
                    Map.of("target", target.getName()));
        }

        // Check if target already has a pending request
        if (pvpManager.hasPendingRequest(target)) {
            return MessageUtils.format("duel.target_has_pending",
                    Map.of("target", target.getName()));
        }

        // Check if players are in the same world
        if (!sender.getWorld().equals(target.getWorld())) {
            return MessageUtils.get("duel.different_worlds");
        }

        // Check distance (optional - prevent cross-world duels)
        double distance = sender.getLocation().distance(target.getLocation());
        double maxDistance = plugin.getConfig().getDouble("pvp.max-request-distance", 100.0);
        if (distance > maxDistance) {
            return MessageUtils.format("duel.too_far",
                    Map.of("distance", String.valueOf((int) distance),
                            "max", String.valueOf((int) maxDistance)));
        }

        return null; // Valid
    }

    /**
     * Validate accepting a duel
     */
    private String validateDuelAccept(Player requester, Player accepter) {
        // Check if requester is still valid
        if (pvpManager.isInDuel(requester)) {
            return MessageUtils.format("duel.requester_in_duel",
                    Map.of("player", requester.getName()));
        }

        // Check if accepter is in another duel
        if (pvpManager.isInDuel(accepter)) {
            return MessageUtils.get("duel.already_in_duel");
        }

        // Check if players are in the same world
        if (!requester.getWorld().equals(accepter.getWorld())) {
            return MessageUtils.get("duel.different_worlds");
        }

        return null; // Valid
    }

    /**
     * Check if player is on cooldown
     */
    private boolean isOnCooldown(Player player) {
        Long lastRequest = requestCooldowns.get(player.getUniqueId());
        if (lastRequest == null) return false;

        long elapsed = System.currentTimeMillis() - lastRequest;
        return elapsed < COOLDOWN_MS;
    }

    /**
     * Get remaining cooldown in milliseconds
     */
    private long getRemainingCooldown(Player player) {
        Long lastRequest = requestCooldowns.get(player.getUniqueId());
        if (lastRequest == null) return 0;

        long elapsed = System.currentTimeMillis() - lastRequest;
        return Math.max(0, COOLDOWN_MS - elapsed);
    }

    /**
     * Set cooldown for player
     */
    private void setCooldown(Player player) {
        requestCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Format time in MM:SS format
     */
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    /**
     * Cleanup cooldowns for offline players
     */
    public void cleanupCooldowns() {
        requestCooldowns.entrySet().removeIf(entry ->
                Bukkit.getPlayer(entry.getKey()) == null
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        // First argument - subcommands or player names
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Add subcommands
            List<String> subcommands = Arrays.asList(
                    "accept", "deny", "info", "forfeit", "help"
            );

            completions.addAll(subcommands.stream()
                    .filter(sub -> sub.startsWith(input))
                    .collect(Collectors.toList()));

            // Add online player names (exclude self and players in duels)
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                    .filter(p -> !pvpManager.isInDuel(p))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList()));
        }

        return completions;
    }

    /**
     * Get statistics for debugging
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("active_cooldowns", requestCooldowns.size());
        return stats;
    }
}