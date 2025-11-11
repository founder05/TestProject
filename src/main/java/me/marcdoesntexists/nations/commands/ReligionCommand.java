package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.managers.SocietiesManager;
import me.marcdoesntexists.nations.societies.Religion;
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
            sender.sendMessage("§cThis command can only be used by players!");
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
            player.sendMessage("§cUsage: /religion create <name>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() != null) {
            player.sendMessage("§cYou are already in a religion!");
            return true;
        }

        String religionName = args[1];

        if (societiesManager.getReligion(religionName) != null) {
            player.sendMessage("§cA religion with this name already exists!");
            return true;
        }

        Religion religion = new Religion(religionName, player.getUniqueId());
        societiesManager.registerReligion(religion);
        data.setReligion(religionName);
        data.setClergyRank("Founder");

        player.sendMessage("§a✔ Religion §6" + religionName + "§a created successfully!");
        player.sendMessage("§7You are now the founder of this religion.");

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /religion join <name>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getReligion() != null) {
            player.sendMessage("§cYou are already in a religion! Leave it first.");
            return true;
        }

        String religionName = args[1];
        Religion religion = societiesManager.getReligion(religionName);

        if (religion == null) {
            player.sendMessage("§cReligion not found!");
            return true;
        }

        religion.addFollower(player.getUniqueId());
        data.setReligion(religionName);

        player.sendMessage("§a✔ You have joined §6" + religionName + "§a!");

        Player founder = plugin.getServer().getPlayer(religion.getFounder());
        if (founder != null) {
            founder.sendMessage("§a✔ §6" + player.getName() + "§a has joined your religion!");
        }

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getReligion() == null) {
            player.sendMessage("§cYou are not in a religion!");
            return true;
        }

        Religion religion = societiesManager.getReligion(data.getReligion());

        if (religion != null && religion.getFounder().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot leave a religion you founded! Transfer leadership first.");
            return true;
        }

        String religionName = data.getReligion();
        data.setReligion(null);
        data.setClergyRank(null);

        player.sendMessage("§a✔ You have left §6" + religionName + "§a!");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String religionName;

        if (args.length < 2) {
            PlayerData data = dataManager.getPlayerData(player.getUniqueId());
            if (data.getReligion() == null) {
                player.sendMessage("§cYou are not in a religion! Usage: /religion info <name>");
                return true;
            }
            religionName = data.getReligion();
        } else {
            religionName = args[1];
        }

        Religion religion = societiesManager.getReligion(religionName);
        if (religion == null) {
            player.sendMessage("§cReligion not found!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6" + religion.getName() + "§7 §m----------");
        player.sendMessage("§eFounder: §6" + plugin.getServer().getOfflinePlayer(religion.getFounder()).getName());
        player.sendMessage("§eFollowers: §6" + religion.getFollowers().size());
        player.sendMessage("§eClergy Members: §6" + religion.getClergy().size());
        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        Collection<Religion> religions = societiesManager.getAllReligions();

        if (religions.isEmpty()) {
            player.sendMessage("§cNo religions exist yet!");
            return true;
        }

        player.sendMessage("§7§m----------§r §6Religions §7(" + religions.size() + ")§m----------");

        for (Religion religion : religions) {
            String founderName = plugin.getServer().getOfflinePlayer(religion.getFounder()).getName();
            player.sendMessage("§e• §6" + religion.getName() + " §7- Founder: §e" + founderName + " §7- Followers: §e" + religion.getFollowers().size());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Religion Commands§7 §m----------");
        player.sendMessage("§e/religion create <name>§7 - Create a religion");
        player.sendMessage("§e/religion join <name>§7 - Join a religion");
        player.sendMessage("§e/religion leave§7 - Leave your religion");
        player.sendMessage("§e/religion info [name]§7 - View religion info");
        player.sendMessage("§e/religion list§7 - List all religions");
        player.sendMessage("§7§m--------------------------------");
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
