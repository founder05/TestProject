package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Religion;
import me.marcdoesntexists.nations.utils.MessageUtils;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ReligionCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;
    private final SocietiesManager societiesManager;

    public ReligionCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.societiesManager = plugin.getSocietiesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.get("commands.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(player, args);
            case "join":
                return handleJoin(player, args);
            case "leave":
                return handleLeave(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("religion.usage_create"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() != null) {
            player.sendMessage(MessageUtils.get("religion.already_in_religion"));
            return true;
        }

        String religionName = args[1];

        if (societiesManager.getReligion(religionName) != null) {
            player.sendMessage(MessageUtils.get("religion.already_exists"));
            return true;
        }

        Religion religion = new Religion(religionName, player.getUniqueId());
        societiesManager.registerReligion(religion);
        data.setReligion(religionName);
        data.setClergyRank("Founder");

        player.sendMessage(MessageUtils.format("religion.created", java.util.Map.of("name", religionName)));
        player.sendMessage(MessageUtils.get("religion.created_note"));

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("religion.usage_join"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() != null) {
            player.sendMessage(MessageUtils.get("religion.must_leave_first"));
            return true;
        }

        String religionName = args[1];
        Religion religion = societiesManager.getReligion(religionName);

        if (religion == null) {
            player.sendMessage(MessageUtils.get("religion.not_found"));
            return true;
        }

        religion.addFollower(player.getUniqueId());
        data.setReligion(religionName);

        player.sendMessage(MessageUtils.format("religion.joined", java.util.Map.of("name", religionName)));

        Player founder = plugin.getServer().getPlayer(religion.getFounder());
        if (founder != null) {
            founder.sendMessage(MessageUtils.format("religion.notify_founder_join", java.util.Map.of("player", player.getName())));
        }

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getReligion() == null) {
            player.sendMessage(MessageUtils.get("religion.not_in_religion"));
            return true;
        }

        Religion religion = societiesManager.getReligion(data.getReligion());

        if (religion != null && religion.getFounder().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.get("religion.cannot_leave_founder"));
            return true;
        }

        String religionName = data.getReligion();
        data.setReligion(null);
        data.setClergyRank(null);

        player.sendMessage(MessageUtils.format("religion.left", java.util.Map.of("name", religionName)));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String religionName;

        if (args.length < 2) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getReligion() == null) {
                player.sendMessage(MessageUtils.get("religion.not_in_religion_usage"));
                return true;
            }
            religionName = data.getReligion();
        } else {
            religionName = args[1];
        }

        Religion religion = societiesManager.getReligion(religionName);
        if (religion == null) {
            player.sendMessage(MessageUtils.get("religion.not_found"));
            return true;
        }

        player.sendMessage(MessageUtils.format("religion.info_header", java.util.Map.of("name", religion.getName())));
        player.sendMessage(MessageUtils.format("religion.info_founder", java.util.Map.of("founder", plugin.getServer().getOfflinePlayer(religion.getFounder()).getName())));
        player.sendMessage(MessageUtils.format("religion.info_followers", java.util.Map.of("count", String.valueOf(religion.getFollowers().size()))));
        player.sendMessage(MessageUtils.format("religion.info_clergy", java.util.Map.of("count", String.valueOf(religion.getClergy().size()))));
        player.sendMessage(MessageUtils.get("religion.info_footer"));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Religion> religions = societiesManager.getAllReligions();

        if (religions.isEmpty()) {
            player.sendMessage(MessageUtils.get("religion.no_religions"));
            return true;
        }

        player.sendMessage(MessageUtils.format("religion.list_header", java.util.Map.of("count", String.valueOf(religions.size()))));

        for (Religion religion : religions) {
            String founderName = plugin.getServer().getOfflinePlayer(religion.getFounder()).getName();
            player.sendMessage(MessageUtils.format("religion.list_item", java.util.Map.of("name", religion.getName(), "founder", founderName, "followers", String.valueOf(religion.getFollowers().size()))));
        }

        player.sendMessage(MessageUtils.get("religion.info_footer"));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("religion.help_header"));
        player.sendMessage(MessageUtils.get("religion.help_create"));
        player.sendMessage(MessageUtils.get("religion.help_join"));
        player.sendMessage(MessageUtils.get("religion.help_leave"));
        player.sendMessage(MessageUtils.get("religion.help_info"));
        player.sendMessage(MessageUtils.get("religion.help_list"));
        player.sendMessage(MessageUtils.get("religion.help_footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "join", "leave", "info", "list")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("info")) {
                return societiesManager.getAllReligions().stream()
                        .map(Religion::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
