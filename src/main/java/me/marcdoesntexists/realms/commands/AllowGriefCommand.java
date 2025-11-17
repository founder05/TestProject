package me.marcdoesntexists.realms.commands;

import me.marcdoesntexists.realms.Realms;
import me.marcdoesntexists.realms.managers.ClaimManager;
import me.marcdoesntexists.realms.utils.Claim;
import me.marcdoesntexists.realms.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AllowGriefCommand implements CommandExecutor, TabCompleter {
    private final Realms plugin;

    public AllowGriefCommand(Realms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }
        Player p = (Player) sender;
        if (args.length < 1) {
            p.sendMessage(MessageUtils.get("commands.usage") + " /allowgrief <player>");
            return true;
        }
        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            p.sendMessage(MessageUtils.get("player.not_online", targetName));
            return true;
        }

        ClaimManager cm = plugin.getClaimManager();
        if (cm == null) {
            p.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.get("claim.claim_manager_unavailable"));
            return true;
        }

        Claim claim = cm.getClaimAt(p.getLocation());
        if (claim == null) {
            p.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.get("claim.no_claim"));
            return true;
        }

        // Check if sender is owner or has FULL permission or admin permission
        String owner = claim.getOwnerUuid();
        boolean isOwner = owner != null && owner.equals(p.getUniqueId().toString());
        boolean hasFullPerm = claim.hasPermission(p.getUniqueId(), Claim.ClaimPermission.FULL);
        if (!isOwner && !hasFullPerm && !p.hasPermission("realms.claims.allowgrief.bypass")) {
            p.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.get("claim.not_owner"));
            return true;
        }

        UUID targetId = target.getUniqueId();
        if (claim.isAllowedToGrief(targetId)) {
            claim.disallowGrief(targetId);
            cm.saveClaimMeta(claim);
            p.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.format("claim.allowgrief.removed", Collections.singletonMap("player", target.getName())));
            target.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.format("claim.allowgrief.removed_notify", Collections.singletonMap("player", p.getName())));
        } else {
            claim.allowGrief(targetId);
            cm.saveClaimMeta(claim);
            p.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.format("claim.allowgrief.added", Collections.singletonMap("player", target.getName())));
            target.sendMessage(MessageUtils.get("general.prefix") + " " + MessageUtils.format("claim.allowgrief.added_notify", Collections.singletonMap("player", p.getName())));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(prefix)) out.add(pl.getName());
            }
            return out;
        }
        return Collections.emptyList();
    }
}

