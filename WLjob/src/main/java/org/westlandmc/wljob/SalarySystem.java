package org.westlandmc.wljob;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SalarySystem extends BukkitRunnable {
    private final Main plugin;

    public SalarySystem(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();
        long salaryInterval = plugin.getConfig().getLong("salary.interval") * 60 * 1000;

        for (Player player : Bukkit.getOnlinePlayers()) {
            JobManager.PlayerJobData data = plugin.getJobManager().getPlayerData(player.getUniqueId());

            if (data != null && data.isOnDuty() &&
                    (currentTime - data.getLastSalaryTime()) >= salaryInterval) {

                if (plugin.getJobManager().paySalary(player.getUniqueId())) {
                    player.sendMessage(ChatColor.GREEN + "حقوق شما پرداخت شد: " +
                            ChatColor.GOLD + "$" +
                            plugin.getJobManager().getRankSalary(data.getJobName(), data.getRank()));
                }
            }
        }
    }
}