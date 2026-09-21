package com.thyagotoledo.companions.core.decision;

/** Revalida uma resposta antes de qualquer mutação no mundo. */
public final class DecisionGate {
    private DecisionGate() { }

    public static boolean accepts(DecisionRequest request, DecisionResult result,
                                  long currentTaskRevision, long currentSnapshotRevision,
                                  long nowNanos, double minimumConfidence) {
        if (request == null || result == null || result.getOutcome() != DecisionResult.Outcome.ACCEPT) return false;
        if (request.isExpired(nowNanos)) return false;
        if (request.getTaskRevision() != currentTaskRevision || request.getSnapshotRevision() != currentSnapshotRevision) return false;
        if (result.getConfidence() < Math.max(0.0, Math.min(1.0, minimumConfidence))) return false;
        for (DecisionCandidate candidate : request.getCandidates()) {
            if (candidate.getId().equals(result.getCandidateId())) return true;
        }
        return false;
    }
}
