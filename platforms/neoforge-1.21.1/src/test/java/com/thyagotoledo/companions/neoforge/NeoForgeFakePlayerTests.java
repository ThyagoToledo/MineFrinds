package com.thyagotoledo.companions.neoforge;

import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.player.FakeClientConnection;
import com.thyagotoledo.companions.neoforge.entity.player.LanIntegrationHelper;
import net.minecraft.network.protocol.PacketFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes automatizados para a infraestrutura de Fake Player e suporte a LAN.
 */
public class NeoForgeFakePlayerTests {

    @BeforeEach
    void setUp() {
        CompanionManager.clearAll();
    }

    @Test
    @DisplayName("Validar FakeClientConnection simulada para conexao em memoria")
    void testFakeClientConnection() {
        FakeClientConnection conn = new FakeClientConnection(PacketFlow.SERVERBOUND);

        assertTrue(conn.isConnected());
        assertTrue(conn.isMemoryConnection());

        // Deve descartar silenciosamente pacotes de saida sem lancar excecoes
        assertDoesNotThrow(() -> conn.send(null));
        assertDoesNotThrow(() -> conn.send(null, null));
        assertDoesNotThrow(() -> conn.send(null, null, false));
        assertDoesNotThrow(conn::handleDisconnection);
    }

    @Test
    @DisplayName("Validar resiliencia do LanIntegrationHelper")
    void testLanIntegrationHelper() {
        // Nao deve lancar excecao com parametros nulos
        assertEquals(-1, LanIntegrationHelper.openWorldToLan(null, 25565));
        assertFalse(LanIntegrationHelper.isLanPublished(null));
    }

    @Test
    @DisplayName("Validar consultas e ciclo de vida seguro de bots no CompanionManager")
    void testCompanionManagerPlayerLifecycle() {
        UUID randomUuid = UUID.randomUUID();

        assertNull(CompanionManager.getPlayerCompanion(randomUuid));
        assertFalse(CompanionManager.recallPlayerCompanion(null));
        assertFalse(CompanionManager.dismissPlayerCompanion(null));
        assertFalse(CompanionManager.dismissPlayerCompanion(randomUuid));

        // Limpeza total
        assertDoesNotThrow(CompanionManager::clearAll);
    }

    @Test
    @DisplayName("Rejeitar replay de requestId sem misturar historico entre donos")
    void testRequestDeduplication() {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        UUID request = UUID.randomUUID();

        assertTrue(CompanionManager.registerRequest(ownerA, request));
        assertFalse(CompanionManager.registerRequest(ownerA, request));
        assertTrue(CompanionManager.registerRequest(ownerB, request));
    }
}
