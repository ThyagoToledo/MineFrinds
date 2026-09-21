package com.thyagotoledo.companions.core.decision;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adaptador Jev em modo opt-in; a resposta só escolhe um candidato fechado. */
public final class JevDecisionProvider implements DecisionProvider {
    private static final Pattern CHOICE = Pattern.compile("\\\"choice\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern CONFIDENCE = Pattern.compile("\\\"confidence\\\"\\s*:\\s*([0-9.]+)");
    private final DecisionTransport transport;
    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final int timeoutMs;
    private final boolean enabled;

    public JevDecisionProvider(DecisionTransport transport, String endpoint, String model,
                               String apiKey, int timeoutMs, boolean enabled) {
        this.transport = transport;
        this.endpoint = endpoint != null ? endpoint : "https://api.typesafe.ai/v1/systemone";
        this.model = model != null && !model.trim().isEmpty() ? model.trim() : "jev-latest";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.timeoutMs = Math.max(250, Math.min(30000, timeoutMs));
        this.enabled = enabled && !this.apiKey.isEmpty() && transport != null;
    }

    @Override
    public CompletableFuture<DecisionResult> decide(DecisionRequest request) {
        if (!enabled) return CompletableFuture.completedFuture(DecisionResult.abstain(getName(), "disabled_or_missing_key"));
        if (request == null || request.isExpired(System.nanoTime())) {
            return CompletableFuture.completedFuture(DecisionResult.abstain(getName(), "deadline_expired"));
        }
        if (request.getCandidates().size() < 2) {
            return CompletableFuture.completedFuture(DecisionResult.abstain(getName(), "not_enough_candidates"));
        }
        String payload = payload(request);
        return transport.post(payload, timeoutMs).handle((body, error) -> {
            if (error != null || body == null) return DecisionResult.abstain(getName(), "transport_failure");
            return parse(body, request);
        });
    }

    @Override public boolean isAvailable() { return enabled; }
    @Override public String getName() { return "jev"; }

    private DecisionResult parse(String body, DecisionRequest request) {
        Matcher choice = CHOICE.matcher(body);
        if (!choice.find()) return DecisionResult.abstain(getName(), "invalid_choice_response");
        String selected = choice.group(1);
        boolean allowed = false;
        for (DecisionCandidate candidate : request.getCandidates()) {
            if (candidate.getId().equals(selected)) { allowed = true; break; }
        }
        if (!allowed) return DecisionResult.abstain(getName(), "choice_outside_allowlist");
        double confidence = 0.0;
        Matcher confidenceMatcher = CONFIDENCE.matcher(body);
        if (confidenceMatcher.find()) {
            try { confidence = Double.parseDouble(confidenceMatcher.group(1)); }
            catch (NumberFormatException ignored) { confidence = 0.0; }
        }
        return DecisionResult.accept(selected, confidence, getName(), "remote_choice");
    }

    private String payload(DecisionRequest request) {
        StringBuilder state = new StringBuilder();
        state.append("{\"locale\":\"").append(escape(request.getLocale()))
                .append("\",\"message\":\"").append(escape(request.getMessage())).append("\"");
        for (java.util.Map.Entry<String, String> entry : request.getContext().entrySet()) {
            state.append(",\"").append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append("\"");
        }
        state.append(",\"allowed\":[");
        for (int i = 0; i < request.getCandidates().size(); i++) {
            if (i > 0) state.append(',');
            state.append('\"').append(escape(request.getCandidates().get(i).getId())).append('\"');
        }
        state.append("]}");
        StringBuilder criteria = new StringBuilder("{");
        for (int i = 0; i < request.getCandidates().size(); i++) {
            if (i > 0) criteria.append(',');
            DecisionCandidate candidate = request.getCandidates().get(i);
            criteria.append('\"').append(escape(candidate.getId())).append("\":\"")
                    .append(escape(candidate.getDescription())).append('\"');
        }
        criteria.append('}');
        return "{\"model\":\"" + escape(model) + "\",\"state\":" + state +
                ",\"questions\":{\"intent\":{\"type\":\"choice\",\"instructions\":\"Choose one allowed action for this Minecraft command.\",\"criteria\":" + criteria + "}}}";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ");
    }
}
