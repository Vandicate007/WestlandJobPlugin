package org.westlandmc.wljob.Listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.track.UserPromoteEvent;
import net.luckperms.api.event.user.track.UserDemoteEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.MetaNode;

import java.util.Optional;

public class RankChangeListener {

    private final LuckPerms luckPerms;

    public RankChangeListener(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
        registerListener();
    }

    private void registerListener() {
        EventBus eventBus = luckPerms.getEventBus();

        // شنود تغییرات در رنک بازیکن
        eventBus.subscribe(UserPromoteEvent.class, this::onRankChange);
        eventBus.subscribe(UserDemoteEvent.class, this::onRankChange);
    }

    private void onRankChange(UserPromoteEvent event) {
        Optional<String> optionalRank = event.getGroupTo();
        optionalRank.ifPresent(rank -> updateRankTime(event.getUser(), rank));
    }

    private void onRankChange(UserDemoteEvent event) {
        Optional<String> optionalRank = event.getGroupTo();
        optionalRank.ifPresent(rank -> updateRankTime(event.getUser(), rank));
    }

    private void updateRankTime(User user, String newRank) {
        if (newRank.startsWith("medic") || newRank.startsWith("news") || newRank.startsWith("sheriff")) {
            setRankTime(user, newRank);
        }
    }

    private void setRankTime(User user, String rank) {
        if (isPlayerOffDuty(user)) {
            System.out.println("[DEBUG] Player " + user.getUsername() + " is Off-Duty, skipping rank time update.");
            return;
        }

        String metaKey = "rank_time_" + rank;
        long currentTime = System.currentTimeMillis();

        user.data().clear(n -> n instanceof MetaNode && ((MetaNode) n).getMetaKey().equals(metaKey));
        MetaNode newMeta = MetaNode.builder(metaKey, String.valueOf(currentTime)).build();
        user.data().add(newMeta);

        luckPerms.getUserManager().saveUser(user);
    }
    private boolean isPlayerOffDuty(User user) {
        return user.getNodes().stream()
                .filter(node -> node instanceof MetaNode)
                .map(node -> (MetaNode) node)
                .anyMatch(metaNode -> metaNode.getMetaKey().equals("onduty") && metaNode.getMetaValue().equals("false"));
    }
}