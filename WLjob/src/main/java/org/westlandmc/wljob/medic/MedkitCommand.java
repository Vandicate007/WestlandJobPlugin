package org.westlandmc.wljob.medic;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.westlandmc.wljob.Main;
import java.util.Arrays;

public class MedkitCommand implements CommandExecutor {
    private final Main plugin;
    private final Economy economy;
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Hospital" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;

    public MedkitCommand(Main plugin, Economy economy) {
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
        double price = plugin.getMedkitPrice(); // Daryaft gheymat az config

        if (economy.getBalance(player) < price) {
            player.sendMessage(prefix + " " + ChatColor.RED + "Shoma poole kafi baraye kharide medkit nadarid!");
            return true;
        }

        economy.withdrawPlayer(player, price); // Kam kardane pool bazikon

        // Sakhtane medkit
        ItemStack medkit = new ItemStack(Material.RED_DYE, 1);
        ItemMeta meta = medkit.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Medkit");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Baraye ehyaaye bazikonane knock shode estefade mishavad.",
                    ChatColor.YELLOW + "Baraye estefade, rooye bazikone knock shode right click konid."
            ));
            medkit.setItemMeta(meta);
        }

        player.getInventory().addItem(medkit);
        player.sendMessage(prefix + " " + ChatColor.GREEN + "Shoma yek medkit kharidid!");
        return true;
    }
}