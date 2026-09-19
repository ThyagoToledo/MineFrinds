package com.thyagotoledo.companions.core.quest;

public class QuestTask {
    public enum Type {
        ITEM,
        KILL,
        CUSTOM
    }

    private final String id;
    private final Type type;
    private final String targetId;
    private final int requiredCount;
    private int completedCount;

    public QuestTask(String id, Type type, String targetId, int requiredCount) {
        this.id = id != null ? id : "task_default";
        this.type = type != null ? type : Type.ITEM;
        this.targetId = targetId != null ? targetId : "minecraft:air";
        this.requiredCount = Math.max(1, requiredCount);
        this.completedCount = 0;
    }

    public boolean isCompleted() {
        return completedCount >= requiredCount;
    }

    public int getRemainingCount() {
        return Math.max(0, requiredCount - completedCount);
    }

    public void recordProgress(int count) {
        this.completedCount += count;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getTargetId() {
        return targetId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }
}
