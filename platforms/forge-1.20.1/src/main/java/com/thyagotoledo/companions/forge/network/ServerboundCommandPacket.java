package com.thyagotoledo.companions.forge.network;

import com.thyagotoledo.companions.forge.entity.CompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ServerboundCommandPacket {
    private final UUID companionUuid;
    private final String command;

    public ServerboundCommandPacket(UUID companionUuid, String command) {
        this.companionUuid = companionUuid != null ? companionUuid : new UUID(0L, 0L);
        this.command = command != null ? command : "";
    }

    public static void encode(ServerboundCommandPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.companionUuid);
        buf.writeUtf(msg.command, 256);
    }

    public static ServerboundCommandPacket decode(FriendlyByteBuf buf) {
        UUID companionUuid = buf.readUUID();
        String command = buf.readUtf(256);
        return new ServerboundCommandPacket(companionUuid, command);
    }

    public static void handle(ServerboundCommandPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            for (Entity entity : sender.serverLevel().getAllEntities()) {
                if (entity.getUUID().equals(msg.companionUuid) && entity instanceof CompanionEntity companion) {
                    if (companion.isOwnedBy(sender)) {
                        companion.handleCommand(msg.command, sender);
                    }
                    break;
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    public UUID getCompanionUuid() {
        return companionUuid;
    }

    public String getCommand() {
        return command;
    }
}
