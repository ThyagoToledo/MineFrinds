package com.thyagotoledo.companions.neoforge.entity;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;
import com.thyagotoledo.companions.neoforge.tensura.TensuraCompanionStats;
import com.thyagotoledo.companions.neoforge.tensura.TensuraNeoOtherworldAdapter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador central de instancias de companheiros por jogador no servidor e cliente.
 */
public class CompanionManager {

    private static final Map<UUID, NeoForgeCompanionEntity> COMPANIONS_BY_OWNER = new ConcurrentHashMap<>();

    public static NeoForgeCompanionEntity getCompanionForOwner(UUID ownerUuid) {
        if (ownerUuid == null) return null;
        return COMPANIONS_BY_OWNER.get(ownerUuid);
    }

    public static NeoForgeCompanionEntity getOrCreateCompanion(UUID ownerUuid, String customName) {
        if (ownerUuid == null) return null;

        return COMPANIONS_BY_OWNER.computeIfAbsent(ownerUuid, uuid -> {
            String finalName = (customName != null && !customName.trim().isEmpty())
                    ? customName.trim()
                    : "Companheiro";

            ConversationMemory memory = new ConversationMemory(6);
            HybridDialogueProvider dialogueProvider = new HybridDialogueProvider(null, null, memory, null);
            NeoForgePermissionService permissionService = new NeoForgePermissionService();
            NeoForgeQuestService questService = new NeoForgeQuestService();
            TensuraCompanionStats tensuraStats = new TensuraCompanionStats();
            TensuraNeoOtherworldAdapter tensuraAdapter = new TensuraNeoOtherworldAdapter();

            return new NeoForgeCompanionEntity(
                    uuid,
                    finalName,
                    Personality.BALANCED,
                    dialogueProvider,
                    permissionService,
                    questService,
                    tensuraStats,
                    tensuraAdapter
            );
        });
    }

    public static NeoForgeCompanionEntity spawnCompanion(UUID ownerUuid, String customName) {
        if (ownerUuid == null) return null;
        String finalName = (customName != null && !customName.trim().isEmpty())
                ? customName.trim()
                : "Companheiro";

        ConversationMemory memory = new ConversationMemory(6);
        HybridDialogueProvider dialogueProvider = new HybridDialogueProvider(null, null, memory, null);
        NeoForgePermissionService permissionService = new NeoForgePermissionService();
        NeoForgeQuestService questService = new NeoForgeQuestService();
        TensuraCompanionStats tensuraStats = new TensuraCompanionStats();
        TensuraNeoOtherworldAdapter tensuraAdapter = new TensuraNeoOtherworldAdapter();

        NeoForgeCompanionEntity entity = new NeoForgeCompanionEntity(
                ownerUuid,
                finalName,
                Personality.BALANCED,
                dialogueProvider,
                permissionService,
                questService,
                tensuraStats,
                tensuraAdapter
        );

        COMPANIONS_BY_OWNER.put(ownerUuid, entity);
        return entity;
    }

    public static void setCustomSkin(UUID ownerUuid, String skinName) {
        NeoForgeCompanionEntity entity = getCompanionForOwner(ownerUuid);
        if (entity != null) {
            entity.setCustomSkin(skinName);
        }
    }

    public static void removeCompanion(UUID ownerUuid) {
        if (ownerUuid != null) {
            COMPANIONS_BY_OWNER.remove(ownerUuid);
        }
    }

    public static void clearAll() {
        COMPANIONS_BY_OWNER.clear();
    }
}
