package com.thyagotoledo.companions.forge.client;

import com.thyagotoledo.companions.forge.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public final class CompanionCameraManager {
    private static Entity originalCameraEntity = null;
    private static CompanionEntity activeCompanion = null;

    private CompanionCameraManager() {
    }

    public static boolean isScouting() {
        return activeCompanion != null;
    }

    public static CompanionEntity getActiveCompanion() {
        return activeCompanion;
    }

    public static void startScouting(CompanionEntity companion) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || companion == null) return;
        if (originalCameraEntity == null) {
            originalCameraEntity = mc.getCameraEntity();
        }
        activeCompanion = companion;
        mc.setCameraEntity(companion);
        mc.player.sendSystemMessage(Component.translatable("message.companions.remote_view_active"));
    }

    public static void stopScouting() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (originalCameraEntity != null) {
            mc.setCameraEntity(originalCameraEntity);
        } else {
            mc.setCameraEntity(mc.player);
        }
        originalCameraEntity = null;
        activeCompanion = null;
        mc.player.sendSystemMessage(Component.translatable("message.companions.remote_view_ended"));
    }

    public static void checkSafetyTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || activeCompanion == null) return;

        if (mc.player.hurtTime > 0 || !activeCompanion.isAlive() || activeCompanion.isRemoved()) {
            stopScouting();
        }
    }
}
