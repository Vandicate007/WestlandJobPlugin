package org.westlandmc.wljob.medic;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.westlandmc.wljob.Main;
import net.milkbowl.vault.economy.Economy;

import java.util.Arrays;

public class GetPillCommand implements CommandExecutor {
    private final Main plugin;
    private final Economy economy;
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Hospital" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;

    public GetPillCommand(Main plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + " " + ChatColor.RED + "In dastoor faghat baraye bazikonan ast.");
            return true;
        }

        Player player = (Player) sender;
        double price = plugin.getPillPrice();

        // Check kardane meghdare pool bazikon
        if (economy.getBalance(player) < price) {
            player.sendMessage(prefix + " " +ChatColor.RED + "Shoma poole kafi baraye kharide in item nadarid!");
            return true;
        }

        // Kam kardane pool
        economy.withdrawPlayer(player, price);

        // Sakhtane item gors
        ItemStack pill = new ItemStack(Material.PRISMARINE_CRYSTALS, 1);
        ItemMeta meta = pill.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "GHORS");
            int healAmount = plugin.getConfig().getInt("medic.pill.healAmount");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Yek ghors darmani baraye bazyabi salamat!",
                    ChatColor.RED + "♥ " + healAmount / 2.0 + " Ghalb bazyabi mikonad!"
            ));
            pill.setItemMeta(meta);
        }

        // Dadane item be bazikon
        player.getInventory().addItem(pill);
        player.sendMessage(prefix + " " +ChatColor.GREEN + "Shoma yek gors ehya kharidid!");

        return true;
    }
}