package com.thyagotoledo.companions.core.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DefaultQuestService implements QuestService {
    private final Map<String, Quest> questsById = new HashMap<>();
    private final Map<UUID, Set<String>> completedQuestsByPlayer = new HashMap<>();

    public void registerQuest(Quest quest) {
        if (quest != null) {
            questsById.put(quest.getId(), quest);
        }
    }

    public void setQuestCompleted(UUID playerUuid, String questId) {
        completedQuestsByPlayer.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(questId);
    }

    @Override
    public List<Quest> getAvailableQuests(UUID playerUuid) {
        List<Quest> available = new ArrayList<>();
        Set<String> completed = completedQuestsByPlayer.getOrDefault(playerUuid, Collections.emptySet());

        for (Quest quest : questsById.values()) {
            if (quest.isHidden()) {
                continue;
            }
            if (completed.contains(quest.getId())) {
                continue;
            }
            if (!areDependenciesMet(playerUuid, quest.getId())) {
                continue;
            }
            available.add(quest);
        }
        return available;
    }

    @Override
    public Quest getQuest(UUID playerUuid, String questId) {
        Quest q = questsById.get(questId);
        if (q != null && q.isHidden()) {
            return null;
        }
        return q;
    }

    @Override
    public boolean isQuestCompleted(UUID playerUuid, String questId) {
        Set<String> completed = completedQuestsByPlayer.get(playerUuid);
        return completed != null && completed.contains(questId);
    }

    @Override
    public boolean areDependenciesMet(UUID playerUuid, String questId) {
        Quest quest = questsById.get(questId);
        if (quest == null) return false;
        Set<String> completed = completedQuestsByPlayer.getOrDefault(playerUuid, Collections.emptySet());
        for (String depId : quest.getDependencies()) {
            if (!completed.contains(depId)) {
                return false;
            }
        }
        return true;
    }
}
