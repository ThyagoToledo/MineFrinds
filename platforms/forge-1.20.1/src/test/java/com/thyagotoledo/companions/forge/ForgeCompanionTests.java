package com.thyagotoledo.companions.forge;

import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ForgeCompanionTests {

    @Test
    @DisplayName("Validar identificador do mod")
    void testModId() {
        assertEquals("companions", CompanionsForgeMod.MODID);
    }

    @Test
    @DisplayName("Validar modos de companheiro disponiveis")
    void testCompanionModes() {
        assertEquals(7, CompanionMode.values().length);
        assertNotNull(CompanionMode.valueOf("FOLLOW"));
        assertNotNull(CompanionMode.valueOf("STAY"));
        assertNotNull(CompanionMode.valueOf("DEFEND"));
        assertNotNull(CompanionMode.valueOf("WORK"));
        assertNotNull(CompanionMode.valueOf("WOOD"));
        assertNotNull(CompanionMode.valueOf("MINE"));
        assertNotNull(CompanionMode.valueOf("IDLE"));
    }

    @Test
    @DisplayName("Validar resolucao de mensagens bilingues no LocaleService")
    void testBilingualMessages() {
        LocaleService service = new LocaleService();

        String recruitedPt = service.translate("pt_br", "message.companions.recruited");
        String recruitedEn = service.translate("en_us", "message.companions.recruited");
        assertNotNull(recruitedPt);
        assertNotNull(recruitedEn);
        assertFalse(recruitedPt.isEmpty());
        assertFalse(recruitedEn.isEmpty());

        String followPt = service.translate("pt_br", "message.companions.mode_follow");
        String followEn = service.translate("en_us", "message.companions.mode_follow");
        assertNotNull(followPt);
        assertNotNull(followEn);
        assertFalse(followPt.isEmpty());
        assertFalse(followEn.isEmpty());

        String recallPt = service.translate("pt_br", "dialogue.recall_ack");
        String recallEn = service.translate("en_us", "dialogue.recall_ack");
        assertNotNull(recallPt);
        assertNotNull(recallEn);
        assertFalse(recallPt.isEmpty());
        assertFalse(recallEn.isEmpty());

        String viewPt = service.translate("pt_br", "dialogue.remote_view_start");
        String viewEn = service.translate("en_us", "dialogue.remote_view_start");
        assertNotNull(viewPt);
        assertNotNull(viewEn);
        assertFalse(viewPt.isEmpty());
        assertFalse(viewEn.isEmpty());
    }

    @Test
    @DisplayName("Validar contratos dos pacotes de rede C2S e S2C")
    void testNetworkPacketContracts() {
        java.util.UUID testUuid = java.util.UUID.randomUUID();
        com.thyagotoledo.companions.forge.network.ServerboundCommandPacket c2s =
                new com.thyagotoledo.companions.forge.network.ServerboundCommandPacket(testUuid, "@Nara vem ca");
        assertEquals(testUuid, c2s.getCompanionUuid());
        assertEquals("@Nara vem ca", c2s.getCommand());

        com.thyagotoledo.companions.forge.network.ClientboundFeedbackPacket s2c =
                new com.thyagotoledo.companions.forge.network.ClientboundFeedbackPacket(testUuid, "Estou a caminho", false);
        assertEquals(testUuid, s2c.getCompanionUuid());
        assertEquals("Estou a caminho", s2c.getSpeech());
        assertFalse(s2c.isToggleRemoteView());
    }

    @Test
    @DisplayName("Contrato de isolamento: modulo core nao deve importar classes de Minecraft ou Forge")
    void testCoreHasNoMinecraftImports() throws IOException {
        Path coreJavaDir = Paths.get("../../core/src/main/java");
        if (!Files.exists(coreJavaDir)) {
            coreJavaDir = Paths.get("core/src/main/java");
        }
        if (!Files.exists(coreJavaDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(coreJavaDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (String line : lines) {
                    assertFalse(line.contains("import net.minecraft."),
                            "Modulo core nao deve conter import de Minecraft: " + javaFile);
                    assertFalse(line.contains("import net.minecraftforge."),
                            "Modulo core nao deve conter import de Forge: " + javaFile);
                }
            }
        }
    }

    @Test
    @DisplayName("Contrato de sidedness: entidades e itens comuns nao devem importar net.minecraft.client")
    void testCommonCodeHasNoClientImports() throws IOException {
        Path commonSourceDir = Paths.get("src/main/java/com/thyagotoledo/companions/forge");
        assertTrue(Files.exists(commonSourceDir), "Diretorio fonte comum deve existir");

        Path entityDir = commonSourceDir.resolve("entity");
        Path itemDir = commonSourceDir.resolve("item");

        checkNoClientImports(entityDir);
        checkNoClientImports(itemDir);
    }

    private void checkNoClientImports(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (String line : lines) {
                    assertFalse(line.contains("import net.minecraft.client."),
                            "Codigo comum/servidor nao pode importar net.minecraft.client: " + javaFile + " -> " + line);
                }
            }
        }
    }

    @Test
    @DisplayName("Validar servico de permissoes e contratos de trabalho da Etapa P3")
    void testP3WorkTaskAndPermissions() {
        com.thyagotoledo.companions.core.permissions.DefaultPermissionService permService =
                new com.thyagotoledo.companions.core.permissions.DefaultPermissionService();
        java.util.UUID testOwner = java.util.UUID.randomUUID();

        // Sem areas restritas, permissao deve ser concedida
        assertTrue(permService.canBreakBlockAt(testOwner, "minecraft:overworld", 100, 64, 100));
        assertTrue(permService.canInteractAt(testOwner, "minecraft:overworld", 100, 64, 100));

        // Registro de area restrita
        permService.addRestrictedArea("minecraft:overworld", 100, 64, 100);

        // Bloqueio na posicao restrita
        assertFalse(permService.canBreakBlockAt(testOwner, "minecraft:overworld", 100, 64, 100));
        assertFalse(permService.canInteractAt(testOwner, "minecraft:overworld", 100, 64, 100));

        // Liberacao apos remocao
        permService.removeRestrictedArea("minecraft:overworld", 100, 64, 100);
        assertTrue(permService.canBreakBlockAt(testOwner, "minecraft:overworld", 100, 64, 100));

        // WorkTask e ToolType
        com.thyagotoledo.companions.core.work.WorkArea area =
                new com.thyagotoledo.companions.core.work.WorkArea("minecraft:overworld", 0, 64, 0, 16);
        com.thyagotoledo.companions.core.work.WorkTask task =
                new com.thyagotoledo.companions.core.work.WorkTask("task_harvest_1", "minecraft:oak_log", 10, com.thyagotoledo.companions.core.work.ToolType.AXE, area);

        assertEquals("minecraft:oak_log", task.getTargetBlockId());
        assertEquals(com.thyagotoledo.companions.core.work.ToolType.AXE, task.getRequiredTool());
        assertEquals(10, task.getTargetCount());
        assertFalse(task.isCompleted());
        assertFalse(task.isCancelled());

        task.recordHarvest(5);
        assertEquals(5, task.getCollectedCount());
        assertFalse(task.isCompleted());

        task.recordHarvest(5);
        assertEquals(10, task.getCollectedCount());
        assertTrue(task.isCompleted());
    }

    @Test
    @DisplayName("Validar mensagens de claims e coleta no sistema de internacionalizacao")
    void testP3ClaimLocalization() {
        LocaleService service = new LocaleService();
        String ptClaim = service.translate("pt_br", "task.blocked.claim", "FTB Chunks");
        String enClaim = service.translate("en_us", "task.blocked.claim", "FTB Chunks");

        assertTrue(ptClaim.contains("FTB Chunks"));
        assertTrue(ptClaim.contains("protegida"));
        assertTrue(enClaim.contains("FTB Chunks"));
        assertTrue(enClaim.contains("protected"));
    }

    @Test
    @DisplayName("Validar servico ForgeQuestService, fallback gracioso e contratos da Etapa P4")
    void testP4ForgeQuestServiceFallbackAndContracts() {
        com.thyagotoledo.companions.forge.service.ForgeQuestService questService =
                new com.thyagotoledo.companions.forge.service.ForgeQuestService();
        assertNotNull(questService);

        java.util.UUID testPlayer = java.util.UUID.randomUUID();

        // Sem quests registradas, lista vazia
        assertTrue(questService.getAvailableQuests(testPlayer).isEmpty());

        // Cadastro no fallback service
        com.thyagotoledo.companions.core.quest.Quest sampleQuest = new com.thyagotoledo.companions.core.quest.Quest(
                "quest_welcome",
                "Boas-vindas",
                "Primeira missao do modpack",
                "welcome",
                java.util.Collections.emptyList(),
                java.util.Collections.singletonList(new com.thyagotoledo.companions.core.quest.QuestTask("t_table", com.thyagotoledo.companions.core.quest.QuestTask.Type.ITEM, "minecraft:crafting_table", 1)),
                java.util.Collections.singletonList(new com.thyagotoledo.companions.core.quest.QuestReward("r_apple", "minecraft:apple", 5)),
                false
        );
        questService.getFallbackService().registerQuest(sampleQuest);

        // Acesso via interface ForgeQuestService
        com.thyagotoledo.companions.core.quest.Quest retrieved = questService.getQuest(testPlayer, "quest_welcome");
        assertNotNull(retrieved);
        assertEquals("Boas-vindas", retrieved.getTitle());
        assertEquals(1, retrieved.getTasks().size());
        assertEquals(1, retrieved.getRewards().size());

        // Invalidacao de cache nao deve lancar excecao
        assertDoesNotThrow(questService::invalidateCache);
    }

    @Test
    @DisplayName("Validar mensagens de assistencia a quests no sistema de internacionalizacao")
    void testP4QuestLocalization() {
        LocaleService service = new LocaleService();

        String ptReady = service.translate("pt_br", "quest.ready", "Primeiros Passos");
        String enReady = service.translate("en_us", "quest.ready", "First Steps");
        assertTrue(ptReady.contains("Primeiros Passos"));
        assertTrue(ptReady.contains("prontos para entrega"));
        assertTrue(enReady.contains("First Steps"));
        assertTrue(enReady.contains("ready to submit"));

        String ptBlocked = service.translate("pt_br", "quest.blocked.dependency", "Capitulo 2");
        String enBlocked = service.translate("en_us", "quest.blocked.dependency", "Chapter 2");
        assertTrue(ptBlocked.contains("dependencias anteriores"));
        assertTrue(enBlocked.contains("prerequisite quests"));
    }

    @Test
    @DisplayName("Validar provedor hibrido de dialogo, memoria de conversa e contratos da Etapa P5")
    void testP5HybridDialogueAndInferenceContracts() {
        LocaleService localeService = new LocaleService();
        com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider detProvider =
                new com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider(localeService);
        com.thyagotoledo.companions.core.ai.MockInferenceClient mockClient =
                new com.thyagotoledo.companions.core.ai.MockInferenceClient();
        com.thyagotoledo.companions.core.ai.ConversationMemory memory =
                new com.thyagotoledo.companions.core.ai.ConversationMemory(6);

        com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider hybridProvider =
                new com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider(
                        detProvider,
                        mockClient,
                        memory,
                        localeService
                );

        com.thyagotoledo.companions.core.model.CompanionProfile profile =
                new com.thyagotoledo.companions.core.model.CompanionProfile(
                        java.util.UUID.randomUUID(),
                        java.util.UUID.randomUUID(),
                        "Nara",
                        CompanionMode.FOLLOW,
                        com.thyagotoledo.companions.core.model.Personality.BALANCED
                );

        com.thyagotoledo.companions.core.model.InventorySnapshot emptyInv =
                new com.thyagotoledo.companions.core.model.InventorySnapshot(java.util.Collections.emptyList(), 27);

        // Ordem canônica direta
        com.thyagotoledo.companions.core.dialogue.DialogueResponse stayResp =
                hybridProvider.processSync("fica aqui", "pt_br", profile, emptyInv, 1000L);
        assertEquals(com.thyagotoledo.companions.core.dialogue.IntentType.STAY, stayResp.getIntent().getType());
        assertEquals("Vou ficar aqui esperando.", stayResp.getSpeech());

        // Dialogo aberto via mock SLM
        mockClient.setNextResponse("{\"intent\": \"CASUAL_CHAT\", \"speech\": \"Estou pronto para a jornada.\"}");
        com.thyagotoledo.companions.core.dialogue.DialogueResponse chatResp =
                hybridProvider.processSync("o que voce acha do dia?", "pt_br", profile, emptyInv, 1000L);
        assertEquals(com.thyagotoledo.companions.core.dialogue.IntentType.CASUAL_CHAT, chatResp.getIntent().getType());
        assertEquals("Estou pronto para a jornada.", chatResp.getSpeech());
        assertEquals(4, memory.size());
    }
}
