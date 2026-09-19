package com.thyagotoledo.companions.legacy;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.ai.MockInferenceClient;
import com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.IntentType;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.legacy.entity.LegacyCompanionEntity;
import com.thyagotoledo.companions.legacy.network.LegacyCompanionNetwork;
import com.thyagotoledo.companions.legacy.service.LegacyPermissionService;
import com.thyagotoledo.companions.legacy.service.LegacyQuestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes automatizados para a plataforma Forge 1.12.2 (Java 8).
 */
public class LegacyForgeCompanionTests {

    @Test
    @DisplayName("P8 - Validar metadados da plataforma Forge 1.12.2")
    void testModMetadataAndLoader() {
        assertEquals("companions", CompanionsLegacyForgeMod.MODID);
        assertEquals("0.1.0-1.12.2", CompanionsLegacyForgeMod.VERSION);
        assertEquals("1.12.2", CompanionsLegacyForgeMod.MINECRAFT_VERSION);
        assertEquals("forge", CompanionsLegacyForgeMod.LOADER);
    }

    @Test
    @DisplayName("P8 - Validar serializacao e desserializacao de rede IMessage (1.12.2)")
    void testNetworkSerializationIMessage() throws IOException {
        UUID companionUuid = UUID.randomUUID();

        // 1. Mensagem de comando do jogador
        LegacyCompanionNetwork.CommandMessage originalCmd =
                new LegacyCompanionNetwork.CommandMessage(companionUuid, "fica aqui", "pt_br");
        byte[] cmdBytes = originalCmd.toBytes();
        assertNotNull(cmdBytes);
        assertTrue(cmdBytes.length > 0);

        LegacyCompanionNetwork.CommandMessage decodedCmd = new LegacyCompanionNetwork.CommandMessage();
        decodedCmd.fromBytes(cmdBytes);
        assertEquals(companionUuid, decodedCmd.getCompanionUuid());
        assertEquals("fica aqui", decodedCmd.getCommand());
        assertEquals("pt_br", decodedCmd.getLocale());

        // 2. Mensagem de feedback do companheiro
        LegacyCompanionNetwork.FeedbackMessage originalFb =
                new LegacyCompanionNetwork.FeedbackMessage(companionUuid, "Vou ficar aqui esperando.", "pt_br", true);
        byte[] fbBytes = originalFb.toBytes();
        assertNotNull(fbBytes);

        LegacyCompanionNetwork.FeedbackMessage decodedFb = new LegacyCompanionNetwork.FeedbackMessage();
        decodedFb.fromBytes(fbBytes);
        assertEquals(companionUuid, decodedFb.getCompanionUuid());
        assertEquals("Vou ficar aqui esperando.", decodedFb.getSpeech());
        assertEquals("pt_br", decodedFb.getLocale());
        assertTrue(decodedFb.isSuccess());
    }

    @Test
    @DisplayName("P8 - Validar protecao de claims legados (FTB Utilities / GriefPrevention)")
    void testLegacyPermissionAndClaims() {
        LegacyPermissionService permService = new LegacyPermissionService();
        UUID unauthorizedPlayer = UUID.randomUUID();

        // Area livre
        assertTrue(permService.canBreakBlockAt(unauthorizedPlayer, "minecraft:overworld", 10, 64, 10));

        // Area restrita (claim protegido)
        permService.getFallbackService().addRestrictedArea("minecraft:overworld", 200, 65, 200);
        assertFalse(permService.canBreakBlockAt(unauthorizedPlayer, "minecraft:overworld", 200, 65, 200));
        assertFalse(permService.canInteractAt(unauthorizedPlayer, "minecraft:overworld", 200, 65, 200));

        // Verificacao contra nulos
        assertFalse(permService.canBreakBlockAt(null, "minecraft:overworld", 10, 64, 10));
    }

    @Test
    @DisplayName("P8 - Validar execucao da entidade companheira Legacy e orquestrador hibrido")
    void testLegacyCompanionEntityDialogueAndExecution() {
        LocaleService localeService = new LocaleService();
        DeterministicDialogueProvider detProvider = new DeterministicDialogueProvider(localeService);
        MockInferenceClient mockClient = new MockInferenceClient();
        ConversationMemory memory = new ConversationMemory(6);
        HybridDialogueProvider hybridProvider = new HybridDialogueProvider(detProvider, mockClient, memory, localeService);

        UUID ownerUuid = UUID.randomUUID();
        LegacyCompanionEntity companion = new LegacyCompanionEntity(
                ownerUuid, "SteveBot", Personality.BALANCED, hybridProvider, null, null
        );

        assertEquals("SteveBot", companion.getProfile().getName());
        assertEquals(CompanionMode.FOLLOW, companion.getProfile().getMode());

        // Ordem deterministica direta (0ms)
        DialogueResponse followResp = companion.handleCommand("me segue", "pt_br");
        assertNotNull(followResp);
        assertEquals(IntentType.FOLLOW_OWNER, followResp.getIntent().getType());
        assertEquals("Entendido, estou te seguindo.", followResp.getSpeech());

        // Dialogo generativo aberto via mock SLM
        mockClient.setNextResponse("{\"intent\": \"CASUAL_CHAT\", \"speech\": \"Explorando o mundo legado 1.12.2 com estabilidade.\"}");
        DialogueResponse chatResp = companion.handleCommand("o que voce acha dessa regiao?", "pt_br");
        assertNotNull(chatResp);
        assertEquals(IntentType.CASUAL_CHAT, chatResp.getIntent().getType());
        assertEquals("Explorando o mundo legado 1.12.2 com estabilidade.", chatResp.getSpeech());
        assertEquals(4, companion.getMemory().size());
    }

    @Test
    @DisplayName("P8 - Validar resiliencia a reload e servico de quests na 1.12.2")
    void testLegacyQuestServiceReloadAndCaching() {
        LegacyQuestService questService = new LegacyQuestService(true);
        assertTrue(questService.isQuestModLoaded());

        UUID playerUuid = UUID.randomUUID();
        assertNull(questService.getSelectedQuestId(playerUuid));

        questService.setSelectedQuestId(playerUuid, "legacy_quest_crafting_table");
        assertEquals("legacy_quest_crafting_table", questService.getSelectedQuestId(playerUuid));

        assertDoesNotThrow(questService::invalidateCache);
    }
}
