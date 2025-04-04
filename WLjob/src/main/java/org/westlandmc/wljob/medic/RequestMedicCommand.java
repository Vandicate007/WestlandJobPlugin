package org.westlandmc.wljob.medic;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestMedicCommand implements CommandExecutor {
    private final KnockdownListener knockdownListener; // ✅ تغییر داده شد
    private final LuckPerms luckPerms;
    private final Map<UUID, Long> lastRequestTime = new HashMap<>();
    private final long REQUEST_COOLDOWN = 60L * 1000; // 60 ثانیه
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Medic System" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;


    public RequestMedicCommand(KnockdownListener knockdownListener, LuckPerms luckPerms) {
        this.knockdownListener = knockdownListener;
        this.luckPerms = luckPerms;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!knockdownListener.isKnocked(player)) { // ✅ knockedPlayers رو از KnockdownListener چک میکنه
            player.sendMessage(prefix + " " + ChatColor.RED + "You are not knocked down.");
            System.out.println("DEBUG: " + player.getName() + " is NOT in knockedPlayers!"); // 🛠 دیباگ
            return true;
        }

        System.out.println("DEBUG: " + player.getName() + " is in knockedPlayers! Proceeding..."); // 🛠 دیباگ

        long lastRequest = lastRequestTime.getOrDefault(player.getUniqueId(), 0L);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequest < REQUEST_COOLDOWN) {
            player.sendMessage(prefix + " " + ChatColor.RED + "You must wait before making another request.");
            return true;
        }

        boolean medicAvailable = false;
        for (Player medic : Bukkit.getOnlinePlayers()) {
            if (isMedicOnDuty(medic)) {
                medic.sendMessage(prefix + " " + ChatColor.RED + player.getName() +
                        " needs medical assistance at " + getLocationString(player.getLocation()));
                medicAvailable = true;
            }
        }

        if (!medicAvailable) {
            player.sendMessage(prefix + " " + ChatColor.RED + "No medics are currently available.");
            return true;
        }

        lastRequestTime.put(player.getUniqueId(), currentTime);
        player.sendMessage(prefix + " " + ChatColor.GREEN + "Your medic request has been sent!");
        return true;
    }

    private boolean isMedicOnDuty(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return false;

        String job = user.getPrimaryGroup();
        String onDuty = user.getCachedData().getMetaData().getMetaValue("onduty");

        return job.startsWith("medic") && "true".equals(onDuty);
    }

    private String getLocationString(Location location) {
        return "X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ();
    }
}