package com.thyagotoledo.companions.core.decision;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Scorer local pequeno para decisões fechadas. Ele usa tokens normalizados,
 * pesos IDF calculados apenas entre as opções e uma softmax calibrável; não
 * abre sockets, não carrega modelo externo e não cria threads por requisição.
 */
public final class LocalDecisionProvider implements DecisionProvider {
    private static final double DEFAULT_MIN_CONFIDENCE = 0.55;
    private static final double DEFAULT_MIN_MARGIN = 0.10;
    private static final double DEFAULT_TEMPERATURE = 1.0;

    private final double minimumConfidence;
    private final double minimumMargin;
    private final double temperature;

    public LocalDecisionProvider() {
        this(DEFAULT_MIN_CONFIDENCE, DEFAULT_MIN_MARGIN, DEFAULT_TEMPERATURE);
    }

    public LocalDecisionProvider(double minimumConfidence, double minimumMargin, double temperature) {
        this.minimumConfidence = clamp(minimumConfidence);
        this.minimumMargin = clamp(minimumMargin);
        this.temperature = Math.max(0.05, temperature);
    }

    @Override
    public CompletableFuture<DecisionResult> decide(DecisionRequest request) {
        if (request == null) return completed(DecisionResult.error(getName(), "request_null"));
        if (request.isExpired(System.nanoTime())) {
            return completed(DecisionResult.abstain(getName(), "deadline_expired"));
        }
        List<DecisionCandidate> candidates = request.getCandidates();
        if (candidates.size() < 2) {
            return completed(DecisionResult.abstain(getName(), "not_enough_candidates"));
        }

        String input = buildInput(request);
        Set<String> inputTokens = tokens(input);
        if (inputTokens.isEmpty()) {
            return completed(DecisionResult.abstain(getName(), "no_local_signal"));
        }

        List<Set<String>> features = new ArrayList<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (DecisionCandidate candidate : candidates) {
            Set<String> featureSet = tokens(candidate.getId().replace('_', ' ') + " " + candidate.getDescription());
            features.add(featureSet);
            for (String token : featureSet) documentFrequency.put(token, documentFrequency.containsKey(token)
                    ? documentFrequency.get(token) + 1 : 1);
        }

        double[] scores = new double[candidates.size()];
        boolean hasSignal = false;
        for (int index = 0; index < candidates.size(); index++) {
            DecisionCandidate candidate = candidates.get(index);
            Set<String> featureSet = features.get(index);
            double score = 0.0;
            String candidateId = normalize(candidate.getId());
            String explicit = request.getContext().get("explicit_intent");
            if (explicit != null && normalize(explicit).equals(candidateId)) score += 8.0;
            for (String token : inputTokens) {
                if (!featureSet.contains(token)) continue;
                int frequency = documentFrequency.containsKey(token) ? documentFrequency.get(token) : 1;
                double idf = Math.log((1.0 + candidates.size()) / (1.0 + frequency)) + 1.0;
                score += idf;
            }
            // Uma pequena vantagem para frases que aparecem inteiras na opção.
            String candidateText = normalize(candidate.getId().replace('_', ' ') + " " + candidate.getDescription());
            if (!candidateText.isEmpty() && input.contains(candidateText)) score += 2.0;
            scores[index] = score;
            hasSignal = hasSignal || score > 0.0;
        }

        if (!hasSignal || request.isExpired(System.nanoTime())) {
            return completed(DecisionResult.abstain(getName(), hasSignal ? "deadline_expired" : "no_local_signal"));
        }

        Map<String, Double> probabilities = softmax(candidates, scores);
        int best = bestIndex(scores);
        int second = secondIndex(scores, best);
        double confidence = probabilities.get(candidates.get(best).getId());
        double margin = confidence - probabilities.get(candidates.get(second).getId());
        if (confidence < minimumConfidence || margin < minimumMargin) {
            return completed(DecisionResult.abstain(probabilities, getName(), "low_local_confidence"));
        }
        return completed(DecisionResult.accept(candidates.get(best).getId(), confidence, probabilities,
                getName(), "local_tfidf_softmax"));
    }

    @Override public boolean isAvailable() { return true; }
    @Override public String getName() { return "local-scorer-v1"; }

    private static String buildInput(DecisionRequest request) {
        StringBuilder builder = new StringBuilder(request.getMessage() == null ? "" : request.getMessage());
        for (Map.Entry<String, String> entry : request.getContext().entrySet()) {
            if ("explicit_intent".equals(entry.getKey())) continue;
            builder.append(' ').append(entry.getKey()).append(' ').append(entry.getValue());
        }
        return normalize(builder.toString());
    }

    private static Set<String> tokens(String text) {
        Set<String> result = new HashSet<>();
        String normalized = normalize(text);
        if (normalized.isEmpty()) return result;
        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.length() >= 2) result.add(token);
        }
        return result;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return decomposed.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
    }

    private Map<String, Double> softmax(List<DecisionCandidate> candidates, double[] scores) {
        double max = scores[0];
        for (double score : scores) if (score > max) max = score;
        double total = 0.0;
        double[] exponents = new double[scores.length];
        for (int index = 0; index < scores.length; index++) {
            exponents[index] = Math.exp((scores[index] - max) / temperature);
            total += exponents[index];
        }
        Map<String, Double> probabilities = new LinkedHashMap<>();
        for (int index = 0; index < candidates.size(); index++) {
            probabilities.put(candidates.get(index).getId(), exponents[index] / total);
        }
        return probabilities;
    }

    private static int bestIndex(double[] scores) {
        int best = 0;
        for (int index = 1; index < scores.length; index++) if (scores[index] > scores[best]) best = index;
        return best;
    }

    private static int secondIndex(double[] scores, int best) {
        int second = best == 0 ? 1 : 0;
        for (int index = 0; index < scores.length; index++) {
            if (index != best && scores[index] > scores[second]) second = index;
        }
        return second;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static <T> CompletableFuture<T> completed(T value) {
        return CompletableFuture.completedFuture(value);
    }

}
