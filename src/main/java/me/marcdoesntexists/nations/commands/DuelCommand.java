package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.PvPManager;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private final PvPManager manager;

    public DuelCommand(Nations plugin) {
        // plugin instance not required in this command currently
        this.manager = PvPManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0) {
            p.sendMessage(MessageUtils.get("duel.usage"));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("accept")) {
            if (!manager.hasPendingRequest(p)) {
                p.sendMessage(MessageUtils.get("duel.no_request"));
                return true;
            }
            var opt = manager.getRequesterUUID(p);
            if (opt.isEmpty()) {
                p.sendMessage(MessageUtils.get("duel.no_request"));
                return true;
            }
            var requester = Bukkit.getPlayer(opt.get());
            if (requester == null) {
                p.sendMessage(MessageUtils.get("duel.requester_offline"));
                return true;
            }
            manager.acceptRequest(p, p.getLocation());
            p.sendMessage(MessageUtils.format("duel.accepted", java.util.Map.of("player", requester.getName())));
            requester.sendMessage(MessageUtils.format("duel.accepted_notify", java.util.Map.of("player", p.getName())));
            return true;
        }

        // send request to player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            p.sendMessage(MessageUtils.get("commands.not_found"));
            return true;
        }
        if (target.getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage(MessageUtils.get("duel.cant_self"));
            return true;
        }
        if (manager.inDuel(p) || manager.inDuel(target)) {
            p.sendMessage(MessageUtils.get("duel.already_in_duel"));
            return true;
        }

        boolean ok = manager.sendRequest(p, target);
        if (!ok) {
            p.sendMessage(MessageUtils.get("duel.send_failed"));
            return true;
        }
        p.sendMessage(MessageUtils.format("duel.sent", java.util.Map.of("player", target.getName())));
        target.sendMessage(MessageUtils.format("duel.request_received", java.util.Map.of("player", p.getName())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.onlinePlayers(args[0]);
        }
        return new ArrayList<>();
    }
}
