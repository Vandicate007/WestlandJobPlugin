package org.westlandmc.wljob.commands;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.westlandmc.wljob.Main;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class JobListCommand implements CommandExecutor {

    private final LuckPerms luckPerms;
    private final Map<String, Integer> rankPriorityMap = new HashMap<>();

    public JobListCommand(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
        loadRankPriority();
    }

    private void loadRankPriority() {
        List<String> priorityList = Main.getInstance().getConfig().getStringList("rank_priority");
        for (int i = 0; i < priorityList.size(); i++) {
            rankPriorityMap.put(priorityList.get(i).toLowerCase(), i);
        }
        Bukkit.getLogger().info("[WLJob] Loaded rank priority: " + rankPriorityMap);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 1) {
            sender.sendMessage(getPrefix("general") + ChatColor.RED + "Usage: /joblist [job]");
            return false;
        }

        boolean isAdmin = sender.hasPermission("wljob.joblist.admin");
        boolean isPlayer = sender.hasPermission("wljob.joblist.player");
        String requestedJob = (args.length == 1) ? args[0].toLowerCase() : null;

        if (!isAdmin && !isPlayer) {
            sender.sendMessage(getPrefix("general") + ChatColor.RED + "You don't have permission to view job members.");
            return false;
        }

        org.bukkit.entity.Player player = null;
        if (requestedJob == null) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(getPrefix("general") + ChatColor.RED + "You must be a player to see your job members.");
                return false;
            }

            player = (org.bukkit.entity.Player) sender;
            String currentJob = getJobCategory(getCurrentRank(luckPerms.getUserManager().getUser(player.getUniqueId())));

            if (currentJob == null) {
                sender.sendMessage(getPrefix("general") + ChatColor.RED + "You are not assigned to any job.");
                return false;
            }

            requestedJob = currentJob;
        }


        if (!isAdmin && !isInJobCategory(sender, requestedJob)) {
            sender.sendMessage(getPrefix("general") + ChatColor.RED + "You are not in the requested job.");
            return false;
        }

        String finalRequestedJob = requestedJob;

        CompletableFuture.runAsync(() -> {
            List<String> jobMembers = getJobMembers(finalRequestedJob);
            if (jobMembers.isEmpty()) {
                sender.sendMessage(getPrefix(finalRequestedJob) + ChatColor.RED + "No players found in this job.");
                return;
            }

            sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "----------------------------------");
            sender.sendMessage(getPrefix(finalRequestedJob) + ChatColor.BOLD + " Members of " + finalRequestedJob.toUpperCase() + ":");
            for (String memberInfo : jobMembers) {
                sender.sendMessage(memberInfo);
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "----------------------------------");
        });

        return true;
    }

    private String getJobCategory(String rank) {
        if (rank == null) return null;

        rank = rank.toLowerCase();

        if (rank.startsWith("medic")) return "medic";
        if (rank.startsWith("sheriff")) return "sheriff";
        if (rank.startsWith("news")) return "news";

        return null;
    }private boolean isInJobCategory(CommandSender sender, String job) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            return false;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        String currentJobCategory = getJobCategory(getCurrentRank(luckPerms.getUserManager().getUser(player.getUniqueId())));

        return currentJobCategory != null && currentJobCategory.equalsIgnoreCase(job);
    }

    private List<String> getJobMembers(String jobCategory) {
        List<String> members = new ArrayList<>();
        List<CompletableFuture<User>> offlineUsers = new ArrayList<>();

        for (User user : luckPerms.getUserManager().getLoadedUsers()) {
            addUserToList(user, jobCategory, members);
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (!luckPerms.getUserManager().isLoaded(offlinePlayer.getUniqueId())) {
                offlineUsers.add(luckPerms.getUserManager().loadUser(offlinePlayer.getUniqueId()));
            }
        }

        CompletableFuture.allOf(offlineUsers.toArray(new CompletableFuture[0])).join();
        for (CompletableFuture<User> futureUser : offlineUsers) {
            User user = futureUser.join();
            if (user != null) {
                addUserToList(user, jobCategory, members);
            }
        }

        members.sort((a, b) -> {
            String rankA = extractRank(a);
            String rankB = extractRank(b);
            int priorityA = rankPriorityMap.getOrDefault(rankA, Integer.MAX_VALUE);
            int priorityB = rankPriorityMap.getOrDefault(rankB, Integer.MAX_VALUE);
            return Integer.compare(priorityA, priorityB);
        });

        return members;
    }

    private String extractRank(String memberInfo) {
        if (memberInfo.contains("❰") && memberInfo.contains("❱")) {
            return memberInfo.substring(memberInfo.indexOf("❰") + 1, memberInfo.indexOf("❱"));
        }
        return memberInfo;
    }

    private void addUserToList(User user, String jobCategory, List<String> members) {
        String rank = getCurrentRank(user);
        if (rank != null && getJobCategory(rank) != null && getJobCategory(rank).equalsIgnoreCase(jobCategory)) {
            String playerName = (user.getUsername() != null) ? user.getUsername() : ChatColor.RED + "[Offline]";
            String dutyStatus = getDutyStatus(user);
            String rankName = getRankName(rank);
            String suffix = getSuffix(user);

            members.add(ChatColor.YELLOW + " • " + ChatColor.WHITE + playerName
                    + " " + ChatColor.BLUE + "❰" + rankName + "❱"
                    + " " + ChatColor.YELLOW + "STATUS" + " " + ChatColor.BOLD + "➔" + " " + dutyStatus);
        }
    }

    private String getDutyStatus(User user) {
        String status = user.getCachedData().getMetaData().getMetaValue("onduty");
        return (status != null && status.equals("true")) ? ChatColor.GREEN + "[On Duty]" : ChatColor.RED + "[Off Duty]";
    }

    private String getRankName(String rank) {
        return Main.getInstance().getConfig().getString("ranks." + rank, rank);
    }

    private String getSuffix(User user) {
        String suffix = user.getCachedData().getMetaData().getSuffix();
        return (suffix != null && !suffix.isEmpty()) ? ChatColor.GRAY + "[" + ChatColor.GOLD + suffix + ChatColor.GRAY + "]" : ChatColor.DARK_GRAY + "[No Rank]";
    }

    private String getCurrentRank(User user) {
        return (user != null && user.getPrimaryGroup() != null) ? user.getPrimaryGroup().toLowerCase() : null;
    }

    private String getPrefix(String jobType) {
        switch (jobType) {
            case "sheriff":
                return ChatColor.YELLOW + "«[Sheriff-Department]» " + ChatColor.RESET;
            case "medic":return ChatColor.LIGHT_PURPLE + "«[Medic-Department]» " + ChatColor.RESET;
            case "news":
                return ChatColor.GRAY + "«[News-Department]» " + ChatColor.RESET;
            default:
                return ChatColor.DARK_GRAY + "«[System]» " + ChatColor.RESET;
        }
    }
}