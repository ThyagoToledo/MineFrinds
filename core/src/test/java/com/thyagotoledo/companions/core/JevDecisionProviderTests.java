package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.decision.DecisionCandidate;
import com.thyagotoledo.companions.core.decision.DecisionRequest;
import com.thyagotoledo.companions.core.decision.DecisionResult;
import com.thyagotoledo.companions.core.decision.DecisionTransport;
import com.thyagotoledo.companions.core.decision.JevDecisionProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class JevDecisionProviderTests {
    private static DecisionRequest request() {
        return new DecisionRequest(UUID.randomUUID(), UUID.randomUUID(), 1, 2,
                System.nanoTime() + 1_000_000_000L, "pt_br", "pegue madeira", null,
                Arrays.asList(new DecisionCandidate("CHOP_WOOD", "coletar madeira"),
                        new DecisionCandidate("STAY", "aguardar")));
    }

    @Test
    void disabledProviderNeverCallsTransport() throws Exception {
        DecisionTransport transport = (payload, timeout) -> { fail("transport must stay disabled"); return null; };
        DecisionResult result = new JevDecisionProvider(transport, "https://example.invalid", "jev-latest", "", 1000, true)
                .decide(request()).get();
        assertEquals(DecisionResult.Outcome.ABSTAIN, result.getOutcome());
    }

    @Test
    void acceptsOnlyAnAllowedChoiceAndBuildsTypedPayload() throws Exception {
        final String[] sent = new String[1];
        DecisionTransport transport = (payload, timeout) -> {
            sent[0] = payload;
            return CompletableFuture.completedFuture("{\"answers\":{\"intent\":{\"type\":\"choice\",\"choice\":\"CHOP_WOOD\",\"confidence\":0.91}}}");
        };
        DecisionResult result = new JevDecisionProvider(transport, "https://example.invalid", "jev-latest", "secret", 1000, true)
                .decide(request()).get();
        assertEquals(DecisionResult.Outcome.ACCEPT, result.getOutcome());
        assertEquals("CHOP_WOOD", result.getCandidateId());
        assertEquals(0.91, result.getConfidence());
        assertTrue(sent[0].contains("CHOP_WOOD"));
        assertFalse(sent[0].contains("secret"));
    }

    @Test
    void rejectsChoiceOutsideAllowlist() throws Exception {
        DecisionTransport transport = (payload, timeout) -> CompletableFuture.completedFuture(
                "{\"answers\":{\"intent\":{\"choice\":\"TELEPORT\",\"confidence\":0.99}}}");
        DecisionResult result = new JevDecisionProvider(transport, "https://example.invalid", "jev-latest", "secret", 1000, true)
                .decide(request()).get();
        assertEquals(DecisionResult.Outcome.ABSTAIN, result.getOutcome());
        assertEquals("choice_outside_allowlist", result.getReason());
    }
}
