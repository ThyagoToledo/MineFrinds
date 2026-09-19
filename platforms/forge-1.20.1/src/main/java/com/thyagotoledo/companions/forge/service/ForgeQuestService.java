package com.thyagotoledo.companions.forge.service;

import com.thyagotoledo.companions.core.quest.DefaultQuestService;
import com.thyagotoledo.companions.core.quest.Quest;
import com.thyagotoledo.companions.core.quest.QuestService;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.UUID;

public class ForgeQuestService implements QuestService {
    private final DefaultQuestService fallbackService = new DefaultQuestService();
    private final boolean ftbQuestsLoaded;

    public ForgeQuestService() {
        this.ftbQuestsLoaded = ModList.get() != null && ModList.get().isLoaded("ftbquests");
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
        if (!ftbQuestsLoaded) {
            return fallbackService.getAvailableQuests(playerUuid);
        }
        try {
            return fallbackService.getAvailableQuests(playerUuid);
        } catch (Throwable t) {
            return fallbackService.getAvailableQuests(playerUuid);
        }
    }

    @Override
    public Quest getQuest(UUID playerUuid, String questId) {
        if (!ftbQuestsLoaded) {
            return fallbackService.getQuest(playerUuid, questId);
        }
        try {
            return fallbackService.getQuest(playerUuid, questId);
        } catch (Throwable t) {
            return fallbackService.getQuest(playerUuid, questId);
        }
    }

    @Override
    public boolean isQuestCompleted(UUID playerUuid, String questId) {
        if (!ftbQuestsLoaded) {
            return fallbackService.isQuestCompleted(playerUuid, questId);
        }
        try {
            return fallbackService.isQuestCompleted(playerUuid, questId);
        } catch (Throwable t) {
            return fallbackService.isQuestCompleted(playerUuid, questId);
        }
    }

    @Override
    public boolean areDependenciesMet(UUID playerUuid, String questId) {
        if (!ftbQuestsLoaded) {
            return fallbackService.areDependenciesMet(playerUuid, questId);
        }
        try {
            return fallbackService.areDependenciesMet(playerUuid, questId);
        } catch (Throwable t) {
            return fallbackService.areDependenciesMet(playerUuid, questId);
        }
    }
}
