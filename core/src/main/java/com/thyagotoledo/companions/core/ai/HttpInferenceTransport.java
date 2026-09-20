package com.thyagotoledo.companions.core.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/** Transporte sem executor próprio. Deve ser chamado por um worker do supervisor. */
public final class HttpInferenceTransport implements InferenceTransport {
    private final String endpointUrl;
    private final int maxResponseBytes;

    public HttpInferenceTransport(String endpointUrl) {
        this(endpointUrl, 64 * 1024);
    }

    public HttpInferenceTransport(String endpointUrl, int maxResponseBytes) {
        this.endpointUrl = endpointUrl != null ? endpointUrl : "http://127.0.0.1:8080/v1/chat/completions";
        this.maxResponseBytes = Math.max(1024, maxResponseBytes);
    }

    @Override
    public CompletableFuture<String> completeAsync(String prompt, String systemPrompt, int timeoutMs) {
        CompletableFuture<String> result = new CompletableFuture<>();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpointUrl);
            if (!"http".equalsIgnoreCase(url.getProtocol()) && !"https".equalsIgnoreCase(url.getProtocol())) {
                throw new IllegalArgumentException("Apenas HTTP/HTTPS sao aceitos");
            }
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setConnectTimeout(Math.max(1000, timeoutMs));
            conn.setReadTimeout(Math.max(1000, timeoutMs));
            conn.setDoOutput(true);
            String payload = "{\"messages\":[{\"role\":\"system\",\"content\":\"" + escape(systemPrompt) +
                    "\"},{\"role\":\"user\",\"content\":\"" + escape(prompt) +
                    "\"}],\"temperature\":0.2,\"max_tokens\":160}";
            try (OutputStream output = conn.getOutputStream()) {
                output.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP status " + code);
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int bytes = 0;
                while ((line = reader.readLine()) != null) {
                    bytes += line.getBytes(StandardCharsets.UTF_8).length;
                    if (bytes > maxResponseBytes) {
                        throw new IllegalStateException("Resposta de inferencia excedeu o limite");
                    }
                    body.append(line);
                }
            }
            result.complete(body.toString());
        } catch (Exception error) {
            result.completeExceptionally(error);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result;
    }

    private static String escape(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\b", "\\b").replace("\f", "\\f")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
