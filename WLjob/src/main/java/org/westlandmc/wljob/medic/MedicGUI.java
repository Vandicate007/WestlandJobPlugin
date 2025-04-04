package org.westlandmc.wljob.medic;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.westlandmc.wljob.Main;

import java.util.*;

public class MedicGUI implements Listener {
    private final Main plugin;
    private final Map<UUID, UUID> trackingMap = new HashMap<>();
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Emergency" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;

    public MedicGUI(Main plugin) {
        this.plugin = plugin;
    }

    public void openMedicGUI(Player medic) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.RED + "EMERGENCY");

        for (UUID uuid : plugin.getKnockedPlayers()) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(target);
                meta.setDisplayName(ChatColor.YELLOW + target.getName());

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Coords: " +
                        (int) target.getLocation().getX() + ", " +
                        (int) target.getLocation().getY() + ", " +
                        (int) target.getLocation().getZ());

                if (trackingMap.containsValue(target.getUniqueId())) {
                    lore.add(ChatColor.RED + "In bazikon dar hale track shodan hast!");
                } else if (trackingMap.containsKey(medic.getUniqueId()) && trackingMap.get(medic.getUniqueId()).equals(target.getUniqueId())) {
                    lore.add(ChatColor.GREEN + "Shoma in bazikon ro track mikonin!");
                } else {
                    lore.add(ChatColor.YELLOW + "Baraye track click konid.");
                }

                meta.setLore(lore);
                skull.setItemMeta(meta);
                gui.addItem(skull);
            }
        }

        medic.openInventory(gui);
    }

    @EventHandler
    public void onMedicQuit(PlayerQuitEvent event) {
        Player medic = event.getPlayer();
        UUID medicUUID = medic.getUniqueId();

        if (trackingMap.containsKey(medicUUID)) {
            trackingMap.remove(medicUUID);
            medic.getInventory().remove(Material.COMPASS);
            System.out.println("DEBUG: " + medic.getName() + " logged out, tracking canceled.");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.RED + "EMERGENCY")) return;
        event.setCancelled(true);

        Player medic = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
        if (target == null || !target.isOnline()) {
            medic.sendMessage(prefix + ChatColor.RED + "In bazikon online nist!");
            return;
        }

        UUID targetUUID = target.getUniqueId();// اگر مدیک از قبل کسی را دنبال می‌کند، اجازه ندارد کسی دیگر را انتخاب کند
        if (trackingMap.containsKey(medic.getUniqueId()) && !trackingMap.get(medic.getUniqueId()).equals(targetUUID)) {
            medic.sendMessage(prefix + ChatColor.RED + "Shoma dar hale track yek bazikon hastid! Aval bayad cancel konid.");
            return;
        }

        // اگر مدیک همان بازیکنی که در حال ترک کردنش بود را کلیک کند، پیگیری لغو می‌شود
        if (trackingMap.containsKey(medic.getUniqueId()) && trackingMap.get(medic.getUniqueId()).equals(targetUUID)) {
            trackingMap.remove(medic.getUniqueId());
            medic.sendMessage(prefix + ChatColor.YELLOW + "Shoma track " + target.getName() + " ro cancel kardid.");
            medic.getInventory().remove(Material.COMPASS);
            openMedicGUI(medic);
            return;
        }

        // اگر بازیکن قبلاً توسط کسی دیگر ترک شده باشد
        if (trackingMap.containsValue(targetUUID)) {
            medic.sendMessage(prefix + ChatColor.RED + "In bazikon ghablan track shode!");
            return;
        }

        // افزودن بازیکن به ترکینگ
        trackingMap.put(medic.getUniqueId(), targetUUID);

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.GOLD + "Tracker: " + target.getName());
        compass.setItemMeta(compassMeta);

        medic.getInventory().addItem(compass);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getKnockedPlayers().contains(targetUUID) ||
                        !medic.isOnline() ||
                        !trackingMap.containsKey(medic.getUniqueId()) ||
                        !trackingMap.get(medic.getUniqueId()).equals(targetUUID)) {
                    this.cancel();
                    medic.getInventory().remove(Material.COMPASS);
                    return;
                }
                medic.setCompassTarget(target.getLocation());
            }
        }.runTaskTimer(plugin, 0L, 20L);

        medic.sendMessage(prefix + ChatColor.GREEN + "Shoma dar hale track " + target.getName() + " hastid!");
        target.sendMessage(prefix + ChatColor.YELLOW + medic.getName() + " dar hale track shodane shomast!");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (plugin.isMedicOnDuty(onlinePlayer) && !onlinePlayer.equals(medic)) {
                onlinePlayer.sendMessage(prefix + ChatColor.RED + medic.getName() + " dar hale track " + target.getName() + " hast.");
            }
        }

        // بستن منوی GUI پس از انتخاب بازیکن برای پیگیری
        medic.closeInventory();
    }

    public void removeTracking(UUID playerUUID) {
        trackingMap.values().remove(playerUUID);
    }

    @EventHandler
    public void onKnockedPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (trackingMap.containsValue(player.getUniqueId())) {
            trackingMap.values().remove(player.getUniqueId());
            System.out.println("DEBUG: " + player.getName() + " logged out, tracking removed.");
        }
    }
}