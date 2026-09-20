package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Small ground-only search. Never loads chunks or digs a route. */
public final class LocalNavigation {
    private LocalNavigation() { }

    public static List<BlockPos> plan(ServerPlayer player, Vec3 target) {
        BlockPos start = player.blockPosition();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        open.add(start);
        parents.put(start, start);
        BlockPos best = start;
        double distance = start.getCenter().distanceToSqr(target);
        int expanded = 0;
        while (!open.isEmpty() && expanded++ < 256) {
            BlockPos current = open.removeFirst();
            double score = current.getCenter().distanceToSqr(target);
            if (score < distance) { best = current; distance = score; }
            if (score < 2.0) break;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int dy : new int[] {0, 1, -1}) {
                    BlockPos next = current.relative(direction).offset(0, dy, 0);
                    if (Math.abs(next.getX() - start.getX()) > 12 || Math.abs(next.getZ() - start.getZ()) > 12
                            || parents.containsKey(next) || !safe(player, next)) continue;
                    if (dy > 0 && !player.level().getBlockState(current.above(2)).isAir()) continue;
                    parents.put(next, current);
                    open.addLast(next);
                    break;
                }
            }
        }
        LinkedList<BlockPos> path = new LinkedList<>();
        while (!best.equals(start)) { path.addFirst(best); best = parents.get(best); }
        return path;
    }

    public static boolean safe(ServerPlayer player, BlockPos pos) {
        if (!player.level().hasChunkAt(pos)) return false;
        var floor = player.level().getBlockState(pos.below());
        return player.level().getBlockState(pos).isAir()
                && player.level().getBlockState(pos.above()).isAir()
                && floor.isSolidRender(player.level(), pos.below())
                && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CACTUS)
                && !floor.is(Blocks.CAMPFIRE) && !floor.is(Blocks.SOUL_CAMPFIRE);
    }
}
