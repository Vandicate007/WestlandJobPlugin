package org.westlandmc.wljob.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

public class SetJobCommand implements CommandExecutor {
    private final Main plugin;

    public SetJobCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setjob <player> [job]");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return false;
        }

        if (plugin.getJobManager().hasJob(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "This player already has a job!");
            return false;
        }

        // تغییر اصلی: بررسی نکردن وضعیت On-Duty برای ادمین‌ها
        if (!player.isOp() && !plugin.getJobManager().isOnDuty(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You must be on duty to set jobs!");
            return true;
        }

        String job;
        if (args.length == 2) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "You don't have permission to specify jobs!");
                return false;
            }
            job = args[1].toLowerCase();
        } else {
            job = plugin.getJobManager().getPlayerJobName(player.getUniqueId());
            if (job == null) {
                player.sendMessage(ChatColor.RED + "You don't have a job to assign!");
                return false;
            }
        }

        if (!plugin.getJobManager().getAvailableJobs().contains(job)) {
            player.sendMessage(ChatColor.RED + "Invalid job type!");
            return false;
        }

        String defaultRank = plugin.getJobManager().getJobRanks(job).get(0);
        plugin.getJobManager().setPlayerJob(target.getUniqueId(), job, defaultRank, false);
        plugin.getTagManager().updateTag(target);

        player.sendMessage(ChatColor.GREEN + "Successfully set " + target.getName() +
                " as " + job + " with rank " + defaultRank);
        target.sendMessage(ChatColor.GREEN + "You are now a " + job + "! You are on duty.");

        return true;
    }
}