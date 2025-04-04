package org.westlandmc.wljob.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

import java.util.List;

public class PromoteCommand implements CommandExecutor {
    private final Main plugin;

    public PromoteCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player promoter)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            promoter.sendMessage(ChatColor.RED + "Usage: /promote <player>");
            return false;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            promoter.sendMessage(ChatColor.RED + "Player not found!");
            return false;
        }

        if (!canPromote(promoter, target)) {
            promoter.sendMessage(ChatColor.RED + "You can't promote this player!");
            return false;
        }

        if (plugin.getJobManager().promotePlayer(target.getUniqueId())) {
            plugin.getTagManager().updateTag(target);
            promoter.sendMessage(ChatColor.GREEN + "Successfully promoted " + target.getName());
            target.sendMessage(ChatColor.GOLD + "You have been promoted!");
        } else {
            promoter.sendMessage(ChatColor.RED + target.getName() + " can't be promoted further!");
        }

        return true;
    }

    private boolean canPromote(Player promoter, Player target) {
        // Check if promoter is on duty
        if (!plugin.getJobManager().isOnDuty(promoter.getUniqueId())) {
            promoter.sendMessage(ChatColor.RED + "You must be on duty to promote!");
            return false;
        }

        // Check if both have same job
        String promoterJob = plugin.getJobManager().getPlayerJobName(promoter.getUniqueId());
        String targetJob = plugin.getJobManager().getPlayerJobName(target.getUniqueId());

        if (promoterJob == null || !promoterJob.equals(targetJob)) {
            promoter.sendMessage(ChatColor.RED + "You can only promote players in your department!");
            return false;
        }

        // Check if promoter has higher rank
        String promoterRank = plugin.getJobManager().getPlayerRank(promoter.getUniqueId());
        String targetRank = plugin.getJobManager().getPlayerRank(target.getUniqueId());

        List<String> ranks = plugin.getJobManager().getJobRanks(promoterJob);
        return ranks.indexOf(promoterRank) > ranks.indexOf(targetRank);
    }
}