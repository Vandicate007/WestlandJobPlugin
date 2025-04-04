package org.westlandmc.wljob.medic;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KnockCommand implements CommandExecutor {
    private final KnockdownListener knockdownListener;

    public KnockCommand(KnockdownListener knockdownListener) {
        this.knockdownListener = knockdownListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wljob.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /knock <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        // بررسی ناک بودن از خود KnockdownListener
        if (knockdownListener.isKnocked(target)) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is already knocked!");
            return true;
        }

        knockdownListener.knockdownPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "You have knocked down " + target.getName() + "!");
        return true;
    }
}