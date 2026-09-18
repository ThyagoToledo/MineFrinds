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
        assertEquals(5, CompanionMode.values().length);
        assertNotNull(CompanionMode.valueOf("FOLLOW"));
        assertNotNull(CompanionMode.valueOf("STAY"));
        assertNotNull(CompanionMode.valueOf("DEFEND"));
        assertNotNull(CompanionMode.valueOf("WORK"));
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
}
