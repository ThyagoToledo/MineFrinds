package com.thyagotoledo.companions.core.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Snapshot pequeno e imutável usado por decisões; não expõe objetos do Minecraft. */
public final class DecisionRequest {
    private final UUID requestId;
    private final UUID companionId;
    private final long taskRevision;
    private final long snapshotRevision;
    private final long deadlineNanos;
    private final String locale;
    private final String message;
    private final Map<String, String> context;
    private final List<DecisionCandidate> candidates;
    private final List<DecisionQuestion> questions;

    public DecisionRequest(UUID requestId, UUID companionId, long taskRevision, long snapshotRevision,
                           long deadlineNanos, String locale, String message,
                           Map<String, String> context, List<DecisionCandidate> candidates) {
        this(requestId, companionId, taskRevision, snapshotRevision, deadlineNanos, locale, message,
                context, candidates, Collections.<DecisionQuestion>emptyList());
    }

    public DecisionRequest(UUID requestId, UUID companionId, long taskRevision, long snapshotRevision,
                           long deadlineNanos, String locale, String message,
                           Map<String, String> context, List<DecisionCandidate> candidates,
                           List<DecisionQuestion> questions) {
        this.requestId = requestId != null ? requestId : UUID.randomUUID();
        this.companionId = companionId;
        this.taskRevision = Math.max(0L, taskRevision);
        this.snapshotRevision = Math.max(0L, snapshotRevision);
        this.deadlineNanos = Math.max(0L, deadlineNanos);
        this.locale = locale != null ? locale : "pt_br";
        this.message = limit(message, 512);
        Map<String, String> copy = new LinkedHashMap<>();
        if (context != null) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                if (copy.size() >= 32) break;
                copy.put(limit(entry.getKey(), 64), limit(entry.getValue(), 256));
            }
        }
        this.context = Collections.unmodifiableMap(copy);
        List<DecisionCandidate> options = new ArrayList<>();
        if (candidates != null) {
            for (DecisionCandidate candidate : candidates) {
                if (candidate != null && !candidate.getId().isEmpty() && options.size() < 64) options.add(candidate);
            }
        }
        this.candidates = Collections.unmodifiableList(options);
        List<DecisionQuestion> questionCopy = new ArrayList<>();
        if (questions != null) {
            for (DecisionQuestion question : questions) {
                if (question != null && questionCopy.size() < 16) questionCopy.add(question);
            }
        }
        this.questions = Collections.unmodifiableList(questionCopy);
    }

    public UUID getRequestId() { return requestId; }
    public UUID getCompanionId() { return companionId; }
    public long getTaskRevision() { return taskRevision; }
    public long getSnapshotRevision() { return snapshotRevision; }
    public long getDeadlineNanos() { return deadlineNanos; }
    public String getLocale() { return locale; }
    public String getMessage() { return message; }
    public Map<String, String> getContext() { return context; }
    public List<DecisionCandidate> getCandidates() { return candidates; }
    public List<DecisionQuestion> getQuestions() { return questions; }

    public boolean isExpired(long nowNanos) { return deadlineNanos > 0L && nowNanos >= deadlineNanos; }

    private static String limit(String value, int max) {
        if (value == null) return "";
        String clean = value.replaceAll("[\\p{Cntrl}&&[^\\t]]", "").trim();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }
}
