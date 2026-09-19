package com.thyagotoledo.companions.neoforge.entity.player;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Utilitario seguro para interagir com o servidor integrado e publicacao LAN no singleplayer.
 */
public class LanIntegrationHelper {

    /**
     * Tenta abrir o mundo singleplayer para LAN se ainda nao estiver publicado.
     * Retorna a porta TCP aberta ou -1 em caso de falha ou se for servidor dedicado.
     */
    public static int openWorldToLan(ServerPlayer owner, int desiredPort) {
        if (owner == null) return -1;
        MinecraftServer server = owner.getServer();
        if (server == null || !server.isSingleplayer()) {
            return -1;
        }

        try {
            if (server instanceof net.minecraft.client.server.IntegratedServer integratedServer) {
                if (integratedServer.isPublished()) {
                    return integratedServer.getPort();
                }

                int port = desiredPort > 0 ? desiredPort : 25565;
                boolean success = integratedServer.publishServer(GameType.SURVIVAL, false, port);
                if (success) {
                    int finalPort = integratedServer.getPort();
                    owner.sendSystemMessage(Component.literal("Mundo aberto para LAN com sucesso! Porta local: " + finalPort));
                    return finalPort;
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    public static boolean isLanPublished(MinecraftServer server) {
        if (server != null && server.isSingleplayer()) {
            try {
                if (server instanceof net.minecraft.client.server.IntegratedServer integratedServer) {
                    return integratedServer.isPublished();
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
