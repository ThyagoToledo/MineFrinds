package com.thyagotoledo.companions.core.decision;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Transporte HTTPS mínimo para a API Jev; a chave fica somente no header. */
public final class HttpDecisionTransport implements DecisionTransport {
    private final String endpoint;
    private final String apiKey;
    private final ExecutorService executor;

    public HttpDecisionTransport(String endpoint, String apiKey) {
        this.endpoint = endpoint != null ? endpoint : "https://api.typesafe.ai/v1/systemone";
        this.apiKey = apiKey != null ? apiKey : "";
        this.executor = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2), runnable -> {
                    Thread thread = new Thread(runnable, "companions-jev-decision");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override public CompletableFuture<String> post(String payload, int timeoutMs) {
        CompletableFuture<String> result = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(endpoint);
                    if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("Jev exige HTTPS");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setConnectTimeout(timeoutMs);
                    connection.setReadTimeout(timeoutMs);
                    connection.setDoOutput(true);
                    try (OutputStream output = connection.getOutputStream()) { output.write(payload.getBytes(StandardCharsets.UTF_8)); }
                    int code = connection.getResponseCode();
                    if (code < 200 || code >= 300) throw new IllegalStateException("Jev HTTP " + code);
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null && body.length() <= 65536) body.append(line);
                    }
                    result.complete(body.toString());
                } catch (Exception error) {
                    result.completeExceptionally(error);
                } finally {
                    if (connection != null) connection.disconnect();
                }
            });
        } catch (RuntimeException error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    public void shutdown() { executor.shutdownNow(); }
}
