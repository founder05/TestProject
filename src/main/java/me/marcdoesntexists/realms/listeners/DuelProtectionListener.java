package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.managers.PvPManager;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.SoundUtils;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;


public class DuelProtectionListener implements Listener {

    private final Plugin plugin;
    private final PvPManager pvpManager;

    // Configurable blocked commands
    private final Set<String> blockedCommands = new HashSet<>();
    private final Set<String> allowedCommands = new HashSet<>();

    // Configuration
    private boolean blockAllCommands = false;
    private boolean blockEnderPearls = true;
    private boolean blockChorusFruit = true;
    private boolean blockVehicleTeleport = true;
    private boolean playErrorSound = true;
    private String errorSound = "ENTITY_VILLAGER_NO";

    public DuelProtectionListener(Plugin plugin) {
        this.plugin = plugin;
        this.pvpManager = PvPManager.getInstance();
        loadConfiguration();
    }

    /**
     * Load configuration from plugin config
     */
    private void loadConfiguration() {
        try {
            var config = plugin.getConfig();

            // Load blocked commands
            blockedCommands.clear();
            List<String> blockedList = config.getStringList("pvp.blocked-commands");
            if (blockedList.isEmpty()) {
                // Default blocked commands
                blockedCommands.addAll(Arrays.asList(
                        "tp", "tpa", "tpaccept", "tpahere",
                        "teleport", "tele",
                        "warp", "warps",
                        "home", "homes", "sethome",
                        "spawn", "setspawn",
                        "back", "return",
                        "tpmy", "tpmytowm", "tpmykingdom", "tpmyempire",
                        "rtp", "randomtp", "wild",
                        "ec", "enderchest"
                ));
            } else {
                for (String cmd : blockedList) {
                    blockedCommands.add(cmd.toLowerCase().trim());
                }
            }

            // Load allowed commands (bypass)
            allowedCommands.clear();
            List<String> allowedList = config.getStringList("pvp.allowed-commands");
            for (String cmd : allowedList) {
                allowedCommands.add(cmd.toLowerCase().trim());
            }

            // Other settings
            blockAllCommands = config.getBoolean("pvp.block-all-commands", false);
            blockEnderPearls = config.getBoolean("pvp.block-ender-pearls", true);
            blockChorusFruit = config.getBoolean("pvp.block-chorus-fruit", true);
            blockVehicleTeleport = config.getBoolean("pvp.block-vehicle-teleport", true);
            playErrorSound = config.getBoolean("pvp.play-error-sound", true);
            errorSound = config.getString("pvp.error-sound", "ENTITY_VILLAGER_NO");

            plugin.getLogger().info("DuelProtectionListener configured: " +
                    blockedCommands.size() + " blocked commands, " +
                    allowedCommands.size() + " allowed commands");

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to load duel protection configuration", e);
        }
    }

    /**
     * Block commands during duels
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Only check if player is in duel
        if (!pvpManager.isInDuel(player)) {
            return;
        }

        String message = event.getMessage().toLowerCase().trim();

        // Remove leading slash
        if (message.startsWith("/")) {
            message = message.substring(1);
        }

        // Get command name (first word)
        String commandName = message.split(" ")[0].toLowerCase();

        // Check if command is in allowed list (bypass)
        if (allowedCommands.contains(commandName)) {
            return;
        }

        // Block all commands if configured
        if (blockAllCommands) {
            blockCommand(player, event, commandName);
            return;
        }

        // Check if command is in blocked list
        if (isCommandBlocked(commandName)) {
            blockCommand(player, event, commandName);
        }
    }

    /**
     * Check if a command should be blocked
     */
    private boolean isCommandBlocked(String commandName) {
        // Exact match
        if (blockedCommands.contains(commandName)) {
            return true;
        }

        // Check if any blocked command starts with this
        for (String blocked : blockedCommands) {
            if (commandName.startsWith(blocked)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Block a command and notify player
     */
    private void blockCommand(Player player, PlayerCommandPreprocessEvent event, String command) {
        event.setCancelled(true);

        String message = MessageUtils.format("duel.cant_use_command",
                Map.of("command", command));
        player.sendMessage(message);

        if (playErrorSound) {
            SoundUtils.playSound(player, errorSound, 0.5f, 1.0f);
        }

        // Log for debugging
        plugin.getLogger().fine("Blocked command '" + command +
                "' from " + player.getName() + " during duel");
    }

    /**
     * Block all teleportation attempts during duels
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Only check if player is in duel
        if (!pvpManager.isInDuel(player)) {
            return;
        }

        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        // Allow certain teleport causes (like respawn)
        if (isAllowedTeleportCause(cause)) {
            return;
        }


        if (cause == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT && !blockChorusFruit) {
            return;
        }

        // Block the teleport
        event.setCancelled(true);

        String causeName = cause.name().toLowerCase().replace('_', ' ');
        String message = MessageUtils.format("duel.cant_teleport_cause",
                Map.of("cause", causeName));
        player.sendMessage(message);

        if (playErrorSound) {
            SoundUtils.playSound(player, errorSound, 0.5f, 1.0f);
        }

        plugin.getLogger().fine("Blocked teleport (" + cause + ") from " +
                player.getName() + " during duel");
    }

    /**
     * Check if a teleport cause should be allowed during duels
     */
    private boolean isAllowedTeleportCause(PlayerTeleportEvent.TeleportCause cause) {
        return false;
    }

    /**
     * Block ender pearls during duels
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!blockEnderPearls) {
            return;
        }

        // Check if it's an ender pearl
        if (!(event.getEntity() instanceof EnderPearl)) {
            return;
        }

        // Check if shooter is a player in a duel
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        if (!pvpManager.isInDuel(player)) {
            return;
        }

        // Block the ender pearl
        event.setCancelled(true);

        player.sendMessage(MessageUtils.get("duel.cant_use_enderpearl"));

        if (playErrorSound) {
            SoundUtils.playSound(player, errorSound, 0.5f, 1.0f);
        }

        plugin.getLogger().fine("Blocked ender pearl from " +
                player.getName() + " during duel");
    }

    /**
     * Block vehicle teleportation with duel participants
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!blockVehicleTeleport) {
            return;
        }

        Entity entity = event.getEntity();

        // Check if entity is a vehicle with passengers
        if (!(entity instanceof Vehicle vehicle)) {
            return;
        }

        // Check if any passenger is in a duel
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof Player player && pvpManager.isInDuel(player)) {
                event.setCancelled(true);

                player.sendMessage(MessageUtils.get("duel.cant_teleport_vehicle"));

                if (playErrorSound) {
                    SoundUtils.playSound(player, errorSound, 0.5f, 1.0f);
                }

                plugin.getLogger().fine("Blocked vehicle teleport with " +
                        player.getName() + " during duel");
                return;
            }
        }
    }

    /**
     * Handle player death during duel
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!pvpManager.isInDuel(player)) {
            return;
        }

        // Get opponent
        Player opponent = pvpManager.getOpponent(player);

        if (opponent != null) {
            // Notify winner
            opponent.sendMessage(MessageUtils.format("duel.winner",
                    Map.of("opponent", player.getName())));

            // Play victory sound
            SoundUtils.playSound(opponent, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
        }

        // Notify loser
        player.sendMessage(MessageUtils.format("duel.loser",
                Map.of("opponent", opponent != null ? opponent.getName() : "Unknown")));

        // End the duel
        pvpManager.endDuel(player);

        plugin.getLogger().info("Duel ended by death: " + player.getName() +
                " vs " + (opponent != null ? opponent.getName() : "Unknown"));
    }

    /**
     * Handle player quit during duel
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!pvpManager.isInDuel(player)) {
            return;
        }

        // Get opponent before ending duel
        Player opponent = pvpManager.getOpponent(player);

        // Notify opponent
        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(MessageUtils.format("duel.opponent_quit",
                    Map.of("player", player.getName())));

            opponent.sendMessage(MessageUtils.get("duel.winner_by_quit"));

            // Play victory sound
            SoundUtils.playSound(opponent, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
        }

        // Handle quit in PvPManager (applies penalties, etc.)
        pvpManager.handlePlayerQuit(player.getUniqueId());

        plugin.getLogger().info("Player quit during duel: " + player.getName() +
                " vs " + (opponent != null ? opponent.getName() : "Unknown"));
    }

    /**
     * Prevent players from damaging their duel opponent's opponent
     * (ensures only the two duel participants can fight each other)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Only care about player vs player combat
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = null;

        // Direct attack
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // Projectile attack
        else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        if (attacker == null) {
            return;
        }

        // If attacker is in duel but victim is not their opponent, block
        if (pvpManager.isInDuel(attacker)) {
            Player opponent = pvpManager.getOpponent(attacker);

            if (opponent == null || !opponent.getUniqueId().equals(victim.getUniqueId())) {
                event.setCancelled(true);
                attacker.sendMessage(MessageUtils.get("duel.can_only_attack_opponent"));

                if (playErrorSound) {
                    SoundUtils.playSound(attacker, errorSound, 0.5f, 1.0f);
                }
            }
        }

        // If victim is in duel but attacker is not their opponent, block
        if (pvpManager.isInDuel(victim)) {
            Player opponent = pvpManager.getOpponent(victim);

            if (opponent == null || !opponent.getUniqueId().equals(attacker.getUniqueId())) {
                event.setCancelled(true);
                attacker.sendMessage(MessageUtils.get("duel.target_in_duel"));

                if (playErrorSound) {
                    SoundUtils.playSound(attacker, errorSound, 0.5f, 1.0f);
                }
            }
        }
    }

    /**
     * Block respawn teleportation to bed/respawn anchor during duel
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // If player died during duel, they should respawn normally
        // The duel was already ended in onPlayerDeath
        if (!pvpManager.isInDuel(player)) {
            return;
        }

        // This shouldn't happen, but just in case
        plugin.getLogger().warning("Player " + player.getName() +
                " respawned while still in duel - ending duel");
        pvpManager.endDuel(player);
    }

    /**
     * Reload configuration
     */
    public void reload() {
        loadConfiguration();
        plugin.getLogger().info("DuelProtectionListener configuration reloaded");
    }

    /**
     * Get statistics for debugging
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("blocked_commands", blockedCommands.size());
        stats.put("allowed_commands", allowedCommands.size());
        stats.put("block_all_commands", blockAllCommands);
        stats.put("block_ender_pearls", blockEnderPearls);
        stats.put("block_chorus_fruit", blockChorusFruit);
        stats.put("block_vehicle_teleport", blockVehicleTeleport);
        return stats;
    }
}