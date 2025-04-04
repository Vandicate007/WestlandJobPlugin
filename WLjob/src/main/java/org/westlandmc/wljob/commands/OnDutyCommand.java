package org.westlandmc.wljob.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

public class OnDutyCommand implements CommandExecutor {
    private final Main plugin;

    public OnDutyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!plugin.getJobManager().hasJob(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have a job!");
            return true;
        }

        if (plugin.getJobManager().isOnDuty(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are already on duty!");
            return true;
        }

        plugin.getJobManager().setOnDuty(player.getUniqueId(), true);
        plugin.getTagManager().updateTag(player);

        player.sendMessage(ChatColor.GREEN + "You are now on duty!");
        return true;
    }
}