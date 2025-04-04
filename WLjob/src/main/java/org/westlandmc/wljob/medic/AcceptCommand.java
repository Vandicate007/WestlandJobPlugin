package org.westlandmc.wljob.medic;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

import java.util.UUID;

public class AcceptCommand implements CommandExecutor {
    private final Main plugin;

    public AcceptCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can accept bandage requests.");
            return false;
        }

        Player player = (Player) sender;

        // Check if this player has a pending bandage request
        if (!BandageListener.hasPendingRequest(player)) {
            player.sendMessage(ChatColor.RED + "You don't have any pending bandage requests.");
            return false;
        }

        // Get the medic who sent the bandage request
        UUID medicId = BandageListener.getMedicForPlayer(player);
        Player medic = plugin.getServer().getPlayer(medicId);

        if (medic == null || !medic.isOnline()) {
            player.sendMessage(ChatColor.RED + "The medic who requested the bandage is no longer online.");
            return false;
        }

        // Accept the bandage request and start the bandage process
        BandageListener.acceptBandage(player, plugin);

        return true;
    }
}