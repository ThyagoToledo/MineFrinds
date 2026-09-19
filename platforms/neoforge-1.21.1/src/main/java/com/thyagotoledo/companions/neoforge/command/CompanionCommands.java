package com.thyagotoledo.companions.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registro de comandos do servidor e cliente para o mod Companions (/companion, /companion gui).
 */
public class CompanionCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("companion")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("Companions: Pressione a tecla C ou clique no icone [C] do inventario para abrir o Painel."), false);
                            return 1;
                        })
                        .then(Commands.literal("gui")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal("Companions: Abrindo painel de controle..."), false);
                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                Commands.literal("companions")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("Companions: Pressione a tecla C ou clique no icone [C] do inventario para abrir o Painel."), false);
                            return 1;
                        })
        );
    }
}
