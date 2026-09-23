package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningProgressWatchdogTest {
    @Test
    void doesNotComparePositionWithSameTickSample() {
        MiningProgressWatchdog watchdog = new MiningProgressWatchdog(3);
        BlockPos target = new BlockPos(2, 2, 2);
        assertEquals(MiningProgressWatchdog.State.RESET,
                watchdog.observe(1, target, Vec3.ZERO, 0.0f, false).state());
        assertEquals(MiningProgressWatchdog.State.WAITING,
                watchdog.observe(2, target, Vec3.ZERO, 0.0f, false).state());
        assertEquals(MiningProgressWatchdog.State.WAITING,
                watchdog.observe(3, target, Vec3.ZERO, 0.0f, false).state());
        assertEquals(MiningProgressWatchdog.State.STUCK,
                watchdog.observe(4, target, Vec3.ZERO, 0.0f, false).state());
    }

    @Test
    void movementAndBreakProgressResetStallCounter() {
        MiningProgressWatchdog watchdog = new MiningProgressWatchdog(2);
        BlockPos target = new BlockPos(2, 2, 2);
        watchdog.observe(1, target, Vec3.ZERO, 0.0f, true);
        watchdog.observe(2, target, Vec3.ZERO, 0.0f, true);
        assertEquals(MiningProgressWatchdog.State.PROGRESS,
                watchdog.observe(3, target, new Vec3(0.25, 0.0, 0.0), 0.0f, true).state());
        assertEquals(0, watchdog.stalledTicks());
        assertEquals(MiningProgressWatchdog.State.PROGRESS,
                watchdog.observe(4, target, new Vec3(0.25, 0.0, 0.0), 0.2f, true).state());
    }

    @Test
    void changingTargetResetsObservation() {
        MiningProgressWatchdog watchdog = new MiningProgressWatchdog(1);
        watchdog.observe(1, new BlockPos(1, 1, 1), Vec3.ZERO, 0.0f, true);
        assertEquals(MiningProgressWatchdog.State.RESET,
                watchdog.observe(2, new BlockPos(2, 1, 1), Vec3.ZERO, 0.0f, true).state());
    }
}
