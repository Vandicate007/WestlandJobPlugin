package org.westlandmc.wljob.medic;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MedicItemHandler implements Listener {
    private final Set<UUID> recentlyUsed = new HashSet<>();
    private final Set<UUID> preventMovement = new HashSet<>();
    private final HashMap<UUID, Integer> timerTaskIds = new HashMap<>();
    private final HashMap<UUID, Integer> remainingTime = new HashMap<>(); // ذخیره زمان باقی‌مانده

    private final JavaPlugin plugin;

    public MedicItemHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // onRightClick - متدی که قرص را می‌دهد
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // چک کردن اینکه آیا بازیکن داره روی قرص (Prismarine Crystals) کلیک می‌کنه
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.PRISMARINE_CRYSTALS) return;

        // جلوگیری از استفاده مجدد در مدت زمان معین
        if (recentlyUsed.contains(player.getUniqueId())) {
            return; // پیام ارسال نمی‌شود
        }

        recentlyUsed.add(player.getUniqueId()); // اضافه کردن به لیست بازیکنانی که اخیرا استفاده کردند
        preventMovement.add(player.getUniqueId()); // جلوگیری از حرکت بازیکن

        // شروع تایمر ۱۰ ثانیه‌ای
        remainingTime.put(player.getUniqueId(), 10); // زمان اولیه ۱۰ ثانیه
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    int timeLeft = remainingTime.get(player.getUniqueId());

                    // طراحی اکشن بار برای حس بهتر بازیکن
                    String timerText = ChatColor.DARK_RED + "❤";
                    String progress = ChatColor.GREEN + "■".repeat(10 - timeLeft) + ChatColor.GRAY + "■".repeat(timeLeft); // طراحی پروگرس بار
                    String formattedText = ChatColor.BOLD + timerText + " " + progress;

                    // نمایش تایمر در اکشن بار
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formattedText));

                    // اگر تایم تموم شد
                    if (timeLeft <= 0) {
                        // اضافه کردن سلامت به بازیکن
                        double newHealth = Math.min(player.getHealth() + 4.0, player.getMaxHealth()); // مثلا ۴ قلب به سلامت اضافه می‌کنه
                        player.setHealth(newHealth);

                        // حذف قرص از موجودی بازیکن
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item != null && item.getType() == Material.PRISMARINE_CRYSTALS) {
                            item.setAmount(item.getAmount() - 1); // کاهش تعداد آیتم
                            player.getInventory().setItemInMainHand(item); // به روز رسانی موجودی
                        }

                        // حذف از لیست
                        recentlyUsed.remove(player.getUniqueId());
                        preventMovement.remove(player.getUniqueId());
                        cancel(); // لغو تایمر
                    } else {
                        remainingTime.put(player.getUniqueId(), timeLeft - 1); // کاهش زمان باقی‌مانده
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 20L).getTaskId();// ذخیره کردن Task ID برای لغو در صورت حرکت
        timerTaskIds.put(player.getUniqueId(), taskId);
    }

    // onPlayerMove - متدی که حرکت بازیکن را بررسی می‌کند
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // چک کردن اینکه آیا بازیکن در حال استفاده از قرص هست یا نه
        if (preventMovement.contains(player.getUniqueId())) {
            // اگر حرکت کنه، عملیات استفاده از قرص لغو میشه
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                preventMovement.remove(player.getUniqueId()); // برداشتن از لیست جلوگیری از حرکت
                int taskId = timerTaskIds.get(player.getUniqueId()); // گرفتن Task ID
                Bukkit.getScheduler().cancelTask(taskId); // لغو تایمر
                recentlyUsed.remove(player.getUniqueId()); // برداشتن از لیست بازیکنانی که قرص استفاده کردن
            }
        }
    }
}