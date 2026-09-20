package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Small ground-only search. Never loads chunks or digs a route. */
public final class LocalNavigation {
    private LocalNavigation() { }

    private static final class Node {
        final BlockPos pos;
        final double gScore;
        final double fScore;

        Node(BlockPos pos, double gScore, double fScore) {
            this.pos = pos;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    public static List<BlockPos> plan(ServerPlayer player, Vec3 target) {
        BlockPos start = player.blockPosition();
        BlockPos targetPos = BlockPos.containing(target);
        if (start.equals(targetPos)) return Collections.emptyList();

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        Map<BlockPos, Double> gScores = new HashMap<>();

        open.add(new Node(start, 0.0, start.getCenter().distanceTo(target)));
        parents.put(start, start);
        gScores.put(start, 0.0);

        BlockPos best = start;
        double bestDistSqr = start.getCenter().distanceToSqr(target);
        int expanded = 0;
        int maxRadius = 32;

        while (!open.isEmpty() && expanded++ < 384) {
            Node current = open.poll();
            BlockPos currentPos = current.pos;
            double currentDistSqr = currentPos.getCenter().distanceToSqr(target);
            if (currentDistSqr < bestDistSqr) {
                best = currentPos;
                bestDistSqr = currentDistSqr;
            }
            if (currentDistSqr < 2.25) {
                best = currentPos;
                break;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int dy : new int[] {0, 1, -1, -2}) {
                    BlockPos next = currentPos.relative(direction).offset(0, dy, 0);
                    if (Math.abs(next.getX() - start.getX()) > maxRadius
                            || Math.abs(next.getZ() - start.getZ()) > maxRadius
                            || Math.abs(next.getY() - start.getY()) > 16) continue;
                    if (parents.containsKey(next) || !safe(player, next)) continue;
                    if (dy > 0 && !isPassable(player.level(), currentPos.above(2))) continue;

                    double tentativeG = gScores.get(currentPos) + 1.0 + (dy != 0 ? 0.2 : 0.0);
                    if (tentativeG < gScores.getOrDefault(next, Double.MAX_VALUE)) {
                        parents.put(next, currentPos);
                        gScores.put(next, tentativeG);
                        double h = next.getCenter().distanceTo(target);
                        open.add(new Node(next, tentativeG, tentativeG + h));
                    }
                    break;
                }
            }
        }

        LinkedList<BlockPos> path = new LinkedList<>();
        BlockPos curr = best;
        while (!curr.equals(start)) {
            path.addFirst(curr);
            curr = parents.get(curr);
            if (curr == null) break;
        }
        return path;
    }

    public static boolean isPassable(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (isHazard(state)) return false;
        if (state.getCollisionShape(level, pos).isEmpty()) return true;
        if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) return true;
        if (state.is(net.minecraft.tags.BlockTags.CLIMBABLE)) return true;
        return false;
    }

    public static boolean isSafeFloor(Level level, BlockPos floorPos) {
        if (!level.hasChunkAt(floorPos)) return false;
        BlockState floor = level.getBlockState(floorPos);
        if (isHazard(floor)) return false;
        if (floor.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || floor.is(Blocks.WATER)) return true;
        if (floor.isAir()) return false;
        if (floor.is(Blocks.FARMLAND) || floor.is(Blocks.DIRT_PATH)) return true;
        if (!floor.getCollisionShape(level, floorPos).isEmpty()) return true;
        return floor.blocksMotion();
    }

    public static boolean isHazard(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW);
    }

    public static boolean safe(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        return isPassable(level, pos)
                && isPassable(level, pos.above())
                && isSafeFloor(level, pos.below());
    }
}
