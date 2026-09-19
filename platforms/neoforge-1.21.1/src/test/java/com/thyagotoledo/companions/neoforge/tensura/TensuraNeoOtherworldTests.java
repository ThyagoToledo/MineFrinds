package com.thyagotoledo.companions.neoforge.tensura;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.ai.MockInferenceClient;
import com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.IntentType;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TensuraNeoOtherworldTests {

    @Test
    public void testTensuraRaceAndEpAttributeScaling() {
        TensuraCompanionStats stats = new TensuraCompanionStats(TensuraRace.OGRE);
        assertEquals(TensuraRace.OGRE, stats.getRace());
        assertEquals(4000, stats.getExistenceValue());
        assertEquals("E", stats.getRank());

        // Teste de escalonamento para 50.000 EP (Rank B)
        stats.setExistenceValue(50000);
        assertEquals("B", stats.getRank());
        assertEquals(5.0, stats.getBonusHealth(), 0.001);
        assertEquals(2.5, stats.getBonusAttack(), 0.001);
        assertEquals(2.5, stats.getBonusArmor(), 0.001);
        assertEquals(25.0, stats.getBonusSpiritualHealth(), 0.001);

        // Teste de escalonamento para 850.000 EP (Catastrophe)
        stats.setExistenceValue(850000);
        assertEquals("Catastrophe", stats.getRank());
        assertEquals(85.0, stats.getBonusHealth(), 0.001);
    }

    @Test
    public void testNamingCeremonyAndRaceEvolution() {
        TensuraCompanionStats ogreStats = new TensuraCompanionStats(TensuraRace.OGRE);
        assertTrue(ogreStats.getRace().canEvolveByNaming());

        boolean evolved = ogreStats.bestowName("Benimaru");
        assertTrue(evolved);
        assertTrue(ogreStats.isNamed());
        assertEquals("Benimaru", ogreStats.getBestowedName());
        assertEquals(TensuraRace.KIJIN, ogreStats.getRace());
        assertTrue(ogreStats.getExistenceValue() >= 15000);

        TensuraCompanionStats goblinStats = new TensuraCompanionStats(TensuraRace.GOBLIN);
        goblinStats.bestowName("Gobta");
        assertEquals(TensuraRace.HOBGOBLIN, goblinStats.getRace());
    }

    @Test
    public void testSubordinateAndAllyProtection() {
        TensuraNeoOtherworldAdapter adapter = new TensuraNeoOtherworldAdapter();
        UUID ownerId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        assertTrue(adapter.isTensuraLoaded());
        assertTrue(adapter.isTensuraTnoLoaded());
        assertTrue(adapter.isFtbTeamsLoaded());
        assertTrue(adapter.isSubordinateProtected(companionId, ownerId));
        assertTrue(adapter.shouldCancelSpiritualDamageInClaim(true));
        assertFalse(adapter.shouldCancelSpiritualDamageInClaim(false));
    }

    @Test
    public void testMagiculePoisonImmunity() {
        TensuraNeoOtherworldAdapter adapter = new TensuraNeoOtherworldAdapter();
        TensuraCompanionStats stats = new TensuraCompanionStats(TensuraRace.HUMAN);

        assertTrue(stats.isMagiculePoisonImmune());
        assertTrue(adapter.shouldProtectFromMagiculePoison(stats, 5000.0)); // Densidade alta de magiculas
    }

    @Test
    public void testCompanionEntityTensuraIntentIntegration() {
        LocaleService localeService = new LocaleService();
        DeterministicDialogueProvider det = new DeterministicDialogueProvider(localeService);
        ConversationMemory memory = new ConversationMemory(6);
        MockInferenceClient mockInference = new MockInferenceClient();
        HybridDialogueProvider hybrid = new HybridDialogueProvider(det, mockInference, memory, localeService);

        NeoForgePermissionService permission = new NeoForgePermissionService();
        NeoForgeQuestService quest = new NeoForgeQuestService();
        UUID ownerUuid = UUID.randomUUID();

        TensuraCompanionStats stats = new TensuraCompanionStats(TensuraRace.OGRE);
        TensuraNeoOtherworldAdapter adapter = new TensuraNeoOtherworldAdapter();

        NeoForgeCompanionEntity entity = new NeoForgeCompanionEntity(
                ownerUuid, "Souei", Personality.BALANCED, hybrid, permission, quest, stats, adapter
        );

        // Teste de consulta de status de magiculas em pt-BR
        DialogueResponse statusResp = entity.handleCommand("status de magiculas", "pt_br");
        assertNotNull(statusResp);
        assertEquals(IntentType.TENSURA_STATUS, statusResp.getIntent().getType());
        assertTrue(statusResp.getSpeech().contains("Relatorio Tensura:"));
        assertTrue(statusResp.getSpeech().contains("Ogro"));

        // Teste de nomeacao e evolucao em pt-BR
        DialogueResponse nameResp = entity.handleCommand("nomear", "pt_br");
        assertNotNull(nameResp);
        assertEquals(IntentType.NAME_GIVING, nameResp.getIntent().getType());
        assertTrue(nameResp.getSpeech().contains("Souei"));
        assertTrue(nameResp.getSpeech().contains("Kijin"));
        assertEquals(TensuraRace.KIJIN, entity.getTensuraStats().getRace());
    }
}
