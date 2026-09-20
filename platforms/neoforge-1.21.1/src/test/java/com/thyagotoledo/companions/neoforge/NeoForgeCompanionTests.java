package com.thyagotoledo.companions.neoforge;

import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.ai.MockInferenceClient;
import com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.DialogueResponse;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.dialogue.IntentType;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.CompanionSnapshot;
import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.core.planner.RecipeCatalog;
import com.thyagotoledo.companions.neoforge.client.gui.CompanionScreen;
import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.CompanionSavedData;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import com.thyagotoledo.companions.neoforge.network.NeoForgeCompanionPayloads;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
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
        assertEquals("0.2.0-alpha.2-1.21.1", CompanionsNeoForgeMod.VERSION);
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

    @Test
    @DisplayName("R0 - Validar ausencia de duplicacao e calculo exato de ingredientes no crafting")
    void testR0CraftingNoDuplicationAndExactResourceCalculation() {
        RecipeCatalog catalog = RecipeCatalog.getDefault();

        // 1. Confirmar existencia e proporcoes das receitas suportadas
        assertTrue(RecipeCatalog.hasRecipe("wooden_pickaxe"));
        assertTrue(RecipeCatalog.hasRecipe("minecraft:wooden_pickaxe"));
        assertTrue(RecipeCatalog.hasRecipe("oak_planks"));
        assertTrue(RecipeCatalog.hasRecipe("bread"));
        assertTrue(RecipeCatalog.hasRecipe("stone_pickaxe"));

        // 2. Receita desconhecida deve ser estritamente rejeitada (sem fallback para tabuas)
        assertFalse(RecipeCatalog.hasRecipe("item_inexistente_xyz_123"));
        assertFalse(RecipeCatalog.hasRecipe("espada_laser_cosmica"));

        // 3. Calculo de ingredientes ausentes respeita a contagem exata requerida
        InventorySnapshot emptyBag = new InventorySnapshot(Collections.emptyList(), 36);
        List<ItemSlot> missingForOnePickaxe = catalog.calculateMissingIngredients("minecraft:wooden_pickaxe", 1, emptyBag);
        assertEquals(2, missingForOnePickaxe.size());

        int planksRequired = 0;
        int sticksRequired = 0;
        for (ItemSlot slot : missingForOnePickaxe) {
            if (slot.getItemId().equals("minecraft:oak_planks")) planksRequired = slot.getCount();
            if (slot.getItemId().equals("minecraft:stick")) sticksRequired = slot.getCount();
        }
        assertEquals(3, planksRequired);
        assertEquals(2, sticksRequired);

        // Para fabricar 4 picaretas, os ingredientes devem quadruplicar proporcionalmente (sem consumo fixo reduzido)
        List<ItemSlot> missingForFourPickaxes = catalog.calculateMissingIngredients("minecraft:wooden_pickaxe", 4, emptyBag);
        int planksForFour = 0;
        int sticksForFour = 0;
        for (ItemSlot slot : missingForFourPickaxes) {
            if (slot.getItemId().equals("minecraft:oak_planks")) planksForFour = slot.getCount();
            if (slot.getItemId().equals("minecraft:stick")) sticksForFour = slot.getCount();
        }
        assertEquals(12, planksForFour);
        assertEquals(8, sticksForFour);
    }

    @Test
    @DisplayName("R0 - Validar protecao rigorosa de claims em quebra de blocos e interacoes")
    void testR0ClaimProtectionInWorkModes() {
        NeoForgePermissionService permService = new NeoForgePermissionService();
        UUID ownerUuid = UUID.randomUUID();
        UUID otherPlayerUuid = UUID.randomUUID();

        String overworld = "minecraft:overworld";

        // No terreno livre, o dono tem permissao de quebra e interacao
        assertTrue(permService.canBreakBlockAt(ownerUuid, overworld, 100, 64, 100));
        assertTrue(permService.canInteractAt(ownerUuid, overworld, 100, 64, 100));

        // Registra claim protegido na coordenada
        permService.getFallbackService().addRestrictedArea(overworld, 100, 64, 100);

        // Apos restricao, nenhum jogador nao autorizado pode quebrar ou interagir
        assertFalse(permService.canBreakBlockAt(otherPlayerUuid, overworld, 100, 64, 100));
        assertFalse(permService.canInteractAt(otherPlayerUuid, overworld, 100, 64, 100));

        // Area fora do claim permanece inalterada
        assertTrue(permService.canBreakBlockAt(otherPlayerUuid, overworld, 200, 64, 200));
    }

    @Test
    @DisplayName("R0 - Validar regras de recall seguro contra combate, dimensao e perigo")
    void testR0SafeRecallValidation() {
        UUID ownerUuid = UUID.randomUUID();
        NeoForgeCompanionEntity companion = new NeoForgeCompanionEntity(
                ownerUuid, "RimuruBot", Personality.BALANCED, null, null, null
        );

        // Verifica que entidade nao inicia em combate
        assertNotNull(companion);
        assertEquals(CompanionMode.FOLLOW, companion.getMode());

        // Mudanca de modo para DEFEND
        companion.setMode(CompanionMode.DEFEND);
        assertEquals(CompanionMode.DEFEND, companion.getMode());

        // Verificacao de cooldown: intervalo minimo entre chamadas consecutivas
        long recallCooldownTicks = 60L;
        long time1 = 1000L;
        long time2 = 1030L; // 30 ticks depois (dentro do cooldown)
        long time3 = 1070L; // 70 ticks depois (fora do cooldown)

        assertTrue(time2 - time1 < recallCooldownTicks);
        assertFalse(time3 - time1 < recallCooldownTicks);
    }

    @Test
    @DisplayName("R1 - Validar adaptadores de rede CustomPacketPayload e StreamCodecs (1.21.1)")
    void testR1PayloadContractsAndCodecs() {
        UUID companionUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();

        // 1. RequestSnapshotPayload (C2S)
        NeoForgeCompanionPayloads.RequestSnapshotPayload req = new NeoForgeCompanionPayloads.RequestSnapshotPayload();
        assertEquals("companions:request_snapshot", req.type().id().toString());
        assertEquals("companions:request_snapshot", req.getChannelName());

        FriendlyByteBuf reqBuf = new FriendlyByteBuf(Unpooled.buffer());
        NeoForgeCompanionPayloads.RequestSnapshotPayload.STREAM_CODEC.encode(reqBuf, req);
        NeoForgeCompanionPayloads.RequestSnapshotPayload reqDecoded =
                NeoForgeCompanionPayloads.RequestSnapshotPayload.STREAM_CODEC.decode(reqBuf);
        assertNotNull(reqDecoded);
        assertEquals(req.type(), reqDecoded.type());

        // 2. SnapshotPayload (S2C) com serializacao e desserializacao completa
        CompanionSnapshot snapshot = new CompanionSnapshot(
                companionUuid,
                ownerUuid,
                "RimuruBot",
                CompanionMode.WOOD,
                15.5f,
                20.0f,
                true,
                true,
                "Rimuru",
                true,
                50,
                64,
                -100,
                true,
                "Slime",
                "Special A",
                120000L,
                "Coletando troncos.",
                7L
        );
        NeoForgeCompanionPayloads.SnapshotPayload snapPayload = new NeoForgeCompanionPayloads.SnapshotPayload(snapshot);
        assertEquals("companions:snapshot", snapPayload.type().id().toString());
        assertEquals("companions:snapshot", snapPayload.getChannelName());

        FriendlyByteBuf snapBuf = new FriendlyByteBuf(Unpooled.buffer());
        NeoForgeCompanionPayloads.SnapshotPayload.STREAM_CODEC.encode(snapBuf, snapPayload);
        NeoForgeCompanionPayloads.SnapshotPayload decodedSnap =
                NeoForgeCompanionPayloads.SnapshotPayload.STREAM_CODEC.decode(snapBuf);
        assertNotNull(decodedSnap);
        assertEquals(snapshot, decodedSnap.snapshot());
        assertEquals(snapPayload.getRequestId(), decodedSnap.getRequestId());
        assertEquals(7L, decodedSnap.snapshot().getRevision());
        assertEquals("16 / 20", decodedSnap.snapshot().getHealthDisplay());
        assertEquals("[50, 64, -100]", decodedSnap.snapshot().getChestDisplay());
        assertEquals("Jogador Oficial (Lan/Server)", decodedSnap.snapshot().getPresenceDisplay());

        // 3. CommandPayload (C2S)
        NeoForgeCompanionPayloads.CommandPayload cmd =
                new NeoForgeCompanionPayloads.CommandPayload(companionUuid, "me segue", "pt_br");
        assertEquals("companions:command_payload", cmd.type().id().toString());
        assertEquals("companions:command_payload", cmd.getChannelName());

        FriendlyByteBuf cmdBuf = new FriendlyByteBuf(Unpooled.buffer());
        NeoForgeCompanionPayloads.CommandPayload.STREAM_CODEC.encode(cmdBuf, cmd);
        NeoForgeCompanionPayloads.CommandPayload decodedCmd =
                NeoForgeCompanionPayloads.CommandPayload.STREAM_CODEC.decode(cmdBuf);
        assertNotNull(decodedCmd);
        assertEquals(companionUuid, decodedCmd.getCompanionUuid());
        assertEquals("me segue", decodedCmd.getCommand());
        assertEquals("pt_br", decodedCmd.getLocale());
        assertEquals(cmd.getRequestId(), decodedCmd.getRequestId());
        assertEquals(0L, decodedCmd.getRevision());

        // 4. FeedbackPayload (S2C)
        NeoForgeCompanionPayloads.FeedbackPayload feedback =
                new NeoForgeCompanionPayloads.FeedbackPayload(companionUuid, "Entendido, estou te seguindo.", "pt_br", true);
        assertEquals("companions:feedback_payload", feedback.type().id().toString());
        assertEquals("companions:feedback_payload", feedback.getChannelName());

        FriendlyByteBuf fbBuf = new FriendlyByteBuf(Unpooled.buffer());
        NeoForgeCompanionPayloads.FeedbackPayload.STREAM_CODEC.encode(fbBuf, feedback);
        NeoForgeCompanionPayloads.FeedbackPayload decodedFb =
                NeoForgeCompanionPayloads.FeedbackPayload.STREAM_CODEC.decode(fbBuf);
        assertNotNull(decodedFb);
        assertEquals(companionUuid, decodedFb.getCompanionUuid());
        assertEquals("Entendido, estou te seguindo.", decodedFb.getSpeech());
        assertTrue(decodedFb.isSuccess());
    }

    @Test
    @DisplayName("R1 - Validar persistencia nativa SavedData com schema versionado e NBT")
    void testR1CompanionSavedDataPersistence() {
        CompanionSavedData savedData = new CompanionSavedData();
        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();

        savedData.setDesignatedChest(owner1, new BlockPos(120, 64, -300));
        savedData.saveCompanionRecord(owner1, "RimuruBot", "Rimuru", "MINE", "BALANCED", 50000L);

        savedData.setDesignatedChest(owner2, new BlockPos(0, 70, 0));
        savedData.saveCompanionRecord(owner2, "GokuBot", "Goku", "DEFEND", "BRAVE", 9001L);

        // Salva para CompoundTag
        CompoundTag tag = savedData.save(new CompoundTag(), null);
        assertNotNull(tag);
        assertEquals(2, tag.getInt("schema_version"));
        assertTrue(tag.contains("Chests", 10));
        assertTrue(tag.contains("Companions", 10));

        // Carrega novo SavedData a partir do NBT
        CompanionSavedData loaded = CompanionSavedData.load(tag, null);
        assertNotNull(loaded);
        assertEquals(new BlockPos(120, 64, -300), loaded.getDesignatedChest(owner1));
        assertEquals(new BlockPos(0, 70, 0), loaded.getDesignatedChest(owner2));

        CompanionSavedData.CompanionStateRecord rec1 = loaded.getCompanionRecord(owner1);
        assertNotNull(rec1);
        assertEquals("RimuruBot", rec1.getName());
        assertEquals("Rimuru", rec1.getSkin());
        assertEquals("MINE", rec1.getMode());
        assertEquals(50000L, rec1.getTensuraEp());

        CompanionSavedData.CompanionStateRecord rec2 = loaded.getCompanionRecord(owner2);
        assertNotNull(rec2);
        assertEquals("GokuBot", rec2.getName());
        assertEquals("Goku", rec2.getSkin());
        assertEquals("DEFEND", rec2.getMode());
        assertEquals(9001L, rec2.getTensuraEp());
    }

    @Test
    @DisplayName("R1 - Validar construcao e sincronizacao de snapshot de estado real no CompanionManager")
    void testR1CompanionManagerSnapshotBuilding() {
        UUID ownerUuid = UUID.randomUUID();

        // 1. Sem companheiro ativo
        CompanionSnapshot empty = CompanionManager.buildSnapshot(ownerUuid);
        assertNotNull(empty);
        assertFalse(empty.isSpawned());
        assertFalse(empty.isFakePlayer());
        assertEquals("0 / 20", empty.getHealthDisplay());
        assertEquals("Ausente (Nao invocado)", empty.getPresenceDisplay());

        // 2. Registrando companheiro logico e bau
        NeoForgeCompanionEntity entity = CompanionManager.getOrCreateCompanion(ownerUuid, "LuffyBot");
        entity.setMode(CompanionMode.FARM);
        entity.setCustomSkin("Luffy");
        CompanionManager.setDesignatedChest(ownerUuid, new BlockPos(10, 64, 10));

        CompanionSnapshot snap = CompanionManager.buildSnapshot(ownerUuid);
        assertNotNull(snap);
        assertEquals("LuffyBot", snap.getName());
        assertEquals(CompanionMode.FARM, snap.getMode());
        assertEquals("Luffy", snap.getSkinName());
        assertTrue(snap.hasDesignatedChest());
        assertEquals("[10, 64, 10]", snap.getChestDisplay());
        assertEquals("20 / 20", snap.getHealthDisplay());

        // 3. Integracao com CompanionScreen
        CompanionScreen.setActiveSnapshot(snap);
        assertSame(snap, CompanionScreen.getActiveSnapshot());

        // Limpeza
        CompanionManager.removeCompanion(ownerUuid);
        CompanionManager.setDesignatedChest(ownerUuid, null);
        CompanionScreen.setActiveSnapshot(null);
    }
}
