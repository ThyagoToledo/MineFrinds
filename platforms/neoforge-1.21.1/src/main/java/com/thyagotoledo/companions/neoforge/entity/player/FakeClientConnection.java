package com.thyagotoledo.companions.neoforge.entity.player;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import java.lang.reflect.Field;
import java.net.SocketAddress;

/**
 * Conexao em memoria simulada para bots e companheiros (padrao Carpet Mod).
 * Permite que o ServerPlayer exista e seja adicionado a PlayerList sem necessitar
 * de conexao TCP de rede real com cliente externo.
 */
public class FakeClientConnection extends Connection {

    public FakeClientConnection(PacketFlow packetFlow) {
        super(packetFlow);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        try {
            Field channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            channelField.set(this, embeddedChannel);

            Field addressField = Connection.class.getDeclaredField("address");
            addressField.setAccessible(true);
            addressField.set(this, new SocketAddress() {
                @Override
                public String toString() {
                    return "local:companion_fake";
                }
            });
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isMemoryConnection() {
        return true;
    }

    @Override
    public void send(Packet<?> packet) {
        // No-op: descarta silenciosamente pacotes de saida para evitar vazamento ou travamento
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {
        if (listener != null) {
            try {
                listener.onSuccess();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener, boolean flush) {
        if (listener != null) {
            try {
                listener.onSuccess();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void handleDisconnection() {
        // No-op: previne fechamento inesperado
    }
}
