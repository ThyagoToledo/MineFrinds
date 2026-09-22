package com.thyagotoledo.companions.core.decision;

import java.util.concurrent.CompletableFuture;

/** Regras têm prioridade; provider secundário só recebe decisões ambíguas. */
public final class HybridDecisionProvider implements DecisionProvider {
    private final DecisionProvider rules;
    private final DecisionProvider secondary;

    public HybridDecisionProvider() {
        this(new RulesDecisionProvider(), new LocalDecisionProvider());
    }

    public HybridDecisionProvider(DecisionProvider rules) {
        this(rules, new LocalDecisionProvider());
    }

    public HybridDecisionProvider(DecisionProvider rules, DecisionProvider secondary) {
        this.rules = rules != null ? rules : new RulesDecisionProvider();
        this.secondary = secondary;
    }

    @Override
    public CompletableFuture<DecisionResult> decide(DecisionRequest request) {
        return rules.decide(request).thenCompose(local -> {
            if (local.getOutcome() == DecisionResult.Outcome.ACCEPT) return CompletableFuture.completedFuture(local);
            if (secondary == null || !secondary.isAvailable()) return CompletableFuture.completedFuture(local);
            return secondary.decide(request).handle((remote, error) -> {
                if (error != null || remote == null) return DecisionResult.abstain(getName(), "secondary_unavailable");
                return remote;
            });
        });
    }

    @Override public boolean isAvailable() { return rules.isAvailable() || (secondary != null && secondary.isAvailable()); }
    @Override public String getName() { return "hybrid"; }
}
