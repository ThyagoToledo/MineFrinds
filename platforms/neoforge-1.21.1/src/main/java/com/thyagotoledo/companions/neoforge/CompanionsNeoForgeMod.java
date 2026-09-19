package com.thyagotoledo.companions.neoforge;

import com.thyagotoledo.companions.neoforge.client.CompanionsClientEvents;
import com.thyagotoledo.companions.neoforge.command.CompanionCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Ponto de entrada oficial do mod Companions para NeoForge 21.1.248+ (Minecraft 1.21.1).
 * Carregado pelo javafml do NeoForge.
 */
@Mod(CompanionsNeoForgeMod.MODID)
public class CompanionsNeoForgeMod {

    public static final String MODID = "companions";
    public static final String VERSION = "0.1.0-1.21.1";
    public static final String LOADER = "neoforge";
    public static final String MINECRAFT_VERSION = "1.21.1";

    public CompanionsNeoForgeMod(IEventBus modEventBus) {
        // Registro de manipuladores de comandos e eventos de jogo
        NeoForge.EVENT_BUS.register(CompanionCommands.class);

        // Registro de ouvintes client-side (GUI, atalho de tecla e botao de inventario estilo FTB)
        NeoForge.EVENT_BUS.register(CompanionsClientEvents.class);
        if (modEventBus != null) {
            modEventBus.addListener(CompanionsClientEvents::onRegisterKeyMappings);
        }
    }
}
