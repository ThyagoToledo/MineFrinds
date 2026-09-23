package com.thyagotoledo.companions.neoforge.service;

/** Limite global de consultas de minério por tick do servidor. */
public final class MiningScanBudget {
    private static final int MAX_PER_TICK = 2048;
    private static long tick = Long.MIN_VALUE;
    private static int used;

    private MiningScanBudget() { }

    public static boolean tryAcquire(long currentTick) {
        if (currentTick != tick) {
            tick = currentTick;
            used = 0;
        }
        if (used >= MAX_PER_TICK) return false;
        used++;
        return true;
    }

    public static int maxPerTick() {
        return MAX_PER_TICK;
    }

    public static void resetForTests() {
        tick = Long.MIN_VALUE;
        used = 0;
    }
}
