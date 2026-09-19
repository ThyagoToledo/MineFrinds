package com.thyagotoledo.companions.core.ai;

import java.util.concurrent.CompletableFuture;

public interface InferenceClient {
    CompletableFuture<String> completeAsync(String prompt, String systemPrompt);
    boolean isAvailable();
    int getPendingQueueSize();
}
