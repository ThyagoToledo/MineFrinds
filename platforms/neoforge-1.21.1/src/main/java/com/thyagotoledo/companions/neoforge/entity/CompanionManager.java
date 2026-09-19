package com.thyagotoledo.companions.neoforge.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.neoforge.entity.player.CompanionServerPlayer;
import com.thyagotoledo.companions.neoforge.entity.player.FakeClientConnection;
import com.thyagotoledo.companions.neoforge.entity.player.LanIntegrationHelper;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;
import com.thyagotoledo.companions.neoforge.tensura.TensuraCompanionStats;
import com.thyagotoledo.companions.neoforge.tensura.TensuraNeoOtherworldAdapter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador central de instancias de companheiros e bots de jogador fake (ServerPlayer).
 */
public class CompanionManager {

    private static final Map<UUID, NeoForgeCompanionEntity> COMPANIONS_BY_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, CompanionServerPlayer> FAKE_PLAYERS_BY_OWNER = new ConcurrentHashMap<>();

    public static NeoForgeCompanionEntity getCompanionForOwner(UUID ownerUuid) {
        if (ownerUuid == null) return null;
        return COMPANIONS_BY_OWNER.get(ownerUuid);
    }

    public static CompanionServerPlayer getPlayerCompanion(UUID ownerUuid) {
        if (ownerUuid == null) return null;
        return FAKE_PLAYERS_BY_OWNER.get(ownerUuid);
    }

    public static NeoForgeCompanionEntity getOrCreateCompanion(UUID ownerUuid, String customName) {
        if (ownerUuid == null) return null;

        return COMPANIONS_BY_OWNER.computeIfAbsent(ownerUuid, uuid -> {
            String finalName = (customName != null && !customName.trim().isEmpty())
                    ? customName.trim()
                    : "Companheiro";

            ConversationMemory memory = new ConversationMemory(6);
            HybridDialogueProvider dialogueProvider = new HybridDialogueProvider(null, null, memory, null);
            NeoForgePermissionService permissionService = new NeoForgePermissionService();
            NeoForgeQuestService questService = new NeoForgeQuestService();
            TensuraCompanionStats tensuraStats = new TensuraCompanionStats();
            TensuraNeoOtherworldAdapter tensuraAdapter = new TensuraNeoOtherworldAdapter();

            return new NeoForgeCompanionEntity(
                    uuid,
                    finalName,
                    Personality.BALANCED,
                    dialogueProvider,
                    permissionService,
                    questService,
                    tensuraStats,
                    tensuraAdapter
            );
        });
    }

    public static NeoForgeCompanionEntity spawnCompanion(UUID ownerUuid, String customName) {
        if (ownerUuid == null) return null;
        String finalName = (customName != null && !customName.trim().isEmpty())
                ? customName.trim()
                : "Companheiro";

        ConversationMemory memory = new ConversationMemory(6);
        HybridDialogueProvider dialogueProvider = new HybridDialogueProvider(null, null, memory, null);
        NeoForgePermissionService permissionService = new NeoForgePermissionService();
        NeoForgeQuestService questService = new NeoForgeQuestService();
        TensuraCompanionStats tensuraStats = new TensuraCompanionStats();
        TensuraNeoOtherworldAdapter tensuraAdapter = new TensuraNeoOtherworldAdapter();

        NeoForgeCompanionEntity entity = new NeoForgeCompanionEntity(
                ownerUuid,
                finalName,
                Personality.BALANCED,
                dialogueProvider,
                permissionService,
                questService,
                tensuraStats,
                tensuraAdapter
        );

        COMPANIONS_BY_OWNER.put(ownerUuid, entity);
        return entity;
    }

    /**
     * Spawna o companheiro como jogador oficial no servidor (ServerPlayer fake estilo Carpet).
     * O bot passa a constar no Tab, emitir aviso de entrada no chat e interagir diretamente.
     */
    public static CompanionServerPlayer spawnPlayerCompanion(ServerPlayer owner, String customName, boolean openLan) {
        if (owner == null) return null;
        MinecraftServer server = owner.getServer();
        if (server == null) return null;

        if (openLan) {
            LanIntegrationHelper.openWorldToLan(owner, 25565);
        }

        UUID ownerUuid = owner.getUUID();
        String finalName = (customName != null && !customName.trim().isEmpty())
                ? customName.trim()
                : "Companheiro";

        // Remove bot anterior se ja estiver no jogo
        CompanionServerPlayer existing = FAKE_PLAYERS_BY_OWNER.remove(ownerUuid);
        if (existing != null) {
            existing.dismiss();
        }

        // Obtem dados logicos do companion
        NeoForgeCompanionEntity dataEntity = getOrCreateCompanion(ownerUuid, finalName);

        // Prepara GameProfile com nome e UUID offline
        UUID botUuid = UUIDUtil.createOfflinePlayerUUID(finalName + "_" + owner.getName().getString());
        GameProfile profile = new GameProfile(botUuid, finalName);

        // Copia skin do jogador dono para o profile por padrao
        if (owner.getGameProfile().getProperties().containsKey("textures")) {
            for (Property prop : owner.getGameProfile().getProperties().get("textures")) {
                profile.getProperties().put("textures", prop);
            }
        }

        // Instancia a conexao simulada e o ServerPlayer fake
        FakeClientConnection fakeConn = new FakeClientConnection(PacketFlow.SERVERBOUND);
        ClientInformation clientInfo = ClientInformation.createDefault();
        CompanionServerPlayer fakePlayer = new CompanionServerPlayer(
                server,
                owner.serverLevel(),
                profile,
                clientInfo,
                ownerUuid,
                dataEntity
        );

        // Posiciona proximo ao dono
        fakePlayer.setPos(owner.getX() + 1.2, owner.getY(), owner.getZ() + 1.2);
        fakePlayer.setYRot(owner.getYRot());
        fakePlayer.setXRot(owner.getXRot());
        fakePlayer.setYHeadRot(owner.getYRot());

        // Adiciona a PlayerList (dispara broadcast "<Nome> entrou no jogo" e adiciona no Tab)
        CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, clientInfo, false);
        server.getPlayerList().placeNewPlayer(fakeConn, fakePlayer, cookie);

        FAKE_PLAYERS_BY_OWNER.put(ownerUuid, fakePlayer);
        return fakePlayer;
    }

    public static boolean recallPlayerCompanion(ServerPlayer owner) {
        if (owner == null) return false;
        CompanionServerPlayer fakePlayer = FAKE_PLAYERS_BY_OWNER.get(owner.getUUID());
        if (fakePlayer != null) {
            fakePlayer.recallToOwner();
            return true;
        }
        return false;
    }

    public static boolean dismissPlayerCompanion(UUID ownerUuid) {
        if (ownerUuid == null) return false;
        CompanionServerPlayer fakePlayer = FAKE_PLAYERS_BY_OWNER.remove(ownerUuid);
        if (fakePlayer != null) {
            fakePlayer.dismiss();
            return true;
        }
        return false;
    }

    public static void setCustomSkin(UUID ownerUuid, String skinName) {
        NeoForgeCompanionEntity entity = getCompanionForOwner(ownerUuid);
        if (entity != null) {
            entity.setCustomSkin(skinName);
        }
    }

    public static void removeCompanion(UUID ownerUuid) {
        if (ownerUuid != null) {
            COMPANIONS_BY_OWNER.remove(ownerUuid);
            dismissPlayerCompanion(ownerUuid);
        }
    }

    public static void clearAll() {
        for (CompanionServerPlayer player : FAKE_PLAYERS_BY_OWNER.values()) {
            player.dismiss();
        }
        FAKE_PLAYERS_BY_OWNER.clear();
        COMPANIONS_BY_OWNER.clear();
    }
}
