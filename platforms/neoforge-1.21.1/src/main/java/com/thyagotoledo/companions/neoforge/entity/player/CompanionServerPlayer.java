package com.thyagotoledo.companions.neoforge.entity.player;

import com.mojang.authlib.GameProfile;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Entidade ServerPlayer fake para o companheiro (estilo Carpet Mod).
 * Faz com que o companheiro conste oficialmente no servidor como jogador:
 * - Aparece na lista do Tab.
 * - Dispara mensagem de entrada e saida no chat.
 * - Renderiza com modelo e textura 3D de jogador humanoide.
 * - Executa IA autonoma motora (seguir o dono, olhar, saltar, atacar e auto-curar).
 */
public class CompanionServerPlayer extends ServerPlayer {

    private final UUID ownerUuid;
    private final NeoForgeCompanionEntity dataEntity;
    private CompanionMode mode = CompanionMode.FOLLOW;
    private int healCooldown = 0;

    public CompanionServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile,
                                ClientInformation clientInfo, UUID ownerUuid,
                                NeoForgeCompanionEntity dataEntity) {
        super(server, level, profile, clientInfo);
        this.ownerUuid = ownerUuid;
        this.dataEntity = dataEntity;
        if (dataEntity != null) {
            this.mode = dataEntity.getMode();
        }
    }

    @Override
    public void tick() {
        super.doTick();
        tickAI();
    }

    /**
     * Executa a logica motora e tomada de decisoes da IA em cada tick do servidor.
     */
    private void tickAI() {
        if (this.server == null || this.level().isClientSide) {
            return;
        }

        // Sincroniza modo com dataEntity se disponivel
        if (dataEntity != null) {
            this.mode = dataEntity.getMode();
        }

        // 1. Auto-Cura quando ferido (vida < 70%)
        if (healCooldown > 0) {
            healCooldown--;
        } else if (this.getHealth() < this.getMaxHealth() * 0.7f) {
            this.heal(1.0f);
            healCooldown = 60; // 3 segundos entre curas
        }

        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);

        // 2. Modo Ficar Aqui (STAY): permanece na posicao sem se deslocar
        if (this.mode == CompanionMode.STAY) {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.5, delta.y, delta.z * 0.5);
            if (owner != null && this.distanceToSqr(owner) <= 64.0) {
                lookAtEntity(owner);
            }
            return;
        }

        // 3. Modo Defender (DEFEND): ataca monstros hostis proximos
        if (this.mode == CompanionMode.DEFEND) {
            AABB searchArea = this.getBoundingBox().inflate(10.0);
            List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, searchArea);
            if (!monsters.isEmpty()) {
                Monster target = monsters.get(0);
                lookAtEntity(target);
                double distSq = this.distanceToSqr(target);
                if (distSq <= 6.0) {
                    this.attack(target);
                    this.swing(InteractionHand.MAIN_HAND, true);
                    this.resetAttackStrengthTicker();
                } else {
                    Vec3 diff = target.position().subtract(this.position());
                    Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.25);
                    double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
                    this.setDeltaMovement(dir.x, jumpY, dir.z);
                }
                return;
            }
        }

        // 4. Modo Seguir (FOLLOW): acompanha o jogador dono
        if (owner != null && owner.level() == this.level()) {
            double distSq = this.distanceToSqr(owner);

            // Teleporte de recuperacao se estiver muito distante (> 36 blocos)
            if (distSq > 1296.0) {
                this.teleportTo(owner.serverLevel(), owner.getX() + 1.0, owner.getY(), owner.getZ() + 1.0, owner.getYRot(), owner.getXRot());
                return;
            }

            // Se estiver a mais de 3 blocos de distancia, caminha em direcao ao dono
            if (distSq > 9.0) {
                lookAtEntity(owner);
                Vec3 diff = owner.position().subtract(this.position());
                Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.24);
                double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
                this.setDeltaMovement(dir.x, jumpY, dir.z);
            } else {
                // Perto do dono: desacelera e olha para ele
                Vec3 delta = this.getDeltaMovement();
                this.setDeltaMovement(delta.x * 0.5, delta.y, delta.z * 0.5);
                lookAtEntity(owner);
            }
        }
    }

    /**
     * Orienta a cabeca e o corpo para olhar em direcao a entidade alvo.
     */
    public void lookAtEntity(net.minecraft.world.entity.Entity target) {
        if (target == null) return;
        Vec3 diff = target.getEyePosition().subtract(this.getEyePosition());
        double dXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float targetYaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(diff.y, dXZ) * (180.0 / Math.PI)));
        this.setYRot(targetYaw);
        this.setXRot(targetPitch);
        this.setYHeadRot(targetYaw);
    }

    /**
     * Teleporta o companheiro para junto do seu dono de forma segura.
     */
    public void recallToOwner() {
        if (this.server == null) return;
        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);
        if (owner != null) {
            this.teleportTo(owner.serverLevel(), owner.getX() + 1.2, owner.getY(), owner.getZ() + 1.2, owner.getYRot(), owner.getXRot());
        }
    }

    /**
     * Desconecta o companion do servidor emitindo a mensagem oficial de saida.
     */
    public void dismiss() {
        if (this.server != null && this.server.getPlayerList() != null) {
            this.server.getPlayerList().broadcastSystemMessage(
                    Component.literal(this.getName().getString() + " saiu do jogo"),
                    false
            );
            this.server.getPlayerList().remove(this);
        }
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public CompanionMode getMode() {
        return mode;
    }

    public void setMode(CompanionMode mode) {
        if (mode != null) {
            this.mode = mode;
            if (dataEntity != null) {
                dataEntity.setMode(mode);
            }
        }
    }

    public NeoForgeCompanionEntity getDataEntity() {
        return dataEntity;
    }
}
