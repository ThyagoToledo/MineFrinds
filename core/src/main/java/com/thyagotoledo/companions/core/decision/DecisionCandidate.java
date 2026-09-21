package com.thyagotoledo.companions.core.decision;

/** Ação fechada que um provedor pode escolher; nunca contém código executável. */
public final class DecisionCandidate {
    private final String id;
    private final String description;

    public DecisionCandidate(String id, String description) {
        this.id = clean(id, 64);
        this.description = clean(description, 256);
    }

    public String getId() { return id; }
    public String getDescription() { return description; }

    private static String clean(String value, int max) {
        if (value == null) return "";
        String clean = value.replaceAll("[\\p{Cntrl}&&[^\\t]]", "").trim();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }
}
