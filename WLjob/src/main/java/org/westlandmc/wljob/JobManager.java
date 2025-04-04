package org.westlandmc.wljob;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.*;

public class JobManager {
    private final Map<UUID, PlayerJobData> playerDataMap = new HashMap<>();
    private final Main plugin;
    private final Map<String, List<String>> jobRanks = new HashMap<>();

    public JobManager(Main plugin) {
        this.plugin = plugin;
        loadJobRanks();
    }

    public static class PlayerJobData {
        private final UUID playerId;
        private String jobName;
        private String rank;
        private boolean onDuty;
        private long lastSalaryTime;
        private double totalEarnings;

        public PlayerJobData(UUID playerId) {
            this.playerId = playerId;
            this.lastSalaryTime = System.currentTimeMillis();
            this.totalEarnings = 0;
        }

        public UUID getPlayerId() { return playerId; }
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public String getRank() { return rank; }
        public void setRank(String rank) { this.rank = rank; }
        public boolean isOnDuty() { return onDuty; }
        public void setOnDuty(boolean onDuty) { this.onDuty = onDuty; }
        public long getLastSalaryTime() { return lastSalaryTime; }
        public void setLastSalaryTime(long lastSalaryTime) { this.lastSalaryTime = lastSalaryTime; }
        public double getTotalEarnings() { return totalEarnings; }
        public void addToEarnings(double amount) { this.totalEarnings += amount; }
    }

    private void loadJobRanks() {
        FileConfiguration config = plugin.getConfig();
        if (config.isConfigurationSection("jobs")) {
            for (String job : config.getConfigurationSection("jobs").getKeys(false)) {
                if (config.isConfigurationSection("jobs." + job + ".ranks")) {
                    List<String> ranks = new ArrayList<>(
                            config.getConfigurationSection("jobs." + job + ".ranks").getKeys(false)
                    );
                    jobRanks.put(job, ranks);
                }
            }
        }
    }

    public void setPlayerJob(UUID playerId, String jobName, String rank, boolean setOnDuty) {
        PlayerJobData data = playerDataMap.computeIfAbsent(playerId, k -> new PlayerJobData(playerId));
        data.setJobName(jobName);
        data.setRank(rank);
        data.setOnDuty(setOnDuty);
        data.setLastSalaryTime(System.currentTimeMillis()); // اضافه کردن زمان حقوق
    }

    public boolean hasJob(UUID playerId) {
        return playerDataMap.containsKey(playerId) && playerDataMap.get(playerId).getJobName() != null;
    }

    public PlayerJobData getPlayerData(UUID playerId) {
        return playerDataMap.get(playerId);
    }

    public String getPlayerJobName(UUID playerId) {
        PlayerJobData data = playerDataMap.get(playerId);
        return data != null ? data.getJobName() : null;
    }

    public String getPlayerRank(UUID playerId) {
        PlayerJobData data = playerDataMap.get(playerId);
        return data != null ? data.getRank() : null;
    }

    public void setOnDuty(UUID playerId, boolean onDuty) {
        PlayerJobData data = playerDataMap.get(playerId);
        if (data != null) {
            data.setOnDuty(onDuty);
        }
    }

    public boolean isOnDuty(UUID playerId) {
        PlayerJobData data = playerDataMap.get(playerId);
        return data != null && data.isOnDuty();
    }

    public boolean promotePlayer(UUID playerId) {
        PlayerJobData data = playerDataMap.get(playerId);
        if (data == null || data.getJobName() == null || data.getRank() == null) return false;

        List<String> ranks = jobRanks.get(data.getJobName());
        if (ranks == null) return false;

        int currentIndex = ranks.indexOf(data.getRank());
        if (currentIndex < 0 || currentIndex >= ranks.size() - 1) return false;

        data.setRank(ranks.get(currentIndex + 1));
        return true;
    }
    public boolean demotePlayer(UUID playerId) {
        PlayerJobData data = playerDataMap.get(playerId);
        if (data == null || data.getJobName() == null || data.getRank() == null) return false;

        List<String> ranks = jobRanks.get(data.getJobName());
        if (ranks == null) return false;

        int currentIndex = ranks.indexOf(data.getRank());
        if (currentIndex <= 0) return false;

        data.setRank(ranks.get(currentIndex - 1));
        return true;
    }

    public String getJobPrefix(String jobName) {
        return plugin.getConfig().getString("jobs." + jobName + ".prefix", "");
    }

    public String getRankDisplay(String jobName, String rank) {
        return plugin.getConfig().getString(
                "jobs." + jobName + ".ranks." + rank + ".display",
                "&7[" + rank + "]"
        );
    }

    public double getRankSalary(String jobName, String rank) {
        return plugin.getConfig().getDouble(
                "salary.amounts." + jobName + "." + rank,
                0.0
        );
    }

    public void saveToConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("players", null);

        for (Map.Entry<UUID, PlayerJobData> entry : playerDataMap.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerJobData data = entry.getValue();
            config.set(path + ".job", data.getJobName());
            config.set(path + ".rank", data.getRank());
            config.set(path + ".onDuty", data.isOnDuty());
            config.set(path + ".lastSalary", data.getLastSalaryTime());
            config.set(path + ".totalEarnings", data.getTotalEarnings());
        }
        plugin.saveConfig();
    }

    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        if (config.isConfigurationSection("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                PlayerJobData data = new PlayerJobData(uuid);
                data.setJobName(config.getString("players." + key + ".job"));
                data.setRank(config.getString("players." + key + ".rank"));
                data.setOnDuty(config.getBoolean("players." + key + ".onDuty"));
                data.setLastSalaryTime(config.getLong("players." + key + ".lastSalary"));
                data.addToEarnings(config.getDouble("players." + key + ".totalEarnings"));
                playerDataMap.put(uuid, data);
            }
        }
    }

    public void resetPlayerJob(UUID playerId) {
        PlayerJobData data = playerDataMap.get(playerId);
        if (data != null) {
            data.setJobName(null);
            data.setRank(null);
            data.setOnDuty(false);
            data.setLastSalaryTime(0);
            data.addToEarnings(-data.getTotalEarnings()); // Reset earnings
        }
    }

    public List<String> getAvailableJobs() {
        return new ArrayList<>(jobRanks.keySet());
    }

    public List<String> getJobRanks(String jobName) {
        return new ArrayList<>(jobRanks.getOrDefault(jobName, Collections.emptyList()));
    }

    public boolean paySalary(UUID playerId) {
        PlayerJobData data = playerDataMap.get(playerId);
        if (data == null || !data.isOnDuty()) return false;

        double amount = getRankSalary(data.getJobName(), data.getRank());
        if (amount <= 0) return false;

        plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(playerId), amount);
        data.setLastSalaryTime(System.currentTimeMillis());
        data.addToEarnings(amount);

        return true;
    }
}