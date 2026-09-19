package com.thyagotoledo.companions.core;

import com.thyagotoledo.companions.core.dialogue.*;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.*;
import com.thyagotoledo.companions.core.planner.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CoreTestSuite {
    private LocaleService localeService;
    private DeterministicDialogueProvider dialogueProvider;
    private CompanionProfile profile;

    @BeforeEach
    public void setUp() {
        localeService = new LocaleService();
        dialogueProvider = new DeterministicDialogueProvider(localeService);
        profile = new CompanionProfile(UUID.randomUUID(), UUID.randomUUID(), "ThyagoBot", CompanionMode.FOLLOW, Personality.BALANCED);
    }

    @Test
    public void testLocaleServiceTranslationsAndPlaceholders() {
        assertEquals("Entendido, estou te seguindo.", localeService.translate("pt_br", "dialogue.follow_ack"));
        assertEquals("Understood, I am following you.", localeService.translate("en_us", "dialogue.follow_ack"));

        String claimEn = localeService.translate("en_us", "task.blocked.claim", "FTB Chunks");
        assertEquals("I cannot act here: this area is protected by FTB Chunks.", claimEn);

        String claimPt = localeService.translate("pt_br", "task.blocked.claim", "FTB Chunks");
        assertEquals("Nao posso agir aqui: esta area esta protegida por FTB Chunks.", claimPt);
    }

    @Test
    public void testDialogueRecognitionBilingual() {
        InventorySnapshot emptyInv = new InventorySnapshot(Collections.emptyList(), 27);

        DialogueResponse respPt = dialogueProvider.process("me segue por favor", "pt_br", profile, emptyInv);
        assertEquals(IntentType.FOLLOW_OWNER, respPt.getIntent().getType());
        assertEquals("Entendido, estou te seguindo.", respPt.getSpeech());

        DialogueResponse respEn = dialogueProvider.process("follow me please", "en_us", profile, emptyInv);
        assertEquals(IntentType.FOLLOW_OWNER, respEn.getIntent().getType());
        assertEquals("Understood, I am following you.", respEn.getSpeech());

        DialogueResponse woodPt = dialogueProvider.process("pega madeira", "pt_br", profile, emptyInv);
        assertEquals(IntentType.CHOP_WOOD, woodPt.getIntent().getType());
        assertEquals(16, woodPt.getIntent().getQuantity());

        DialogueResponse woodEn = dialogueProvider.process("chop wood", "en_us", profile, emptyInv);
        assertEquals(IntentType.CHOP_WOOD, woodEn.getIntent().getType());
    }

    @Test
    public void testRecipeCatalogMissingIngredients() {
        RecipeCatalog catalog = new RecipeCatalog();
        RecipeRequirement recipe = new RecipeRequirement(
                "minecraft:crafting_table",
                new ItemSlot("minecraft:crafting_table", 1),
                Collections.singletonList(new ItemSlot("minecraft:oak_planks", 4))
        );
        catalog.registerRecipe(recipe);

        InventorySnapshot emptyInv = new InventorySnapshot(Collections.emptyList(), 27);
        List<ItemSlot> missing = catalog.calculateMissingIngredients("minecraft:crafting_table", 1, emptyInv);

        assertEquals(1, missing.size());
        assertEquals("minecraft:oak_planks", missing.get(0).getItemId());
        assertEquals(4, missing.get(0).getCount());

        ItemSlot twoPlanks = new ItemSlot("minecraft:oak_planks", 2);
        InventorySnapshot partialInv = new InventorySnapshot(Collections.singletonList(twoPlanks), 27);
        List<ItemSlot> missingPartial = catalog.calculateMissingIngredients("minecraft:crafting_table", 1, partialInv);

        assertEquals(1, missingPartial.size());
        assertEquals(2, missingPartial.get(0).getCount());
    }

    @Test
    public void testTaskPriorityComparison() {
        assertTrue(TaskPriority.EMERGENCY.isHigherThan(TaskPriority.SELF_DEFENSE));
        assertTrue(TaskPriority.SELF_DEFENSE.isHigherThan(TaskPriority.MISSION_WORK));
        assertTrue(TaskPriority.OWNER_ORDER.isHigherThan(TaskPriority.IDLE_ROAM));
    }

    @Test
    public void testP2NewCommandsBilingual() {
        InventorySnapshot emptyInv = new InventorySnapshot(Collections.emptyList(), 27);

        DialogueResponse defendPt = dialogueProvider.process("defenda agora", "pt_br", profile, emptyInv);
        assertEquals(IntentType.DEFEND, defendPt.getIntent().getType());
        assertEquals("Estou em alerta, protegendo voce.", defendPt.getSpeech());

        DialogueResponse defendEn = dialogueProvider.process("please defend", "en_us", profile, emptyInv);
        assertEquals(IntentType.DEFEND, defendEn.getIntent().getType());
        assertEquals("I am on alert, protecting you.", defendEn.getSpeech());

        DialogueResponse recallPt = dialogueProvider.process("vem ca amigo", "pt_br", profile, emptyInv);
        assertEquals(IntentType.RECALL, recallPt.getIntent().getType());
        assertEquals("Estou a caminho, me aproximando agora.", recallPt.getSpeech());

        DialogueResponse recallEn = dialogueProvider.process("come here please", "en_us", profile, emptyInv);
        assertEquals(IntentType.RECALL, recallEn.getIntent().getType());
        assertEquals("I am on my way, heading to you now.", recallEn.getSpeech());

        DialogueResponse viewPt = dialogueProvider.process("quero ver sua visao", "pt_br", profile, emptyInv);
        assertEquals(IntentType.REMOTE_VIEW, viewPt.getIntent().getType());
        assertEquals("Transmitindo minha visao para voce.", viewPt.getSpeech());

        DialogueResponse viewEn = dialogueProvider.process("remote view now", "en_us", profile, emptyInv);
        assertEquals(IntentType.REMOTE_VIEW, viewEn.getIntent().getType());
        assertEquals("Sharing my view with you.", viewEn.getSpeech());

        DialogueResponse invPt = dialogueProvider.process("abrir mochila", "pt_br", profile, emptyInv);
        assertEquals(IntentType.OPEN_INVENTORY, invPt.getIntent().getType());
        assertEquals("Aqui esta minha mochila.", invPt.getSpeech());

        DialogueResponse invEn = dialogueProvider.process("open backpack", "en_us", profile, emptyInv);
        assertEquals(IntentType.OPEN_INVENTORY, invEn.getIntent().getType());
        assertEquals("Here is my inventory.", invEn.getSpeech());

        DialogueResponse statusPt = dialogueProvider.process("qual o seu status", "pt_br", profile, emptyInv);
        assertEquals(IntentType.REPORT_STATUS, statusPt.getIntent().getType());

        DialogueResponse statusEn = dialogueProvider.process("give me a report", "en_us", profile, emptyInv);
        assertEquals(IntentType.REPORT_STATUS, statusEn.getIntent().getType());
    }

    @Test
    public void testLanguageKeyParity() {
        String[] requiredKeys = {
                "dialogue.empty", "dialogue.follow_ack", "dialogue.stay_ack", "dialogue.defend_ack",
                "dialogue.recall_ack", "dialogue.recall_blocked_combat", "dialogue.recall_blocked_dimension",
                "dialogue.recall_blocked_hazard", "dialogue.remote_view_start", "dialogue.remote_view_stop",
                "dialogue.inventory_open", "dialogue.status_report", "dialogue.wood_ack",
                "dialogue.deposit_ack", "dialogue.quest_ack", "dialogue.unknown_ack", "task.blocked.claim",
                "quest.blocked.dependency", "quest.ready", "quest.missing", "quest.raw_materials",
                "quest.unknown_process", "quest.not_found"
        };

        for (String key : requiredKeys) {
            String pt = localeService.translate("pt_br", key);
            String en = localeService.translate("en_us", key);
            assertNotEquals(key, pt, "Missing pt_br translation for " + key);
            assertNotEquals(key, en, "Missing en_us translation for " + key);
            assertFalse(pt.isEmpty());
            assertFalse(en.isEmpty());
        }
    }

    @Test
    public void testPermissionServiceRestriction() {
        com.thyagotoledo.companions.core.permissions.DefaultPermissionService permService =
                new com.thyagotoledo.companions.core.permissions.DefaultPermissionService();
        UUID owner = UUID.randomUUID();

        assertTrue(permService.canBreakBlockAt(owner, "minecraft:overworld", 100, 64, 100));

        permService.addRestrictedArea("minecraft:overworld", 100, 64, 100);
        assertFalse(permService.canBreakBlockAt(owner, "minecraft:overworld", 100, 64, 100));
        assertTrue(permService.canBreakBlockAt(owner, "minecraft:overworld", 101, 64, 100));

        permService.removeRestrictedArea("minecraft:overworld", 100, 64, 100);
        assertTrue(permService.canBreakBlockAt(owner, "minecraft:overworld", 100, 64, 100));
    }

    @Test
    public void testWorkAreaAndTaskProgress() {
        com.thyagotoledo.companions.core.work.WorkArea area =
                new com.thyagotoledo.companions.core.work.WorkArea("minecraft:overworld", 0, 64, 0, 16);

        assertTrue(area.contains("minecraft:overworld", 10, 64, -10));
        assertFalse(area.contains("minecraft:overworld", 20, 64, 0));
        assertFalse(area.contains("minecraft:the_nether", 0, 64, 0));

        com.thyagotoledo.companions.core.work.WorkTask task =
                new com.thyagotoledo.companions.core.work.WorkTask("harvest_logs", "minecraft:oak_log", 8, com.thyagotoledo.companions.core.work.ToolType.AXE, area);

        assertFalse(task.isCompleted());
        assertEquals(8, task.getRemainingCount());

        task.recordHarvest(5);
        assertEquals(3, task.getRemainingCount());
        assertFalse(task.isCompleted());

        task.recordHarvest(3);
        assertEquals(0, task.getRemainingCount());
        assertTrue(task.isCompleted());

        task.cancel();
        assertTrue(task.isCancelled());
    }

    @Test
    public void testCraftingPlannerAndReload() {
        RecipeCatalog catalog = new RecipeCatalog();
        catalog.registerRecipe(new RecipeRequirement(
                "minecraft:oak_planks",
                new ItemSlot("minecraft:oak_planks", 4),
                Collections.singletonList(new ItemSlot("minecraft:oak_log", 1))
        ));

        CraftingPlanner planner = new CraftingPlanner(catalog);

        InventorySnapshot emptyInv = new InventorySnapshot(Collections.emptyList(), 27);
        assertFalse(planner.canCraft("minecraft:oak_planks", 4, emptyInv));

        ItemSlot logSlot = new ItemSlot("minecraft:oak_log", 2);
        InventorySnapshot filledInv = new InventorySnapshot(Collections.singletonList(logSlot), 27);
        assertTrue(planner.canCraft("minecraft:oak_planks", 4, filledInv));

        // Teste de invalidacao no reload
        catalog.clear();
        assertNull(catalog.getRecipe("minecraft:oak_planks"));
        assertFalse(planner.canCraft("minecraft:oak_planks", 4, filledInv));
    }

    @Test
    public void testQuestVisibilityAndDependencies() {
        com.thyagotoledo.companions.core.quest.DefaultQuestService questService =
                new com.thyagotoledo.companions.core.quest.DefaultQuestService();
        UUID playerUuid = UUID.randomUUID();

        // Quest 1 (raiz)
        com.thyagotoledo.companions.core.quest.Quest q1 = new com.thyagotoledo.companions.core.quest.Quest(
                "q1", "Primeiros Passos", "Faca uma bancada", "intro",
                Collections.emptyList(),
                Collections.singletonList(new com.thyagotoledo.companions.core.quest.QuestTask("t1", com.thyagotoledo.companions.core.quest.QuestTask.Type.ITEM, "minecraft:crafting_table", 1)),
                Collections.singletonList(new com.thyagotoledo.companions.core.quest.QuestReward("r1", "meadow:cheese_sandwich", 4)),
                false
        );

        // Quest 2 (depende de q1)
        com.thyagotoledo.companions.core.quest.Quest q2 = new com.thyagotoledo.companions.core.quest.Quest(
                "q2", "Mineracao", "Faca uma picareta", "intro",
                Collections.singletonList("q1"),
                Collections.singletonList(new com.thyagotoledo.companions.core.quest.QuestTask("t2", com.thyagotoledo.companions.core.quest.QuestTask.Type.ITEM, "minecraft:wooden_pickaxe", 1)),
                Collections.emptyList(),
                false
        );

        // Quest 3 (oculta)
        com.thyagotoledo.companions.core.quest.Quest qHidden = new com.thyagotoledo.companions.core.quest.Quest(
                "q_secret", "Segredo", "Missao oculta", "secret",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                true
        );

        questService.registerQuest(q1);
        questService.registerQuest(q2);
        questService.registerQuest(qHidden);

        // qHidden nao deve vazar
        assertNull(questService.getQuest(playerUuid, "q_secret"));
        List<com.thyagotoledo.companions.core.quest.Quest> available = questService.getAvailableQuests(playerUuid);
        assertEquals(1, available.size());
        assertEquals("q1", available.get(0).getId());

        // q2 esta bloqueada por q1
        assertFalse(questService.areDependenciesMet(playerUuid, "q2"));

        // Conclui q1
        questService.setQuestCompleted(playerUuid, "q1");
        assertTrue(questService.isQuestCompleted(playerUuid, "q1"));
        assertTrue(questService.areDependenciesMet(playerUuid, "q2"));

        available = questService.getAvailableQuests(playerUuid);
        assertEquals(1, available.size());
        assertEquals("q2", available.get(0).getId());
    }

    @Test
    public void testQuestPlannerAndRewardDistinction() {
        com.thyagotoledo.companions.core.quest.DefaultQuestService questService =
                new com.thyagotoledo.companions.core.quest.DefaultQuestService();
        RecipeCatalog catalog = new RecipeCatalog();
        catalog.registerRecipe(new RecipeRequirement(
                "minecraft:crafting_table",
                new ItemSlot("minecraft:crafting_table", 1),
                Collections.singletonList(new ItemSlot("minecraft:oak_planks", 4))
        ));

        com.thyagotoledo.companions.core.quest.QuestPlanner planner =
                new com.thyagotoledo.companions.core.quest.QuestPlanner(questService, catalog);

        UUID playerUuid = UUID.randomUUID();
        com.thyagotoledo.companions.core.quest.Quest q = new com.thyagotoledo.companions.core.quest.Quest(
                "botania_fixture", "Inicio Botania", "Obter bancada", "botania",
                Collections.emptyList(),
                Collections.singletonList(new com.thyagotoledo.companions.core.quest.QuestTask("t1", com.thyagotoledo.companions.core.quest.QuestTask.Type.ITEM, "minecraft:crafting_table", 1)),
                Collections.singletonList(new com.thyagotoledo.companions.core.quest.QuestReward("r1", "meadow:cheese_sandwich", 4)),
                false
        );
        questService.registerQuest(q);

        // Inventario do jogador contem a recompensa ("meadow:cheese_sandwich"), mas NAO o requisito ("minecraft:crafting_table")
        InventorySnapshot playerInv = new InventorySnapshot(Collections.singletonList(new ItemSlot("meadow:cheese_sandwich", 4)), 27);
        InventorySnapshot companionInv = new InventorySnapshot(Collections.emptyList(), 27);

        // A recompensa nao pode satisfazer a task e nunca pode ser marcada como faltante
        com.thyagotoledo.companions.core.quest.QuestPlanResult result = planner.planQuest(playerUuid, "botania_fixture", playerInv, companionInv);
        assertEquals(com.thyagotoledo.companions.core.quest.QuestPlanResult.Status.MISSING_ITEMS, result.getStatus());
        assertTrue(result.getMissingItems().containsKey("minecraft:crafting_table"));
        assertFalse(result.getMissingItems().containsKey("meadow:cheese_sandwich"));
        assertTrue(result.getRawMaterialsNeeded().containsKey("minecraft:oak_planks"));

        // Quando o jogador possui o item da quest
        InventorySnapshot readyInv = new InventorySnapshot(Collections.singletonList(new ItemSlot("minecraft:crafting_table", 1)), 27);
        com.thyagotoledo.companions.core.quest.QuestPlanResult readyResult = planner.planQuest(playerUuid, "botania_fixture", readyInv, companionInv);
        assertEquals(com.thyagotoledo.companions.core.quest.QuestPlanResult.Status.READY_TO_SUBMIT, readyResult.getStatus());
    }

    @Test
    public void testConversationMemoryBufferLimit() {
        com.thyagotoledo.companions.core.ai.ConversationMemory memory =
                new com.thyagotoledo.companions.core.ai.ConversationMemory(4);

        assertEquals(0, memory.size());

        memory.addEntry("user", "mensagem 1");
        memory.addEntry("companion", "resposta 1");
        memory.addEntry("user", "mensagem 2");
        memory.addEntry("companion", "resposta 2");
        assertEquals(4, memory.size());

        // Entrada que excede a capacidade maxima
        memory.addEntry("user", "mensagem 3");
        assertEquals(4, memory.size());
        assertEquals("resposta 1", memory.getEntries().get(0).getText());
        assertEquals("mensagem 3", memory.getEntries().get(3).getText());

        memory.clear();
        assertEquals(0, memory.size());
    }

    @Test
    public void testHybridDialogueProviderPriorityAndFallback() {
        com.thyagotoledo.companions.core.ai.MockInferenceClient mockClient =
                new com.thyagotoledo.companions.core.ai.MockInferenceClient();
        com.thyagotoledo.companions.core.ai.ConversationMemory memory =
                new com.thyagotoledo.companions.core.ai.ConversationMemory(6);
        com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider provider =
                new com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider(
                        dialogueProvider,
                        mockClient,
                        memory,
                        localeService
                );

        CompanionProfile profile = new CompanionProfile(
                UUID.randomUUID(), UUID.randomUUID(), "Lia", CompanionMode.FOLLOW, Personality.BALANCED
        );
        InventorySnapshot emptyInv = new InventorySnapshot(Collections.emptyList(), 27);

        // 1. Comando deterministico: deve responder imediatamente sem acionar inferencia
        DialogueResponse followResp = provider.processSync("me segue", "pt_br", profile, emptyInv, 1000L);
        assertEquals(IntentType.FOLLOW_OWNER, followResp.getIntent().getType());
        assertEquals("Entendido, estou te seguindo.", followResp.getSpeech());

        // 2. Dialogo casual com IA online
        mockClient.setNextResponse("{\"intent\": \"CASUAL_CHAT\", \"speech\": \"O dia esta otimo para minerar!\"}");
        DialogueResponse chatResp = provider.processSync("o que voce acha do dia?", "pt_br", profile, emptyInv, 1000L);
        assertEquals(IntentType.CASUAL_CHAT, chatResp.getIntent().getType());
        assertEquals("O dia esta otimo para minerar!", chatResp.getSpeech());
        assertTrue(memory.size() >= 2);

        // 3. Fallback sob indisponibilidade da IA
        mockClient.setAvailable(false);
        DialogueResponse offlineResp = provider.processSync("conta uma historia", "pt_br", profile, emptyInv, 1000L);
        assertEquals(IntentType.UNKNOWN_OR_BLOCKED, offlineResp.getIntent().getType());
        assertEquals("Nao entendi muito bem. Pode repetir?", offlineResp.getSpeech());

        // 4. Fallback sob fila de inferencia cheia (>= 8)
        mockClient.setAvailable(true);
        mockClient.setPendingQueueSize(8);
        DialogueResponse queueFullResp = provider.processSync("mais uma pergunta", "pt_br", profile, emptyInv, 1000L);
        assertEquals(IntentType.UNKNOWN_OR_BLOCKED, queueFullResp.getIntent().getType());
    }

    @Test
    public void testHttpInferenceClientQueueLimitAndContract() {
        com.thyagotoledo.companions.core.ai.HttpInferenceClient client =
                new com.thyagotoledo.companions.core.ai.HttpInferenceClient("http://127.0.0.1:8080/v1/chat/completions", 1500);

        assertTrue(client.isAvailable());
        assertEquals(0, client.getPendingQueueSize());
        assertDoesNotThrow(client::shutdown);
    }
}
