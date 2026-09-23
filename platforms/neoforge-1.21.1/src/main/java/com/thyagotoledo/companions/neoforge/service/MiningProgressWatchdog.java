package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Observa progresso entre ticks consecutivos sem comparar a posição com uma
 * amostra criada no mesmo tick. A classe não altera o mundo e pode ser
 * exercitada em testes unitários.
 */
public final class MiningProgressWatchdog {
    public enum State { RESET, PROGRESS, WAITING, STUCK }

    public record Observation(State state, int stalledTicks, long tick) { }

    private final int stallLimit;
    private BlockPos previousTarget;
    private Vec3 previousPosition;
    private float previousBreakProgress;
    private int stalledTicks;

    public MiningProgressWatchdog(int stallLimit) {
        this.stallLimit = Math.max(1, stallLimit);
    }

    public Observation observe(long tick, BlockPos target, Vec3 position,
                               float breakProgress, boolean workActive) {
        if (target == null || position == null) {
            reset();
            return new Observation(State.RESET, 0, tick);
        }

        if (previousTarget == null || !previousTarget.equals(target) || previousPosition == null) {
            previousTarget = target.immutable();
            previousPosition = position;
            previousBreakProgress = breakProgress;
            stalledTicks = 0;
            return new Observation(State.RESET, 0, tick);
        }

        boolean moved = position.distanceToSqr(previousPosition) >= 0.04;
        boolean broke = breakProgress > previousBreakProgress + 0.01f;
        boolean progress = moved || broke;
        if (progress) {
            stalledTicks = 0;
        } else if (workActive || target.equals(previousTarget)) {
            stalledTicks++;
        }

        previousTarget = target.immutable();
        previousPosition = position;
        previousBreakProgress = breakProgress;
        State state = progress ? State.PROGRESS
                : (stalledTicks >= stallLimit ? State.STUCK : State.WAITING);
        return new Observation(state, stalledTicks, tick);
    }

    public void reset() {
        previousTarget = null;
        previousPosition = null;
        previousBreakProgress = 0.0f;
        stalledTicks = 0;
    }

    public int stalledTicks() {
        return stalledTicks;
    }
}
