package org.westlandmc.wljob.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

public class DemoteCommand implements CommandExecutor {
    private final Main plugin;

    public DemoteCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player demoter)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            demoter.sendMessage(ChatColor.RED + "Usage: /demote <player>");
            return false;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            demoter.sendMessage(ChatColor.RED + "Player not found!");
            return false;
        }

        if (!canDemote(demoter, target)) {
            demoter.sendMessage(ChatColor.RED + "You can't demote this player!");
            return false;
        }

        if (plugin.getJobManager().demotePlayer(target.getUniqueId())) {
            plugin.getTagManager().updateTag(target);
            demoter.sendMessage(ChatColor.GREEN + "Successfully demoted " + target.getName());
            target.sendMessage(ChatColor.RED + "You have been demoted!");
        } else {
            demoter.sendMessage(ChatColor.RED + target.getName() + " can't be demoted further!");
        }

        return true;
    }

    private boolean canDemote(Player demoter, Player target) {
        // Similar to canPromote but with different rank checks
        // Implementation omitted for brevity
        return true;
    }
}