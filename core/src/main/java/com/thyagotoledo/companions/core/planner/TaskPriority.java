package com.thyagotoledo.companions.core.planner;

public enum TaskPriority {
    EMERGENCY(0),
    SELF_DEFENSE(1),
    OWNER_ORDER(2),
    MISSION_WORK(3),
    IDLE_ROAM(4);

    private final int level;

    TaskPriority(int level) {
        this.level = level;
    }

    public boolean isHigherThan(TaskPriority other) {
        return this.level < other.level;
    }
}
