package com.thyagotoledo.companions.core.quest;

import java.util.Collections;
import java.util.Map;

public class QuestPlanResult {
    public enum Status {
        READY_TO_SUBMIT,
        MISSING_ITEMS,
        BLOCKED_DEPENDENCY,
        UNKNOWN_RECIPE_OR_MACHINE,
        QUEST_NOT_FOUND
    }

    private final Status status;
    private final String questId;
    private final Map<String, Integer> missingItems;
    private final Map<String, Integer> rawMaterialsNeeded;
    private final String explanation;

    public QuestPlanResult(Status status, String questId, Map<String, Integer> missingItems,
                           Map<String, Integer> rawMaterialsNeeded, String explanation) {
        this.status = status;
        this.questId = questId;
        this.missingItems = missingItems != null ? missingItems : Collections.emptyMap();
        this.rawMaterialsNeeded = rawMaterialsNeeded != null ? rawMaterialsNeeded : Collections.emptyMap();
        this.explanation = explanation != null ? explanation : "";
    }

    public Status getStatus() {
        return status;
    }

    public String getQuestId() {
        return questId;
    }

    public Map<String, Integer> getMissingItems() {
        return Collections.unmodifiableMap(missingItems);
    }

    public Map<String, Integer> getRawMaterialsNeeded() {
        return Collections.unmodifiableMap(rawMaterialsNeeded);
    }

    public String getExplanation() {
        return explanation;
    }
}
