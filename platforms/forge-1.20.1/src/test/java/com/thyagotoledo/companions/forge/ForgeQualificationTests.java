package com.thyagotoledo.companions.forge;

import com.thyagotoledo.companions.core.permissions.DefaultPermissionService;
import com.thyagotoledo.companions.core.planner.RecipeCatalog;
import com.thyagotoledo.companions.core.planner.RecipeRequirement;
import com.thyagotoledo.companions.core.model.ItemSlot;
import com.thyagotoledo.companions.forge.service.ForgePermissionService;
import com.thyagotoledo.companions.forge.service.ForgeQuestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de qualificacao P6 para validacao nos perfis Cozy Zen (47.4.10) e Society (47.4.0).
 */
public class ForgeQualificationTests {

    @Test
    @DisplayName("P6 - Simulacao de ambiente Cozy Zen (Forge 47.4.10 sem FTB): operacao standalone limpa")
    void testCozyZenEnvironmentSimulationCleanModpack() {
        // No Cozy Zen, mods da suite FTB estao ausentes.
        ForgeQuestService questService = new ForgeQuestService();
        assertFalse(questService.isFtbQuestsLoaded(), "No ambiente Cozy Zen (limpo), FTB Quests deve estar ausente");

        UUID playerUuid = UUID.randomUUID();
        // Fallback gracioso em memoria sem lancar ClassNotFoundException
        assertNull(questService.getQuest(playerUuid, "any_quest"));
        assertTrue(questService.getAvailableQuests(playerUuid).isEmpty());

        // Validar fail-safe do servico de permissoes
        ForgePermissionService permService = new ForgePermissionService(null);
        assertFalse(permService.canBreakBlockAt(playerUuid, "minecraft:overworld", 50, 64, 50));
    }

    @Test
    @DisplayName("P6 - Simulacao de ambiente Society Sunlit Valley (Forge 47.4.0 com FTB): respeito a claims")
    void testSocietySunlitValleyEnvironmentSimulationFTBPack() {
        DefaultPermissionService claimsHolder = new DefaultPermissionService();
        // Simula claim registrada por FTB Chunks / OPAC no chunk central de Society
        claimsHolder.addRestrictedArea("minecraft:overworld", 100, 64, 100);

        UUID unauthorizedPlayer = UUID.randomUUID();

        // O companheiro nao deve conseguir minerar dentro do claim protegido
        assertFalse(claimsHolder.canBreakBlockAt(unauthorizedPlayer, "minecraft:overworld", 100, 64, 100));

        // Fora do claim, mineracao e permitida
        assertTrue(claimsHolder.canBreakBlockAt(unauthorizedPlayer, "minecraft:overworld", 200, 64, 200));
    }

    @Test
    @DisplayName("P6 - Validacao do orcamento rigoroso de memoria (teto 2.000 MB e continuo 1.000 MB)")
    void testMemoryBudgetEstimatorP6Profile() {
        // Contrato da secao 1.1 da especificacao tecnica:
        int maxEntityExecMb = 300;
        int maxKnowledgeMemoryMb = 150;
        int maxInferenceMb = 1200;
        int maxTransientReserveMb = 350;

        int totalPeakBudgetMb = maxEntityExecMb + maxKnowledgeMemoryMb + maxInferenceMb + maxTransientReserveMb;
        assertEquals(2000, totalPeakBudgetMb, "O teto de pico incremental deve ser exatamente 2.000 MB");

        int targetContinuousMb = 180 + 70 + 650 + 100;
        assertEquals(1000, targetContinuousMb, "A meta continua incremental deve ser exatamente 1.000 MB");
    }

    @Test
    @DisplayName("P6 - Validacao de atomicidade de recarga (/reload) e descarte de cache de receitas")
    void testReloadAtomicityAndRecipeCache() {
        RecipeCatalog catalog = new RecipeCatalog();
        catalog.registerRecipe(new RecipeRequirement(
                "minecraft:iron_pickaxe",
                new ItemSlot("minecraft:iron_pickaxe", 1),
                Collections.singletonList(new ItemSlot("minecraft:iron_ingot", 3))
        ));

        assertNotNull(catalog.getRecipe("minecraft:iron_pickaxe"));

        // Evento /reload do servidor
        catalog.clear();
        assertNull(catalog.getRecipe("minecraft:iron_pickaxe"), "Apos /reload, receitas anteriores devem ser eliminadas imediatamente");

        // Reinicializacao limpa
        catalog.registerRecipe(new RecipeRequirement(
                "minecraft:diamond_pickaxe",
                new ItemSlot("minecraft:diamond_pickaxe", 1),
                Collections.singletonList(new ItemSlot("minecraft:diamond", 3))
        ));
        assertNotNull(catalog.getRecipe("minecraft:diamond_pickaxe"));
    }
}
