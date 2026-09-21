package com.thyagotoledo.companions.core.decision;

import java.util.concurrent.CompletableFuture;

public interface DecisionProvider {
    CompletableFuture<DecisionResult> decide(DecisionRequest request);
    boolean isAvailable();
    String getName();
}
