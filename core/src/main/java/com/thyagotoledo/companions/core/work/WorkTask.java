package com.thyagotoledo.companions.core.work;

public class WorkTask {
    private final String taskId;
    private final String targetBlockId;
    private final int targetCount;
    private final ToolType requiredTool;
    private final WorkArea area;
    private int collectedCount = 0;
    private boolean cancelled = false;

    public WorkTask(String taskId, String targetBlockId, int targetCount, ToolType requiredTool, WorkArea area) {
        this.taskId = taskId != null ? taskId : "task_default";
        this.targetBlockId = targetBlockId != null ? targetBlockId : "minecraft:oak_log";
        this.targetCount = Math.max(1, targetCount);
        this.requiredTool = requiredTool != null ? requiredTool : ToolType.NONE;
        this.area = area;
    }

    public boolean isCompleted() {
        return collectedCount >= targetCount;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public void recordHarvest(int count) {
        this.collectedCount += count;
    }

    public int getRemainingCount() {
        return Math.max(0, targetCount - collectedCount);
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTargetBlockId() {
        return targetBlockId;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getCollectedCount() {
        return collectedCount;
    }

    public ToolType getRequiredTool() {
        return requiredTool;
    }

    public WorkArea getArea() {
        return area;
    }
}
