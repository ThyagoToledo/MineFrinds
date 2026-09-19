package com.thyagotoledo.companions.core.quest;

import java.util.List;
import java.util.UUID;

public interface QuestService {
    List<Quest> getAvailableQuests(UUID playerUuid);
    Quest getQuest(UUID playerUuid, String questId);
    boolean isQuestCompleted(UUID playerUuid, String questId);
    boolean areDependenciesMet(UUID playerUuid, String questId);
}
