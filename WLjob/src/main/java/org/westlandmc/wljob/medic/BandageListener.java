package org.westlandmc.wljob.medic;

import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.westlandmc.wljob.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BandageListener implements Listener {
    public static final double BANDAGE_COST = 50.0;
    private final Main plugin;
    private static final Map<UUID, UUID> bandageRequests = new HashMap<>();
    private static final Map<UUID, Long> requestTimes = new HashMap<>();
    private static final long REQUEST_TIMEOUT = 120000L; // 2 minutes timeout for request
    private static final Map<UUID, Long> bandageTimers = new HashMap<>();
    private static final Map<UUID, UUID> activeBandages = new HashMap<>(); // to track players who accepted bandage
    private static final String PREFIX = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Medik System" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;

    public BandageListener(Main plugin) {
        this.plugin = plugin;
    }

    // ارسال درخواست باندیج
    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player medic = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player)) return;
        Player target = (Player) event.getRightClicked();

        if (medic.getInventory().getItemInMainHand().getType() != Material.PAPER) return;

        if (!plugin.isMedicOnDuty(medic)) {
            medic.sendMessage(PREFIX + " " + ChatColor.RED + "Only medics can use the Bandage!");
            return;
        }

        if (bandageRequests.containsKey(target.getUniqueId())) {
            return;
        }

        if (activeBandages.containsKey(target.getUniqueId())) {
            medic.sendMessage(PREFIX + " " +ChatColor.RED + target.getName() + " is already receiving a bandage.");
            return;
        }

        if (plugin.getEconomy().getBalance(target) < BANDAGE_COST) {
            medic.sendMessage(PREFIX + " " +ChatColor.RED + target.getName() + " doesn't have enough money for a bandage.");
            return;
        }

        bandageRequests.put(target.getUniqueId(), medic.getUniqueId());
        requestTimes.put(target.getUniqueId(), System.currentTimeMillis());

        target.sendMessage(PREFIX + " " +ChatColor.AQUA + "Medic " + medic.getName() + " wants to bandage you. Type /accept to confirm the request. Cost: " + BANDAGE_COST);
        medic.sendMessage(PREFIX + " " +ChatColor.YELLOW + "You have sent a bandage request to " + target.getName() + ". Waiting for acceptance...");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (bandageRequests.containsKey(target.getUniqueId())) {
                bandageRequests.remove(target.getUniqueId());
                requestTimes.remove(target.getUniqueId());
                target.sendMessage(PREFIX + " " +ChatColor.RED + "Your bandage request has expired!");
            }
        }, REQUEST_TIMEOUT / 50);
    }

    public static void acceptBandage(Player player, Main plugin) {
        if (!bandageRequests.containsKey(player.getUniqueId())) {
            player.sendMessage(PREFIX + " " +ChatColor.RED + "You don't have any pending bandage requests.");
            return;
        }

        UUID medicId = bandageRequests.get(player.getUniqueId());
        Player medic = plugin.getServer().getPlayer(medicId);
        if (medic == null || !medic.isOnline()) {
            player.sendMessage(PREFIX + " " +ChatColor.RED + "The medic is no longer online.");
            return;
        }

        double distance = player.getLocation().distance(medic.getLocation());
        if (distance > 5) { // مثلا 5 بلاک
            player.sendMessage(PREFIX + " " + ChatColor.RED + "You are too far from " + medic.getName() + " to accept the bandage.");
            medic.sendMessage(PREFIX + " " + ChatColor.RED + player.getName() + " is too far away to receive the bandage.");
            return;
        }

        if (plugin.getEconomy().getBalance(player) < BANDAGE_COST) {
            player.sendMessage(PREFIX + " " +ChatColor.RED + "You don't have enough money for the bandage.");
            return;
        }

        activeBandages.put(player.getUniqueId(), medic.getUniqueId());
        player.sendMessage(PREFIX + " " +ChatColor.YELLOW + "Bandage is being applied! Stay still.");
        medic.sendMessage(PREFIX + " " +ChatColor.YELLOW + "Applying bandage to " + player.getName());

        new BukkitRunnable() {
            int seconds = 10;

            @Override
            public void run() {
                if (!player.isOnline() || !medic.isOnline() || activeBandages.get(player.getUniqueId()) == null) {
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    applyBandage(medic, player,plugin);
                    cancel();
                } else {
                    String progressBar = ChatColor.GREEN  + "■".repeat(10 - seconds) + ChatColor.GRAY + "■".repeat(seconds) + ChatColor.GREEN ;
                    medic.spigot().sendMessage(ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(ChatColor.DARK_RED  +"❤" +" "+ progressBar));
                    seconds--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        bandageRequests.remove(player.getUniqueId());
    }

    public static void applyBandage(Player medic, Player target, Main plugin) {
        if (medic == null || target == null || !medic.isOnline() || !target.isOnline()) return;

        if (plugin.getEconomy().getBalance(target) < BANDAGE_COST) {
            medic.sendMessage(PREFIX + " " +ChatColor.RED + target.getName() + " doesn't have enough money to complete the bandage.");
            target.sendMessage(PREFIX + " " +ChatColor.RED + "You don't have enough money to complete the bandage.");
            return;
        }

        target.setHealth(Math.min(target.getHealth() + 8.0, target.getMaxHealth()));

        boolean success = plugin.getServer().dispatchCommand(target, "pay " + medic.getName() + " " + BANDAGE_COST);
        if (success) {

        } else {
            medic.sendMessage(PREFIX + " " +ChatColor.RED + "Payment failed! No money was transferred.");
            target.sendMessage(PREFIX + " " +ChatColor.RED + "Payment failed! Please check your balance.");
        }

        medic.getInventory().removeItem(new org.bukkit.inventory.ItemStack(Material.PAPER, 1));
        medic.sendMessage(PREFIX + " " +ChatColor.GREEN + "You have successfully applied a bandage to " + target.getName());
        target.sendMessage(PREFIX + " " +ChatColor.GREEN + "You have received healing from " + medic.getName() + "'s bandage!");

        activeBandages.remove(target.getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (activeBandages.containsKey(player.getUniqueId())) {
            // دریافت موقعیت قبلی و جدید بازیکن
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                cancelBandage(player);
            }
        } else if (activeBandages.containsValue(player.getUniqueId())) {
            activeBandages.forEach((key, value) -> {
                if (value.equals(player.getUniqueId())) {
                    cancelBandage(plugin.getServer().getPlayer(key));
                }
            });
        }
    }

    private void cancelBandage(Player player) {
        if (player == null) return;
        UUID medicId = activeBandages.remove(player.getUniqueId());
        Player medic = plugin.getServer().getPlayer(medicId);

        if (medic != null) {
            medic.sendMessage(PREFIX + " " +ChatColor.RED + "The bandage was cancelled because " + player.getName() + " moved.");
        }
        player.sendMessage(PREFIX + " " +ChatColor.RED + "The bandage was cancelled because you moved.");
    }


    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (activeBandages.containsKey(player.getUniqueId())) {
            UUID medicId = activeBandages.get(player.getUniqueId());
            Player medic = player.getServer().getPlayer(medicId);

            if (medic != null && medic.isOnline()) {
                // Cancel the bandage request if player attacks
                activeBandages.remove(player.getUniqueId());
                medic.sendMessage(PREFIX + " " +ChatColor.RED + "The bandage request was cancelled because " + player.getName() + " attacked.");
                player.sendMessage(PREFIX + " " +ChatColor.RED + "The bandage request was cancelled because you attacked.");
            }
        }
    }

    // متد برای دسترسی به درخواست‌های باندیج
    public static Map<UUID, UUID> getBandageRequests() {
        return bandageRequests;
    }

    // متد برای حذف درخواست‌ها
    public static void removeRequest(Player playerUUID) {
        bandageRequests.remove(playerUUID);
        requestTimes.remove(playerUUID);
    }

    public static boolean hasPendingRequest(Player player) {
        return bandageRequests.containsKey(player.getUniqueId());
    }

    public static UUID getMedicForPlayer(Player player) {
        return bandageRequests.get(player.getUniqueId());
    }
}