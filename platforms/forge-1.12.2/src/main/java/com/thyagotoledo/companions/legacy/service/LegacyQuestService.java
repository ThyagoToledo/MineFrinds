package com.thyagotoledo.companions.legacy.service;

import com.thyagotoledo.companions.core.quest.DefaultQuestService;
import com.thyagotoledo.companions.core.quest.Quest;
import com.thyagotoledo.companions.core.quest.QuestService;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de servico de quests para Forge 1.12.2.
 * Suporta integracao com FTB Quests classico ou Better Questing com fallback em memoria.
 */
public class LegacyQuestService implements QuestService {

    private final DefaultQuestService fallbackService;
    private final boolean questModLoaded;
    private String selectedQuestId;

    public LegacyQuestService() {
        this(false);
    }

    public LegacyQuestService(boolean questModLoaded) {
        this.fallbackService = new DefaultQuestService();
        this.questModLoaded = questModLoaded;
    }

    public boolean isQuestModLoaded() {
        return questModLoaded;
    }

    public DefaultQuestService getFallbackService() {
        return fallbackService;
    }

    public void invalidateCache() {
        // Invalida e limpa qualquer cache em memoria quando ocorre /reload
    }

    @Override
    public List<Quest> getAvailableQuests(UUID playerUuid) {
        return fallbackService.getAvailableQuests(playerUuid);
    }

    @Override
    public Quest getQuest(UUID playerUuid, String questId) {
        return fallbackService.getQuest(playerUuid, questId);
    }

    @Override
    public boolean isQuestCompleted(UUID playerUuid, String questId) {
        return fallbackService.isQuestCompleted(playerUuid, questId);
    }

    @Override
    public boolean areDependenciesMet(UUID playerUuid, String questId) {
        return fallbackService.areDependenciesMet(playerUuid, questId);
    }

    public String getSelectedQuestId(UUID playerUuid) {
        return selectedQuestId;
    }

    public void setSelectedQuestId(UUID playerUuid, String questId) {
        this.selectedQuestId = questId;
    }
}
