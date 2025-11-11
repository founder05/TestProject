package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.enums.JobType;
import me.marcdoesntexists.nations.managers.DataManager;
import me.marcdoesntexists.nations.utils.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JobCommand implements CommandExecutor, TabCompleter {

    private final Nations plugin;
    private final DataManager dataManager;

    public JobCommand(Nations plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
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
            case "list":
                return handleList(player, args);
            case "join":
                return handleJoin(player, args);
            case "quit":
                return handleQuit(player, args);
            case "info":
                return handleInfo(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleList(Player player, String[] args) {
        player.sendMessage("§7§m----------§r §6Available Jobs§7 §m----------");

        for (JobType jobType : JobType.values()) {
            player.sendMessage("§e• §6" + jobType.getDisplayName());
            player.sendMessage("§7  Base Salary: §a$" + jobType.getBaseSalary() + "§7/day");
            player.sendMessage("§7  Tax: §c" + jobType.getTaxPercentage() + "%");
        }

        player.sendMessage("§7§m--------------------------------");
        player.sendMessage("§7Use §e/job join <type>§7 to get a job!");

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /job join <type>");
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage("§cYou must be in a town to get a job!");
            return true;
        }

        if (data.getJob() != null) {
            player.sendMessage("§cYou already have a job! Use /job quit first.");
            return true;
        }

        JobType jobType;
        try {
            jobType = JobType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid job type! Use /job list to see available jobs.");
            return true;
        }

        data.setJob(jobType.name());
        data.setJobExperience(0);

        player.sendMessage("§a✔ You are now a §6" + jobType.getDisplayName() + "§a!");
        player.sendMessage("§7Salary: §a$" + jobType.getBaseSalary() + "§7/day");
        player.sendMessage("§7Tax: §c" + jobType.getTaxPercentage() + "%");

        return true;
    }

    private boolean handleQuit(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getJob() == null) {
            player.sendMessage("§cYou don't have a job!");
            return true;
        }

        String currentJob = data.getJob();
        data.setJob(null);
        data.setJobExperience(0);

        JobType jobType = JobType.valueOf(currentJob);
        player.sendMessage("§a✔ You have quit your job as §6" + jobType.getDisplayName() + "§a!");

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getJob() == null) {
            player.sendMessage("§cYou don't have a job!");
            player.sendMessage("§7Use §e/job list§7 to see available jobs.");
            return true;
        }

        JobType jobType = JobType.valueOf(data.getJob());

        player.sendMessage("§7§m----------§r §6Your Job§7 §m----------");
        player.sendMessage("§eJob: §6" + jobType.getDisplayName());
        player.sendMessage("§eSalary: §a$" + jobType.getBaseSalary() + "§7/day");
        player.sendMessage("§eTax: §c" + jobType.getTaxPercentage() + "%");
        player.sendMessage("§eExperience: §6" + data.getJobExperience());

        if (data.getTown() != null) {
            player.sendMessage("§eEmployer: §6" + data.getTown());
        }

        player.sendMessage("§7§m--------------------------------");

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§7§m----------§r §6Job Commands§7 §m----------");
        player.sendMessage("§e/job list§7 - View available jobs");
        player.sendMessage("§e/job join <type>§7 - Get a job");
        player.sendMessage("§e/job quit§7 - Quit your job");
        player.sendMessage("§e/job info§7 - View your job info");
        player.sendMessage("§7§m--------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "join", "quit", "info")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return Arrays.stream(JobType.values())
                    .map(jt -> jt.name().toLowerCase())
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
