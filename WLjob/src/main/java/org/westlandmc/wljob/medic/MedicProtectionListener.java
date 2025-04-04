package org.westlandmc.wljob.medic;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.westlandmc.wljob.Main;

public class MedicProtectionListener implements Listener {

    private final Main plugin;

    public MedicProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    // زمانی که مدیک‌ها آسیب می‌بینند، اگر در حالت "آن دیوتی" باشند، آسیب را متوقف می‌کنیم.
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // بررسی وضعیت آن دیوتی
            if (plugin.isMedicOnDuty(player)) {
                event.setCancelled(true); // اگر مدیک باشد و آن دیوتی باشد، آسیب را لغو می‌کنیم.
            }
        }
    }

    // زمانی که مدیک به بازیکن دیگری آسیب می‌زند، اگر آن دیوتی باشد، آسیب را متوقف می‌کنیم.
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();

            // بررسی وضعیت آن دیوتی
            if (plugin.isMedicOnDuty(damager)) {
                event.setCancelled(true); // اگر مدیک باشد و آن دیوتی باشد، آسیب را لغو می‌کنیم.
            }
        }
    }
}