package com.thyagotoledo.companions.core.dialogue;

import com.thyagotoledo.companions.core.decision.DecisionCandidate;
import com.thyagotoledo.companions.core.decision.ChoiceQuestion;
import com.thyagotoledo.companions.core.decision.DecisionProvider;
import com.thyagotoledo.companions.core.decision.DecisionRequest;
import com.thyagotoledo.companions.core.decision.DecisionResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptera o contrato de decisão fechado para intents do diálogo. O provider
 * só pode devolver IDs desta lista; ele nunca recebe ou executa objetos do jogo.
 */
public final class IntentDecisionProvider {
    private static final List<DecisionCandidate> CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            new DecisionCandidate("FOLLOW_OWNER", "follow me seguir acompanhar jogador"),
            new DecisionCandidate("STAY", "stay here ficar aqui esperar parar"),
            new DecisionCandidate("DEFEND", "defend protect defender proteger guardar"),
            new DecisionCandidate("COLLECT_ITEMS", "collect items pegar coletar itens drops"),
            new DecisionCandidate("CHOP_WOOD", "chop wood gather logs pegar coletar madeira arvore troncos"),
            new DecisionCandidate("MINE_BLOCK", "mine ore mining minerar minerio pedra ferro diamante carvao"),
            new DecisionCandidate("DEPOSIT_CHEST", "deposit store items guardar depositar bau chest"),
            new DecisionCandidate("ASSIST_SELECTED_QUEST", "quest mission missao ajudar tarefa objetivo"),
            new DecisionCandidate("RECALL", "come here recall chamar aproximar puxar"),
            new DecisionCandidate("REMOTE_VIEW", "remote view camera visao observar camera"),
            new DecisionCandidate("OPEN_INVENTORY", "open inventory mochila inventario itens"),
            new DecisionCandidate("REPORT_STATUS", "status report relatorio situacao vida"),
            new DecisionCandidate("CASUAL_CHAT", "casual chat conversation conversa conversar"),
            new DecisionCandidate("UNKNOWN_OR_BLOCKED", "unknown blocked desconhecido bloqueado insuficiente")
    ));

    private final DecisionProvider provider;

    public IntentDecisionProvider() {
        this(null);
    }

    public IntentDecisionProvider(DecisionProvider provider) {
        this.provider = provider != null ? provider : new com.thyagotoledo.companions.core.decision.HybridDecisionProvider();
    }

    public CompletableFuture<Intent> decide(String input, String locale, UUID companionId) {
        Map<String, String> context = new HashMap<>();
        context.put("locale", locale != null ? locale : "en_us");
        DecisionRequest request = new DecisionRequest(UUID.randomUUID(), companionId, 0L, 0L,
                System.nanoTime() + 50_000_000L, locale, input, context, CANDIDATES,
                Collections.<com.thyagotoledo.companions.core.decision.DecisionQuestion>singletonList(
                        new ChoiceQuestion("intent", "Classify only the expressed intent.", optionIds())));
        return provider.decide(request)
                .thenApply(IntentDecisionProvider::toIntent)
                .exceptionally(error -> null);
    }

    public DecisionProvider getProvider() {
        return provider;
    }

    public static List<DecisionCandidate> candidates() {
        return CANDIDATES;
    }

    private static List<String> optionIds() {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (DecisionCandidate candidate : CANDIDATES) ids.add(candidate.getId());
        return ids;
    }

    private static Intent toIntent(DecisionResult result) {
        if (result == null || result.getOutcome() != DecisionResult.Outcome.ACCEPT) return null;
        try {
            IntentType type = IntentType.valueOf(result.getCandidateId());
            switch (type) {
                case CHOP_WOOD:
                    return new Intent(type, "minecraft:oak_log", 16);
                case MINE_BLOCK:
                    return new Intent(type, null, 0);
                default:
                    return new Intent(type);
            }
        } catch (IllegalArgumentException invalidCandidate) {
            return null;
        }
    }
}
