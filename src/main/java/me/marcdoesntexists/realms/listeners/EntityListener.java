package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.managers.PvPManager;
import me.marcdoesntexists.realms.societies.Town;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.MessageUtils;
import me.marcdoesntexists.realms.utils.SoundUtils;
import org.bukkit.Chunk;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;
import java.util.logging.Level;

public class EntityListener implements Listener {

    private final Realms plugin;
    private final ClaimManager claimManager;
    private final SocietiesManager societiesManager;
    private final PvPManager pvpManager;

    // Configuration
    private boolean protectPassiveMobs = true;
    private boolean protectVillagers = true;
    private boolean protectIronGolems = true;
    private boolean blockExplosionsInClaims = true;
    private boolean blockMonsterSpawning = true;
    private boolean allowDuelPvP = true;
    private boolean playProtectionSounds = true;

    // Statistics
    private final Map<String, Integer> protectionStats = new HashMap<>();

    public EntityListener(Realms plugin) {
        this.plugin = plugin;
        this.claimManager = ClaimManager.getInstance(plugin);
        this.societiesManager = plugin.getSocietiesManager();
        this.pvpManager = PvPManager.getInstance();
        loadConfiguration();
        initializeStats();
    }

    /**
     * Load configuration from plugin config
     */
    private void loadConfiguration() {
        try {
            var config = plugin.getConfig();

            protectPassiveMobs = config.getBoolean("protection.passive-mobs", true);
            protectVillagers = config.getBoolean("protection.villagers", true);
            protectIronGolems = config.getBoolean("protection.iron-golems", true);
            blockExplosionsInClaims = config.getBoolean("protection.explosions", true);
            blockMonsterSpawning = config.getBoolean("protection.monster-spawning", true);
            allowDuelPvP = config.getBoolean("protection.allow-duel-pvp", true);
            playProtectionSounds = config.getBoolean("protection.play-sounds", true);

            plugin.getLogger().info("EntityListener configured successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to load entity listener configuration", e);
        }
    }

    /**
     * Initialize statistics tracking
     */
    private void initializeStats() {
        protectionStats.put("pvp_blocked", 0);
        protectionStats.put("mob_damage_blocked", 0);
        protectionStats.put("explosion_blocked", 0);
        protectionStats.put("spawn_blocked", 0);
    }

    /**
     * Main entity damage handler
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        // Handle player damage (PvP)
        if (entity instanceof Player victim) {
            handlePlayerDamage(event, victim);
            return;
        }

        // Handle passive mob protection
        if (isProtectedMob(entity)) {
            handleMobDamage(event, entity);
        }
    }

    /**
     * Handle player vs player damage
     */
    private void handlePlayerDamage(EntityDamageByEntityEvent event, Player victim) {
        Player attacker = getAttacker(event.getDamager());

        if (attacker == null) {
            return;
        }

        // Allow duel PvP if configured
        if (allowDuelPvP && pvpManager.isDuelAcceptedBetween(
                attacker.getUniqueId(), victim.getUniqueId())) {
            return; // Allow damage between duel participants
        }

        // Check claim protection
        Chunk chunk = victim.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim == null) {
            return; // No claim, allow damage (wilderness PvP)
        }

        // Check if PvP is enabled in this claim
        if (!claim.isPvpEnabled()) {
            blockPvP(event, attacker, claim);
            return;
        }

        // Even with PvP enabled, prevent friendly fire
        if (isFriendlyFire(attacker, victim, claim)) {
            blockFriendlyFire(event, attacker);
        }
    }

    /**
     * Block PvP in protected claim
     */
    private void blockPvP(EntityDamageByEntityEvent event, Player attacker, Claim claim) {
        event.setCancelled(true);
        incrementStat("pvp_blocked");

        attacker.sendMessage(MessageUtils.format("entity.pvp_disabled",
                Map.of("town", claim.getTownName())));

        if (playProtectionSounds) {
            SoundUtils.playSound(attacker, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
        }

        plugin.getLogger().fine("Blocked PvP: " + attacker.getName() +
                " in claim " + claim.getTownName());
    }

    /**
     * Block friendly fire between town members
     */
    private void blockFriendlyFire(EntityDamageByEntityEvent event, Player attacker) {
        event.setCancelled(true);
        incrementStat("pvp_blocked");

        attacker.sendMessage(MessageUtils.get("entity.cannot_attack_members"));

        if (playProtectionSounds) {
            SoundUtils.playSound(attacker, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
        }
    }

    /**
     * Check if damage is friendly fire
     */
    private boolean isFriendlyFire(Player attacker, Player victim, Claim claim) {
        Town town = societiesManager.getTown(claim.getTownName());
        if (town == null) {
            return false;
        }

        return town.getMembers().contains(attacker.getUniqueId()) &&
                town.getMembers().contains(victim.getUniqueId());
    }

    /**
     * Handle damage to protected mobs
     */
    private void handleMobDamage(EntityDamageByEntityEvent event, Entity mob) {
        Player attacker = getAttacker(event.getDamager());

        if (attacker == null) {
            return;
        }

        // Bypass for admins
        if (attacker.hasPermission("realms.bypass.mob-protection")) {
            return;
        }

        Chunk chunk = mob.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim == null) {
            return; // No claim, allow damage
        }

        // Check permissions
        if (canInteractWithMob(attacker, claim)) {
            return; // Has permission
        }

        // Block damage
        blockMobDamage(event, attacker, claim, mob);
    }

    /**
     * Block mob damage in protected claim
     */
    private void blockMobDamage(EntityDamageByEntityEvent event, Player attacker,
                                Claim claim, Entity mob) {
        event.setCancelled(true);
        incrementStat("mob_damage_blocked");

        String mobType = getMobTypeName(mob);
        attacker.sendMessage(MessageUtils.format("entity.cannot_damage_mobs",
                Map.of("town", claim.getTownName(), "mob", mobType)));

        if (playProtectionSounds) {
            SoundUtils.playSound(attacker, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
        }
    }

    /**
     * Check if player can interact with mobs in claim
     */
    private boolean canInteractWithMob(Player player, Claim claim) {
        Town town = societiesManager.getTown(claim.getTownName());
        if (town == null) {
            return false;
        }

        // Members can interact
        if (town.getMembers().contains(player.getUniqueId())) {
            return true;
        }

        // Trusted players can interact
        if (town.getMembers().contains(player.getUniqueId())) {
            return true;
        }

        // Check claim-specific permissions
        try {
            if (claim.hasPermission(player.getUniqueId(), Claim.ClaimPermission.FULL)) {
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to check claim permission", e);
        }

        return false;
    }

    /**
     * Get the attacking player from various damage sources
     */
    private Player getAttacker(Entity damager) {
        // Direct player attack
        if (damager instanceof Player player) {
            return player;
        }

        // Projectile attack
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        // TNT placed by player
        if (damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player player) {
                return player;
            }
        }

        // End crystal explosion
        if (damager instanceof EnderCrystal crystal) {
            // Try to get the last player who interacted
            // This is tricky as Bukkit doesn't track this directly
            return null;
        }

        // Wither skull
        if (damager instanceof WitherSkull skull) {
            ProjectileSource shooter = skull.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        // Fireball
        if (damager instanceof Fireball fireball) {
            ProjectileSource shooter = fireball.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    /**
     * Check if entity is a protected mob
     */
    private boolean isProtectedMob(Entity entity) {
        // Passive animals
        if (protectPassiveMobs && entity instanceof Animals) {
            return true;
        }

        // Water mobs (fish, dolphins, etc.)
        if (protectPassiveMobs && entity instanceof WaterMob) {
            return true;
        }

        // Villagers
        if (protectVillagers && entity instanceof Villager) {
            return true;
        }

        // Iron Golems
        if (protectIronGolems && entity instanceof IronGolem) {
            return true;
        }

        // Other passive entities
        if (protectPassiveMobs) {
            return entity instanceof Snowman || entity instanceof Ambient || entity instanceof Allay;
        }

        return false;
    }

    /**
     * Get friendly name for mob type
     */
    private String getMobTypeName(Entity entity) {
        String type = entity.getType().name().toLowerCase().replace('_', ' ');
        return type.substring(0, 1).toUpperCase() + type.substring(1);
    }

    /**
     * Protect players from explosion damage in protected claims
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only handle explosion damage
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION &&
                cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim != null && !claim.isPvpEnabled()) {
            event.setCancelled(true);
            incrementStat("explosion_blocked");

            player.sendMessage(MessageUtils.format("protection.explosion_blocked",
                    Map.of("town", claim.getTownName())));
        }
    }

    /**
     * Prevent explosions from destroying blocks in protected claims
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!blockExplosionsInClaims) {
            return;
        }

        try {
            // Filter out blocks in protected claims
            int originalSize = event.blockList().size();

            event.blockList().removeIf(block -> {
                try {
                    Chunk chunk = block.getLocation().getChunk();
                    Claim claim = claimManager.getClaimAt(chunk);
                    return claim != null && !claim.isExplosionsEnabled();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Error checking explosion protection", e);
                    return false;
                }
            });

            // Track blocked explosions
            int blockedCount = originalSize - event.blockList().size();
            if (blockedCount > 0) {
                incrementStat("explosion_blocked", blockedCount);
            }

            // Cancel if no blocks remain
            if (event.blockList().isEmpty()) {
                event.setCancelled(true);
            }

        } catch (Exception e) {
            // Fallback: check explosion center
            plugin.getLogger().log(Level.SEVERE,
                    "Critical error in explosion handler, using fallback", e);

            Chunk chunk = event.getLocation().getChunk();
            Claim claim = claimManager.getClaimAt(chunk);

            if (claim != null && !claim.isExplosionsEnabled()) {
                event.blockList().clear();
                event.setCancelled(true);
                incrementStat("explosion_blocked");
            }
        }
    }

    /**
     * Control monster spawning in claims
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!blockMonsterSpawning) {
            return;
        }

        // Only check hostile mobs
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // Allow natural spawning outside claims
        Chunk chunk = event.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim == null) {
            return; // No claim, allow spawning
        }

        // Check claim settings
        if (!claim.isMobSpawningEnabled()) {
            event.setCancelled(true);
            incrementStat("spawn_blocked");

            plugin.getLogger().fine("Blocked monster spawn in claim: " +
                    claim.getTownName() + " (" + event.getEntity().getType() + ")");
        }
    }

    /**
     * Protect armor stands in claims
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }

        Player attacker = getAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        // Bypass for admins
        if (attacker.hasPermission("realms.bypass.armorstand-protection")) {
            return;
        }

        Chunk chunk = armorStand.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim == null) {
            return;
        }

        // Check permissions
        if (!canInteractWithMob(attacker, claim)) {
            event.setCancelled(true);
            attacker.sendMessage(MessageUtils.format("entity.cannot_damage_armorstand",
                    Map.of("town", claim.getTownName())));

            if (playProtectionSounds) {
                SoundUtils.playSound(attacker, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            }
        }
    }

    /**
     * Protect item frames in claims
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemFrameDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame itemFrame)) {
            return;
        }

        Player attacker = getAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        // Bypass for admins
        if (attacker.hasPermission("realms.bypass.itemframe-protection")) {
            return;
        }

        Chunk chunk = itemFrame.getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim == null) {
            return;
        }

        // Check permissions
        if (!canInteractWithMob(attacker, claim)) {
            event.setCancelled(true);
            attacker.sendMessage(MessageUtils.format("entity.cannot_damage_itemframe",
                    Map.of("town", claim.getTownName())));

            if (playProtectionSounds) {
                SoundUtils.playSound(attacker, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            }
        }
    }

    /**
     * Prevent entity pickup in claims (item frames, paintings, etc.)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(org.bukkit.event.hanging.HangingBreakByEntityEvent event) {
        Player attacker = getAttacker(event.getRemover());
        if (attacker == null) {
            return;
        }

        // Bypass for admins
        if (attacker.hasPermission("realms.bypass.hanging-protection")) {
            return;
        }

        Chunk chunk = event.getEntity().getLocation().getChunk();
        Claim claim = claimManager.getClaimAt(chunk);

        if (claim == null) {
            return;
        }

        // Check permissions
        if (!canInteractWithMob(attacker, claim)) {
            event.setCancelled(true);

            String entityType = event.getEntity().getType().name()
                    .toLowerCase().replace('_', ' ');
            attacker.sendMessage(MessageUtils.format("entity.cannot_remove_hanging",
                    Map.of("town", claim.getTownName(), "type", entityType)));

            if (playProtectionSounds) {
                SoundUtils.playSound(attacker, "ENTITY_VILLAGER_NO", 0.5f, 1.0f);
            }
        }
    }

    /**
     * Increment a statistic counter
     */
    private void incrementStat(String key) {
        incrementStat(key, 1);
    }

    /**
     * Increment a statistic counter by amount
     */
    private void incrementStat(String key, int amount) {
        protectionStats.merge(key, amount, Integer::sum);
    }

    /**
     * Get statistics
     */
    public Map<String, Integer> getStatistics() {
        return new HashMap<>(protectionStats);
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        protectionStats.replaceAll((k, v) -> 0);
    }

    /**
     * Reload configuration
     */
    public void reload() {
        loadConfiguration();
        plugin.getLogger().info("EntityListener configuration reloaded");
    }
}