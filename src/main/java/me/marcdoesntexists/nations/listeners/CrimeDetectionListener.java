package me.marcdoesntexists.nations.listeners;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.CrimeType;
import me.marcdoesntexists.nations.events.CrimeCommittedEvent;
import me.marcdoesntexists.nations.law.JusticeService;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.PvPManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Kingdom;
import me.marcdoesntexists.nations.societies.Town;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CrimeDetectionListener implements Listener {
    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;
    private final JusticeService justiceService;
    // cooldown tracking: stores timestamp (millis) of last recorded crime per player
    private final Map<java.util.UUID, Long> lastCrimeTimestamp = new ConcurrentHashMap<>();
    // Config-driven settings
    private double assaultThreshold = 4.0;
    private Set<Material> valuableMaterials = Set.of(Material.DIAMOND_BLOCK);
    private long cooldownMs = 3000L;

    public CrimeDetectionListener(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
        this.justiceService = plugin.getJusticeService();
        loadConfigSettings();
    }

    private void loadConfigSettings() {
        try {
            var cfg = plugin.getConfig();
            this.assaultThreshold = cfg.getDouble("crime.assault-threshold-damage", 4.0);
            this.cooldownMs = cfg.getLong("crime.cooldown-ms", 3000L);
            var list = cfg.getStringList("crime.valuable-blocks");
            this.valuableMaterials = list.stream().map(String::toUpperCase)
                    .map(s -> {
                        try {
                            return Material.valueOf(s);
                        } catch (Throwable t) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load crime config, using defaults: " + t.getMessage());
        }
    }

    // ========== THEFT DETECTION ==========
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerAccess(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Check if accessing a villager or container entity
        if (!(entity instanceof InventoryHolder)) return;

        Location location = entity.getLocation();
        String locationTownName = plugin.getHybridClaimManager().getTownAtLocation(location);
        if (locationTownName == null) return; // Wilderness

        CrimeContext context = analyzeCrimeContext(player, locationTownName);

        // If enemy territory, we do not prosecute
        if (context.isEnemy()) return;

        // Fire event; allow other plugins/listeners to cancel
        CrimeCommittedEvent evt = new CrimeCommittedEvent(player, player.getUniqueId(), CrimeType.THEFT, locationTownName, location,
                "Tentativo di accesso non autorizzato a contenitore",
                context.shouldProsecute(), context.sameKingdom(), context.allied(), false);
        plugin.getServer().getPluginManager().callEvent(evt);
        if (evt.isCancelled()) return;

        // Record the crime
        recordCrime(player, CrimeType.THEFT, locationTownName, location,
                "Tentativo di accesso non autorizzato a contenitore");
    }

    // ========== MURDER DETECTION ==========
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (killer.equals(victim)) return; // Suicide

        // If killer and victim are in an accepted duel, do not treat as crime
        PvPManager pvp = PvPManager.getInstance();
        if (pvp.isDuelAcceptedBetween(killer.getUniqueId(), victim.getUniqueId())) return;

        Location location = victim.getLocation();
        String locationTownName = plugin.getHybridClaimManager().getTownAtLocation(location);
        if (locationTownName == null) return; // Wilderness

        CrimeContext context = analyzeCrimeContext(killer, locationTownName);
        if (context.isEnemy()) return; // do not prosecute in enemy territory

        CrimeCommittedEvent evt = new CrimeCommittedEvent(killer, killer.getUniqueId(), CrimeType.MURDER, locationTownName, location,
                "Omicidio di " + victim.getName(),
                context.shouldProsecute(), context.sameKingdom(), context.allied(), false);
        plugin.getServer().getPluginManager().callEvent(evt);
        if (evt.isCancelled()) return;

        recordCrime(killer, CrimeType.MURDER, locationTownName, location,
                "Omicidio di " + victim.getName());
    }

    // ========== ASSAULT DETECTION ==========
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (victim.equals(attacker)) return;

        Location location = victim.getLocation();
        String locationTownName = plugin.getHybridClaimManager().getTownAtLocation(location);
        if (locationTownName == null) return; // Wilderness

        CrimeContext context = analyzeCrimeContext(attacker, locationTownName);
        if (context.isEnemy()) return;

        // If attacker and victim are in an accepted duel, do not register assault as crime
        PvPManager pvp = PvPManager.getInstance();
        if (pvp.isDuelAcceptedBetween(attacker.getUniqueId(), victim.getUniqueId())) return;

        if (event.getFinalDamage() >= assaultThreshold) {
            CrimeCommittedEvent evt = new CrimeCommittedEvent(attacker, attacker.getUniqueId(), CrimeType.ASSAULT, locationTownName, location,
                    "Aggressione a " + victim.getName(),
                    context.shouldProsecute(), context.sameKingdom(), context.allied(), false);
            plugin.getServer().getPluginManager().callEvent(evt);
            if (evt.isCancelled()) return;

            if (isOnCooldown(attacker.getUniqueId())) return;
            recordCrime(attacker, CrimeType.ASSAULT, locationTownName, location,
                    "Aggressione a " + victim.getName());
            touchCooldown(attacker.getUniqueId());
        }
    }

    // ========== PROPERTY DAMAGE DETECTION ==========
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        String locationTownName = plugin.getHybridClaimManager().getTownAtLocation(location);
        if (locationTownName == null) return;

        CrimeContext context = analyzeCrimeContext(player, locationTownName);
        if (context.isEnemy()) return;

        Material type = event.getBlock().getType();
        if (valuableMaterials.contains(type)) {
            CrimeCommittedEvent evt = new CrimeCommittedEvent(player, player.getUniqueId(), CrimeType.PROPERTY_DAMAGE, locationTownName, location,
                    "Distruzione di proprietà: " + type.name(),
                    context.shouldProsecute(), context.sameKingdom(), context.allied(), false);
            plugin.getServer().getPluginManager().callEvent(evt);
            if (evt.isCancelled()) return;

            if (isOnCooldown(player.getUniqueId())) return;
            recordCrime(player, CrimeType.PROPERTY_DAMAGE, locationTownName, location,
                    "Distruzione di proprietà: " + type.name());
            touchCooldown(player.getUniqueId());
        }
    }

    // ========== ARSON DETECTION ==========
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireStart(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        Location location = event.getBlock().getLocation();
        String locationTownName = plugin.getHybridClaimManager().getTownAtLocation(location);
        if (locationTownName == null) return;

        CrimeContext context = analyzeCrimeContext(player, locationTownName);
        if (context.isEnemy()) return;

        CrimeCommittedEvent evt = new CrimeCommittedEvent(player, player.getUniqueId(), CrimeType.ARSON, locationTownName, location,
                "Incendio doloso",
                context.shouldProsecute(), context.sameKingdom(), context.allied(), false);
        plugin.getServer().getPluginManager().callEvent(evt);
        if (evt.isCancelled()) return;

        recordCrime(player, CrimeType.ARSON, locationTownName, location,
                "Incendio doloso");
    }

    // ========== TRESPASSING DETECTION ==========
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        Location location = event.getTo();
        String locationTownName = plugin.getHybridClaimManager().getTownAtLocation(location);
        if (locationTownName == null) return;

        CrimeContext context = analyzeCrimeContext(player, locationTownName);
        Town locationTown = societiesManager.getTown(locationTownName);
        if (locationTown != null && context.isEnemy()) return; // do not prosecute in enemy

        // record trespass only when player is not member and not in enemy territory
        if (locationTown != null && !locationTown.getMembers().contains(player.getUniqueId())) {
            CrimeCommittedEvent evt = new CrimeCommittedEvent(player, player.getUniqueId(), CrimeType.TRESPASSING, locationTownName, location,
                    "Ingresso non autorizzato in territorio",
                    context.shouldProsecute(), context.sameKingdom(), context.allied(), false);
            plugin.getServer().getPluginManager().callEvent(evt);
            if (evt.isCancelled()) return;

            if (isOnCooldown(player.getUniqueId())) return;
            recordCrime(player, CrimeType.TRESPASSING, locationTownName, location,
                    "Ingresso non autorizzato in territorio");
            touchCooldown(player.getUniqueId());
        }
    }

    // ========== CRIME CONTEXT ANALYSIS ==========
    private CrimeContext analyzeCrimeContext(Player player, String locationTownName) {
        PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
        String playerTownName = playerData == null ? null : playerData.getTown();

        Town locationTown = societiesManager.getTown(locationTownName);
        if (locationTown == null) return new CrimeContext(false, false, false, false);

        // Player in same town -> still treated as crime (owner may prosecute), but we let the event fire; set shouldProsecute to true
        if (locationTownName.equals(playerTownName)) return new CrimeContext(true, false, false, false);

        // Player has no town -> prosecute
        if (playerTownName == null) return new CrimeContext(true, false, false, false);

        Town playerTown = societiesManager.getTown(playerTownName);
        if (playerTown == null) return new CrimeContext(true, false, false, false);

        String locationKingdomName = locationTown.getKingdom();
        String playerKingdomName = playerTown.getKingdom();

        // Default: prosecute unless enemy
        boolean enemy = false;
        boolean allied = false;
        boolean sameKingdom = locationKingdomName != null && locationKingdomName.equals(playerKingdomName);

        if (locationKingdomName != null && playerKingdomName != null) {
            Kingdom locationKingdom = societiesManager.getKingdom(locationKingdomName);
            Kingdom playerKingdom = societiesManager.getKingdom(playerKingdomName);
            if (locationKingdom != null && playerKingdom != null) {
                if (locationKingdom.getEnemies().contains(playerKingdomName) || playerKingdom.getEnemies().contains(locationKingdomName)) {
                    enemy = true;
                }
                if (areInSameAlliance(locationKingdom, playerKingdom)) allied = true;
                if (areInFeudalRelationship(locationKingdom, playerKingdom)) {
                    // feudal relationships considered allied for prosecution purposes
                    allied = true;
                }
            }
        }

        // shouldProsecute = not enemy
        return new CrimeContext(!enemy, sameKingdom, allied, enemy);
    }

    private boolean areInSameAlliance(Kingdom k1, Kingdom k2) {
        if (k1 == null || k2 == null) return false;
        Set<String> k1Alliances = k1.getAlliances();
        Set<String> k2Alliances = k2.getAlliances();
        for (String allianceName : k1Alliances) if (k2Alliances.contains(allianceName)) return true;
        return false;
    }

    private boolean areInFeudalRelationship(Kingdom k1, Kingdom k2) {
        if (k1 == null || k2 == null) return false;
        if (k1.getSuzerain() != null && k1.getSuzerain().equals(k2.getName())) return true;
        if (k2.getSuzerain() != null && k2.getSuzerain().equals(k1.getName())) return true;
        if (k1.getSuzerain() != null && k1.getSuzerain().equals(k2.getSuzerain())) return true;
        return k1.getVassals().contains(k2.getName()) || k2.getVassals().contains(k1.getName());
    }

    // ========== CRIME RECORDING (via JusticeService) ==========
    private void recordCrime(Player criminal, CrimeType crimeType, String townName, Location location, String evidence) {
        String locationStr = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        boolean recorded = justiceService.recordCrime(criminal.getUniqueId(), crimeType, townName, locationStr);
        if (!recorded) return;

        // Notify criminal
        criminal.sendMessage(MessageUtils.get("justice.prefix") + " " + MessageUtils.get("crime.registered"));
        criminal.sendMessage(MessageUtils.format("crime.type", java.util.Map.of("type", crimeType.getDisplayName())));
        criminal.sendMessage(MessageUtils.format("crime.location", java.util.Map.of("town", townName)));
        criminal.sendMessage(MessageUtils.format("crime.fine", java.util.Map.of("fine", String.valueOf(crimeType.getBaseFine()))));

        // Notify mayor
        Town town = societiesManager.getTown(townName);
        if (town != null) {
            Player mayor = plugin.getServer().getPlayer(town.getMayor());
            if (mayor != null) {
                mayor.sendMessage(MessageUtils.get("justice.prefix") + " " + MessageUtils.get("justice.detected"));
                mayor.sendMessage(MessageUtils.format("justice.mayor_criminal", java.util.Map.of("criminal", criminal.getName())));
                mayor.sendMessage(MessageUtils.format("justice.mayor_type", java.util.Map.of("type", crimeType.getDisplayName())));
                if (evidence != null && !evidence.isEmpty()) {
                    mayor.sendMessage(MessageUtils.format("justice.mayor_evidence", java.util.Map.of("evidence", evidence)));
                }
                mayor.sendMessage(MessageUtils.get("justice.mayor_hint"));
            }
        }

        plugin.getLogger().info("[Crime] " + criminal.getName() + " committed " + crimeType.name() + " in " + townName);
    }

    private boolean isOnCooldown(java.util.UUID uuid) {
        Long last = lastCrimeTimestamp.get(uuid);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < cooldownMs;
    }

    private void touchCooldown(java.util.UUID uuid) {
        lastCrimeTimestamp.put(uuid, System.currentTimeMillis());
    }

    // ========== CRIME CONTEXT RECORD ==========
    public static record CrimeContext(boolean shouldProsecute, boolean sameKingdom, boolean allied, boolean enemy) {
        public boolean isEnemy() {
            return enemy;
        }

        public boolean shouldProsecute() {
            return shouldProsecute;
        }

        public boolean sameKingdom() {
            return sameKingdom;
        }

        public boolean allied() {
            return allied;
        }
    }
}
