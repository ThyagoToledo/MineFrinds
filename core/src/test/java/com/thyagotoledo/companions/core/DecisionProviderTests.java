package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.decision.DecisionCandidate;
import com.thyagotoledo.companions.core.decision.DecisionRequest;
import com.thyagotoledo.companions.core.decision.DecisionResult;
import com.thyagotoledo.companions.core.decision.DecisionGate;
import com.thyagotoledo.companions.core.decision.HybridDecisionProvider;
import com.thyagotoledo.companions.core.decision.RulesDecisionProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class DecisionProviderTests {
    private static DecisionRequest request(String message, String explicit) {
        HashMap<String, String> context = new HashMap<>();
        if (explicit != null) context.put("explicit_intent", explicit);
        return new DecisionRequest(UUID.randomUUID(), UUID.randomUUID(), 2L, 4L,
                System.nanoTime() + 1_000_000_000L, "pt_br", message, context,
                Arrays.asList(new DecisionCandidate("FOLLOW_OWNER", "seguir"),
                        new DecisionCandidate("STAY", "aguardar"),
                        new DecisionCandidate("DEFEND", "defender")));
    }

    @Test
    void requestIsBoundedAndImmutable() {
        DecisionRequest request = request("follow", null);
        assertEquals(3, request.getCandidates().size());
        assertThrows(UnsupportedOperationException.class, () -> request.getCandidates().add(new DecisionCandidate("X", "x")));
        assertThrows(UnsupportedOperationException.class, () -> request.getContext().put("x", "y"));
    }

    @Test
    void rulesSelectsOnlyExplicitCandidate() throws Exception {
        DecisionResult result = new RulesDecisionProvider().decide(request("follow", null)).get();
        assertEquals(DecisionResult.Outcome.ACCEPT, result.getOutcome());
        assertEquals("FOLLOW_OWNER", result.getCandidateId());
        assertEquals(1.0, result.getConfidence());

        DecisionResult unknown = new RulesDecisionProvider().decide(request("talvez", null)).get();
        assertEquals(DecisionResult.Outcome.ABSTAIN, unknown.getOutcome());
    }

    @Test
    void hybridDoesNotCallSecondaryWhenRulesKnowTheAnswer() throws Exception {
        DecisionProviderProbe secondary = new DecisionProviderProbe();
        DecisionResult result = new HybridDecisionProvider(new RulesDecisionProvider(), secondary)
                .decide(request("x", "STAY")).get();
        assertEquals("STAY", result.getCandidateId());
        assertEquals(0, secondary.calls);
    }

    @Test
    void hybridAbstainsWhenSecondaryFails() throws Exception {
        DecisionProviderProbe secondary = new DecisionProviderProbe();
        secondary.failure = true;
        DecisionResult result = new HybridDecisionProvider(new RulesDecisionProvider(), secondary)
                .decide(request("ambiguous", null)).get();
        assertEquals(DecisionResult.Outcome.ABSTAIN, result.getOutcome());
        assertEquals("secondary_unavailable", result.getReason());
    }

    @Test
    void gateRejectsStaleOrLowConfidenceResults() throws Exception {
        DecisionRequest request = request("ambiguous", null);
        DecisionResult result = DecisionResult.accept("DEFEND", 0.8, "test", "fixture");
        assertTrue(DecisionGate.accepts(request, result, 2L, 4L, System.nanoTime(), 0.7));
        assertFalse(DecisionGate.accepts(request, result, 3L, 4L, System.nanoTime(), 0.7));
        assertFalse(DecisionGate.accepts(request, DecisionResult.accept("DEFEND", 0.2, "test", "fixture"), 2L, 4L, System.nanoTime(), 0.7));
    }

    private static final class DecisionProviderProbe implements com.thyagotoledo.companions.core.decision.DecisionProvider {
        private int calls;
        private boolean failure;
        @Override public CompletableFuture<DecisionResult> decide(DecisionRequest request) {
            calls++;
            if (failure) {
                CompletableFuture<DecisionResult> failed = new CompletableFuture<>();
                failed.completeExceptionally(new IllegalStateException("offline"));
                return failed;
            }
            return CompletableFuture.completedFuture(DecisionResult.accept("DEFEND", 0.8, "probe", "test"));
        }
        @Override public boolean isAvailable() { return true; }
        @Override public String getName() { return "probe"; }
    }
}
