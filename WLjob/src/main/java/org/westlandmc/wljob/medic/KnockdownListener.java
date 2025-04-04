package org.westlandmc.wljob.medic;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.westlandmc.wljob.Main;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KnockdownListener implements Listener {
    private static final long KNOCKDOWN_DURATION = 12000L; // 5 دقیقه
    private final Set<UUID> knockedPlayers = new HashSet<>();
    private final JavaPlugin plugin;
    private final LuckPerms luckPerms;
    private final String prefix = ChatColor.DARK_PURPLE + "❰ " + ChatColor.LIGHT_PURPLE + "Medic System" + ChatColor.DARK_PURPLE + " ❱ " + ChatColor.RESET;


    public KnockdownListener(JavaPlugin plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }
    public Set<UUID> getKnockedPlayers() {
        return knockedPlayers;
    }
    public boolean isKnocked(Player player) {
        return knockedPlayers.contains(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();
        double finalHealth = player.getHealth() - event.getFinalDamage();

        // اگر بازیکن قبلاً مرده باشه، چیزی پردازش نکن
        if (player.isDead()) return;

        // اگر بازیکن در حالت ناک باشه و جانش صفر بشه، یکبار بکشش و حذفش کن
        if (knockedPlayers.contains(playerId) && finalHealth <= 0) {
            knockedPlayers.remove(playerId); // ✅ حذف از لیست ناک‌شده‌ها
            System.out.println("DEBUG: " + player.getName() + " removed from knockedPlayers on death."); // 🛠 دیباگ
            Bukkit.getScheduler().runTask(plugin, () -> player.setHealth(0)); // مرگ بازیکن
            return;
        }

        // اگر بازیکن قراره ناک بشه ولی هنوز ناک نشده
        if (finalHealth <= 0 && !knockedPlayers.contains(playerId)) {
            event.setCancelled(true); // مرگ عادی رو کنسل کن
            knockdownPlayer(player); // بازیکن رو ناک کن
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        // اگه بازیکن توی لیست ناک شده‌ها باشه یا مرده باشه، حرکتش قفل بشه
        if (knockedPlayers.contains(playerId) || player.isDead()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (knockedPlayers.contains(player.getUniqueId())) {
            knockedPlayers.remove(player.getUniqueId());
            player.setHealth(0);
        }
    }

    @EventHandler
    public void onToggleSwim(EntityToggleSwimEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (knockedPlayers.contains(player.getUniqueId())) {
            if (!event.isSwimming()) { // فقط وقتی از شنا خارج شد، لغوش کن
                event.setCancelled(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {if (knockedPlayers.contains(player.getUniqueId())) {
                        player.setSwimming(true); // کمی بعد دوباره به حالت شنا برگردون
                    }
                    }
                }.runTaskLater(plugin, 1L); // 1 تیک بعد اجرا بشه
            }
        }
    }

    void knockdownPlayer(Player player) {
        knockedPlayers.add(player.getUniqueId());
        player.setHealth(10.0);
        player.sendTitle(ChatColor.RED + "🚨 Request Medic!", ChatColor.YELLOW + "Use /requestmedic", 10, 70, 20);

        notifyOnDutyMedics(player);

        UUID playerUUID = player.getUniqueId();
        Main plugin = Main.getInstance();
        plugin.removeTrackedPlayer(playerUUID);

        player.setSwimming(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 12000, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 12000, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 1, false, false));


        new BukkitRunnable() {
            int secondsLeft = (int) (KNOCKDOWN_DURATION / 20);
            final int totalDuration = secondsLeft;

            @Override
            public void run() {
                if (!knockedPlayers.contains(playerUUID) || player.isDead()) {
                    cancel();
                    return;
                }

                ChatColor timeColor;
                if (secondsLeft > 400) {
                    timeColor = ChatColor.GREEN;
                } else if (secondsLeft > 200) {
                    timeColor = ChatColor.YELLOW;
                } else {
                    timeColor = ChatColor.RED;
                }

                int progressBars = (int) ((secondsLeft / (double) totalDuration) * 20);
                String progressBar = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "█".repeat(progressBars)
                        + ChatColor.GRAY + "█".repeat(20 - progressBars) + ChatColor.DARK_GRAY + "]";

                String actionBarText = timeColor + "⏳ " + ChatColor.YELLOW + "Time Left: "
                        + timeColor + secondsLeft + "s  " + progressBar;

                // 📌 نمایش تایمر برای بازیکن
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarText));


                if (secondsLeft <= 0) {
                    revivePlayer(player);
                    cancel();
                }

                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (knockedPlayers.contains(player.getUniqueId())) {
                    revivePlayer(player);
                }
            }
        }.runTaskLater(plugin, KNOCKDOWN_DURATION);
    }

    void revivePlayer(Player player) {
        if (knockedPlayers.contains(player.getUniqueId())) {
            knockedPlayers.remove(player.getUniqueId());
            System.out.println("DEBUG: " + player.getName() + " removed from knockedPlayers in revivePlayer.");
        }

        UUID playerUUID = player.getUniqueId();
        Main plugin = Main.getInstance();
        plugin.removeTrackedPlayer(playerUUID);

        player.setSwimming(false);
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);

        player.getWorld().spawnParticle(Particle.HEART, player.getLocation(), 10);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        player.setHealth(player.getMaxHealth() / 2);

        // 📌 حذف پیام ActionBar بعد از ریوایو
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(" "));

        player.sendMessage(prefix + " " + ChatColor.GREEN + "✅ You have been revived!");
    }

    private void notifyOnDutyMedics(Player player) {
        String location = "X:" + player.getLocation().getBlockX() + " Y:" + player.getLocation().getBlockY() + " Z:" + player.getLocation().getBlockZ();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (isMedicOnDuty(onlinePlayer)) {
                onlinePlayer.sendMessage(prefix + " " + ChatColor.RED + player.getName() +
                        " needs medical assistance! " + ChatColor.YELLOW + location);
            }
        }
    }

    private boolean isMedicOnDuty(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return false;
        String job = user.getPrimaryGroup();
        String onDuty = user.getCachedData().getMetaData().getMetaValue("onduty");
        return job.startsWith("medic") && "true".equals(onDuty);
    }
}