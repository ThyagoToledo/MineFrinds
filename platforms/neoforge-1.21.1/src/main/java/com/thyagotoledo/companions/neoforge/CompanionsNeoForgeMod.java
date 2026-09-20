package com.thyagotoledo.companions.neoforge;

import com.thyagotoledo.companions.neoforge.client.CompanionsClientEvents;
import com.thyagotoledo.companions.neoforge.command.CompanionCommands;
import com.thyagotoledo.companions.neoforge.network.NeoForgeCompanionPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Ponto de entrada oficial do mod Companions para NeoForge 21.1.248+ (Minecraft 1.21.1).
 * Carregado pelo javafml do NeoForge.
 */
@Mod(CompanionsNeoForgeMod.MODID)
public class CompanionsNeoForgeMod {

    public static final String MODID = "companions";
    public static final String VERSION = "0.2.0-alpha.2-1.21.1";
    public static final String LOADER = "neoforge";
    public static final String MINECRAFT_VERSION = "1.21.1";

    public CompanionsNeoForgeMod(IEventBus modEventBus) {
        // Registro de manipuladores de comandos e eventos de jogo
        NeoForge.EVENT_BUS.register(CompanionCommands.class);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(com.thyagotoledo.companions.neoforge.service.RemoteViewService.class);

        // Registro de ouvintes client-side (GUI, atalho de tecla e botao de inventario estilo FTB)
        if (modEventBus != null) {
            modEventBus.addListener(NeoForgeCompanionPayloads::onRegisterPayloadHandlers);
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                NeoForge.EVENT_BUS.register(CompanionsClientEvents.class);
                modEventBus.addListener(CompanionsClientEvents::onRegisterKeyMappings);
                modEventBus.addListener(CompanionsClientEvents::onAddLayers);
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        com.thyagotoledo.companions.neoforge.entity.CompanionManager.startInference();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        com.thyagotoledo.companions.neoforge.entity.CompanionManager.shutdownInference();
    }
}
