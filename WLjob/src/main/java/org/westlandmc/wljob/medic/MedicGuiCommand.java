package org.westlandmc.wljob.medic;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

public class MedicGuiCommand implements CommandExecutor {
    private final MedicGUI medicGUI;
    private final Main plugin;
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Emergency" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;

    public MedicGuiCommand(Main plugin, MedicGUI medicGUI) {
        this.plugin = plugin;
        this.medicGUI = medicGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Faghat bazikon ha mitunan in command ro bezanan!");
            return false;
        }

        if (!plugin.isMedicOnDuty(player)) {
            player.sendMessage(prefix + ChatColor.RED + "Shoma medic nistid ya on-duty nistid!");
            return false;
        }

        medicGUI.openMedicGUI(player);
        return true;
    }
}