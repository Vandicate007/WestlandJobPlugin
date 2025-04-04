package org.westlandmc.wljob;

import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.luckperms.api.model.user.User;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.westlandmc.wljob.Listeners.RankChangeListener;
import org.westlandmc.wljob.commands.*;
import org.westlandmc.wljob.medic.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
    private MedicGUI medicGUI;
    private Economy economy;
    private LuckPerms luckPerms;
    private Chat vaultChat;
    private static Main instance;
    private KnockdownListener knockdownListener; // ✅ اضافه شد
    private final HashMap<UUID, Long> activePlaytime = new HashMap<>();
    private final Set<UUID> knockedPlayers = new HashSet<>();
    private final Set<UUID> revivingPlayers = new HashSet<>();

    private double pillPrice;
    private int pillHealthRestore;
    private double medkitPrice;
    private double bandagePrice;
    private JobManager jobManager;
    private TagManager tagManager;
    private SalarySystem salarySystem;

    public static Main getInstance() {
        return instance;
    }

    // اضافه کردن متد برای دریافت KnockdownListener
    public KnockdownListener getKnockdownListener() {
        return knockdownListener;
    }

    // اضافه کردن متد برای دریافت knockedPlayers از KnockdownListener
    public Set<UUID> getKnockedPlayers() {
        return knockdownListener.getKnockedPlayers();
    }
    public void removeTrackedPlayer(UUID playerUUID) {
        if (medicGUI != null) {
            medicGUI.removeTracking(playerUUID);
        }
    }
    public void loadConfigValues() {
        pillPrice = getConfig().getDouble("items.pill.price", 50.0);
        pillHealthRestore = getConfig().getInt("items.healing_pill.health_restore", 4);
        bandagePrice = getConfig().getDouble("items.bandage.price", 30.0);
        medkitPrice = getConfig().getDouble("items.medkit.price", 100.0);
    }

    public double getPillPrice() {
        return pillPrice;
    }

    public int getPillHealthRestore() {
        return pillHealthRestore;
    }

    public double getBandagePrice() {
        return bandagePrice;
    }

    public double getMedkitPrice() {
        return medkitPrice;
    }

    @Override
    public void onEnable() {
        medicGUI = new MedicGUI(this);
        instance = this;
        saveDefaultConfig();
        loadConfigValues();
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[WLJob] Plugin is enabling...");
        this.jobManager = new JobManager(this);
        this.tagManager = new TagManager(this);
        this.salarySystem = new SalarySystem(this);
        jobManager.loadFromConfig();


        if (!setupEconomy() || !setupLuckPerms() || !setupVaultChat()) {
            getLogger().severe("[WLJob] Failed to initialize dependencies. Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        knockdownListener = new KnockdownListener(this, luckPerms);
        medicGUI = new MedicGUI(this);

        getServer().getPluginManager().registerEvents(knockdownListener, this);
        getServer().getPluginManager().registerEvents(medicGUI, this);
        getServer().getPluginManager().registerEvents(new MedicProtectionListener(this), this);
        MedicItemHandler medicItemHandler = new MedicItemHandler(this);
        getServer().getPluginManager().registerEvents(medicItemHandler, this);
        getServer().getPluginManager().registerEvents(new BandageListener(this), this);
        getServer().getPluginManager().registerEvents(new MedkitListener(this, knockdownListener, economy,luckPerms), this);


        registerCommands();
        getServer().getPluginManager().registerEvents(this, this);


        // 2. محاسبه interval به تیک (20 tick = 1 ثانیه)
        long intervalMinutes = getConfig().getLong("salary.interval", 30);
        long intervalTicks = intervalMinutes * 60 * 20;
        // 3. راه‌اندازی تسک تکراری
        salarySystem.runTaskTimer(this, intervalTicks, intervalTicks);
        getLogger().info("سیستم حقوق با موفقیت راه‌اندازی شد (هر " + intervalMinutes + " دقیقه)");



        new RankChangeListener(luckPerms);
    }

    @Override
    public void onDisable() {
        jobManager.saveToConfig(); // ذخیره داده‌ها هنگام خاموشی
    }

    private void registerCommands() {
        getCommand("promote").setExecutor(new PromoteCommand(this));
        getCommand("demote").setExecutor(new DemoteCommand(this));
        getCommand("onduty").setExecutor(new OnDutyCommand(this));
        getCommand("offduty").setExecutor(new OffDutyCommand(this));
        getCommand("joblist").setExecutor(new JobListCommand(luckPerms));
        getCommand("requestmedic").setExecutor(new RequestMedicCommand(knockdownListener, luckPerms));
        getCommand("knock").setExecutor(new KnockCommand(knockdownListener));
        getCommand("revive").setExecutor(new ReviveCommand(knockdownListener));
        getCommand("medicgui").setExecutor(new MedicGuiCommand(this, medicGUI));
        getCommand("getpill").setExecutor(new GetPillCommand(this));
        getCommand("getbandage").setExecutor(new GiveBandageCommand(this,economy));
        this.getCommand("accept").setExecutor(new AcceptCommand(this));
        getCommand("getmedkit").setExecutor(new MedkitCommand(this,economy));
        getCommand("setjob").setExecutor(new SetJobCommand(this));
        getCommand("firejob").setExecutor(new FireJobCommand(this));


        getCommand("wljobreload").setExecutor((sender, command, label, args) -> {
            reloadConfig();
            loadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "WLJob config reloaded!");
            return true;
        });
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> rsp = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (rsp == null) return false;
        luckPerms = rsp.getProvider();
        return luckPerms != null;
    }

    private boolean setupVaultChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) return false;
        vaultChat = rsp.getProvider();
        return vaultChat != null;
    }

    public Economy getEconomy() {
        return economy;
    }
    public Set<UUID> getRevivingPlayers() {
        return revivingPlayers;
    }

    // متد برای اضافه کردن بازیکن به لیست در حال ریوایو
    public void addRevivingPlayer(UUID playerId) {
        revivingPlayers.add(playerId);
    }

    // متد برای حذف بازیکن از لیست در حال ریوایو
    public void removeRevivingPlayer(UUID playerId) {
        revivingPlayers.remove(playerId);
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public Chat getVaultChat() {
        return vaultChat;
    }

    public JobManager getJobManager() { return jobManager; }
    public TagManager getTagManager() { return tagManager; }


    public boolean isMedicOnDuty(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return false;
        String job = user.getPrimaryGroup();
        String onDuty = user.getCachedData().getMetaData().getMetaValue("onduty");
        return job.startsWith("medic") && "true".equals(onDuty);
    }

    public boolean isPlayerOnDuty(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return user != null && "true".equals(user.getCachedData().getMetaData().getMetaValue("onduty"));
    }
}