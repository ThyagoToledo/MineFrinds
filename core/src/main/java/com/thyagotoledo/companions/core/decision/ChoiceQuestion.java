package com.thyagotoledo.companions.core.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Escolha exatamente um ID entre opções fechadas. */
public final class ChoiceQuestion implements DecisionQuestion {
    private final String id;
    private final String instructions;
    private final List<String> options;

    public ChoiceQuestion(String id, String instructions, List<String> options) {
        this.id = clean(id, "choice");
        this.instructions = clean(instructions, "Choose one allowed option.");
        List<String> copy = new ArrayList<>();
        if (options != null) {
            for (String option : options) {
                if (option != null && !option.trim().isEmpty() && copy.size() < 64) copy.add(clean(option, ""));
            }
        }
        this.options = Collections.unmodifiableList(copy);
    }

    @Override public String getId() { return id; }
    @Override public Type getType() { return Type.CHOICE; }
    @Override public String getInstructions() { return instructions; }
    public List<String> getOptions() { return options; }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String result = value.replaceAll("[\\p{Cntrl}&&[^\\t]]", "").trim();
        return result.length() > 256 ? result.substring(0, 256) : result;
    }
}
