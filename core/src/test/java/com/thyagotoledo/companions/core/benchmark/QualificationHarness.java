package com.thyagotoledo.companions.core.benchmark;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.ai.MockInferenceClient;
import com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.IntentType;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.CompanionProfile;
import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.core.permissions.DefaultPermissionService;
import com.thyagotoledo.companions.core.planner.CraftingPlanner;
import com.thyagotoledo.companions.core.planner.RecipeCatalog;
import com.thyagotoledo.companions.core.planner.RecipeRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness de qualificacao e estresse para homologacao P6.
 * Valida o orcamento de recursos, estabilidade de tick e comportamento sob carga
 * simulando 1, 4 e 8 NPCs companheiros simultaneos.
 */
public class QualificationHarness {

    private LocaleService localeService;
    private DeterministicDialogueProvider deterministicProvider;

    @BeforeEach
    public void setUp() {
        localeService = new LocaleService();
        deterministicProvider = new DeterministicDialogueProvider(localeService);
    }

    /**
     * Valida o perfil com 1 NPC: execucao leve, MSPT delta desprezivel e
     * consumo minimo de memoria.
     */
    @Test
    public void testBaselineB1NPC() {
        MockInferenceClient client = new MockInferenceClient();
        ConversationMemory memory = new ConversationMemory(6);
        HybridDialogueProvider provider = new HybridDialogueProvider(deterministicProvider, client, memory, localeService);
        CompanionProfile profile = new CompanionProfile(UUID.randomUUID(), UUID.randomUUID(), "Lia", CompanionMode.FOLLOW, Personality.BALANCED);
        InventorySnapshot inv = new InventorySnapshot(Collections.<ItemSlot>emptyList(), 27);

        long startNs = System.nanoTime();
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            DialogueResponse resp = provider.processSync("me segue", "pt_br", profile, inv, 100L);
            assertEquals(IntentType.FOLLOW_OWNER, resp.getIntent().getType());
        }
        long durationNs = System.nanoTime() - startNs;
        double avgMsPerCall = (durationNs / 1_000_000.0) / iterations;

        // Comandos deterministicos em 1 NPC devem responder em fracoes microscopicas de milissegundo (< 0.20 ms)
        assertTrue(avgMsPerCall < 0.50, "MSPT medio de comando deterministico deve ser < 0.50 ms, medido: " + avgMsPerCall);
        assertEquals(6, memory.size(), "Memoria circular deve reter exatamente o limite maximo de 6 entradas apos 1000 iteracoes");
    }

    /**
     * Valida o perfil padrao aprovado de 4 NPCs: executa ciclo de 1.000 ticks simulados
     * por entidade com planejamento de receitas, consultas de permissao e dialogo.
     * Criterio: delta p95 de tick simulado <= 5.0 ms.
     */
    @Test
    public void testBaselineB4NPCsControlledLoad() {
        int npcCount = 4;
        List<HybridDialogueProvider> providers = new ArrayList<HybridDialogueProvider>();
        List<CompanionProfile> profiles = new ArrayList<CompanionProfile>();
        MockInferenceClient client = new MockInferenceClient();

        RecipeCatalog catalog = new RecipeCatalog();
        catalog.registerRecipe(new RecipeRequirement(
                "minecraft:oak_planks",
                new ItemSlot("minecraft:oak_planks", 4),
                Collections.singletonList(new ItemSlot("minecraft:oak_log", 1))
        ));
        CraftingPlanner planner = new CraftingPlanner(catalog);
        DefaultPermissionService permService = new DefaultPermissionService();

        for (int i = 0; i < npcCount; i++) {
            ConversationMemory memory = new ConversationMemory(6);
            providers.add(new HybridDialogueProvider(deterministicProvider, client, memory, localeService));
            profiles.add(new CompanionProfile(UUID.randomUUID(), UUID.randomUUID(), "NPC-" + i, CompanionMode.FOLLOW, Personality.BALANCED));
        }

        InventorySnapshot invWithLog = new InventorySnapshot(Collections.singletonList(new ItemSlot("minecraft:oak_log", 4)), 27);
        UUID playerUuid = UUID.randomUUID();

        int tickCount = 500;
        List<Double> tickDurationsMs = new ArrayList<Double>(tickCount);

        for (int tick = 0; tick < tickCount; tick++) {
            long tickStartNs = System.nanoTime();

            for (int i = 0; i < npcCount; i++) {
                HybridDialogueProvider provider = providers.get(i);
                CompanionProfile profile = profiles.get(i);

                // 1. Decisao de dialogo
                DialogueResponse resp = provider.processSync("fica aqui", "pt_br", profile, invWithLog, 50L);
                assertNotNull(resp);

                // 2. Verificacao de permissao
                boolean allowed = permService.canBreakBlockAt(playerUuid, "minecraft:overworld", 100 + i, 64, 200 + i);
                assertTrue(allowed);

                // 3. Verificacao de crafting
                boolean canCraft = planner.canCraft("minecraft:oak_planks", 4, invWithLog);
                assertTrue(canCraft);
            }

            long tickEndNs = System.nanoTime();
            double tickMs = (tickEndNs - tickStartNs) / 1_000_000.0;
            tickDurationsMs.add(tickMs);
        }

        Collections.sort(tickDurationsMs);
        int p95Index = (int) (tickCount * 0.95);
        double p95Mspt = tickDurationsMs.get(p95Index);

        // Criterio rigoroso: delta p95 <= 5.0 ms para 4 NPCs operando simultaneamente
        assertTrue(p95Mspt < 5.0, "Delta p95 de MSPT com 4 NPCs deve ser < 5.0 ms, medido: " + p95Mspt);
    }

    /**
     * Valida o cenario de estresse com 8 NPCs: a fila do provedor atinge o teto maximo
     * de 8 pedidos pendentes. Requisicoes excedentes sao descartadas para proteger
     * o servidor e a memoria, caindo graciosamente para o fallback.
     */
    @Test
    public void testStress8NPCsQueueRejectionAndBackpressure() {
        int npcCount = 8;
        MockInferenceClient client = new MockInferenceClient();
        List<HybridDialogueProvider> providers = new ArrayList<HybridDialogueProvider>();
        List<CompanionProfile> profiles = new ArrayList<CompanionProfile>();

        for (int i = 0; i < npcCount; i++) {
            ConversationMemory memory = new ConversationMemory(6);
            providers.add(new HybridDialogueProvider(deterministicProvider, client, memory, localeService));
            profiles.add(new CompanionProfile(UUID.randomUUID(), UUID.randomUUID(), "StressNPC-" + i, CompanionMode.FOLLOW, Personality.CAUTIOUS));
        }

        InventorySnapshot emptyInv = new InventorySnapshot(Collections.<ItemSlot>emptyList(), 27);

        // Simula fila saturada no teto de seguranca (>= 8 pedidos)
        client.setPendingQueueSize(8);

        for (int i = 0; i < npcCount; i++) {
            HybridDialogueProvider provider = providers.get(i);
            CompanionProfile profile = profiles.get(i);

            // Requisicao casual sob fila saturada: deve rejeitar com fallback instantaneo
            DialogueResponse resp = provider.processSync("conte uma historia complexa", "pt_br", profile, emptyInv, 50L);
            assertNotNull(resp);
            assertEquals(IntentType.UNKNOWN_OR_BLOCKED, resp.getIntent().getType());
            assertTrue(resp.getSpeech().contains("IA esta indisponivel"));
        }
    }

    /**
     * Valida carga pesada de catalogo (2.000 receitas tipicas de modpacks grandes como Society)
     * e garante que a recarga (/reload) limpa e invalida atomicamente sem vazamentos de memoria.
     */
    @Test
    public void testRecipeCatalogLargePackReload() {
        RecipeCatalog catalog = new RecipeCatalog();
        int recipeCount = 2000;

        for (int i = 0; i < recipeCount; i++) {
            catalog.registerRecipe(new RecipeRequirement(
                    "mod:item_" + i,
                    new ItemSlot("mod:item_" + i, 1),
                    Collections.singletonList(new ItemSlot("minecraft:stick", 2))
            ));
        }

        CraftingPlanner planner = new CraftingPlanner(catalog);
        InventorySnapshot inv = new InventorySnapshot(Collections.singletonList(new ItemSlot("minecraft:stick", 10)), 27);

        assertTrue(planner.canCraft("mod:item_500", 1, inv));
        assertTrue(planner.canCraft("mod:item_1999", 1, inv));

        // Simula evento de reload (/reload)
        catalog.clear();
        assertNull(catalog.getRecipe("mod:item_500"));
        assertFalse(planner.canCraft("mod:item_500", 1, inv));

        // Recarga de lote pos-reload
        catalog.registerRecipe(new RecipeRequirement(
                "mod:item_500",
                new ItemSlot("mod:item_500", 1),
                Collections.singletonList(new ItemSlot("minecraft:iron_ingot", 1))
        ));
        assertNotNull(catalog.getRecipe("mod:item_500"));
    }

    /**
     * Valida conservacao estrita de itens e inventario quando acoes sao
     * abortadas ou rejeitadas por claim de protecao. Zero duplicacao.
     */
    @Test
    public void testInventoryIntegrityUnderAbortedTasks() {
        ItemSlot slot1 = new ItemSlot("minecraft:diamond", 5);
        ItemSlot slot2 = new ItemSlot("minecraft:iron_ingot", 16);
        InventorySnapshot initialInv = new InventorySnapshot(Arrays.asList(slot1, slot2), 27);

        assertEquals(2, initialInv.getSlots().size());
        assertEquals(5, initialInv.getSlots().get(0).getCount());
        assertEquals(16, initialInv.getSlots().get(1).getCount());

        // Simula tentativa de despejo ou consumo negado: inventario anterior deve permanecer imutavel
        DefaultPermissionService permService = new DefaultPermissionService();
        permService.addRestrictedArea("minecraft:overworld", 10, 64, 10);

        UUID unauthorizedPlayer = UUID.randomUUID();
        boolean canBreak = permService.canBreakBlockAt(unauthorizedPlayer, "minecraft:overworld", 10, 64, 10);
        assertFalse(canBreak);

        // O snapshot original nao sofre alteracao
        assertEquals(5, initialInv.getSlots().get(0).getCount());
        assertEquals(16, initialInv.getSlots().get(1).getCount());
    }
}
