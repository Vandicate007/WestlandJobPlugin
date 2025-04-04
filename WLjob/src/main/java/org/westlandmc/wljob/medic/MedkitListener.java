package org.westlandmc.wljob.medic;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.westlandmc.wljob.Main;

public class MedkitListener implements Listener {

    private final Main plugin;
    private final KnockdownListener knockdownListener;
    private final Economy economy;
    private final LuckPerms luckPerms;
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Medic System" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;


    public MedkitListener(Main plugin, KnockdownListener knockdownListener, Economy economy,LuckPerms luckPerms) {
        this.plugin = plugin;
        this.knockdownListener = knockdownListener;
        this.economy = economy;
        this.luckPerms = luckPerms;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // بررسی اینکه بازیکن داره روی مدکیت کلیک می‌کنه
        if (player.getInventory().getItemInMainHand().getType() != Material.RED_DYE) return;

        if (!isMedicOnDuty(player)) {
            player.sendMessage(prefix + " " + ChatColor.RED + "You are not on duty/medic. You cannot use the medkit.");
            return;
        }

        // بررسی اینکه آیا روی بازیکن کلیک شده است
        if (!(event.getRightClicked() instanceof Player clickedPlayer)) {
            player.sendMessage(prefix + " " + ChatColor.RED + "You must click on a player to revive them.");
            return;
        }

        // بررسی اینکه آیا بازیکن ناک شده است یا خیر
        if (!knockdownListener.isKnocked(clickedPlayer)) {
            player.sendMessage(prefix + " " + ChatColor.RED + "This player is not knocked down.");
            return;
        }

        // بررسی حرکت مدیک در حین ریوایو
        if (isReviving(player)) {
            return;
        }

        // پیام تایید برای شروع ریوایو
        player.sendMessage(prefix + " " + ChatColor.YELLOW + "Revival process started! Please stay still.");
        clickedPlayer.sendMessage(prefix + " " + ChatColor.YELLOW + "You are being revived by " + player.getName());

        // شروع تایمر و اکشن بار 10 ثانیه‌ای
        startRevivalTimer(player, clickedPlayer);
    }

    private boolean isMedicOnDuty(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return false;
        String job = user.getPrimaryGroup();
        String onDuty = user.getCachedData().getMetaData().getMetaValue("onduty");
        return job.startsWith("medic") && "true".equals(onDuty);
    }

    // متد چک کردن حرکت مدیک در حین ریوایو
    private boolean isReviving(Player player) {
        return plugin.getRevivingPlayers().contains(player.getUniqueId());
    }

    // شروع تایمر ریوایو
    private void startRevivalTimer(Player medic, Player knockedPlayer) {
        final int[] timeLeft = {10}; // تایمر ۱۰ ثانیه‌ای
        plugin.addRevivingPlayer(medic.getUniqueId()); // افزودن مدیک به لیست ریوایوکننده‌ها
        final Location initialLocation = medic.getLocation().clone(); // ذخیره مکان اولیه مدیک

        new BukkitRunnable() {
            @Override
            public void run() {
                // چک کردن حرکت مدیک یا ترک کردن سرور
                if (medic.isDead() || knockedPlayer.isDead() ||
                !medic.isOnline() || !knockedPlayer.isOnline() ||
                hasMoved(medic, initialLocation)) { // بررسی حرکت مدیک

                    cancel(); // متوقف کردن تایمر
                    medic.sendMessage(prefix + " " + ChatColor.RED + "The revival process has been cancelled because you moved!");
                    knockedPlayer.sendMessage(prefix + " " + ChatColor.RED + "The revival process was cancelled.");
                    plugin.removeRevivingPlayer(medic.getUniqueId()); // حذف مدیک از لیست ریوایوکننده‌ها
                    return;
                }

                if (timeLeft[0] <= 0) {
                    // ریوایو کردن بازیکن
                    knockdownListener.revivePlayer(knockedPlayer);
                    // کم کردن آیتم مدکیت از اینونتوری
                    ItemStack medkit = medic.getInventory().getItemInMainHand();
                    if (medkit.getAmount() > 1) {
                        medkit.setAmount(medkit.getAmount() - 1);
                    } else {
                        medic.getInventory().setItemInMainHand(new ItemStack(Material.AIR)); // حذف آیتم
                    }
                    // پرداخت پول به مدیک
                    giveMoneyToMedic(medic);
                    // پیام موفقیت
                    medic.sendMessage(prefix + " " + ChatColor.GREEN + "You have successfully revived " + knockedPlayer.getName() + "!");

                    cancel();
                    plugin.removeRevivingPlayer(medic.getUniqueId()); // حذف مدیک از لیست ریوایوکننده‌ها
                } else {
                    // نمایش تایمر روی اکشن بار
                    String progress = ChatColor.GREEN + "■".repeat(10 - timeLeft[0]) + ChatColor.RED + "■".repeat(timeLeft[0]);
                    medic.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Reviving..." + progress + " " + timeLeft[0] + "s"));
                    timeLeft[0]--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // متد چک کردن حرکت مدیک
    private boolean hasMoved(Player player, Location initialLocation) {
        return player.getLocation().distanceSquared(initialLocation) > 0.1; // بررسی تغییر مکان
    }

    // متد برای پرداخت پول به مدیک
    private void giveMoneyToMedic(Player medic) {
        // دادن پول به مدیک بدون استفاده از متغیر rewardAmount
        economy.depositPlayer(medic, 50.0); // مبلغ 50.0 به طور مستقیم پرداخت می‌شود
        medic.sendMessage(prefix + " " + ChatColor.GREEN + "You have been paid 50.0 for your revival service.");
    }
}