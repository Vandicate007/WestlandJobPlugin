package org.westlandmc.wljob.medic;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReviveCommand implements CommandExecutor {
    private final KnockdownListener knockdownListener;

    public ReviveCommand(KnockdownListener knockdownListener) {
        this.knockdownListener = knockdownListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wljob.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /revive <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        System.out.println("DEBUG: Checking if " + target.getName() + " is knocked.");

        if (!knockdownListener.isKnocked(target)) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is not knocked!");
            return true;
        }

        System.out.println("DEBUG: " + target.getName() + " is knocked, reviving...");

        knockdownListener.revivePlayer(target);
        sender.sendMessage(ChatColor.GREEN + "You have revived " + target.getName() + "!");
        return true;
    }
}