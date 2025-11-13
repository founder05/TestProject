package me.marcdoesntexists.nations.commands;

import me.marcdoesntexists.nations.Nations;
import me.marcdoesntexists.nations.economy.EconomyService;
import me.marcdoesntexists.nations.enums.JobType;
import me.marcdoesntexists.nations.utils.MessageUtils;
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
import java.util.Map;

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
        player.sendMessage(MessageUtils.get("jobs.header"));

        for (JobType jobType : JobType.values()) {
            player.sendMessage(MessageUtils.format("jobs.list_item", Map.of("display", jobType.getDisplayName())));
            player.sendMessage(MessageUtils.format("jobs.joined", Map.of("job", jobType.getDisplayName(), "salary", String.valueOf(jobType.getBaseSalary()), "tax", String.valueOf(jobType.getTaxPercentage()))));
        }

        player.sendMessage(MessageUtils.get("jobs.footer"));
        player.sendMessage(MessageUtils.get("jobs.join_hint"));

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtils.get("jobs.usage_join"));
            return true;
        }

        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getTown() == null) {
            player.sendMessage(MessageUtils.get("jobs.must_be_in_town"));
            return true;
        }

        if (data.getJob() != null) {
            player.sendMessage(MessageUtils.get("jobs.already_have_job"));
            return true;
        }

        JobType jobType;
        try {
            jobType = JobType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtils.get("jobs.invalid_type"));
            return true;
        }

        data.setJob(jobType.name());
        data.setJobExperience(0);

        player.sendMessage(MessageUtils.format("jobs.joined", Map.of("job", jobType.getDisplayName(), "salary", String.valueOf(jobType.getBaseSalary()), "tax", String.valueOf(jobType.getTaxPercentage()))));

        return true;
    }

    private boolean handleQuit(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getJob() == null) {
            player.sendMessage(MessageUtils.get("jobs.no_job"));
            return true;
        }

        String currentJob = data.getJob();
        data.setJob(null);
        data.setJobExperience(0);

        JobType jobType = JobType.valueOf(currentJob);
        player.sendMessage(MessageUtils.format("jobs.you_quit", Map.of("job", jobType.getDisplayName())));

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());

        if (data.getJob() == null) {
            player.sendMessage(MessageUtils.get("jobs.no_job"));
            player.sendMessage(MessageUtils.get("jobs.header"));
            return true;
        }

        JobType jobType = JobType.valueOf(data.getJob());

        player.sendMessage(MessageUtils.get("jobs.info_header"));
        player.sendMessage(MessageUtils.format("jobs.joined", Map.of("job", jobType.getDisplayName(), "salary", String.valueOf(jobType.getBaseSalary()), "tax", String.valueOf(jobType.getTaxPercentage()))));
        player.sendMessage(MessageUtils.format("jobs.info_footer", Map.of()));

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtils.get("jobs.header"));
        player.sendMessage(MessageUtils.get("jobs.footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.match(Arrays.asList("list", "join", "quit", "info"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return me.marcdoesntexists.nations.utils.TabCompletionUtils.matchDistinct(
                    Arrays.stream(JobType.values()).map(jt -> jt.name()).collect(java.util.stream.Collectors.toList()),
                    args[1]
            );
        }

        return new ArrayList<>();
    }
}
