package com.thyagotoledo.companions.neoforge.entity;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.CompanionCommandRequest;
import com.thyagotoledo.companions.core.model.CompanionProfile;
import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;
import com.thyagotoledo.companions.neoforge.tensura.TensuraCompanionStats;
import com.thyagotoledo.companions.neoforge.tensura.TensuraNeoOtherworldAdapter;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    private final TensuraCompanionStats tensuraStats;
    private final TensuraNeoOtherworldAdapter tensuraAdapter;
    private InventorySnapshot inventory;
    private String customSkin = "";
    private boolean modelSlim = false;

    public NeoForgeCompanionEntity(UUID ownerUuid, String name, Personality personality,
                                  HybridDialogueProvider dialogueProvider,
                                  NeoForgePermissionService permissionService,
                                  NeoForgeQuestService questService) {
        this(ownerUuid, name, personality, dialogueProvider, permissionService, questService,
                new TensuraCompanionStats(), new TensuraNeoOtherworldAdapter());
    }

    public NeoForgeCompanionEntity(UUID ownerUuid, String name, Personality personality,
                                  HybridDialogueProvider dialogueProvider,
                                  NeoForgePermissionService permissionService,
                                  NeoForgeQuestService questService,
                                  TensuraCompanionStats tensuraStats,
                                  TensuraNeoOtherworldAdapter tensuraAdapter) {
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
        this.tensuraStats = tensuraStats != null ? tensuraStats : new TensuraCompanionStats();
        this.tensuraAdapter = tensuraAdapter != null ? tensuraAdapter : new TensuraNeoOtherworldAdapter();
        this.inventory = new InventorySnapshot(Collections.<ItemSlot>emptyList(), 27);
        this.customSkin = "";
        this.modelSlim = false;
    }

    public DialogueResponse handleCommand(String rawCommand, String preferredLocale) {
        if (dialogueProvider == null) {
            return null;
        }
        // Compatibilidade de API síncrona para integrações antigas/testes. Os
        // comandos do jogo usam handleCommandAsync e não entram neste método.
        DialogueResponse response = dialogueProvider.processSync(
                rawCommand, preferredLocale, profile, inventory, 1500L);
        if (response != null && response.getIntent() != null) {
            if (response.getIntent().getType() == com.thyagotoledo.companions.core.dialogue.IntentType.TENSURA_STATUS) {
                String formatted = tensuraAdapter.formatTensuraStatus(tensuraStats, preferredLocale);
                return new DialogueResponse(response.getLocale(), formatted, response.getIntent());
            } else if (response.getIntent().getType() == com.thyagotoledo.companions.core.dialogue.IntentType.NAME_GIVING) {
                String newName = profile.getName();
                tensuraAdapter.nameCompanion(tensuraStats, newName);
                String raceName = tensuraStats.getRace().getDisplayName(preferredLocale);
                String msg = (preferredLocale != null && preferredLocale.toLowerCase().startsWith("pt"))
                        ? String.format("Com o nome %s concedido por meu mestre, sinto minhas magiculas despertarem e evoluo para a raca %s!", newName, raceName)
                        : String.format("With the name %s bestowed by my master, I feel my magicules awaken and evolve into race %s!", newName, raceName);
                return new DialogueResponse(response.getLocale(), msg, response.getIntent());
            }
        }
        return response;
    }

    /**
     * Enfileira a conversa no supervisor de IA sem bloquear a thread do servidor.
     * O chamador deve aplicar efeitos de mundo no executor do servidor.
     */
    public CompletableFuture<DialogueResponse> handleCommandAsync(String rawCommand, String preferredLocale) {
        if (dialogueProvider == null) {
            return CompletableFuture.completedFuture(null);
        }
        return dialogueProvider.processAsync(rawCommand, preferredLocale, profile, inventory);
    }

    /** Processa somente conversa livre já validada pelo protocolo C2S. */
    public CompletableFuture<DialogueResponse> handleCommandAsync(CompanionCommandRequest request) {
        if (request == null || !request.isValid()
                || request.getIntent() != com.thyagotoledo.companions.core.dialogue.IntentType.CASUAL_CHAT) {
            return CompletableFuture.completedFuture(null);
        }
        return handleCommandAsync(request.getTarget(), request.getLocale());
    }

    public TensuraCompanionStats getTensuraStats() {
        return tensuraStats;
    }

    public TensuraNeoOtherworldAdapter getTensuraAdapter() {
        return tensuraAdapter;
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

    public String getCustomSkin() {
        return customSkin;
    }

    public void setCustomSkin(String customSkin) {
        this.customSkin = customSkin != null ? customSkin.trim() : "";
    }

    private CompanionMode currentMode = CompanionMode.FOLLOW;

    public boolean isModelSlim() {
        return modelSlim;
    }

    public void setModelSlim(boolean modelSlim) {
        this.modelSlim = modelSlim;
    }

    public UUID getOwnerUuid() {
        return profile != null ? profile.getOwnerId() : null;
    }

    public String getName() {
        return profile != null ? profile.getName() : "Companheiro";
    }

    public CompanionMode getMode() {
        return currentMode;
    }

    public Personality getPersonality() {
        return profile != null ? profile.getPersonality() : Personality.BALANCED;
    }


    public void setMode(CompanionMode mode) {
        if (mode != null) {
            this.currentMode = mode;
        }
    }
}
