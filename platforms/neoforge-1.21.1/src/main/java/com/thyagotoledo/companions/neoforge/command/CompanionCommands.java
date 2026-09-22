package com.thyagotoledo.companions.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
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
 * - /companion mode <follow|stay|defend|auto>, /companion action <wood|mine>
 * - /companion inventory, /companion deposit, /companion view
 * - /companion skin <nome>, /skin <nome>
 * - /companion tensura <status|name>
 * - Ouvinte de chat em tempo real (ServerChatEvent) para comandos de voz.
 */
public class CompanionCommands {

    private static final List<String> SKIN_SUGGESTIONS = Arrays.asList(
            "reset", "self", "Rimuru", "Goku", "Luffy", "Naruto", "Kirito", "Gojo", "Zoro", "Tanjiro"
    );

    private static final List<String> MINE_PRIORITY_SUGGESTIONS = Arrays.asList(
            "diamante", "ferro", "carvao", "ouro", "redstone", "lapis", "netherite", "cobre", "all", "qualquer"
    );

    private static final List<String> FARM_MODE_SUGGESTIONS = Arrays.asList(
            "padrao", "arar", "standard", "till", "colher"
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
                        .then(Commands.literal("ai").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(CompanionManager.inferenceStatus()), false);
                            return 1;
                        }))
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
                                .then(Commands.literal("auto").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.WORK)))
                                .then(Commands.literal("follow").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.FOLLOW)))
                                .then(Commands.literal("stay").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.STAY)))
                                .then(Commands.literal("defend").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.DEFEND)))
                        )
                        .then(Commands.literal("action")
                                .then(Commands.literal("wood").executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.WOOD)))
                                .then(Commands.literal("mine")
                                        .executes(ctx -> executeMineCommand(ctx.getSource(), "all"))
                                        .then(Commands.argument("prioridade", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MINE_PRIORITY_SUGGESTIONS, builder))
                                                .executes(ctx -> executeMineCommand(ctx.getSource(), StringArgumentType.getString(ctx, "prioridade")))
                                        )
                                )
                                .then(Commands.literal("farm")
                                        .executes(ctx -> executeFarmCommand(ctx.getSource(), "padrao"))
                                        .then(Commands.argument("modo", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FARM_MODE_SUGGESTIONS, builder))
                                                .executes(ctx -> executeFarmCommand(ctx.getSource(), StringArgumentType.getString(ctx, "modo")))
                                        )
                                )
                        )
                        .then(Commands.literal("mine")
                                .executes(ctx -> executeMineCommand(ctx.getSource(), "all"))
                                .then(Commands.argument("prioridade", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MINE_PRIORITY_SUGGESTIONS, builder))
                                        .executes(ctx -> executeMineCommand(ctx.getSource(), StringArgumentType.getString(ctx, "prioridade")))
                                )
                        )
                        .then(Commands.literal("farm")
                                .executes(ctx -> executeFarmCommand(ctx.getSource(), "padrao"))
                                .then(Commands.argument("modo", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FARM_MODE_SUGGESTIONS, builder))
                                        .executes(ctx -> executeFarmCommand(ctx.getSource(), StringArgumentType.getString(ctx, "modo")))
                                )
                        )
                        .then(Commands.literal("wood")
                                .executes(ctx -> executeSetMode(ctx.getSource(), CompanionMode.WOOD))
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
                                .then(Commands.literal("observe").executes(ctx -> com.thyagotoledo.companions.neoforge.service.RemoteViewService.start(ctx.getSource().getPlayer(), false) ? 1 : 0))
                                .then(Commands.literal("control").executes(ctx -> com.thyagotoledo.companions.neoforge.service.RemoteViewService.start(ctx.getSource().getPlayer(), true) ? 1 : 0))
                                .then(Commands.literal("exit").executes(ctx -> com.thyagotoledo.companions.neoforge.service.RemoteViewService.stop(ctx.getSource().getPlayer()) ? 1 : 0))
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

        if (companion.getMode() == mode && mode != CompanionMode.FOLLOW) {
            companion.setMode(CompanionMode.FOLLOW);
            source.sendSuccess(() -> Component.literal("Modo desativado. Voltando a te seguir."), true);
            companion.speakToOwner("Modo desativado. Voltando a te seguir.");
            return 1;
        }

        companion.setMode(mode);
        String desc = switch (mode) {
            case FOLLOW -> "Seguir o jogador";
            case STAY -> "Aguardar no local (Ficar aqui)";
            case DEFEND -> "Postura defensiva de guarda";
            case WOOD -> "Coleta de madeira nas proximidades";
            case MINE -> "Mineracao de minerios nas proximidades";
            case FARM -> "Agricultura e colheita nas plantacoes";
            case WORK -> "Sobrevivencia automatica basica / Basic automatic survival";
            default -> mode.name();
        };

        source.sendSuccess(() -> Component.literal("Modo do companheiro alterado para: " + desc), true);
        companion.speakToOwner("Modo alterado para: " + desc + ".");
        return 1;
    }

    private static int executeMineCommand(CommandSourceStack source, String priority) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        String prio = (priority == null || priority.trim().isEmpty()) ? "all" : priority.trim().toLowerCase(Locale.ROOT);

        // Se ja estiver no modo MINE com a mesma prioridade, toggle para FOLLOW
        if (companion.getMode() == CompanionMode.MINE && prio.equalsIgnoreCase(companion.getMiningPriority())) {
            companion.setMode(CompanionMode.FOLLOW);
            source.sendSuccess(() -> Component.literal("Mineracao desativada. Voltando a te seguir."), true);
            companion.speakToOwner("Mineracao desativada. Voltando a te seguir.");
            return 1;
        }

        companion.setMiningPriority(prio);
        companion.setMode(CompanionMode.MINE);

        String prioName = formatPriorityDisplayName(prio);

        source.sendSuccess(() -> Component.literal("Modo de mineracao ativado com foco em: " + prioName + "."), true);
        companion.speakToOwner("Iniciando mineracao com foco em: " + prioName + ".");
        return 1;
    }

    public static String formatPriorityDisplayName(String prio) {
        if (prio == null || prio.trim().isEmpty() || prio.equalsIgnoreCase("all") || prio.equalsIgnoreCase("qualquer")) {
            return "Todos os minerios (por valor)";
        }
        String[] parts = prio.split("[,;+\\s|]+");
        List<String> formatted = new java.util.ArrayList<>();
        for (String part : parts) {
            String p = part.trim().toLowerCase(Locale.ROOT);
            if (p.isEmpty()) continue;
            String name = switch (p) {
                case "diamante", "diamond" -> "Diamantes";
                case "ferro", "iron" -> "Ferro";
                case "carvao", "coal" -> "Carvao";
                case "ouro", "gold" -> "Ouro";
                case "redstone" -> "Redstone";
                case "lapis" -> "Lapis-lazuli";
                case "netherite", "debris" -> "Netherite";
                case "cobre", "copper" -> "Cobre";
                default -> Character.toUpperCase(p.charAt(0)) + p.substring(1);
            };
            if (!formatted.contains(name)) {
                formatted.add(name);
            }
        }
        return formatted.isEmpty() ? "Todos os minerios (por valor)" : String.join(", ", formatted);
    }

    private static int executeFarmCommand(CommandSourceStack source, String modeArg) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionServerPlayer companion = CompanionManager.getPlayerCompanion(player.getUUID());
        if (companion == null) {
            source.sendFailure(Component.literal("Voce nao possui um companheiro ativo. Use /companion spawn primeiro."));
            return 0;
        }

        boolean till = modeArg != null && (modeArg.equalsIgnoreCase("arar") || modeArg.equalsIgnoreCase("till"));

        // Se ja estiver no modo FARM com a mesma configuracao, toggle para FOLLOW
        if (companion.getMode() == CompanionMode.FARM && companion.isFarmTillEnabled() == till) {
            companion.setMode(CompanionMode.FOLLOW);
            source.sendSuccess(() -> Component.literal("Agricultura desativada. Voltando a te seguir."), true);
            companion.speakToOwner("Agricultura desativada. Voltando a te seguir.");
            return 1;
        }

        companion.setFarmTillEnabled(till);
        companion.setMode(CompanionMode.FARM);

        if (till) {
            source.sendSuccess(() -> Component.literal("Modo de agricultura ativado com permissao de arar terra com enxada proxima a agua."), true);
            companion.speakToOwner("Iniciando agricultura com arado! Vou arar terra proxima a agua e plantar minhas sementes.");
        } else {
            source.sendSuccess(() -> Component.literal("Modo de agricultura padrao ativado (colheita e replantio apenas em terra arada existente)."), true);
            companion.speakToOwner("Iniciando agricultura conservadora! Vou colher safras maduras e replantar em canteiros existentes.");
        }
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
            CompanionManager.setDesignatedChest(player.getUUID(), targetChestPos, player.serverLevel());
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
        ServerPlayer owner = source.getPlayer();
        if (com.thyagotoledo.companions.neoforge.service.RemoteViewService.stop(owner)) return 1;
        if (!com.thyagotoledo.companions.neoforge.service.RemoteViewService.start(owner, false)) {
            source.sendFailure(Component.literal("Companheiro indisponivel / Companion unavailable."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Observando. Shift sai; /companion view control assume o controle."), false);
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
        String lower = rawText.toLowerCase(Locale.ROOT).trim()
                .replaceFirst("^(?:por favor|please)\\s+", "")
                .replaceFirst("\\s+(?:por favor|please|agora|now|amigo)[.!]?$", "")
                .replaceFirst("[.!]+$", "");
        if (lower.equals("auto") || lower.equals("jogue sozinho")) {
            companion.setMode(CompanionMode.WORK);
            companion.speakToOwner("Modo automatico basico / Basic automatic mode. Use stay para parar.");
            return true;
        }

        if (lower.equals("follow me") || lower.equals("me segue") || lower.equals("vem comigo") || lower.equals("follow") || lower.equals("seguir")) {
            companion.setMode(CompanionMode.FOLLOW);
            companion.speakToOwner("Entendido! Estou te seguindo.");
            return true;
        }

        if (lower.equals("stay here") || lower.equals("fica aqui") || lower.equals("espera") || lower.equals("stay") || lower.equals("parar")) {
            companion.setMode(CompanionMode.STAY);
            companion.speakToOwner("Certo! Vou aguardar aqui nesta posicao.");
            return true;
        }

        if (lower.equals("defenda") || lower.equals("proteja") || lower.equals("defend") || lower.equals("guarda")) {
            companion.setMode(CompanionMode.DEFEND);
            companion.speakToOwner("Postura de combate ativada! Vou te proteger de monstros.");
            return true;
        }

        if (lower.equals("pega madeira") || lower.equals("corta madeira") || lower.equals("coleta madeira") || lower.equals("wood") || lower.equals("gather wood")) {
            companion.setMode(CompanionMode.WOOD);
            companion.speakToOwner("Iniciando coleta de madeira nas proximidades!");
            return true;
        }

        if (lower.startsWith("minerar") || lower.startsWith("pega minerio") || lower.startsWith("mine") || lower.startsWith("mina")) {
            String prio = lower.replaceFirst("^(?:minerar|pega minerio|mine|mina)\\s*", "").trim();
            if (prio.isEmpty()) prio = "all";
            executeMineCommand(player.createCommandSourceStack(), prio);
            return true;
        }

        if (lower.startsWith("plantar") || lower.startsWith("colher") || lower.startsWith("fazenda") || lower.startsWith("farm") || lower.startsWith("agricultura") || lower.startsWith("arar")) {
            boolean till = lower.contains("arar") || lower.contains("till");
            executeFarmCommand(player.createCommandSourceStack(), till ? "arar" : "padrao");
            return true;
        }

        if (lower.equals("marcar bau") || lower.equals("este e o bau") || lower.equals("salvar bau") || lower.equals("definir bau")) {
            executeChest(player.createCommandSourceStack());
            return true;
        }

        if (lower.startsWith("craft ") || lower.startsWith("fabrica ") || lower.startsWith("fabricar ") || lower.startsWith("faz uma ") || lower.startsWith("faz um ") || lower.startsWith("faz ")) {
            String item = rawText.replaceFirst("(?i)^(craft|fabrica|fabricar|faz uma|faz um|faz)\\s+", "").trim();
            if (!item.isEmpty()) {
                companion.executeAutonomousCraft(player, item, 1);
                return true;
            }
        }

        if (lower.equals("sair da camera") || lower.equals("minha visao") || lower.equals("voltar visao")) {
            com.thyagotoledo.companions.neoforge.service.RemoteViewService.stop(player);
            companion.speakToOwner("Retornando ao seu local / Returning to your position.");
            return true;
        }

        if (lower.equals("vem ca") || lower.equals("venha aqui") || lower.equals("recall")) {
            companion.recallToOwner();
            companion.speakToOwner("Ja cheguei ao seu lado!");
            return true;
        }

        if (lower.equals("guardar") || lower.equals("deposito") || lower.equals("bau")) {
            companion.depositToNearbyChest();
            return true;
        }

        if (lower.equals("inventario") || lower.equals("mochila") || lower.equals("bolsa")) {
            companion.openCompanionInventory(player);
            return true;
        }

        if (lower.equals("visao") || lower.equals("olhar") || lower.equals("camera")) {
            executeView(player.createCommandSourceStack());
            return true;
        }

        // Se o jogador estiver conversando diretamente
        if (!rawText.trim().isEmpty()) {
            NeoForgeCompanionEntity dataEntity = companion.getDataEntity();
            if (dataEntity != null && dataEntity.getDialogueProvider() != null) {
                long requestRevision = CompanionManager.getRevision(player.getUUID());
                dataEntity.handleCommandAsync(rawText, player.clientInformation().language())
                        .thenAccept(response -> {
                            if (response != null && response.getSpeech() != null && player.getServer() != null) {
                                player.getServer().execute(() -> {
                                    if (player.isAlive() && companion.isAlive()
                                            && player.level() == companion.level()
                                            && CompanionManager.getPlayerCompanion(player.getUUID()) == companion
                                            && CompanionManager.getRevision(player.getUUID()) == requestRevision) {
                                        applyDialogueIntent(player, companion, response);
                                        companion.speakToOwner(response.getSpeech());
                                    }
                                });
                            }
                        });
                return true;
            }
        }

        return false;
    }

    /** Aplica somente intents aceitas pelo adapter local; o texto nunca vira código executável. */
    private static void applyDialogueIntent(ServerPlayer player, CompanionServerPlayer companion,
                                            DialogueResponse response) {
        if (response == null || response.getIntent() == null) return;
        switch (response.getIntent().getType()) {
            case FOLLOW_OWNER:
                companion.setMode(CompanionMode.FOLLOW);
                break;
            case STAY:
                companion.setMode(CompanionMode.STAY);
                break;
            case DEFEND:
                companion.setMode(CompanionMode.DEFEND);
                break;
            case CHOP_WOOD:
                companion.setMode(CompanionMode.WOOD);
                break;
            case MINE_BLOCK:
                executeMineCommand(player.createCommandSourceStack(), response.getIntent().getTarget() == null
                        ? "all" : response.getIntent().getTarget());
                break;
            case RECALL:
                companion.recallToOwner();
                break;
            case DEPOSIT_CHEST:
                companion.depositToNearbyChest();
                break;
            case OPEN_INVENTORY:
                companion.openCompanionInventory(player);
                break;
            case REMOTE_VIEW:
                executeView(player.createCommandSourceStack());
                break;
            default:
                // Conversa, status e intents sem executor não alteram o mundo.
                break;
        }
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
        source.sendSuccess(() -> createClickableCommand("/companion mode <follow|stay|defend|auto>", "Altera o comportamento do companheiro", "/companion mode "), false);
        source.sendSuccess(() -> createClickableCommand("/companion action <wood|mine|farm>", "Ordena corte de madeira, mineracao ou colheita/plantio", "/companion action "), false);
        source.sendSuccess(() -> createClickableCommand("/companion mine [prioridade]", "Mineracao estilo jogador com escadas e tuneis (diamante, ferro, carvao, etc.)", "/companion mine "), false);
        source.sendSuccess(() -> createClickableCommand("/companion farm [padrao|arar]", "Agricultura conservadora ou com arado de terra com enxada proxima a agua", "/companion farm "), false);
        source.sendSuccess(() -> createClickableCommand("/companion chest", "Define o bau proximo como estoque e deposito principal", "/companion chest"), false);
        source.sendSuccess(() -> createClickableCommand("/companion craft <item> [qtd]", "Fabrica com receitas da mochila e bancada proxima; tenta novamente por 60 segundos", "/companion craft "), false);
        source.sendSuccess(() -> createClickableCommand("/companion inventory", "Abre o inventario completo com armaduras e mochila", "/companion inventory"), false);
        source.sendSuccess(() -> createClickableCommand("/companion deposit", "Guarda itens coletados no bau designado ou proximo", "/companion deposit"), false);
        source.sendSuccess(() -> createClickableCommand("/companion view", "Alterna visao remota da camera do companheiro", "/companion view"), false);
        source.sendSuccess(() -> createClickableCommand("/companion recall", "Chama o companheiro para perto de voce", "/companion recall"), false);
        source.sendSuccess(() -> createClickableCommand("/companion dismiss", "Dispensa o companheiro do servidor", "/companion dismiss"), false);
        source.sendSuccess(() -> createClickableCommand("/skin <nome>", "Altera a skin em tempo real (ex: Rimuru, Goku, Luffy)", "/skin "), false);
        source.sendSuccess(() -> createClickableCommand("/skin reset", "Restaura para a sua propria skin", "/skin reset"), false);
        source.sendSuccess(() -> Component.literal("Comandos por Chat de Voz:"), false);
        source.sendSuccess(() -> Component.literal("  Digite 'me segue', 'fica aqui', 'defenda', 'pega madeira', 'minerar [minerio]', 'plantar', 'arar', 'marcar bau', 'fabrica <item>', 'guardar' ou 'mochila'."), false);
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
