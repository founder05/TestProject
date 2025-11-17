package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.Claim.ClaimPermission;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

public class FireLightningListener implements Listener {
    private final ClaimManager claimManager;

    public FireLightningListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    private boolean hasFireBypass(Player player) {
        if (player == null) return false;
        try {
            return player.hasPermission("realms.claims.bypass") || player.hasPermission("realms.admin.bypass");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasClaimBuildPermission(Player player, Claim claim) {
        if (player == null || claim == null) return false;
        try {
            ClaimPermission perm = claim.getPermission(player.getUniqueId().toString());
            return perm == ClaimPermission.BUILD || perm == ClaimPermission.FULL;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * If a block is about to ignite, prevent it when the destination chunk is claim-protected
     * and fire spread is disabled, unless the igniter is allowed.
     */
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        Chunk chunk = loc.getChunk();

        try {
            if (!claimManager.isClaimed(chunk)) return; // not claimed -> allow

            Claim claim = claimManager.getClaimAt(chunk);
            if (claim == null) return; // sanity: allow

            // If fire is allowed in this claim, don't interfere
            if (claim.isFireSpreadEnabled()) return;

            // If player cause exists and has bypass/permission or is trusted in town, allow
            Player player = event.getPlayer();
            if (player != null) {
                if (hasFireBypass(player) || isTrustedCascade(player, claim) || hasClaimBuildPermission(player, claim)) return;
            }

            // Otherwise cancel ignite
            event.setCancelled(true);
        } catch (Exception ignored) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent fire spreading across chunk borders into protected claims when fireSpread is disabled.
     */
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        Block to = event.getBlock();
        Block from = event.getSource();

        // Only care about fire/liquid spreading
        String name = from.getType().name();
        if (!(name.contains("FIRE") || name.contains("LAVA") || name.contains("WATER"))) return;

        Chunk toChunk = to.getChunk();
        Chunk fromChunk = from.getChunk();

        try {
            // If destination not claimed -> allow
            if (!claimManager.isClaimed(toChunk)) return;

            // If same chunk -> allow
            if (toChunk.getWorld().getName().equals(fromChunk.getWorld().getName())
                    && toChunk.getX() == fromChunk.getX() && toChunk.getZ() == fromChunk.getZ()) return;

            Claim toClaim = claimManager.getClaimAt(toChunk);
            Claim fromClaim = claimManager.getClaimAt(fromChunk);

            if (toClaim == null) return; // sanity

            // If fire allowed in destination claim -> allow
            if (toClaim.isFireSpreadEnabled()) return;

            // Allow spread if both chunks belong to same town claim
            if (fromClaim != null && fromClaim.getTownName().equals(toClaim.getTownName())) return;

            // It's not possible to reliably identify a player responsible for natural block spread events here,
            // so we only rely on chunk/claim ownership and same-town checks above. If a player action caused it
            // (bucket, dispenser) other listeners already handle those cases.
            event.setCancelled(true);
        } catch (Exception ignored) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent lightning creating fire in protected claims (cancel strike when destination in protected claim).
     */
    @EventHandler
    public void onLightningStrike(LightningStrikeEvent event) {
        Location loc = event.getLightning().getLocation();
        Chunk chunk = loc.getChunk();
        try {
            if (!claimManager.isClaimed(chunk)) return;

            Claim claim = claimManager.getClaimAt(chunk);
            if (claim == null) return;

            if (claim.isFireSpreadEnabled()) return;

            // Cancel the lightning event so it won't start fires
            event.setCancelled(true);
        } catch (Exception ignored) {
            event.setCancelled(true);
        }
    }

    private boolean isTrustedCascade(Player player, Claim claim) {
        if (player == null || claim == null) return false;
        try {
            java.util.UUID pid = player.getUniqueId();
            PlayerData pd = DataManager.getInstance().getPlayerData(pid);
            // PlayerTown mayor/trusted
            if (pd != null && pd.getTown() != null) {
                var pTown = SocietiesManager.getInstance().getTown(pd.getTown());
                if (pTown != null && pTown.isMayor(pid)) return true;
                if (pTown != null && pTown.isTrusted(pid)) return true;
            }

            // Claim town
            var claimTown = SocietiesManager.getInstance().getTown(claim.getTownName());
            if (claimTown != null) {
                if (claimTown.isMayor(pid)) return true;
                if (claimTown.isTrusted(pid)) return true;
                String kName = claimTown.getKingdom();
                if (kName != null) {
                    var kingdom = SocietiesManager.getInstance().getKingdom(kName);
                    if (kingdom != null && kingdom.isTrusted(pid)) return true;
                    String eName = kingdom != null ? kingdom.getEmpire() : null;
                    if (eName != null) {
                        var empire = SocietiesManager.getInstance().getEmpire(eName);
                        if (empire != null && empire.isTrusted(pid)) return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
