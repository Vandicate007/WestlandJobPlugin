package org.westlandmc.wljob.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.JobManager;
import org.westlandmc.wljob.Main;

public class FireJobCommand implements CommandExecutor {
    private final Main plugin;

    public FireJobCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /firejob <player>");
            return false;
        }

        // بررسی ادمین بودن
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return false;
        }

        // بررسی اینکه بازیکن اصلاً شغلی دارد یا نه
        if (!plugin.getJobManager().hasJob(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "This player doesn't have a job!");
            return false;
        }

        // دریافت اطلاعات شغل
        JobManager.PlayerJobData targetData = plugin.getJobManager().getPlayerData(target.getUniqueId());
        String jobName = targetData.getJobName();

        // انجام عملیات اخراج
        firePlayer(target, targetData);

        sender.sendMessage(ChatColor.GREEN + "You have fired " + ChatColor.AQUA + target.getName() +
                ChatColor.GREEN + " from " + ChatColor.GOLD + jobName + ChatColor.GREEN + " job.");

        // اگر بازیکن آنلاین است به او پیام بده
        if (target.isOnline()) {
            target.getPlayer().sendMessage(ChatColor.RED + "You have been fired from your job!");
        }

        return true;
    }

    private void firePlayer(OfflinePlayer player, JobManager.PlayerJobData data) {
        // 1. حذف شغل بازیکن
        plugin.getJobManager().resetPlayerJob(player.getUniqueId());

        // 2. به روزرسانی تگ (اگر آنلاین است)
        if (player.isOnline()) {
            plugin.getTagManager().updateTag(player.getPlayer());
        }

        // 3. ذخیره تغییرات
        plugin.getJobManager().saveToConfig();
    }
}