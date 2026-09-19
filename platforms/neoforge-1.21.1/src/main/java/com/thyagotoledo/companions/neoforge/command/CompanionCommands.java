package com.thyagotoledo.companions.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thyagotoledo.companions.neoforge.client.skin.SkinCacheManager;
import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Arvore completa de comandos para o mod Companions:
 * /companion spawn, /companion recall, /companion skin, /skin, /companion help e /companion gui.
 */
public class CompanionCommands {

    private static final List<String> SKIN_SUGGESTIONS = Arrays.asList(
            "reset", "self", "Rimuru", "Goku", "Luffy", "Naruto", "Kirito", "Gojo", "Zoro", "Tanjiro"
    );

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        register(dispatcher);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Comando Principal: /companion
        dispatcher.register(
                Commands.literal("companion")
                        .executes(ctx -> {
                            sendHelpMessage(ctx.getSource());
                            return 1;
                        })
                        .then(Commands.literal("help")
                                .executes(ctx -> {
                                    sendHelpMessage(ctx.getSource());
                                    return 1;
                                })
                        )
                        .then(Commands.literal("gui")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal("Companions: Pressione C ou clique no icone [C] do inventario para abrir o painel."), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("spawn")
                                .executes(ctx -> executeSpawn(ctx.getSource(), null, false))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> executeSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "nome"), false))
                                )
                        )
                        .then(Commands.literal("lan")
                                .executes(ctx -> executeSpawn(ctx.getSource(), null, true))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> executeSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "nome"), true))
                                )
                        )
                        .then(Commands.literal("recall")
                                .executes(ctx -> executeRecall(ctx.getSource()))
                        )
                        .then(Commands.literal("dismiss")
                                .executes(ctx -> executeDismiss(ctx.getSource()))
                        )
                        .then(Commands.literal("skin")
                                .executes(ctx -> executeSkin(ctx.getSource(), "reset"))
                                .then(Commands.argument("nome", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SKIN_SUGGESTIONS, builder))
                                        .executes(ctx -> executeSkin(ctx.getSource(), StringArgumentType.getString(ctx, "nome")))
                                )
                        )
        );

        // Atalho: /companions
        dispatcher.register(
                Commands.literal("companions")
                        .executes(ctx -> {
                            sendHelpMessage(ctx.getSource());
                            return 1;
                        })
        );

        // Atalho Global: /skin <nome>
        dispatcher.register(
                Commands.literal("skin")
                        .executes(ctx -> executeSkin(ctx.getSource(), "reset"))
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SKIN_SUGGESTIONS, builder))
                                .executes(ctx -> executeSkin(ctx.getSource(), StringArgumentType.getString(ctx, "nome")))
                        )
        );

        // Atalho de Ajuda: /help companions
        dispatcher.register(
                Commands.literal("help")
                        .then(Commands.literal("companions")
                                .executes(ctx -> {
                                    sendHelpMessage(ctx.getSource());
                                    return 1;
                                })
                        )
        );
    }

    private static int executeSpawn(CommandSourceStack source, String customName, boolean openLan) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Este comando so pode ser executado por um jogador."));
            return 0;
        }

        String chosenName = (customName != null && !customName.trim().isEmpty())
                ? customName.trim()
                : "Companheiro";

        try {
            CompanionManager.spawnPlayerCompanion(player, chosenName, openLan);
            source.sendSuccess(() -> Component.literal("Companheiro \"" + chosenName + "\" entrou no jogo como jogador oficial!"), true);
            source.sendSuccess(() -> Component.literal("Pressione Tab para ve-lo na lista de jogadores ou C para abrir as acoes."), false);
            return 1;
        } catch (Throwable t) {
            NeoForgeCompanionEntity companion = CompanionManager.spawnCompanion(player.getUUID(), chosenName);
            companion.setCustomSkin("");
            source.sendSuccess(() -> Component.literal("Companheiro \"" + companion.getName() + "\" invocado com sucesso!"), true);
            return 1;
        }
    }

    private static int executeRecall(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Este comando so pode ser executado por um jogador."));
            return 0;
        }

        boolean recalled = CompanionManager.recallPlayerCompanion(player);
        if (!recalled) {
            NeoForgeCompanionEntity companion = CompanionManager.getCompanionForOwner(player.getUUID());
            if (companion == null) {
                source.sendFailure(Component.literal("Voce ainda nao possui um companheiro. Use /companion spawn para invocar um."));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.literal("Companheiro chamado com seguranca para perto de voce."), true);
        return 1;
    }

    private static int executeDismiss(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Este comando so pode ser executado por um jogador."));
            return 0;
        }

        boolean dismissed = CompanionManager.dismissPlayerCompanion(player.getUUID());
        if (dismissed) {
            source.sendSuccess(() -> Component.literal("Companheiro dispensado do servidor com sucesso."), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Voce nao possui nenhum companheiro ativo no momento."));
            return 0;
        }
    }

    private static int executeSkin(CommandSourceStack source, String skinName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Este comando so pode ser executado por um jogador."));
            return 0;
        }

        NeoForgeCompanionEntity companion = CompanionManager.getOrCreateCompanion(player.getUUID(), null);

        if (skinName.equalsIgnoreCase("reset") || skinName.equalsIgnoreCase("self") || skinName.equalsIgnoreCase("player")) {
            companion.setCustomSkin("");
            source.sendSuccess(() -> Component.literal("Aparencia do companheiro redefinida para a sua skin original de jogador."), true);
            return 1;
        }

        if (!SkinCacheManager.isValidSkinName(skinName)) {
            source.sendFailure(Component.literal("Nome de skin invalido! Use de 1 a 16 caracteres alfanumericos. Exemplo: /skin Rimuru"));
            return 0;
        }

        companion.setCustomSkin(skinName);
        SkinCacheManager.getOrFetchSkin(skinName);

        source.sendSuccess(() -> Component.literal("Skin do companheiro alterada para: " + skinName + ". Baixando e aplicando textura..."), true);
        return 1;
    }

    public static void sendHelpMessage(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== GUIA DO MOD COMPANIONS (MINEFRIENDS) ==="), false);
        source.sendSuccess(() -> Component.literal("Como Abrir o Painel de Controle:"), false);
        source.sendSuccess(() -> Component.literal("  - Pressione a tecla C a qualquer momento durante o jogo."), false);
        source.sendSuccess(() -> Component.literal("  - Ou clique no icone [C] na lateral do seu inventario (tecla E)."), false);
        source.sendSuccess(() -> Component.literal("  - Ou digite /companion gui."), false);
        source.sendSuccess(() -> Component.literal("Comandos Principais:"), false);
        source.sendSuccess(() -> createClickableCommand("/companion spawn [nome]", "Invoca o companheiro como jogador oficial no servidor", "/companion spawn "), false);
        source.sendSuccess(() -> createClickableCommand("/companion lan [nome]", "Abre o mundo para LAN e invoca o companheiro", "/companion lan "), false);
        source.sendSuccess(() -> createClickableCommand("/companion recall", "Chama o companheiro para perto de voce", "/companion recall"), false);
        source.sendSuccess(() -> createClickableCommand("/companion dismiss", "Dispensa o companheiro do servidor", "/companion dismiss"), false);
        source.sendSuccess(() -> createClickableCommand("/skin <nome>", "Clique para personalizar a skin", "/skin "), false);
        source.sendSuccess(() -> createClickableCommand("/skin reset", "Restaura para a sua propria skin", "/skin reset"), false);
        source.sendSuccess(() -> Component.literal("Dica: Voce tambem pode falar diretamente com ele digitando ordens no chat (ex: 'me segue', 'fica aqui', 'pega madeira', 'visao')."), false);
        source.sendSuccess(() -> Component.literal("============================================"), false);
    }

    private static Component createClickableCommand(String text, String tooltip, String commandToSuggest) {
        return Component.literal("  " + text)
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandToSuggest))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tooltip)))
                );
    }
}
