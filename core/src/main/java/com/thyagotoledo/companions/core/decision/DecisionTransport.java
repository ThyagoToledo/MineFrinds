package com.thyagotoledo.companions.core.decision;

import java.util.concurrent.CompletableFuture;

/** Transporte separado do provider para testar API, timeout e respostas sem rede. */
public interface DecisionTransport {
    CompletableFuture<String> post(String payload, int timeoutMs);
}
