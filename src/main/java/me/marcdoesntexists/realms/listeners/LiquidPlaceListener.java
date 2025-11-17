// java
package me.marcdoesntexists.realms.listeners;

import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.managers.DataManager;
import me.marcdoesntexists.realms.managers.SocietiesManager;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.Claim.ClaimPermission;
import me.marcdoesntexists.realms.utils.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class LiquidPlaceListener implements Listener {
    private final ClaimManager claimService;

    public LiquidPlaceListener(ClaimManager claimService) {
        this.claimService = claimService;
    }

    // Returns true when the material is NOT a liquid (or liquid-bucket)
    private boolean isNotLiquidMaterial(Material m) {
        if (m == null) return true;
        String name = m.name();
        // Matches both BUCKET types and block types like WATER, LAVA
        return !(name.contains("WATER") || name.contains("LAVA"));
    }

    // Returns true when the item is NOT a liquid item
    private boolean isNotLiquidItem(ItemStack item) {
        if (item == null) return true;
        Material t = item.getType();
        String name = t.name();
        // Catch WATER_BUCKET, LAVA_BUCKET, WATER, LAVA
        return !(name.contains("WATER") || name.contains("LAVA"));
    }

    /**
     * Returns true if the player is NOT allowed to place liquids in this chunk.
     * Uses chunk-based ClaimManager checks for performance and adds an admin bypass permission.
     */
    private boolean cannotPlaceInChunk(Player player, Chunk chunk) {
        if (chunk == null) return false;
        // If chunk isn't claimed, allow
        if (!claimService.isClaimed(chunk)) return false;

        // Admin bypass permission
        if (player != null) {
            try {
                if (player.hasPermission("realms.claims.bypass") || player.hasPermission("realms.admin.bypass")) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }

        // Fetch claim object for permission checks
        Claim claim = claimService.getClaimAt(chunk);
        if (claim == null) {
            // claimed according to isClaimed but no Claim object -> be conservative (deny)
            return true;
        }

        // Allow town members of the owning town
        if (player != null) {
            try {
                // If the player is trusted at any level (town/kingdom/empire) for this claim, allow
                if (isTrustedCascade(player, claim)) return false;

                // Live check: allow if player is member or trusted of the claim's town
                var claimTown = SocietiesManager.getInstance().getTown(claim.getTownName());
                if (claimTown != null) {
                    if (claimTown.getMembers().contains(player.getUniqueId()) || claimTown.isTrusted(player.getUniqueId())) {
                        return false;
                    }
                }

                // otherwise, if player belongs to claim's town according to PlayerData, deny
                // fallback: check PlayerData only as last resort (deny if indicates same town but not actually trusted)
                PlayerData pd = DataManager.getInstance().getPlayerData(player.getUniqueId());
                if (pd != null && pd.getTown() != null && pd.getTown().equals(claim.getTownName())) return true;
            } catch (Exception ignored) {
            }

            // Allow players who have BUILD or FULL permission on this claim
            try {
                ClaimPermission perm = claim.getPermission(player.getUniqueId().toString());
                if (perm == ClaimPermission.BUILD || perm == ClaimPermission.FULL) return false;
            } catch (Exception ignored) {
            }
        }

        return true;
    }

    private boolean isTrustedCascade(Player player, Claim claim) {
        if (player == null || claim == null) return false;
        try {
            UUID pid = player.getUniqueId();
            PlayerData pd = DataManager.getInstance().getPlayerData(pid);
            // If player is mayor of their town, treat as trusted
            if (pd != null && pd.getTown() != null) {
                var playerTown = SocietiesManager.getInstance().getTown(pd.getTown());
                if (playerTown != null && playerTown.isMayor(pid)) return true;
                if (playerTown != null && playerTown.isTrusted(pid)) return true;
            }

            // Claim owner town
            String claimTownName = claim.getTownName();
            var claimTown = SocietiesManager.getInstance().getTown(claimTownName);
            if (claimTown != null) {
                // If player is mayor of claim town allow
                if (claimTown.isMayor(pid)) return true;

                // Check kingdom of claim town
                String kName = claimTown.getKingdom();
                if (kName != null) {
                    var kingdom = SocietiesManager.getInstance().getKingdom(kName);
                    if (kingdom != null && kingdom.isTrusted(pid)) return true;

                    // Check empire via kingdom
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

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Material bucket = event.getBucket();
        if (isNotLiquidMaterial(bucket)) return;
        Location placeLoc = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        Player p = event.getPlayer();
        Chunk chunk = placeLoc.getChunk();

        try {
            if (cannotPlaceInChunk(p, chunk)) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {
            // If ClaimManager fails for some reason, be conservative and cancel
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material placed = event.getBlock().getType();
        if (isNotLiquidMaterial(placed)) return;
        Location loc = event.getBlock().getLocation();
        Player p = event.getPlayer();
        Chunk chunk = loc.getChunk();

        try {
            if (cannotPlaceInChunk(p, chunk)) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (isNotLiquidItem(item)) return;

        Location targetLoc;
        // Try to compute the block in front of the dispenser using BlockData Directional
        if (event.getBlock().getBlockData() instanceof Directional directional) {
            targetLoc = event.getBlock().getRelative(directional.getFacing()).getLocation();
        } else {
            // Fallback: use block above
            targetLoc = event.getBlock().getRelative(BlockFace.UP).getLocation();
        }

        try {
            Chunk chunk = targetLoc.getChunk();
            if (claimService.isClaimed(chunk)) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        Material type = event.getBlock().getType();
        String name = type.name();
        if (!name.contains("WATER") && !name.contains("LAVA")) return;

        Chunk fromChunk = event.getBlock().getChunk();
        Chunk toChunk = event.getToBlock().getChunk();

        try {
            // If destination chunk is not claimed, allow flow
            if (!claimService.isClaimed(toChunk)) return;

            // If source and destination are same chunk, allow
            if (fromChunk.getWorld().getName().equals(toChunk.getWorld().getName())
                    && fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) return;

            // If source chunk is claimed by the same town as destination, allow
            if (claimService.isClaimed(fromChunk)) {
                Claim fromClaim = claimService.getClaimAt(fromChunk);
                Claim toClaim = claimService.getClaimAt(toChunk);
                if (fromClaim != null && toClaim != null && fromClaim.getTownName().equals(toClaim.getTownName())) {
                    return;
                }
            }

            // Otherwise cancel the flow into the claimed chunk
            event.setCancelled(true);
        } catch (Exception ignored) {
            // Be conservative and cancel on errors
            event.setCancelled(true);
        }
    }

}
