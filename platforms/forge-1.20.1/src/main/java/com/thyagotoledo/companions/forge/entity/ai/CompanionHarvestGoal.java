package com.thyagotoledo.companions.forge.entity.ai;

import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.work.WorkArea;
import com.thyagotoledo.companions.core.work.WorkTask;
import com.thyagotoledo.companions.forge.entity.CompanionEntity;
import com.thyagotoledo.companions.forge.service.ForgePermissionService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class CompanionHarvestGoal extends Goal {
    private final CompanionEntity companion;
    private BlockPos targetPos = null;
    private int breakCooldown = 0;

    public CompanionHarvestGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.getMode() != CompanionMode.WORK) return false;
        WorkTask task = companion.getActiveWorkTask();
        return task != null && !task.isCompleted() && !task.isCancelled();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && targetPos != null;
    }

    @Override
    public void start() {
        findNextTarget();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.breakCooldown = 0;
        this.companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        WorkTask task = companion.getActiveWorkTask();
        if (task == null || task.isCompleted() || task.isCancelled()) {
            stop();
            return;
        }

        if (targetPos == null || companion.level().getBlockState(targetPos).isAir()) {
            findNextTarget();
            if (targetPos == null) return;
        }

        companion.getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        double distSq = companion.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);

        if (distSq > 9.0D) {
            companion.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D, 1.0D);
            return;
        }

        companion.getNavigation().stop();
        if (breakCooldown > 0) {
            breakCooldown--;
            return;
        }

        if (companion.level() instanceof ServerLevel serverLevel) {
            ForgePermissionService permService = new ForgePermissionService(serverLevel);
            String dim = serverLevel.dimension().location().toString();

            if (!permService.canBreakBlockAt(companion.getOwnerUUID(), dim, targetPos.getX(), targetPos.getY(), targetPos.getZ())) {
                task.cancel();
                companion.setMode(CompanionMode.FOLLOW);
                companion.speakKey("task.blocked.claim");
                stop();
                return;
            }

            BlockState state = serverLevel.getBlockState(targetPos);
            serverLevel.destroyBlock(targetPos, true, companion);
            task.recordHarvest(1);

            ItemStack mainHand = companion.getMainHandItem();
            if (mainHand.isDamageableItem()) {
                mainHand.hurtAndBreak(1, companion, e -> {});
            }

            breakCooldown = 15;

            if (task.isCompleted()) {
                companion.setMode(CompanionMode.FOLLOW);
                companion.speakKey("dialogue.deposit_ack");
                stop();
            } else {
                targetPos = null;
            }
        }
    }

    private void findNextTarget() {
        WorkTask task = companion.getActiveWorkTask();
        if (task == null) return;

        WorkArea area = task.getArea();
        if (area == null) return;

        BlockPos center = new BlockPos(area.getCenterX(), area.getCenterY(), area.getCenterZ());
        int radius = area.getRadius();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    BlockState state = companion.level().getBlockState(candidate);
                    if (!state.isAir()) {
                        String blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                        if (blockId.equals(task.getTargetBlockId()) || matchesCategory(blockId, task.getTargetBlockId())) {
                            this.targetPos = candidate;
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean matchesCategory(String actualBlockId, String targetId) {
        if (targetId.contains("log") && actualBlockId.contains("log")) return true;
        if (targetId.contains("ore") && actualBlockId.contains("ore")) return true;
        return actualBlockId.equals(targetId);
    }
}
