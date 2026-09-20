package com.thyagotoledo.companions.core.ai;

import java.util.concurrent.CompletableFuture;

/** Compatibilidade para callers antigos; a fila real vive no InferenceSupervisor. */
public final class HttpInferenceClient implements InferenceClient {
    private final InferenceSupervisor supervisor;

    public HttpInferenceClient(String endpointUrl, int timeoutMs) {
        this.supervisor = new InferenceSupervisor(true, endpointUrl, timeoutMs, 8);
    }

    public HttpInferenceClient() {
        this(Boolean.parseBoolean(System.getProperty("companions.ai.enabled", "false")),
                "http://127.0.0.1:8080/v1/chat/completions", 1500);
    }

    public HttpInferenceClient(boolean enabled, String endpointUrl, int timeoutMs) {
        this.supervisor = new InferenceSupervisor(enabled, endpointUrl, timeoutMs, 8);
    }

    @Override public CompletableFuture<String> completeAsync(String prompt, String systemPrompt) {
        return supervisor.completeAsync(prompt, systemPrompt);
    }
    @Override public boolean isAvailable() { return supervisor.isAvailable(); }
    @Override public int getPendingQueueSize() { return supervisor.getPendingQueueSize(); }
    public void shutdown() { supervisor.shutdown(); }
}
