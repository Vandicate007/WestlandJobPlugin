package org.westlandmc.wljob.medic;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.westlandmc.wljob.Main;

public class GiveBandageCommand implements CommandExecutor {
    private final Main plugin;
    private final Economy economy;
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Hospital" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;

    public GiveBandageCommand(Main plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + "In dastoor faghat baraye bazikonan ast.");
            return true;
        }

        Player player = (Player) sender;
        double price = plugin.getBandagePrice(); // Daryaft gheymat bandage az config

        if (economy.getBalance(player) < price) {
            player.sendMessage(prefix + " " + ChatColor.RED + "Shoma poole kafi baraye kharide bandage nadarid!");
            return true;
        }

        economy.withdrawPlayer(player, price); // Kam kardane pool bazikon
        player.getInventory().addItem(BandageItem.createBandage()); // Dadane item be bazikon
        player.sendMessage(prefix + " " + ChatColor.GREEN + "Shoma yek bandage kharidid!");
        return true;
    }
}