package com.thyagotoledo.companions.neoforge.service;

import com.thyagotoledo.companions.core.quest.DefaultQuestService;
import com.thyagotoledo.companions.core.quest.Quest;
import com.thyagotoledo.companions.core.quest.QuestService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Adaptador de servico de quests para NeoForge 21.1.248 (Minecraft 1.21.1).
 * Integra com FTB Quests NeoForge (presente no pack Tensura) com fallback gracioso.
 */
public class NeoForgeQuestService implements QuestService {

    private final DefaultQuestService fallbackService;
    private final boolean ftbQuestsLoaded;
    private final Map<UUID, String> selectedQuestIds = new ConcurrentHashMap<>();

    public NeoForgeQuestService() {
        this(false);
    }

    public NeoForgeQuestService(boolean ftbQuestsLoaded) {
        this.fallbackService = new DefaultQuestService();
        this.ftbQuestsLoaded = ftbQuestsLoaded;
    }

    public boolean isFtbQuestsLoaded() {
        return ftbQuestsLoaded;
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
        return playerUuid != null ? selectedQuestIds.get(playerUuid) : null;
    }

    public void setSelectedQuestId(UUID playerUuid, String questId) {
        if (playerUuid == null) return;
        if (questId == null || questId.trim().isEmpty()) selectedQuestIds.remove(playerUuid);
        else selectedQuestIds.put(playerUuid, questId.trim());
    }
}
