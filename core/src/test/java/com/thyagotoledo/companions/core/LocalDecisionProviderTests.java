package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.decision.DecisionCandidate;
import com.thyagotoledo.companions.core.decision.DecisionRequest;
import com.thyagotoledo.companions.core.decision.DecisionResult;
import com.thyagotoledo.companions.core.decision.LocalDecisionProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDecisionProviderTests {
    private static DecisionRequest request(String message, long deadline) {
        Map<String, String> context = new HashMap<>();
        context.put("dimension", "overworld");
        return new DecisionRequest(UUID.randomUUID(), UUID.randomUUID(), 1L, 1L, deadline,
                "pt_br", message, context, Arrays.asList(
                new DecisionCandidate("CHOP_WOOD", "coletar madeira e troncos"),
                new DecisionCandidate("MINE_IRON", "minerar ferro"),
                new DecisionCandidate("FOLLOW_OWNER", "seguir o jogador")));
    }

    @Test
    void selectsPortugueseOptionWithProbabilityDistribution() throws Exception {
        DecisionResult result = new LocalDecisionProvider().decide(request("pegue madeira da arvore", System.nanoTime() + 1_000_000_000L)).get();
        assertEquals(DecisionResult.Outcome.ACCEPT, result.getOutcome());
        assertEquals("CHOP_WOOD", result.getCandidateId());
        assertTrue(result.getConfidence() >= 0.55);
        assertEquals(3, result.getProbabilities().size());
        assertEquals(1.0, result.getProbabilities().values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
    }

    @Test
    void selectsEnglishOption() throws Exception {
        DecisionResult result = new LocalDecisionProvider().decide(request("mine iron ore", System.nanoTime() + 1_000_000_000L)).get();
        assertEquals(DecisionResult.Outcome.ACCEPT, result.getOutcome());
        assertEquals("MINE_IRON", result.getCandidateId());
    }

    @Test
    void abstainsWhenInputHasNoCandidateSignal() throws Exception {
        DecisionResult result = new LocalDecisionProvider().decide(request("faça alguma coisa", System.nanoTime() + 1_000_000_000L)).get();
        assertEquals(DecisionResult.Outcome.ABSTAIN, result.getOutcome());
        assertTrue(result.getProbabilities().isEmpty());
        assertEquals("no_local_signal", result.getReason());
    }

    @Test
    void abstainsWhenDeadlineHasExpired() throws Exception {
        DecisionResult result = new LocalDecisionProvider().decide(request("mine iron", System.nanoTime() - 1L)).get();
        assertEquals(DecisionResult.Outcome.ABSTAIN, result.getOutcome());
        assertEquals("deadline_expired", result.getReason());
    }

    @Test
    void remainsAvailableWithoutExternalServices() {
        LocalDecisionProvider provider = new LocalDecisionProvider();
        assertTrue(provider.isAvailable());
        assertEquals("local-scorer-v1", provider.getName());
    }
}
