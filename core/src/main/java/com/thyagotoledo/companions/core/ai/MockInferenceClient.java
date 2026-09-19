package com.thyagotoledo.companions.core.ai;

import java.util.concurrent.CompletableFuture;

public class MockInferenceClient implements InferenceClient {
    private boolean available = true;
    private String nextResponse = "{\"intent\": \"CASUAL_CHAT\", \"speech\": \"Ola! Como posso ajudar voce hoje?\"}";
    private boolean simulateTimeout = false;
    private int pendingQueueSize = 0;

    @Override
    public CompletableFuture<String> completeAsync(String prompt, String systemPrompt) {
        if (!available) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("Provedor de inferencia indisponivel"));
            return failed;
        }

        if (simulateTimeout) {
            return new CompletableFuture<>();
        }

        return CompletableFuture.completedFuture(nextResponse);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public int getPendingQueueSize() {
        return pendingQueueSize;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setNextResponse(String nextResponse) {
        this.nextResponse = nextResponse;
    }

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    public void setPendingQueueSize(int pendingQueueSize) {
        this.pendingQueueSize = pendingQueueSize;
    }
}
