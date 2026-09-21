package com.thyagotoledo.companions.core.decision;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Provider determinístico para comandos inequívocos e fallback offline. */
public final class RulesDecisionProvider implements DecisionProvider {
    @Override
    public CompletableFuture<DecisionResult> decide(DecisionRequest request) {
        if (request == null) return CompletableFuture.completedFuture(DecisionResult.error(getName(), "request_null"));
        if (request.isExpired(System.nanoTime())) return CompletableFuture.completedFuture(DecisionResult.abstain(getName(), "deadline_expired"));
        String explicit = request.getContext().get("explicit_intent");
        if (explicit == null || explicit.trim().isEmpty()) {
            String message = request.getMessage().toLowerCase(Locale.ROOT);
            if (message.equals("follow") || message.contains("me segue") || message.contains("follow me")) explicit = "FOLLOW_OWNER";
            else if (message.equals("stay") || message.contains("fica aqui") || message.contains("stay here")) explicit = "STAY";
            else if (message.equals("defend") || message.contains("defenda") || message.contains("protect")) explicit = "DEFEND";
        }
        if (explicit != null) {
            for (DecisionCandidate candidate : request.getCandidates()) {
                if (explicit.equalsIgnoreCase(candidate.getId())) {
                    return CompletableFuture.completedFuture(DecisionResult.accept(candidate.getId(), 1.0, getName(), "explicit_rule"));
                }
            }
        }
        return CompletableFuture.completedFuture(DecisionResult.abstain(getName(), "no_unambiguous_rule"));
    }

    @Override public boolean isAvailable() { return true; }
    @Override public String getName() { return "rules"; }
}
