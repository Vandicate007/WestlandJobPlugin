package org.westlandmc.wljob;

import java.util.UUID;

public class PlayerJobData {
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