package com.thyagotoledo.companions.core.ai;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/** Supervisor único: fila, circuit breaker e ciclo de vida, sem bloquear a thread do jogo. */
public final class InferenceSupervisor implements InferenceClient {
    private final boolean enabled;
    private final int timeoutMs;
    private final InferenceTransport transport;
    private final ThreadPoolExecutor executor;
    private final long cooldownMs;
    private final CompanionMetrics metrics = new CompanionMetrics();
    private volatile int consecutiveFailures;
    private volatile long openUntil;
    private volatile boolean closed;

    public InferenceSupervisor(boolean enabled, String endpointUrl, int timeoutMs, int maxQueueSize) {
        this(enabled, new HttpInferenceTransport(endpointUrl), timeoutMs, maxQueueSize, 30_000L);
    }

    public InferenceSupervisor(boolean enabled, InferenceTransport transport, int timeoutMs,
                               int maxQueueSize, long cooldownMs) {
        this.enabled = enabled;
        this.transport = transport;
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.cooldownMs = Math.max(1000L, cooldownMs);
        int queueSize = Math.max(1, Math.min(8, maxQueueSize));
        this.executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(queueSize), runnable -> {
                    Thread thread = new Thread(runnable, "companions-ai-supervisor");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public CompletableFuture<String> completeAsync(String prompt, String systemPrompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        metrics.request();
        if (!enabled || closed || isCircuitOpen()) {
            metrics.rejected();
            future.completeExceptionally(new IllegalStateException("Inferencia desativada ou circuito aberto"));
            return future;
        }
        try {
            executor.execute(() -> {
                try {
                    String raw = transport.completeAsync(prompt, systemPrompt, timeoutMs)
                            .get(timeoutMs + 250L, TimeUnit.MILLISECONDS);
                    consecutiveFailures = 0;
                    metrics.success();
                    future.complete(raw);
                } catch (Exception error) {
                    metrics.failure();
                    if (++consecutiveFailures >= 3) openUntil = System.currentTimeMillis() + cooldownMs;
                    future.completeExceptionally(error);
                }
            });
        } catch (RejectedExecutionException error) {
            metrics.rejected();
            future.completeExceptionally(new IllegalStateException("Fila de inferencia cheia", error));
        }
        return future;
    }

    private boolean isCircuitOpen() {
        long until = openUntil;
        if (until == 0L || System.currentTimeMillis() >= until) {
            if (until != 0L) openUntil = 0L;
            return false;
        }
        return true;
    }

    @Override public boolean isAvailable() { return enabled && !closed && !isCircuitOpen(); }
    @Override public int getPendingQueueSize() { return executor.getQueue().size(); }
    public CompanionMetrics.Snapshot getMetrics() { return metrics.snapshot(); }
    public void shutdown() { closed = true; executor.shutdownNow(); }
}
