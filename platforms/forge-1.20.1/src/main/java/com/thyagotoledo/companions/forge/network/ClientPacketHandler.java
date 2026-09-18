package com.thyagotoledo.companions.forge.network;

import com.thyagotoledo.companions.forge.client.CompanionCameraManager;
import com.thyagotoledo.companions.forge.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleFeedback(ClientboundFeedbackPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (msg.getSpeech() != null && !msg.getSpeech().isEmpty()) {
            mc.player.sendSystemMessage(Component.literal(msg.getSpeech()));
        }

        if (msg.isToggleRemoteView()) {
            if (CompanionCameraManager.isScouting()) {
                CompanionCameraManager.stopScouting();
            } else if (mc.level != null) {
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.getUUID().equals(msg.getCompanionUuid()) && entity instanceof CompanionEntity companion) {
                        CompanionCameraManager.startScouting(companion);
                        break;
                    }
                }
            }
        }
    }
}
