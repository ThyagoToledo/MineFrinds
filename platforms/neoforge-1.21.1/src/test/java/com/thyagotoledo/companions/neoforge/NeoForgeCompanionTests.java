package com.thyagotoledo.companions.neoforge;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.ai.MockInferenceClient;
import com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.IntentType;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import com.thyagotoledo.companions.neoforge.network.NeoForgeCompanionPayloads;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes automatizados para a plataforma NeoForge 21.1.248 (Minecraft 1.21.1).
 */
public class NeoForgeCompanionTests {

    @Test
    @DisplayName("P7 - Validar metadados da plataforma NeoForge 1.21.1")
    void testModMetadataAndLoader() {
        assertEquals("companions", CompanionsNeoForgeMod.MODID);
        assertEquals("0.1.0-1.21.1", CompanionsNeoForgeMod.VERSION);
        assertEquals("neoforge", CompanionsNeoForgeMod.LOADER);
        assertEquals("1.21.1", CompanionsNeoForgeMod.MINECRAFT_VERSION);
    }

    @Test
    @DisplayName("P7 - Validar adaptadores de rede CustomPacketPayload (1.21.1)")
    void testPayloadContracts1211() {
        UUID companionUuid = UUID.randomUUID();

        NeoForgeCompanionPayloads.CommandPayload cmdPayload =
                new NeoForgeCompanionPayloads.CommandPayload(companionUuid, "me segue", "pt_br");
        assertEquals("companions:command_payload", cmdPayload.getChannelName());
        assertEquals("me segue", cmdPayload.getCommand());
        assertEquals("pt_br", cmdPayload.getLocale());
        assertEquals(companionUuid, cmdPayload.getCompanionUuid());

        NeoForgeCompanionPayloads.FeedbackPayload feedbackPayload =
                new NeoForgeCompanionPayloads.FeedbackPayload(companionUuid, "Entendido, estou te seguindo.", "pt_br", true);
        assertEquals("companions:feedback_payload", feedbackPayload.getChannelName());
        assertEquals("Entendido, estou te seguindo.", feedbackPayload.getSpeech());
        assertTrue(feedbackPayload.isSuccess());
    }

    @Test
    @DisplayName("P7 - Validar protecao de claims composta (Tensura OPAC + FTB Chunks NeoForge)")
    void testTensuraAndFtbProtectionOnNeoForge() {
        NeoForgePermissionService permService = new NeoForgePermissionService();
        UUID unauthorizedPlayer = UUID.randomUUID();

        // Area livre: mineracao permitida
        assertTrue(permService.canBreakBlockAt(unauthorizedPlayer, "minecraft:overworld", 10, 64, 10));

        // Area reivindicada por faccao Tensura ou time FTB Chunks NeoForge
        permService.getFallbackService().addRestrictedArea("minecraft:overworld", 500, 70, 500);

        // Bloqueio rigoroso confirmado
        assertFalse(permService.canBreakBlockAt(unauthorizedPlayer, "minecraft:overworld", 500, 70, 500));
        assertFalse(permService.canInteractAt(unauthorizedPlayer, "minecraft:overworld", 500, 70, 500));

        // Verificacao de null-safety
        assertFalse(permService.canBreakBlockAt(null, "minecraft:overworld", 10, 64, 10));
    }

    @Test
    @DisplayName("P7 - Validar execucao da entidade companheira NeoForge e orquestrador de IA")
    void testNeoForgeCompanionEntityDialogueAndExecution() {
        LocaleService localeService = new LocaleService();
        DeterministicDialogueProvider detProvider = new DeterministicDialogueProvider(localeService);
        MockInferenceClient mockClient = new MockInferenceClient();
        ConversationMemory memory = new ConversationMemory(6);
        HybridDialogueProvider hybridProvider = new HybridDialogueProvider(detProvider, mockClient, memory, localeService);

        UUID ownerUuid = UUID.randomUUID();
        NeoForgeCompanionEntity companion = new NeoForgeCompanionEntity(
                ownerUuid, "RimuruBot", Personality.BALANCED, hybridProvider, null, null
        );

        assertEquals("RimuruBot", companion.getProfile().getName());
        assertEquals(CompanionMode.FOLLOW, companion.getProfile().getMode());

        // Ordem deterministica direta
        DialogueResponse stayResp = companion.handleCommand("fica aqui", "pt_br");
        assertNotNull(stayResp);
        assertEquals(IntentType.STAY, stayResp.getIntent().getType());
        assertEquals("Vou ficar aqui esperando.", stayResp.getSpeech());

        // Dialogo aberto generativo via mock SLM
        mockClient.setNextResponse("{\"intent\": \"CASUAL_CHAT\", \"speech\": \"Tudo calmo na Federacao de Jura Tempest.\"}");
        DialogueResponse chatResp = companion.handleCommand("como esta a tempest hoje?", "pt_br");
        assertNotNull(chatResp);
        assertEquals(IntentType.CASUAL_CHAT, chatResp.getIntent().getType());
        assertEquals("Tudo calmo na Federacao de Jura Tempest.", chatResp.getSpeech());
        assertEquals(4, companion.getMemory().size());
    }

    @Test
    @DisplayName("P7 - Validar resiliencia a reload e servico de quests no NeoForge 1.21.1")
    void testNeoForgeReloadAndQuestCache() {
        NeoForgeQuestService questService = new NeoForgeQuestService(true);
        assertTrue(questService.isFtbQuestsLoaded());

        UUID playerUuid = UUID.randomUUID();
        assertNull(questService.getSelectedQuestId(playerUuid));

        questService.setSelectedQuestId(playerUuid, "quest_tensura_slime");
        assertEquals("quest_tensura_slime", questService.getSelectedQuestId(playerUuid));

        // Teste de invalidacao de cache pos /reload
        assertDoesNotThrow(questService::invalidateCache);
    }
}
