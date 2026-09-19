package com.thyagotoledo.companions.neoforge.entity;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.CompanionProfile;
import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;

import java.util.Collections;
import java.util.UUID;

/**
 * Entidade companheira adaptada para a arquitetura NeoForge 21.1.248 (Minecraft 1.21.1).
 */
public class NeoForgeCompanionEntity {

    private final UUID entityUuid;
    private final CompanionProfile profile;
    private final ConversationMemory memory;
    private final HybridDialogueProvider dialogueProvider;
    private final NeoForgePermissionService permissionService;
    private final NeoForgeQuestService questService;

    private InventorySnapshot inventory;

    public NeoForgeCompanionEntity(UUID ownerUuid, String name, Personality personality,
                                  HybridDialogueProvider dialogueProvider,
                                  NeoForgePermissionService permissionService,
                                  NeoForgeQuestService questService) {
        this.entityUuid = UUID.randomUUID();
        this.profile = new CompanionProfile(
                this.entityUuid,
                ownerUuid,
                name != null ? name : "Companheiro",
                CompanionMode.FOLLOW,
                personality != null ? personality : Personality.BALANCED
        );
        this.memory = dialogueProvider != null ? dialogueProvider.getMemory() : new ConversationMemory(6);
        this.dialogueProvider = dialogueProvider;
        this.permissionService = permissionService != null ? permissionService : new NeoForgePermissionService();
        this.questService = questService != null ? questService : new NeoForgeQuestService();
        this.inventory = new InventorySnapshot(Collections.<ItemSlot>emptyList(), 27);
    }

    public DialogueResponse handleCommand(String rawCommand, String preferredLocale) {
        if (dialogueProvider == null) {
            return null;
        }
        return dialogueProvider.processSync(rawCommand, preferredLocale, profile, inventory, 1500L);
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public CompanionProfile getProfile() {
        return profile;
    }

    public ConversationMemory getMemory() {
        return memory;
    }

    public HybridDialogueProvider getDialogueProvider() {
        return dialogueProvider;
    }

    public NeoForgePermissionService getPermissionService() {
        return permissionService;
    }

    public NeoForgeQuestService getQuestService() {
        return questService;
    }

    public InventorySnapshot getInventory() {
        return inventory;
    }

    public void setInventory(InventorySnapshot inventory) {
        this.inventory = inventory != null ? inventory : new InventorySnapshot(Collections.<ItemSlot>emptyList(), 27);
    }
}
