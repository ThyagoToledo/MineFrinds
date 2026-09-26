package com.thyagotoledo.companions.neoforge.network;

import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.CompanionCommandRequest;
import com.thyagotoledo.companions.core.model.CompanionSnapshot;
import com.thyagotoledo.companions.neoforge.client.gui.CompanionScreen;
import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import com.thyagotoledo.companions.neoforge.entity.player.CompanionServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

/**
 * Adaptadores de payload de rede compativeis com a arquitetura CustomPacketPayload de Minecraft 1.21.1 / NeoForge.
 * Implementa comunicacao bidirecional C2S e S2C tipada, com codecs de fluxo oficiais e integracao direta ao CompanionScreen.
 */
public final class NeoForgeCompanionPayloads {

    public static final String MODID = "companions";
    public static final String CHANNEL_COMMAND = "companions:command_payload";
    public static final String CHANNEL_FEEDBACK = "companions:feedback_payload";
    public static final String CHANNEL_SNAPSHOT = "companions:snapshot";
    public static final String CHANNEL_REQUEST_SNAPSHOT = "companions:request_snapshot";

    /**
     * Payload C2S: solicitacao de snapshot do companheiro ao abrir a interface grafica.
     */
    public record RequestSnapshotPayload(UUID requestId) implements CustomPacketPayload {
        public static final Type<RequestSnapshotPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "request_snapshot"));
        public static final StreamCodec<FriendlyByteBuf, RequestSnapshotPayload> STREAM_CODEC =
                StreamCodec.of(RequestSnapshotPayload::write, RequestSnapshotPayload::read);

        public RequestSnapshotPayload() {
            this(UUID.randomUUID());
        }

        public RequestSnapshotPayload {
            if (requestId == null) {
                requestId = UUID.randomUUID();
            }
        }

        private static void write(FriendlyByteBuf buf, RequestSnapshotPayload payload) {
            buf.writeUUID(payload.requestId());
        }

        private static RequestSnapshotPayload read(FriendlyByteBuf buf) {
            return new RequestSnapshotPayload(buf.readUUID());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public String getChannelName() {
            return CHANNEL_REQUEST_SNAPSHOT;
        }
    }

    /**
     * Payload S2C: sincronizacao do snapshot de estado real do companheiro com o cliente.
     */
    public record SnapshotPayload(UUID requestId, CompanionSnapshot snapshot) implements CustomPacketPayload {
        public static final Type<SnapshotPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "snapshot"));
        public static final StreamCodec<FriendlyByteBuf, SnapshotPayload> STREAM_CODEC =
                StreamCodec.of(SnapshotPayload::write, SnapshotPayload::read);

        public static void write(FriendlyByteBuf buf, SnapshotPayload payload) {
            buf.writeUUID(payload.requestId());
            CompanionSnapshot s = payload.snapshot() != null ? payload.snapshot() : CompanionSnapshot.empty(null);
            buf.writeBoolean(s.getCompanionUuid() != null);
            if (s.getCompanionUuid() != null) {
                buf.writeUUID(s.getCompanionUuid());
            }
            buf.writeBoolean(s.getOwnerUuid() != null);
            if (s.getOwnerUuid() != null) {
                buf.writeUUID(s.getOwnerUuid());
            }
            buf.writeUtf(s.getName() != null ? s.getName() : "");
            buf.writeUtf(s.getMode() != null ? s.getMode().name() : "FOLLOW");
            buf.writeFloat(s.getCurrentHealth());
            buf.writeFloat(s.getMaxHealth());
            buf.writeBoolean(s.isSpawned());
            buf.writeBoolean(s.isFakePlayer());
            buf.writeUtf(s.getSkinName() != null ? s.getSkinName() : "");
            buf.writeBoolean(s.hasDesignatedChest());
            buf.writeInt(s.getDesignatedChestX());
            buf.writeInt(s.getDesignatedChestY());
            buf.writeInt(s.getDesignatedChestZ());
            buf.writeBoolean(s.isTensuraActive());
            buf.writeUtf(s.getTensuraRace() != null ? s.getTensuraRace() : "");
            buf.writeUtf(s.getTensuraRank() != null ? s.getTensuraRank() : "");
            buf.writeLong(s.getTensuraEp());
            buf.writeUtf(s.getLastMessage() != null ? s.getLastMessage() : "");
            buf.writeLong(s.getRevision());
        }

        public static SnapshotPayload read(FriendlyByteBuf buf) {
            UUID requestId = buf.readUUID();
            UUID companionUuid = buf.readBoolean() ? buf.readUUID() : null;
            UUID ownerUuid = buf.readBoolean() ? buf.readUUID() : null;
            String name = buf.readUtf();
            String modeName = buf.readUtf();
            CompanionMode mode;
            try {
                mode = CompanionMode.valueOf(modeName);
            } catch (Exception e) {
                mode = CompanionMode.FOLLOW;
            }
            float currentHealth = buf.readFloat();
            float maxHealth = buf.readFloat();
            boolean spawned = buf.readBoolean();
            boolean fakePlayer = buf.readBoolean();
            String skinName = buf.readUtf();
            boolean hasChest = buf.readBoolean();
            int chestX = buf.readInt();
            int chestY = buf.readInt();
            int chestZ = buf.readInt();
            boolean tensuraActive = buf.readBoolean();
            String tensuraRace = buf.readUtf();
            String tensuraRank = buf.readUtf();
            long tensuraEp = buf.readLong();
            String lastMessage = buf.readUtf();
            long revision = buf.readLong();

            CompanionSnapshot snapshot = new CompanionSnapshot(
                    companionUuid, ownerUuid, name, mode, currentHealth, maxHealth,
                    spawned, fakePlayer, skinName, hasChest, chestX, chestY, chestZ,
                    tensuraActive, tensuraRace, tensuraRank, tensuraEp, lastMessage, revision
            );
            return new SnapshotPayload(requestId, snapshot);
        }

        public SnapshotPayload(CompanionSnapshot snapshot) {
            this(UUID.randomUUID(), snapshot);
        }

        public SnapshotPayload {
            if (requestId == null) {
                requestId = UUID.randomUUID();
            }
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public CompanionSnapshot getSnapshot() {
            return snapshot;
        }

        public UUID getRequestId() {
            return requestId;
        }

        public String getChannelName() {
            return CHANNEL_SNAPSHOT;
        }
    }

    /**
     * Payload C2S: envio tipado de comando ou mensagem para o companheiro.
     */
    public record CommandPayload(UUID companionUuid, String command, String locale, UUID requestId, long revision) implements CustomPacketPayload {
        public static final Type<CommandPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "command_payload"));
        public static final StreamCodec<FriendlyByteBuf, CommandPayload> STREAM_CODEC =
                StreamCodec.of(CommandPayload::write, CommandPayload::read);

        public CommandPayload(UUID companionUuid, String command, String locale) {
            this(companionUuid, command, locale, UUID.randomUUID(), 0L);
        }

        public CommandPayload(UUID companionUuid, String command, String locale, UUID requestId, long revision) {
            this.companionUuid = companionUuid;
            this.command = command != null ? command : "";
            this.locale = locale != null ? locale : "pt_br";
            this.requestId = requestId != null ? requestId : UUID.randomUUID();
            this.revision = Math.max(0L, revision);
        }

        public static void write(FriendlyByteBuf buf, CommandPayload payload) {
            buf.writeBoolean(payload.companionUuid != null);
            if (payload.companionUuid != null) {
                buf.writeUUID(payload.companionUuid);
            }
            buf.writeUUID(payload.requestId);
            buf.writeLong(payload.revision);
            buf.writeUtf(payload.command != null ? payload.command : "", 256);
            buf.writeUtf(payload.locale != null ? payload.locale : "pt_br", 16);
        }

        public static CommandPayload read(FriendlyByteBuf buf) {
            UUID companionUuid = buf.readBoolean() ? buf.readUUID() : null;
            UUID requestId = buf.readUUID();
            long revision = buf.readLong();
            String command = buf.readUtf(256);
            String locale = buf.readUtf(16);
            return new CommandPayload(companionUuid, command, locale, requestId, revision);
        }

        public UUID getCompanionUuid() {
            return companionUuid;
        }

        public String getCommand() {
            return command;
        }

        public String getLocale() {
            return locale;
        }

        public UUID getRequestId() {
            return requestId;
        }

        public long getRevision() {
            return revision;
        }

        public CompanionCommandRequest toRequest() {
            return CompanionCommandRequest.fromText(requestId, companionUuid, command, locale, revision);
        }

        public String getChannelName() {
            return CHANNEL_COMMAND;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Payload S2C: feedback e fala do companheiro para a interface do cliente.
     */
    public record FeedbackPayload(UUID companionUuid, String speech, String locale, boolean success) implements CustomPacketPayload {
        public static final Type<FeedbackPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "feedback_payload"));
        public static final StreamCodec<FriendlyByteBuf, FeedbackPayload> STREAM_CODEC =
                StreamCodec.of(FeedbackPayload::write, FeedbackPayload::read);

        public FeedbackPayload(UUID companionUuid, String speech, String locale, boolean success) {
            this.companionUuid = companionUuid;
            this.speech = speech != null ? speech : "";
            this.locale = locale != null ? locale : "pt_br";
            this.success = success;
        }

        public static void write(FriendlyByteBuf buf, FeedbackPayload payload) {
            buf.writeBoolean(payload.companionUuid != null);
            if (payload.companionUuid != null) {
                buf.writeUUID(payload.companionUuid);
            }
            buf.writeUtf(payload.speech != null ? payload.speech : "");
            buf.writeUtf(payload.locale != null ? payload.locale : "pt_br");
            buf.writeBoolean(payload.success);
        }

        public static FeedbackPayload read(FriendlyByteBuf buf) {
            UUID companionUuid = buf.readBoolean() ? buf.readUUID() : null;
            String speech = buf.readUtf();
            String locale = buf.readUtf();
            boolean success = buf.readBoolean();
            return new FeedbackPayload(companionUuid, speech, locale, success);
        }

        public UUID getCompanionUuid() {
            return companionUuid;
        }

        public String getSpeech() {
            return speech;
        }

        public String getLocale() {
            return locale;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getChannelName() {
            return CHANNEL_FEEDBACK;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Ouvinte oficial do evento RegisterPayloadHandlersEvent do NeoForge 1.21.1.
     */
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.1.0").optional();
        com.thyagotoledo.companions.neoforge.service.RemoteViewService.register(registrar);

        registrar.playToServer(
                RequestSnapshotPayload.TYPE,
                RequestSnapshotPayload.STREAM_CODEC,
                NeoForgeCompanionPayloads::handleRequestSnapshotServer
        );

        registrar.playToClient(
                SnapshotPayload.TYPE,
                SnapshotPayload.STREAM_CODEC,
                NeoForgeCompanionPayloads::handleSnapshotClient
        );

        registrar.playToServer(
                CommandPayload.TYPE,
                CommandPayload.STREAM_CODEC,
                NeoForgeCompanionPayloads::handleCommandServer
        );

        registrar.playToClient(
                FeedbackPayload.TYPE,
                FeedbackPayload.STREAM_CODEC,
                NeoForgeCompanionPayloads::handleFeedbackClient
        );
    }

    public static void handleRequestSnapshotServer(RequestSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                CompanionSnapshot snapshot = CompanionManager.buildSnapshot(serverPlayer.getUUID(), serverPlayer.serverLevel());
                context.reply(new SnapshotPayload(payload.requestId(), snapshot));
            }
        });
    }

    public static void handleSnapshotClient(SnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPacketHandler.applySnapshot(payload.requestId(), payload.snapshot());
        });
    }

    public static void handleCommandServer(CommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                if (serverPlayer.getServer() != null) {
                    String cmd = payload.getCommand();
                    if (payload.getCompanionUuid() == null || cmd == null || cmd.length() > 256) {
                        return;
                    }
                    CompanionSnapshot snapshot = CompanionManager.buildSnapshot(serverPlayer.getUUID(), serverPlayer.serverLevel());
                    if (snapshot.getCompanionUuid() == null || !snapshot.getCompanionUuid().equals(payload.getCompanionUuid())) {
                        return;
                    }
                    if (payload.getRevision() > 0L && payload.getRevision() != snapshot.getRevision()) {
                        return;
                    }
                    if (!payload.toRequest().isValid()) {
                        return;
                    }
                    if (!CompanionManager.registerRequest(serverPlayer.getUUID(), payload.getRequestId())) {
                        return;
                    }
                    CompanionCommandRequest request = payload.toRequest();
                    if (request.getIntent() == com.thyagotoledo.companions.core.dialogue.IntentType.CASUAL_CHAT) {
                        NeoForgeCompanionEntity dataEntity = CompanionManager.getCompanionForOwner(serverPlayer.getUUID());
                        if (dataEntity == null) return;
                        dataEntity.handleCommandAsync(request).thenAccept(response ->
                                serverPlayer.getServer().execute(() -> {
                                    CompanionSnapshot current = CompanionManager.buildSnapshot(
                                            serverPlayer.getUUID(), serverPlayer.serverLevel());
                                    if (current.getRevision() != request.getExpectedRevision()) return;
                                    if (response != null && response.getSpeech() != null) {
                                        CompanionServerPlayer fake = CompanionManager.getPlayerCompanion(serverPlayer.getUUID());
                                        if (fake != null) fake.speakToOwner(response.getSpeech());
                                    }
                                }));
                        return;
                    }
                    if (cmd != null && !cmd.trim().isEmpty()) {
                        String clean = cmd.trim();
                        if (clean.startsWith("/")) {
                            String commandBody = clean.substring(1).trim();
                            String root = commandBody.contains(" ")
                                    ? commandBody.substring(0, commandBody.indexOf(' '))
                                    : commandBody;
                            if (!root.equalsIgnoreCase("companion")) {
                                return;
                            }
                            serverPlayer.getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), commandBody);
                        } else {
                            serverPlayer.getServer().getCommands().performPrefixedCommand(serverPlayer.createCommandSourceStack(), "companion chat " + clean);
                        }
                    }
                }
            }
        });
    }

    public static void handleFeedbackClient(FeedbackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPacketHandler.applyFeedback(payload.getSpeech());
        });
    }


    private static final class ClientPacketHandler {
        static void applySnapshot(UUID requestId, CompanionSnapshot snapshot) {
            CompanionScreen.setActiveSnapshot(requestId, snapshot);
        }

        static void applyFeedback(String speech) {
            CompanionScreen.setLastStatusFeedback(speech);
        }
    }

    private NeoForgeCompanionPayloads() {
    }
}
