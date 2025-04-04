package org.westlandmc.wljob.medic;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BandageItem {
    public static ItemStack createBandage() {
        ItemStack bandage = new ItemStack(Material.PAPER);
        ItemMeta meta = bandage.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Bandage");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Use this to heal players!",
                    ChatColor.GREEN + "Right-click on a player to offer healing."
            ));
            bandage.setItemMeta(meta);
        }
        return bandage;
    }
}