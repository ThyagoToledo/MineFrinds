package com.thyagotoledo.companions.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientboundFeedbackPacket {
    private final UUID companionUuid;
    private final String speech;
    private final boolean toggleRemoteView;

    public ClientboundFeedbackPacket(UUID companionUuid, String speech, boolean toggleRemoteView) {
        this.companionUuid = companionUuid != null ? companionUuid : new UUID(0L, 0L);
        this.speech = speech != null ? speech : "";
        this.toggleRemoteView = toggleRemoteView;
    }

    public static void encode(ClientboundFeedbackPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.companionUuid);
        buf.writeUtf(msg.speech, 512);
        buf.writeBoolean(msg.toggleRemoteView);
    }

    public static ClientboundFeedbackPacket decode(FriendlyByteBuf buf) {
        UUID companionUuid = buf.readUUID();
        String speech = buf.readUtf(512);
        boolean toggleRemoteView = buf.readBoolean();
        return new ClientboundFeedbackPacket(companionUuid, speech, toggleRemoteView);
    }

    public static void handle(ClientboundFeedbackPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleFeedback(msg));
        });
        ctx.setPacketHandled(true);
    }

    public UUID getCompanionUuid() {
        return companionUuid;
    }

    public String getSpeech() {
        return speech;
    }

    public boolean isToggleRemoteView() {
        return toggleRemoteView;
    }
}
