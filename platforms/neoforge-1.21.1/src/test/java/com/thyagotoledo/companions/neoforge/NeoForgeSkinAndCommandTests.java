package com.thyagotoledo.companions.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.neoforge.client.skin.SkinCacheManager;
import com.thyagotoledo.companions.neoforge.command.CompanionCommands;
import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes automatizados para o sistema de skins, gerenciamento de ciclo de vida e comandos.
 */
public class NeoForgeSkinAndCommandTests {

    @BeforeEach
    void setUp() {
        CompanionManager.clearAll();
        SkinCacheManager.clearMemoryCache();
    }

    @Test
    @DisplayName("Validar validacao de nomes de skin (jogadores e personagens de anime)")
    void testSkinNameValidation() {
        // Nomes validos (alfanumericos de 1 a 16 caracteres)
        assertTrue(SkinCacheManager.isValidSkinName("Rimuru"));
        assertTrue(SkinCacheManager.isValidSkinName("Goku"));
        assertTrue(SkinCacheManager.isValidSkinName("Luffy"));
        assertTrue(SkinCacheManager.isValidSkinName("Naruto"));
        assertTrue(SkinCacheManager.isValidSkinName("Kirito"));
        assertTrue(SkinCacheManager.isValidSkinName("ThyagoToledo"));
        assertTrue(SkinCacheManager.isValidSkinName("Steve"));
        assertTrue(SkinCacheManager.isValidSkinName("Alex"));
        assertTrue(SkinCacheManager.isValidSkinName("a_1"));

        // Nomes invalidos
        assertFalse(SkinCacheManager.isValidSkinName(""));
        assertFalse(SkinCacheManager.isValidSkinName(" "));
        assertFalse(SkinCacheManager.isValidSkinName(null));
        assertFalse(SkinCacheManager.isValidSkinName("nome com espaco"));
        assertFalse(SkinCacheManager.isValidSkinName("NomeMuitoLongoComMaisDe16Caracteres"));
        assertFalse(SkinCacheManager.isValidSkinName("invalid$name!"));
    }

    @Test
    @DisplayName("Validar gerenciador de cache em memoria de skins")
    void testSkinCacheMemory() {
        SkinCacheManager.LoadedSkin loadedSkin = new SkinCacheManager.LoadedSkin("rimuru", true);

        SkinCacheManager.putLoadedSkin("Rimuru", loadedSkin);

        SkinCacheManager.LoadedSkin retrieved = SkinCacheManager.getLoadedSkin("rimuru");
        assertNotNull(retrieved);
        assertEquals("rimuru", retrieved.skinKey());
        assertTrue(retrieved.isSlim());

        assertNull(SkinCacheManager.getLoadedSkin("desconhecido"));
    }

    @Test
    @DisplayName("Validar ciclo de vida e vinculacao no CompanionManager")
    void testCompanionManagerLifecycle() {
        UUID ownerUuid = UUID.randomUUID();

        // 1. Spawna o companheiro
        NeoForgeCompanionEntity companion = CompanionManager.spawnCompanion(ownerUuid, "MeuParceiro");
        assertNotNull(companion);
        assertEquals("MeuParceiro", companion.getName());
        assertEquals(ownerUuid, companion.getOwnerUuid());
        assertEquals("", companion.getCustomSkin()); // Padrao: herda skin do dono
        assertFalse(companion.isModelSlim());

        // 2. Consulta pelo dono
        NeoForgeCompanionEntity found = CompanionManager.getCompanionForOwner(ownerUuid);
        assertSame(companion, found);

        // 3. Altera a skin para personagem de anime
        CompanionManager.setCustomSkin(ownerUuid, "Rimuru");
        assertEquals("Rimuru", companion.getCustomSkin());

        companion.setModelSlim(true);
        assertTrue(companion.isModelSlim());

        // 4. Modifica o modo
        companion.setMode(CompanionMode.DEFEND);
        assertEquals(CompanionMode.DEFEND, companion.getMode());

        // 5. Remove o companheiro
        CompanionManager.removeCompanion(ownerUuid);
        assertNull(CompanionManager.getCompanionForOwner(ownerUuid));
    }

    @Test
    @DisplayName("Validar registro da arvore de comandos Brigadier")
    void testBrigadierCommandRegistration() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        assertDoesNotThrow(() -> CompanionCommands.register(dispatcher));

        // Valida nos registrados
        assertNotNull(dispatcher.getRoot().getChild("companion"));
        assertNotNull(dispatcher.getRoot().getChild("companions"));
        assertNotNull(dispatcher.getRoot().getChild("skin"));
        assertNotNull(dispatcher.getRoot().getChild("help"));

        // Subcomandos de /companion
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("spawn"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("lan"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("recall"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("dismiss"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("skin"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("mode"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("mode").getChild("follow"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("mode").getChild("stay"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("mode").getChild("defend"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("action"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("action").getChild("wood"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("action").getChild("mine"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("inventory"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("deposit"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("view"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("tensura"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("help"));
        assertNotNull(dispatcher.getRoot().getChild("companion").getChild("gui"));
    }

    @Test
    @DisplayName("Validar catalogo de presets de skins oficiais e geracao de Base64")
    void testSkinPresetCatalog() {
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Rimuru"));
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Goku"));
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Luffy"));
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Naruto"));
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Kirito"));
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Gojo"));
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Zoro"));
        assertTrue(com.thyagotoledo.companions.core.skin.SkinPresetCatalog.hasPreset("Tanjiro"));

        com.thyagotoledo.companions.core.skin.SkinPresetCatalog.PresetSkin rimuru =
                com.thyagotoledo.companions.core.skin.SkinPresetCatalog.getPreset("Rimuru");
        assertNotNull(rimuru);
        assertEquals("Rimuru", rimuru.getName());
        assertTrue(rimuru.isSlim());
        assertNotNull(rimuru.getBase64Value());
        assertFalse(rimuru.getBase64Value().isEmpty());

        com.thyagotoledo.companions.core.skin.SkinPresetCatalog.PresetSkin goku =
                com.thyagotoledo.companions.core.skin.SkinPresetCatalog.getPreset("goku");
        assertNotNull(goku);
        assertFalse(goku.isSlim());

        // Modos novos WOOD e MINE
        NeoForgeCompanionEntity companion = CompanionManager.spawnCompanion(UUID.randomUUID(), "Operario");
        companion.setMode(CompanionMode.WOOD);
        assertEquals(CompanionMode.WOOD, companion.getMode());
        companion.setMode(CompanionMode.MINE);
        assertEquals(CompanionMode.MINE, companion.getMode());
    }
}
