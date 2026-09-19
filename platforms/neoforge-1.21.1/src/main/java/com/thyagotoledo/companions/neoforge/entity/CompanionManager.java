package com.thyagotoledo.companions.neoforge.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.core.skin.SkinPresetCatalog;
import com.thyagotoledo.companions.neoforge.entity.player.CompanionServerPlayer;
import com.thyagotoledo.companions.neoforge.entity.player.FakeClientConnection;
import com.thyagotoledo.companions.neoforge.entity.player.LanIntegrationHelper;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import com.thyagotoledo.companions.neoforge.service.NeoForgeQuestService;
import com.thyagotoledo.companions.neoforge.tensura.TensuraCompanionStats;
import com.thyagotoledo.companions.neoforge.tensura.TensuraNeoOtherworldAdapter;
import com.thyagotoledo.companions.core.model.CompanionSnapshot;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Gerenciador central de instancias de companheiros e bots de jogador fake (ServerPlayer).
 */
public class CompanionManager {

    private static final Map<UUID, NeoForgeCompanionEntity> COMPANIONS_BY_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, CompanionServerPlayer> FAKE_PLAYERS_BY_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, net.minecraft.core.BlockPos> DESIGNATED_CHESTS = new ConcurrentHashMap<>();

    public static void setDesignatedChest(UUID ownerUuid, net.minecraft.core.BlockPos pos) {
        setDesignatedChest(ownerUuid, pos, null);
    }

    public static void setDesignatedChest(UUID ownerUuid, net.minecraft.core.BlockPos pos, ServerLevel level) {
        if (ownerUuid == null) return;
        if (pos == null) {
            DESIGNATED_CHESTS.remove(ownerUuid);
            if (level != null) {
                CompanionSavedData savedData = CompanionSavedData.get(level);
                if (savedData != null) {
                    savedData.setDesignatedChest(ownerUuid, null);
                }
            }
        } else {
            DESIGNATED_CHESTS.put(ownerUuid, pos.immutable());
            if (level != null) {
                CompanionSavedData savedData = CompanionSavedData.get(level);
                if (savedData != null) {
                    savedData.setDesignatedChest(ownerUuid, pos);
                }
            }
        }
    }

    public static net.minecraft.core.BlockPos getDesignatedChest(UUID ownerUuid) {
        return getDesignatedChest(ownerUuid, null);
    }

    public static net.minecraft.core.BlockPos getDesignatedChest(UUID ownerUuid, ServerLevel level) {
        if (ownerUuid == null) return null;
        net.minecraft.core.BlockPos pos = DESIGNATED_CHESTS.get(ownerUuid);
        if (pos == null && level != null) {
            CompanionSavedData savedData = CompanionSavedData.get(level);
            if (savedData != null) {
                pos = savedData.getDesignatedChest(ownerUuid);
                if (pos != null) {
                    DESIGNATED_CHESTS.put(ownerUuid, pos.immutable());
                }
            }
        }
        return pos;
    }

    public static CompanionSnapshot buildSnapshot(UUID ownerUuid) {
        return buildSnapshot(ownerUuid, null);
    }

    public static CompanionSnapshot buildSnapshot(UUID ownerUuid, ServerLevel level) {
        if (ownerUuid == null) {
            return CompanionSnapshot.empty(null);
        }

        CompanionServerPlayer fakePlayer = FAKE_PLAYERS_BY_OWNER.get(ownerUuid);
        NeoForgeCompanionEntity entity = COMPANIONS_BY_OWNER.get(ownerUuid);
        net.minecraft.core.BlockPos chestPos = getDesignatedChest(ownerUuid, level);

        boolean hasChest = chestPos != null;
        int cx = hasChest ? chestPos.getX() : 0;
        int cy = hasChest ? chestPos.getY() : 0;
        int cz = hasChest ? chestPos.getZ() : 0;

        boolean tensuraLoaded = false;
        try {
            tensuraLoaded = net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("tensura");
        } catch (Throwable ignored) {
        }

        String tensuraRace = "Humano";
        String tensuraRank = "F";
        long tensuraEp = 0L;

        if (entity != null && entity.getTensuraStats() != null) {
            tensuraRace = entity.getTensuraStats().getRace() != null ? entity.getTensuraStats().getRace().getDisplayName("pt_br") : "Humano";
            tensuraRank = entity.getTensuraStats().getRank();
            tensuraEp = entity.getTensuraStats().getEvolutionPoints();
        }


        if (fakePlayer != null) {
            return new CompanionSnapshot(
                    fakePlayer.getUUID(),
                    ownerUuid,
                    fakePlayer.getName().getString(),
                    fakePlayer.getMode(),
                    fakePlayer.getHealth(),
                    fakePlayer.getMaxHealth(),
                    true,
                    true,
                    entity != null ? entity.getCustomSkin() : "",
                    hasChest,
                    cx,
                    cy,
                    cz,
                    tensuraLoaded,
                    tensuraRace,
                    tensuraRank,
                    tensuraEp,
                    "Companheiro ativo e operando."
            );
        }

        if (entity != null) {
            return new CompanionSnapshot(
                    entity.getProfile().getId(),
                    ownerUuid,
                    entity.getProfile().getName(),
                    entity.getMode(),
                    20.0f,
                    20.0f,
                    false,
                    false,
                    entity.getCustomSkin(),
                    hasChest,
                    cx,
                    cy,
                    cz,
                    tensuraLoaded,
                    tensuraRace,
                    tensuraRank,
                    tensuraEp,
                    "Companheiro registrado (aguardando invocacao)."
            );
        }

        if (level != null) {
            CompanionSavedData savedData = CompanionSavedData.get(level);
            if (savedData != null) {
                CompanionSavedData.CompanionStateRecord rec = savedData.getCompanionRecord(ownerUuid);
                if (rec != null) {
                    CompanionMode mode = CompanionMode.FOLLOW;
                    try {
                        mode = CompanionMode.valueOf(rec.getMode());
                    } catch (Exception ignored) {
                    }
                    return new CompanionSnapshot(
                            null,
                            ownerUuid,
                            rec.getName(),
                            mode,
                            20.0f,
                            20.0f,
                            false,
                            false,
                            rec.getSkin(),
                            hasChest,
                            cx,
                            cy,
                            cz,
                            tensuraLoaded,
                            tensuraRace,
                            tensuraRank,
                            rec.getTensuraEp(),
                            "Companheiro carregado da persistencia do mundo."
                    );
                }
            }
        }

        return CompanionSnapshot.empty(ownerUuid);
    }


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
            existing.dismissSilent();
        }

        // Obtem dados logicos do companion
        NeoForgeCompanionEntity dataEntity = getOrCreateCompanion(ownerUuid, finalName);
        String customSkin = dataEntity.getCustomSkin();

        // Prepara GameProfile com nome e UUID
        UUID botUuid = UUIDUtil.createOfflinePlayerUUID(finalName + "_" + owner.getName().getString() + "_" + System.currentTimeMillis());
        GameProfile profile = new GameProfile(botUuid, finalName);

        // Aplica skin inicial
        applySkinToProfile(profile, owner, customSkin);

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

        if (owner.serverLevel() != null) {
            CompanionSavedData savedData = CompanionSavedData.get(owner.serverLevel());
            if (savedData != null) {
                long ep = dataEntity.getTensuraStats() != null ? dataEntity.getTensuraStats().getEvolutionPoints() : 0L;
                savedData.saveCompanionRecord(ownerUuid, finalName, customSkin, dataEntity.getMode().name(), dataEntity.getPersonality().name(), ep);
            }
        }

        return fakePlayer;
    }

    /**
     * Atualiza a skin do companheiro ativo em tempo real.
     * Preserva posicao, inventario, equipamentos completos, experiencia, vida e modo do bot.
     */
    public static boolean updateCompanionSkin(ServerPlayer owner, String skinName) {
        if (owner == null) return false;
        MinecraftServer server = owner.getServer();
        if (server == null) return false;

        UUID ownerUuid = owner.getUUID();
        CompanionServerPlayer existing = FAKE_PLAYERS_BY_OWNER.get(ownerUuid);
        if (existing == null) {
            setCustomSkin(ownerUuid, skinName);
            return true;
        }

        // Salva dados do estado atual
        double x = existing.getX();
        double y = existing.getY();
        double z = existing.getZ();
        float yRot = existing.getYRot();
        float xRot = existing.getXRot();
        float health = existing.getHealth();
        CompanionMode mode = existing.getMode();
        String companionName = existing.getName().getString();

        int expLevel = existing.experienceLevel;
        int totalExp = existing.totalExperience;
        float expProgress = existing.experienceProgress;

        ItemStack[] savedInventory = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            savedInventory[i] = existing.getInventory().getItem(i).copy();
        }

        Map<EquipmentSlot, ItemStack> savedEquipment = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            savedEquipment.put(slot, existing.getItemBySlot(slot).copy());
        }

        // Remove o bot antigo sem broadcast de saida
        existing.dismissSilent();
        FAKE_PLAYERS_BY_OWNER.remove(ownerUuid);

        // Cria novo profile com novo UUID para forcar o cliente a recarregar a textura da skin
        UUID newBotUuid = UUIDUtil.createOfflinePlayerUUID(companionName + "_" + skinName.toLowerCase(Locale.ROOT) + "_" + System.currentTimeMillis());
        GameProfile newProfile = new GameProfile(newBotUuid, companionName);

        applySkinToProfile(newProfile, owner, skinName);
        setCustomSkin(ownerUuid, skinName);

        // Spawna o novo bot preservando todo o progresso
        FakeClientConnection fakeConn = new FakeClientConnection(PacketFlow.SERVERBOUND);
        ClientInformation clientInfo = ClientInformation.createDefault();
        NeoForgeCompanionEntity dataEntity = getOrCreateCompanion(ownerUuid, companionName);
        CompanionServerPlayer newBot = new CompanionServerPlayer(
                server,
                owner.serverLevel(),
                newProfile,
                clientInfo,
                ownerUuid,
                dataEntity
        );

        newBot.setPos(x, y, z);
        newBot.setYRot(yRot);
        newBot.setXRot(xRot);
        newBot.setYHeadRot(yRot);
        newBot.setHealth(health);
        newBot.setMode(mode);
        newBot.setExperienceLevels(expLevel);
        newBot.setExperiencePoints(totalExp);
        newBot.experienceProgress = expProgress;

        for (int i = 0; i < 36; i++) {
            if (savedInventory[i] != null && !savedInventory[i].isEmpty()) {
                newBot.getInventory().setItem(i, savedInventory[i]);
            }
        }

        for (Map.Entry<EquipmentSlot, ItemStack> entry : savedEquipment.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                newBot.setItemSlot(entry.getKey(), entry.getValue());
            }
        }

        CommonListenerCookie cookie = new CommonListenerCookie(newProfile, 0, clientInfo, false);
        server.getPlayerList().placeNewPlayer(fakeConn, newBot, cookie);

        FAKE_PLAYERS_BY_OWNER.put(ownerUuid, newBot);

        if (owner.serverLevel() != null) {
            CompanionSavedData savedData = CompanionSavedData.get(owner.serverLevel());
            if (savedData != null) {
                long ep = dataEntity.getTensuraStats() != null ? dataEntity.getTensuraStats().getEvolutionPoints() : 0L;
                savedData.saveCompanionRecord(ownerUuid, companionName, skinName, mode.name(), dataEntity.getPersonality().name(), ep);
            }
        }

        return true;
    }


    private static void applySkinToProfile(GameProfile profile, ServerPlayer owner, String skinName) {
        if (skinName == null || skinName.trim().isEmpty() ||
                skinName.equalsIgnoreCase("reset") || skinName.equalsIgnoreCase("self") || skinName.equalsIgnoreCase("player")) {
            if (owner.getGameProfile().getProperties().containsKey("textures")) {
                for (Property prop : owner.getGameProfile().getProperties().get("textures")) {
                    profile.getProperties().put("textures", prop);
                }
            }
            return;
        }

        // Verifica catalogo de presets de anime
        if (SkinPresetCatalog.hasPreset(skinName)) {
            SkinPresetCatalog.PresetSkin preset = SkinPresetCatalog.getPreset(skinName);
            if (preset != null && preset.hasSignature()) {
                profile.getProperties().put("textures", new Property("textures", preset.getBase64Value(), preset.getSignature()));
            } else if (preset != null) {
                profile.getProperties().put("textures", new Property("textures", preset.getBase64Value()));
            }
            return;
        }

        // Tenta buscar via Ashcon API de forma rapida
        boolean remoteFound = fetchRemoteSkinProperties(profile, skinName);
        if (!remoteFound) {
            // Fallback para skin do dono se offline ou inexistente
            if (owner.getGameProfile().getProperties().containsKey("textures")) {
                for (Property prop : owner.getGameProfile().getProperties().get("textures")) {
                    profile.getProperties().put("textures", prop);
                }
            }
        }
    }

    private static boolean fetchRemoteSkinProperties(GameProfile profile, String username) {
        try {
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);
            conn.setRequestProperty("User-Agent", "MineFriends-CompanionsMod");

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("textures") && root.getAsJsonObject("textures").has("raw")) {
                        JsonObject raw = root.getAsJsonObject("textures").getAsJsonObject("raw");
                        String val = raw.get("value").getAsString();
                        String sig = raw.has("signature") && !raw.get("signature").isJsonNull() ? raw.get("signature").getAsString() : null;
                        profile.getProperties().put("textures", new Property("textures", val, sig));
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean recallPlayerCompanion(ServerPlayer owner) {
        if (owner == null) return false;
        CompanionServerPlayer fakePlayer = FAKE_PLAYERS_BY_OWNER.get(owner.getUUID());
        if (fakePlayer != null) {
            return fakePlayer.tryRecallToOwner(owner);
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
        DESIGNATED_CHESTS.clear();
    }
}
