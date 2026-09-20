package com.thyagotoledo.companions.neoforge.service;

import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.player.CompanionServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.*;

/** Server owns authority, session nonce, survival actions and the return position. */
public final class RemoteViewService {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final class Session {
        final ServerPlayer owner;
        final CompanionServerPlayer npc;
        final UUID token = UUID.randomUUID();
        final GameType originalMode;
        final ServerLevel level;
        final Vec3 position;
        final float yaw, pitch;
        final boolean control;
        long sequence = -1, inputTick;
        Input input;
        Session(ServerPlayer owner, CompanionServerPlayer npc, boolean control) {
            this.owner = owner; this.npc = npc; this.control = control;
            originalMode = owner.gameMode.getGameModeForPlayer();
            level = owner.serverLevel(); position = owner.position();
            yaw = owner.getYRot(); pitch = owner.getXRot(); inputTick = owner.serverLevel().getGameTime();
        }
    }

    public static boolean start(ServerPlayer owner, boolean control) {
        if (owner == null) return false;
        stop(owner);
        CompanionServerPlayer npc = CompanionManager.getPlayerCompanion(owner.getUUID());
        if (npc == null || !npc.isAlive() || !owner.isAlive() || npc.level() != owner.level()) return false;
        Session session = new Session(owner, npc, control);
        SESSIONS.put(owner.getUUID(), session);
        var anchor = new net.minecraft.nbt.CompoundTag();
        anchor.putString("mode", session.originalMode.name());
        anchor.putString("dimension", session.level.dimension().location().toString());
        anchor.putDouble("x", session.position.x); anchor.putDouble("y", session.position.y); anchor.putDouble("z", session.position.z);
        anchor.putFloat("yaw", session.yaw); anchor.putFloat("pitch", session.pitch);
        ((net.neoforged.neoforge.common.extensions.IEntityExtension) owner).getPersistentData().put("companions_return", anchor);
        owner.setGameMode(GameType.SPECTATOR);
        owner.setCamera(npc);
        owner.setYRot(npc.getYRot()); owner.setXRot(npc.getXRot());
        PacketDistributor.sendToPlayer(owner, new State(true, control, session.token, npc.getId()));
        return true;
    }

    public static boolean stop(ServerPlayer owner) {
        if (owner == null) return false;
        Session session = SESSIONS.remove(owner.getUUID());
        if (session == null) return false;
        owner.setCamera(owner);
        owner.setGameMode(session.originalMode);
        owner.teleportTo(session.level, session.position.x, session.position.y, session.position.z, session.yaw, session.pitch);
        ((net.neoforged.neoforge.common.extensions.IEntityExtension) owner).getPersistentData().remove("companions_return");
        session.npc.setDeltaMovement(0, session.npc.getDeltaMovement().y, 0);
        session.npc.stopUsingItem();
        PacketDistributor.sendToPlayer(owner, new State(false, false, session.token, -1));
        return true;
    }

    public static boolean tickControlled(CompanionServerPlayer npc) {
        for (Session session : SESSIONS.values()) {
            if (session.npc != npc || !session.control) continue;
            if (!session.owner.isAlive() || session.owner.level() != npc.level()) return true;
            if (session.input != null && npc.serverLevel().getGameTime() - session.inputTick <= 10) {
                Input input = session.input;
                npc.applyRemoteInput(input.forward, input.sideways, input.yaw, input.pitch,
                        input.jump, input.attack, input.use, input.slot);
            } else npc.setDeltaMovement(0, npc.getDeltaMovement().y, 0);
            return true;
        }
        return false;
    }

    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            if (!session.owner.isAlive() || !session.npc.isAlive() || session.npc.isRemoved()
                    || session.owner.level() != session.npc.level()
                    || CompanionManager.getPlayerCompanion(session.owner.getUUID()) != session.npc
                    || (session.control && session.level.getGameTime() - session.inputTick > 40)) stop(session.owner);
        }
    }

    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }

    @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) return;
        var persistent = ((net.neoforged.neoforge.common.extensions.IEntityExtension) owner).getPersistentData();
        if (!persistent.contains("companions_return", 10)) return;
        var anchor = persistent.getCompound("companions_return");
        try {
            var dimension = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(anchor.getString("dimension")));
            ServerLevel level = owner.getServer().getLevel(dimension);
            owner.setCamera(owner);
            owner.setGameMode(GameType.valueOf(anchor.getString("mode")));
            if (level != null) owner.teleportTo(level, anchor.getDouble("x"), anchor.getDouble("y"), anchor.getDouble("z"), anchor.getFloat("yaw"), anchor.getFloat("pitch"));
            persistent.remove("companions_return");
        } catch (IllegalArgumentException invalid) {
            owner.setGameMode(GameType.SURVIVAL);
            persistent.remove("companions_return");
        }
    }

    @SubscribeEvent public static void shutdown(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (Session session : new ArrayList<>(SESSIONS.values())) stop(session.owner);
    }

    public record State(boolean active, boolean control, UUID token, int entityId) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("companions", "remote_state"));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buffer, state) -> { buffer.writeBoolean(state.active); buffer.writeBoolean(state.control); buffer.writeUUID(state.token); buffer.writeVarInt(state.entityId); },
                buffer -> new State(buffer.readBoolean(), buffer.readBoolean(), buffer.readUUID(), buffer.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Input(UUID token, long sequence, float forward, float sideways, float yaw, float pitch,
                        boolean jump, boolean attack, boolean use, boolean exit, int slot) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("companions", "remote_input"));
        public static final StreamCodec<FriendlyByteBuf, Input> CODEC = StreamCodec.of((b, p) -> {
            b.writeUUID(p.token); b.writeLong(p.sequence); b.writeFloat(p.forward); b.writeFloat(p.sideways);
            b.writeFloat(p.yaw); b.writeFloat(p.pitch); b.writeBoolean(p.jump); b.writeBoolean(p.attack);
            b.writeBoolean(p.use); b.writeBoolean(p.exit); b.writeByte(p.slot);
        }, b -> new Input(b.readUUID(), b.readLong(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        public boolean valid() {
            return token != null && sequence >= 0 && Float.isFinite(forward) && Math.abs(forward) <= 1
                    && Float.isFinite(sideways) && Math.abs(sideways) <= 1 && Float.isFinite(yaw)
                    && Math.abs(yaw) <= 36000 && Float.isFinite(pitch) && Math.abs(pitch) <= 90 && slot >= 0 && slot < 9;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(State.TYPE, State.CODEC, (state, context) -> context.enqueueWork(() ->
                com.thyagotoledo.companions.neoforge.client.RemoteViewClient.accept(state)));
        registrar.playToServer(Input.TYPE, Input.CODEC, (input, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer owner) || !input.valid()) return;
            Session session = SESSIONS.get(owner.getUUID());
            if (session == null || !session.token.equals(input.token) || input.sequence <= session.sequence) return;
            session.sequence = input.sequence;
            if (input.exit) { stop(owner); return; }
            if (!session.control) return;
            session.input = input;
            session.inputTick = owner.serverLevel().getGameTime();
        }));
    }
}
