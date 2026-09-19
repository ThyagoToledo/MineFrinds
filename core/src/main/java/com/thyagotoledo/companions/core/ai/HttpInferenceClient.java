package com.thyagotoledo.companions.core.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HttpInferenceClient implements InferenceClient {
    private final String endpointUrl;
    private final int timeoutMs;
    private final BlockingQueue<Runnable> workQueue;
    private final ThreadPoolExecutor executor;
    private volatile boolean available = true;

    public HttpInferenceClient(String endpointUrl, int timeoutMs) {
        this.endpointUrl = endpointUrl != null ? endpointUrl : "http://127.0.0.1:8080/v1/chat/completions";
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.workQueue = new ArrayBlockingQueue<>(8);
        this.executor = new ThreadPoolExecutor(
                1, 1,
                60L, TimeUnit.SECONDS,
                workQueue,
                r -> {
                    Thread t = new Thread(r, "companions-ai-worker");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public HttpInferenceClient() {
        this("http://127.0.0.1:8080/v1/chat/completions", 3000);
    }

    @Override
    public CompletableFuture<String> completeAsync(String prompt, String systemPrompt) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            executor.submit(() -> {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(endpointUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setConnectTimeout(timeoutMs);
                    conn.setReadTimeout(timeoutMs);
                    conn.setDoOutput(true);

                    String escapedPrompt = escapeJson(prompt);
                    String escapedSystem = escapeJson(systemPrompt);

                    String jsonPayload = "{\"messages\": [" +
                            "{\"role\": \"system\", \"content\": \"" + escapedSystem + "\"}," +
                            "{\"role\": \"user\", \"content\": \"" + escapedPrompt + "\"}" +
                            "], \"temperature\": 0.2, \"max_tokens\": 128}";

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        StringBuilder response = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                        }
                        this.available = true;
                        future.complete(response.toString());
                    } else {
                        this.available = false;
                        future.completeExceptionally(new RuntimeException("HTTP error code: " + responseCode));
                    }
                } catch (Exception e) {
                    this.available = false;
                    future.completeExceptionally(e);
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            });
        } catch (RejectedExecutionException ree) {
            future.completeExceptionally(new IllegalStateException("Fila de inferencia cheia (limite de 8 pedidos atingido)"));
        }

        return future;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public int getPendingQueueSize() {
        return workQueue.size();
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
