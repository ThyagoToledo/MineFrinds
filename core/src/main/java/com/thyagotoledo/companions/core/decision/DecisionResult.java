package com.thyagotoledo.companions.core.decision;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resultado estruturado; ausência/erro é uma decisão explícita de abstenção. */
public final class DecisionResult {
    public enum Outcome { ACCEPT, ABSTAIN, ERROR }

    private final String candidateId;
    private final Outcome outcome;
    private final double confidence;
    private final Map<String, Double> probabilities;
    private final String provider;
    private final String reason;

    private DecisionResult(String candidateId, Outcome outcome, double confidence,
                           Map<String, Double> probabilities, String provider, String reason) {
        this.candidateId = candidateId != null ? candidateId : "";
        this.outcome = outcome != null ? outcome : Outcome.ERROR;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.probabilities = probabilities == null ? Collections.emptyMap() :
                Collections.unmodifiableMap(new LinkedHashMap<>(probabilities));
        this.provider = provider != null ? provider : "unknown";
        this.reason = reason != null ? reason : "";
    }

    public static DecisionResult accept(String candidateId, double confidence, String provider, String reason) {
        return new DecisionResult(candidateId, Outcome.ACCEPT, confidence, null, provider, reason);
    }

    public static DecisionResult abstain(String provider, String reason) {
        return new DecisionResult("", Outcome.ABSTAIN, 0.0, null, provider, reason);
    }

    public static DecisionResult error(String provider, String reason) {
        return new DecisionResult("", Outcome.ERROR, 0.0, null, provider, reason);
    }

    public String getCandidateId() { return candidateId; }
    public Outcome getOutcome() { return outcome; }
    public double getConfidence() { return confidence; }
    public Map<String, Double> getProbabilities() { return probabilities; }
    public String getProvider() { return provider; }
    public String getReason() { return reason; }
}
