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
                "dialogue.deposit_ack", "dialogue.quest_ack", "dialogue.unknown_ack", "task.blocked.claim"
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
}
