package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/** Estado pequeno e independente do controlador de mineração do companheiro. */
public final class MiningController {
    private final MiningProgressWatchdog watchdog = new MiningProgressWatchdog(80);
    private final Deque<String> events = new ArrayDeque<>(12);
    private MiningProgressWatchdog.Observation lastObservation =
            new MiningProgressWatchdog.Observation(MiningProgressWatchdog.State.RESET, 0, 0L);

    public MiningProgressWatchdog.Observation observe(long tick, BlockPos target, Vec3 position,
                                                        float progress, boolean active) {
        lastObservation = watchdog.observe(tick, target, position, progress, active);
        if (lastObservation.state() == MiningProgressWatchdog.State.STUCK) {
            record("stuck target=" + target + " ticks=" + lastObservation.stalledTicks());
        }
        return lastObservation;
    }

    public void record(String event) {
        if (event == null || event.isBlank()) return;
        if (events.size() >= 12) events.removeFirst();
        events.addLast(event);
    }

    public void reset() {
        watchdog.reset();
        lastObservation = new MiningProgressWatchdog.Observation(
                MiningProgressWatchdog.State.RESET, 0, lastObservation.tick());
        record("reset");
    }

    public String debugSummary() {
        return "watchdog=" + lastObservation.state()
                + ", stalled=" + lastObservation.stalledTicks()
                + ", tick=" + lastObservation.tick()
                + ", events=" + String.join(" | ", events);
    }

    public boolean isStuck() {
        return lastObservation.state() == MiningProgressWatchdog.State.STUCK;
    }

    public int stalledTicks() {
        return lastObservation.stalledTicks();
    }
}
