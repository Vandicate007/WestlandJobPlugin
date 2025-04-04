package org.westlandmc.wljob;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public class TagManager {
    private final Main plugin;

    public TagManager(Main plugin) {
        this.plugin = plugin;
    }

    public void updateTag(Player player) {
        UUID playerId = player.getUniqueId();
        JobManager.PlayerJobData data = plugin.getJobManager().getPlayerData(playerId);

        if (data == null || data.getJobName() == null) {
            clearTag(player);
            return;
        }

        String prefix = plugin.getJobManager().getJobPrefix(data.getJobName());
        String suffix = data.isOnDuty()
                ? plugin.getJobManager().getRankDisplay(data.getJobName(), data.getRank())
                : ChatColor.RED + "[Off-Duty]";

        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getTeam(player.getName()) == null
                ? scoreboard.registerNewTeam(player.getName())
                : scoreboard.getTeam(player.getName());

        team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefix));
        team.setSuffix(ChatColor.translateAlternateColorCodes('&', suffix));
        team.addEntry(player.getName());
    }

    private void clearTag(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Team team = scoreboard.getTeam(player.getName());
        if (team != null) {
            team.unregister();
        }
    }
}