package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.ChatManager;
import me.marcdoesntexists.nations.managers.ChatManager.Channel;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.utils.PlayerData;
import me.marcdoesntexists.nations.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChannelCommand implements CommandExecutor, TabCompleter {
    private final Nations plugin;
    private final ChatManager chatManager = ChatManager.getInstance();

    public ChannelCommand(Nations plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(MessageUtils.format("channel.current", Map.of("channel", chatManager.getChannel(player.getUniqueId()).name())));
            return true;
        }

        if (args[0].equalsIgnoreCase("spy")) {
            if (!player.hasPermission("nations.chat.spy")) {
                player.sendMessage(MessageUtils.get("commands.no_permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(MessageUtils.get("channel.usage"));
                return true;
            }
            if (args[1].equalsIgnoreCase("on")) {
                chatManager.addSpy(player.getUniqueId());
                player.sendMessage(MessageUtils.get("channel.spy_enabled"));
            } else {
                chatManager.removeSpy(player.getUniqueId());
                player.sendMessage(MessageUtils.get("channel.spy_disabled"));
            }
            return true;
        }

        String candidate = args[0].toUpperCase(Locale.ROOT);
        try {
            Channel ch = Channel.valueOf(candidate);
            chatManager.setChannel(player.getUniqueId(), ch);
            // Persist to player's data
            PlayerData pd = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (pd != null) {
                pd.setChatChannel(ch.name());
                DataManager.getInstance().savePlayerData(player.getUniqueId());
            }
            player.sendMessage(MessageUtils.format("channel.current", Map.of("channel", ch.name())));
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cUnknown channel. Available: GLOBAL, TOWN, KINGDOM, EMPIRE, RELIGION, ALLIANCE");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("GLOBAL","TOWN","KINGDOM","EMPIRE","RELIGION","ALLIANCE","spy"));
            String pref = args[0].toUpperCase(Locale.ROOT);
            suggestions.removeIf(s -> !s.startsWith(pref));
            return suggestions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spy")) {
            return Arrays.asList("on","off");
        }
        return List.of();
    }
}
