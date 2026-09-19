package com.thyagotoledo.companions.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.neoforge.client.skin.SkinCacheManager;
import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import com.thyagotoledo.companions.neoforge.entity.player.CompanionServerPlayer;
import com.thyagotoledo.companions.neoforge.tensura.TensuraCompanionStats;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Arvore completa de comandos e manipulador de eventos de chat para o mod Companions:
 * - /companion spawn, /companion lan, /companion recall, /companion dismiss
 * - /companion mode <follow|stay|defend>, /companion action <wood|mine>
 * - /companion inventory, /companion deposit, /companion view
 * - /companion skin <nome>, /skin <nome>
 * - /companion tensura <status|name>
 * - Ouvinte de chat em tempo real (ServerChatEvent) para comandos de voz.
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
                        .then(Commands.literal("mode")
                                .then(Commands.literal("follow").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.FOLLOW)))
                                .then(Commands.literal("stay").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.STAY)))
                                .then(Commands.literal("defend").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.DEFEND)))
                        )
                        .then(Commands.literal("action")
                                .then(Commands.literal("wood").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.WOOD)))
                                .then(Commands.literal("mine").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.MINE)))
                                .then(Commands.literal("farm").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.FARM)))
                        )
                        .then(Commands.literal("inventory")
                                .executes(ctx -> executeInventory(ctx.getSource()))
                        )
                        .then(Commands.literal("deposit")
                                .executes(ctx -> executeDeposit(ctx.getSource()))
                        )
                        .then(Commands.literal("chest")
                                .executes(ctx -> executeChest(ctx.getSource()))
                        )
                        .then(Commands.literal("bau")
                                .executes(ctx -> executeChest(ctx.getSource()))
                        )
                        .then(Commands.literal("craft")
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .executes(ctx -> executeCraft(ctx.getSource(), StringArgumentType.getString(ctx, "item"), 1))
                                        .then(Commands.argument("quantidade", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> executeCraft(ctx.getSource(), StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "quantidade")))
                                        )
                                )
                        )
                        .then(Commands.literal("view")
                                .executes(ctx -> executeView(ctx.getSource()))
                        )
                        .then(Commands.literal("chat")
                                .then(Commands.argument("msg", StringArgumentType.greedyString())
                                        .executes(ctx -> executeChat(ctx.getSource(), StringArgumentType.getString(ctx, "msg")))
                                )
                        )
                        .then(Commands.literal("skin")
                                .executes(ctx -> executeSkin(ctx.getSource(), "reset"))
                                .then(Commands.argument("nome", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SKIN_SUGGESTIONS, builder))
                                        .executes(ctx -> executeSkin(ctx.getSource(), StringArgumentType.getString(ctx, "nome")))
                                )
                        )
                        .then(Commands.literal("tensura")
                                .then(Commands.literal("status").executes(ctx -> executeTensuraStatus(ctx.getSource())))
                                .then(Commands.literal("name")
                                        .executes(ctx -> executeTensuraName(ctx.getSource(), null))
                                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                                .executes(ctx -> executeTensuraName(ctx.getSource(), StringArgumentType.getString(ctx, "nome")))
                                        )
                                )
                        )
        );

        // Atalhos Globais
        dispatcher.register(
                Commands.literal("companions")
                        .executes(ctx -> {
                            sendHelpMessage(ctx.getSource());
                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("skin")
                        .executes(ctx -> executeSkin(ctx.getSource(), "reset"))
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SKIN_SUGGESTIONS, builder))
                                .executes(ctx -> executeSkin(ctx.getSource(), StringArgumentType.getString(ctx, "nome")))
                        )
        );

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
        if (recalled) {
            source.sendSuccess(() -> Component.literal("Companheiro chamado com seguranca para perto de voce."), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Nao foi possivel trazer o companheiro: verifique se ele esta em combate, sem chao seguro, em outra dimensao ou em cooldown."));
            return 0;
        }
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

    private static int executeSetMode(CommandSourceStack source, CompanionMode mode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        companion.setMode(mode);
        String desc = switch (mode) {
            case FOLLOW -> "Seguir o jogador";
            case STAY -> "Aguardar no local (Ficar aqui)";
            case DEFEND -> "Postura defensiva de guarda";
            case WOOD -> "Coleta de madeira nas proximidades";
            case MINE -> "Mineracao de minerios nas proximidades";
            case FARM -> "Agricultura e colheita nas plantacoes";
            default -> mode.name();
        };

        source.sendSuccess(() -> Component.literal("Modo do companheiro alterado para: " + desc), true);
        companion.speakToOwner("Modo alterado para: " + desc + ".");
        return 1;
    }

    private static int executeInventory(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        companion.openCompanionInventory(player);
        return 1;
    }

    private static int executeDeposit(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        int moved = companion.depositToNearbyChest();
        source.sendSuccess(() -> Component.literal("Companheiro descarregou " + moved + " itens em bau proximo."), true);
        return 1;
    }

    private static int executeChest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        BlockPos targetChestPos = null;
        for (BlockPos p : BlockPos.betweenClosed(player.blockPosition().offset(-4, -2, -4), player.blockPosition().offset(4, 2, 4))) {
            if (player.serverLevel().getBlockEntity(p) instanceof net.minecraft.world.Container) {
                targetChestPos = p.immutable();
                break;
            }
        }

        if (targetChestPos != null) {
            CompanionManager.setDesignatedChest(player.getUUID(), targetChestPos);
            final BlockPos savedPos = targetChestPos;
            source.sendSuccess(() -> Component.literal("Bau designado registrado em [" + savedPos.getX() + ", " + savedPos.getY() + ", " + savedPos.getZ() + "]! O companheiro usara este bau para guardar e buscar itens."), true);
            CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
            if (companion != null) {
                companion.speakToOwner("Entendido! Registrei este bau como nosso estoque e deposito principal.");
            }
            return 1;
        } else {
            source.sendFailure(Component.literal("Nenhum bau encontrado por perto (fique a ate 4 blocos de um bau ou barril)."));
            return 0;
        }
    }

    private static int executeCraft(CommandSourceStack source, String item, int quantity) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[MineFriends] Modo experimental de crafting autonomo seguro ativado para " + quantity + "x " + item + "."), false);
        companion.executeAutonomousCraft(player, item, quantity);
        return 1;
    }

    private static int executeView(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        if (player.getCamera() == companion) {
            player.setCamera(player);
            source.sendSuccess(() -> Component.literal("Voce retornou para a sua propria visao de camera."), false);
            return 1;
        }

        player.setCamera(companion);
        source.sendSuccess(() -> Component.literal("Visualizando pelos olhos de " + companion.getName().getString() + ". Digite /companion view novamente ou agache (Shift) para sair."), false);
        return 1;
    }

    private static int executeSkin(CommandSourceStack source, String skinName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Este comando so pode ser executado por um jogador."));
            return 0;
        }

        if (skinName.equalsIgnoreCase("reset") || skinName.equalsIgnoreCase("self") || skinName.equalsIgnoreCase("player")) {
            CompanionManager.updateCompanionSkin(player, "reset");
            source.sendSuccess(() -> Component.literal("Aparencia do companheiro redefinida para a sua skin original de jogador."), true);
            return 1;
        }

        if (!SkinCacheManager.isValidSkinName(skinName)) {
            source.sendFailure(Component.literal("Nome de skin invalido! Use de 1 a 16 caracteres alfanumericos. Exemplo: /skin Rimuru"));
            return 0;
        }

        CompanionManager.updateCompanionSkin(player, skinName);
        source.sendSuccess(() -> Component.literal("Skin do companheiro alterada para: " + skinName + "."), true);
        return 1;
    }

    private static int executeChat(CommandSourceStack source, String message) {
        ServerPlayer player = source.getPlayer();
        if (player == null || message == null || message.trim().isEmpty()) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        processDirectOrder(player, companion, message.trim());
        return 1;
    }

    private static int executeTensuraStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        NeoForgeCompanionEntity dataEntity = companion != null ? companion.getDataEntity() : CompanionManager.getCompanionForOwner(player.getUUID());

        if (dataEntity != null && dataEntity.getTensuraStats() != null) {
            TensuraCompanionStats stats = dataEntity.getTensuraStats();
            source.sendSuccess(() -> Component.literal("=== STATUS TENSURA DO COMPANHEIRO ==="), false);
            source.sendSuccess(() -> Component.literal("Nome: " + dataEntity.getName()), false);
            source.sendSuccess(() -> Component.literal("Raca: " + stats.getRace().getDisplayName("pt_br")), false);
            source.sendSuccess(() -> Component.literal("Rank: " + stats.getRank()), false);
            source.sendSuccess(() -> Component.literal("EP: " + stats.getExistenceValue() + " | Magiculas: " + (int) stats.getMagicule()), false);
        } else {
            source.sendSuccess(() -> Component.literal("Status: O modpack atual esta em modo Vanilla (sem estatisticas de Tensura)."), false);
        }
        return 1;
    }

    private static int executeTensuraName(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        NeoForgeCompanionEntity dataEntity = companion != null ? companion.getDataEntity() : CompanionManager.getCompanionForOwner(player.getUUID());

        if (dataEntity != null && dataEntity.getTensuraStats() != null) {
            String newName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Benimaru";
            boolean success = dataEntity.getTensuraStats().bestowName(newName);
            if (success) {
                source.sendSuccess(() -> Component.literal("Cerimonia de Nomear concluida! O companheiro agora se chama " + newName + " e sua raca evoluiu para " + dataEntity.getTensuraStats().getRace().getDisplayName("pt_br") + "!"), true);
            } else {
                source.sendFailure(Component.literal("O companheiro ja recebeu um nome anteriormente ou o nome fornecido e invalido."));
            }
        } else {
            source.sendFailure(Component.literal("A cerimonia de nomear requer o modpack Tensura Neo Otherworld ativo."));
        }
        return 1;
    }

    /**
     * Ouvinte global de eventos de chat do servidor.
     * Intercepta mensagens enviadas pelo dono e comanda o companheiro de forma inteligente.
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) return;

        String rawText = event.getRawText();
        if (rawText == null || rawText.trim().isEmpty()) return;

        boolean handled = processDirectOrder(player, companion, rawText.trim());
        if (handled) {
            // Ordem compreendida com sucesso
        }
    }

    private static boolean processDirectOrder(ServerPlayer player, CompanionServerPlayer companion, String rawText) {
        String lower = rawText.toLowerCase(Locale.ROOT);

        if (lower.contains("me segue") || lower.contains("vem comigo") || lower.equals("follow") || lower.equals("seguir")) {
            companion.setMode(CompanionMode.FOLLOW);
            companion.speakToOwner("Entendido! Estou te seguindo.");
            return true;
        }

        if (lower.contains("fica aqui") || lower.contains("espera") || lower.equals("stay") || lower.equals("parar")) {
            companion.setMode(CompanionMode.STAY);
            companion.speakToOwner("Certo! Vou aguardar aqui nesta posicao.");
            return true;
        }

        if (lower.contains("defenda") || lower.contains("proteja") || lower.equals("defend") || lower.contains("guarda")) {
            companion.setMode(CompanionMode.DEFEND);
            companion.speakToOwner("Postura de combate ativada! Vou te proteger de monstros.");
            return true;
        }

        if (lower.contains("pega madeira") || lower.contains("corta madeira") || lower.contains("coleta madeira") || lower.equals("wood")) {
            companion.setMode(CompanionMode.WOOD);
            companion.speakToOwner("Iniciando coleta de madeira nas proximidades!");
            return true;
        }

        if (lower.contains("minerar") || lower.contains("pega minerio") || lower.equals("mine") || lower.contains("mina")) {
            companion.setMode(CompanionMode.MINE);
            companion.speakToOwner("Iniciando mineracao de minerios proximos!");
            return true;
        }

        if (lower.contains("plantar") || lower.contains("colher") || lower.contains("fazenda") || lower.equals("farm") || lower.contains("agricultura")) {
            companion.setMode(CompanionMode.FARM);
            companion.speakToOwner("Iniciando trabalho de agricultura! Vou colher safras maduras e replantar sementes.");
            return true;
        }

        if (lower.contains("marcar bau") || lower.contains("este e o bau") || lower.contains("salvar bau") || lower.contains("definir bau")) {
            executeChest(player.createCommandSourceStack());
            return true;
        }

        if (lower.startsWith("fabrica ") || lower.startsWith("fabricar ") || lower.startsWith("faz uma ") || lower.startsWith("faz um ") || lower.startsWith("faz ")) {
            String item = rawText.replaceFirst("(?i)^(fabrica|fabricar|faz uma|faz um|faz)\\s+", "").trim();
            if (!item.isEmpty()) {
                companion.executeAutonomousCraft(player, item, 1);
                return true;
            }
        }

        if (lower.contains("sair da camera") || lower.contains("minha visao") || lower.contains("voltar visao")) {
            player.setCamera(player);
            companion.speakToOwner("Restaurando visao de camera para o seu jogador.");
            return true;
        }

        if (lower.contains("vem ca") || lower.contains("venha aqui") || lower.equals("recall")) {
            companion.recallToOwner();
            companion.speakToOwner("Ja cheguei ao seu lado!");
            return true;
        }

        if (lower.contains("guardar") || lower.contains("deposito") || lower.contains("bau")) {
            companion.depositToNearbyChest();
            return true;
        }

        if (lower.contains("inventario") || lower.contains("mochila") || lower.contains("bolsa")) {
            companion.openCompanionInventory(player);
            return true;
        }

        if (lower.contains("visao") || lower.contains("olhar") || lower.contains("camera")) {
            if (player.getCamera() == companion) {
                player.setCamera(player);
                companion.speakToOwner("Restaurando visao de camera para o seu jogador.");
            } else {
                player.setCamera(companion);
                companion.speakToOwner("Conectado a visao remota. Digite /companion view novamente ou agache para sair.");
            }
            return true;
        }

        // Se o jogador estiver conversando diretamente
        if (lower.startsWith("ei ") || lower.startsWith("companheiro") || lower.endsWith("?")) {
            NeoForgeCompanionEntity dataEntity = companion.getDataEntity();
            if (dataEntity != null && dataEntity.getDialogueProvider() != null) {
                dataEntity.getDialogueProvider().processAsync(rawText, "pt_br", null, null)
                        .thenAccept(response -> {
                            if (response != null && response.getSpeech() != null) {
                                companion.speakToOwner(response.getSpeech());
                            }
                        });
                return true;
            }
        }

        return false;
    }

    public static void sendHelpMessage(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== GUIA DO MOD COMPANIONS (MINEFRIENDS) ==="), false);
        source.sendSuccess(() -> Component.literal("Como Abrir o Painel de Controle:"), false);
        source.sendSuccess(() -> Component.literal("  - Pressione a tecla C a qualquer momento durante o jogo."), false);
        source.sendSuccess(() -> Component.literal("  - Ou clique no icone [C] na lateral do seu inventario (tecla E)."), false);
        source.sendSuccess(() -> Component.literal("  - Ou digite /companion gui."), false);
        source.sendSuccess(() -> Component.literal("Comandos de Acao e Modos:"), false);
        source.sendSuccess(() -> createClickableCommand("/companion spawn [nome]", "Invoca o companheiro como jogador oficial no servidor", "/companion spawn "), false);
        source.sendSuccess(() -> createClickableCommand("/companion lan [nome]", "Abre o mundo para LAN e invoca o companheiro", "/companion lan "), false);
        source.sendSuccess(() -> createClickableCommand("/companion mode <follow|stay|defend>", "Altera o comportamento do companheiro", "/companion mode "), false);
        source.sendSuccess(() -> createClickableCommand("/companion action <wood|mine|farm>", "Ordena corte de madeira, mineracao ou colheita/plantio", "/companion action "), false);
        source.sendSuccess(() -> createClickableCommand("/companion chest", "Define o bau proximo como estoque e deposito principal", "/companion chest"), false);
        source.sendSuccess(() -> createClickableCommand("/companion craft <item> [qtd]", "Fabrica itens autonomamente (coleta na floresta/mina se faltar)", "/companion craft "), false);
        source.sendSuccess(() -> createClickableCommand("/companion inventory", "Abre o inventario completo com armaduras e mochila", "/companion inventory"), false);
        source.sendSuccess(() -> createClickableCommand("/companion deposit", "Guarda itens coletados no bau designado ou proximo", "/companion deposit"), false);
        source.sendSuccess(() -> createClickableCommand("/companion view", "Alterna visao remota da camera do companheiro", "/companion view"), false);
        source.sendSuccess(() -> createClickableCommand("/companion recall", "Chama o companheiro para perto de voce", "/companion recall"), false);
        source.sendSuccess(() -> createClickableCommand("/companion dismiss", "Dispensa o companheiro do servidor", "/companion dismiss"), false);
        source.sendSuccess(() -> createClickableCommand("/skin <nome>", "Altera a skin em tempo real (ex: Rimuru, Goku, Luffy)", "/skin "), false);
        source.sendSuccess(() -> createClickableCommand("/skin reset", "Restaura para a sua propria skin", "/skin reset"), false);
        source.sendSuccess(() -> Component.literal("Comandos por Chat de Voz:"), false);
        source.sendSuccess(() -> Component.literal("  Digite 'me segue', 'fica aqui', 'defenda', 'pega madeira', 'minerar', 'plantar', 'marcar bau', 'fabrica <item>', 'guardar' ou 'mochila'."), false);
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
