package org.westlandmc.wljob.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

public class OffDutyCommand implements CommandExecutor {
    private final Main plugin;

    public OffDutyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!plugin.getJobManager().isOnDuty(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are already off duty!");
            return true;
        }

        plugin.getJobManager().setOnDuty(player.getUniqueId(), false);
        plugin.getTagManager().updateTag(player);

        player.sendMessage(ChatColor.RED + "You are now off duty!");
        return true;
    }
}