package com.thyagotoledo.companions.core.ai;

import java.util.concurrent.CompletableFuture;

/** Faz uma requisição de inferência; fila, limites e ciclo de vida pertencem ao supervisor. */
public interface InferenceTransport {
    CompletableFuture<String> completeAsync(String prompt, String systemPrompt, int timeoutMs);
}
