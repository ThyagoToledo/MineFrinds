package com.thyagotoledo.companions.core.decision;

/** Pergunta binária factual; não expõe confidence como se fosse precisão garantida. */
public final class NoulQuestion implements DecisionQuestion {
    private final String id;
    private final String instructions;
    private final String proposition;

    public NoulQuestion(String id, String instructions, String proposition) {
        this.id = id != null && !id.trim().isEmpty() ? id.trim() : "noul";
        this.instructions = instructions != null ? instructions.trim() : "Evaluate the proposition from the supplied state.";
        this.proposition = proposition != null ? proposition.trim() : "";
    }

    @Override public String getId() { return id; }
    @Override public Type getType() { return Type.NOUL; }
    @Override public String getInstructions() { return instructions; }
    public String getProposition() { return proposition; }
}
