package com.thyagotoledo.companions.forge.service;

import com.thyagotoledo.companions.core.permissions.PermissionService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.UUID;

public class ForgePermissionService implements PermissionService {
    private final ServerLevel level;

    public ForgePermissionService(ServerLevel level) {
        this.level = level;
    }

    @Override
    public boolean canInteractAt(UUID ownerUuid, String dimension, int x, int y, int z) {
        return canBreakBlockAt(ownerUuid, dimension, x, y, z);
    }

    @Override
    public boolean canBreakBlockAt(UUID ownerUuid, String dimension, int x, int y, int z) {
        if (level == null || ownerUuid == null) return false;
        if (level.getServer() == null) return false;

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (player == null) return false;

        BlockPos pos = new BlockPos(x, y, z);
        if (!level.mayInteract(player, pos)) {
            return false;
        }

        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
        MinecraftForge.EVENT_BUS.post(breakEvent);
        return !breakEvent.isCanceled();
    }
}
