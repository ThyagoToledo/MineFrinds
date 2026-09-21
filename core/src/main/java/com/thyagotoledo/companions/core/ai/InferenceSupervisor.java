package com.thyagotoledo.companions.core.ai;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
    private final Set<RequestTask> activeTasks = Collections.synchronizedSet(new HashSet<RequestTask>());
    private final ScheduledExecutorService deadlineExecutor;

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
        this.deadlineExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "companions-ai-deadline");
            thread.setDaemon(true);
            return thread;
        });
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
        RequestTask task = new RequestTask(prompt, systemPrompt, future);
        activeTasks.add(task);
        task.deadlineTask = deadlineExecutor.schedule(() -> task.cancel(new TimeoutException("Inferencia expirou na fila")),
                timeoutMs, TimeUnit.MILLISECONDS);
        try {
            executor.execute(task);
        } catch (RejectedExecutionException error) {
            activeTasks.remove(task);
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
    public void shutdown() {
        if (closed) return;
        closed = true;
        List<Runnable> queued = executor.shutdownNow();
        for (Runnable runnable : queued) {
            if (runnable instanceof RequestTask) ((RequestTask) runnable).cancel(new IllegalStateException("Supervisor encerrado"));
        }
        deadlineExecutor.shutdownNow();
        synchronized (activeTasks) {
            for (RequestTask task : activeTasks.toArray(new RequestTask[0])) {
                task.cancel(new IllegalStateException("Supervisor encerrado"));
            }
            activeTasks.clear();
        }
    }

    private final class RequestTask implements Runnable {
        private final String prompt;
        private final String systemPrompt;
        private final CompletableFuture<String> result;
        private final long deadlineNanos;
        private volatile CompletableFuture<String> transportResult;
        private volatile ScheduledFuture<?> deadlineTask;

        private RequestTask(String prompt, String systemPrompt, CompletableFuture<String> result) {
            this.prompt = prompt;
            this.systemPrompt = systemPrompt;
            this.result = result;
            this.deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        }

        @Override public void run() {
            try {
                if (result.isDone()) return;
                if (closed) {
                    cancel(new IllegalStateException("Supervisor encerrado"));
                    return;
                }
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0L) throw new TimeoutException("Inferencia expirou na fila");
                int remainingMs = (int) Math.max(1L, Math.min(timeoutMs,
                        TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
                transportResult = transport.completeAsync(prompt, systemPrompt, remainingMs);
                long waitMs = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()));
                String raw = transportResult.get(waitMs, TimeUnit.MILLISECONDS);
                consecutiveFailures = 0;
                metrics.success();
                result.complete(raw);
            } catch (Exception error) {
                CompletableFuture<String> pending = transportResult;
                if (pending != null && !pending.isDone()) pending.cancel(true);
                metrics.failure();
                if (++consecutiveFailures >= 3) openUntil = System.currentTimeMillis() + cooldownMs;
                result.completeExceptionally(error);
            } finally {
                ScheduledFuture<?> deadline = deadlineTask;
                if (deadline != null) deadline.cancel(false);
                activeTasks.remove(this);
            }
        }

        private void cancel(Throwable reason) {
            CompletableFuture<String> pending = transportResult;
            if (pending != null) pending.cancel(true);
            result.completeExceptionally(reason);
            ScheduledFuture<?> deadline = deadlineTask;
            if (deadline != null) deadline.cancel(false);
            activeTasks.remove(this);
        }
    }
}
