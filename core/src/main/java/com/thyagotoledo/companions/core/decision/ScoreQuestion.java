package com.thyagotoledo.companions.core.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Distribui uma pontuação entre níveis ordenados, como perigo baixo/alto. */
public final class ScoreQuestion implements DecisionQuestion {
    private final String id;
    private final String instructions;
    private final List<String> levels;

    public ScoreQuestion(String id, String instructions, List<String> levels) {
        this.id = id != null && !id.trim().isEmpty() ? id.trim() : "score";
        this.instructions = instructions != null ? instructions.trim() : "Score the ordered levels.";
        List<String> copy = new ArrayList<>();
        if (levels != null) for (String level : levels) if (level != null && !level.trim().isEmpty() && copy.size() < 16) copy.add(level.trim());
        this.levels = Collections.unmodifiableList(copy);
    }

    @Override public String getId() { return id; }
    @Override public Type getType() { return Type.SCORE; }
    @Override public String getInstructions() { return instructions; }
    public List<String> getLevels() { return levels; }
}
