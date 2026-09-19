package com.thyagotoledo.companions.core.quest;

public class QuestReward {
    private final String id;
    private final String rewardItemId;
    private final int count;

    public QuestReward(String id, String rewardItemId, int count) {
        this.id = id != null ? id : "reward_default";
        this.rewardItemId = rewardItemId != null ? rewardItemId : "minecraft:air";
        this.count = Math.max(1, count);
    }

    public String getId() {
        return id;
    }

    public String getRewardItemId() {
        return rewardItemId;
    }

    public int getCount() {
        return count;
    }
}
