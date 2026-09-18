package com.thyagotoledo.companions.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CompanionsNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("companions", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private CompanionsNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ServerboundCommandPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundCommandPacket::encode)
                .decoder(ServerboundCommandPacket::decode)
                .consumerMainThread(ServerboundCommandPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClientboundFeedbackPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundFeedbackPacket::encode)
                .decoder(ClientboundFeedbackPacket::decode)
                .consumerMainThread(ClientboundFeedbackPacket::handle)
                .add();
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
