package com.thyagotoledo.companions.neoforge.entity.player;

import com.mojang.authlib.GameProfile;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Entidade ServerPlayer fake para o companheiro (estilo Carpet Mod).
 * Faz com que o companheiro conste oficialmente no servidor como jogador:
 * - Aparece na lista do Tab com cabeca e nome.
 * - Dispara mensagem de entrada e saida no chat.
 * - Renderiza com modelo e textura 3D de jogador humanoide.
 * - Executa IA autonoma motora (seguir, defender, coletar madeira, minerar, guardar em baus e auto-cura).
 */
public class CompanionServerPlayer extends ServerPlayer {

    private final UUID ownerUuid;
    private final NeoForgeCompanionEntity dataEntity;
    private CompanionMode mode = CompanionMode.FOLLOW;
    private int healCooldown = 0;

    // Estado para tarefas ativas de trabalho (Madeira / Mineracao)
    private BlockPos targetWorkPos = null;
    private int workBreakTicks = 0;
    private int harvestedCount = 0;

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

        // 1. Auto-Cura quando ferido (vida < 70%)
        if (healCooldown > 0) {
            healCooldown--;
        } else if (this.getHealth() < this.getMaxHealth() * 0.7f) {
            this.heal(1.0f);
            healCooldown = 60; // 3 segundos entre curas
        }

        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);

        // 2. Coleta automatica de drops do chao ao redor (raio de 2.5 blocos)
        pickupNearbyItems();

        // 3. Execucao de acordo com o modo atual
        switch (this.mode) {
            case STAY -> handleStayMode(owner);
            case DEFEND -> handleDefendMode(owner);
            case WOOD -> handleWoodMode(owner);
            case MINE -> handleMineMode(owner);
            case FOLLOW, WORK, IDLE -> handleFollowMode(owner);
        }
    }

    private void pickupNearbyItems() {
        AABB vacuumBox = this.getBoundingBox().inflate(2.5, 1.5, 2.5);
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, vacuumBox);
        for (ItemEntity itemEntity : items) {
            if (itemEntity.isAlive() && !itemEntity.hasPickUpDelay()) {
                ItemStack stack = itemEntity.getItem();
                if (!stack.isEmpty()) {
                    int originalCount = stack.getCount();
                    if (this.getInventory().add(stack)) {
                        this.take(itemEntity, originalCount - stack.getCount());
                        if (stack.isEmpty()) {
                            itemEntity.discard();
                        }
                    }
                }
            }
        }
    }

    private void handleStayMode(ServerPlayer owner) {
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(delta.x * 0.5, delta.y, delta.z * 0.5);
        if (owner != null && this.distanceToSqr(owner) <= 64.0) {
            lookAtEntity(owner);
        }
    }

    private void handleDefendMode(ServerPlayer owner) {
        LivingEntity priorityTarget = null;
        if (owner != null && owner.getLastHurtByMob() != null && owner.getLastHurtByMob().isAlive()) {
            priorityTarget = owner.getLastHurtByMob();
        }

        if (priorityTarget == null) {
            AABB searchArea = this.getBoundingBox().inflate(12.0);
            List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, searchArea);
            if (!monsters.isEmpty()) {
                priorityTarget = monsters.get(0);
            }
        }

        if (priorityTarget != null && priorityTarget.isAlive()) {
            lookAtEntity(priorityTarget);
            double distSq = this.distanceToSqr(priorityTarget);
            if (distSq <= 6.0) {
                this.attack(priorityTarget);
                this.swing(InteractionHand.MAIN_HAND, true);
                this.resetAttackStrengthTicker();
            } else {
                Vec3 diff = priorityTarget.position().subtract(this.position());
                Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.26);
                double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
                this.setDeltaMovement(dir.x, jumpY, dir.z);
            }
            return;
        }

        handleFollowMode(owner);
    }

    private void handleWoodMode(ServerPlayer owner) {
        if (targetWorkPos == null || this.level().getBlockState(targetWorkPos).isAir()) {
            targetWorkPos = findNearestLog();
            workBreakTicks = 0;
            if (targetWorkPos == null) {
                setMode(CompanionMode.FOLLOW);
                speakToOwner("Toda a madeira proxima foi coletada (" + harvestedCount + " blocos). Voltando a te seguir!");
                harvestedCount = 0;
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);
        double distSq = this.distanceToSqr(targetCenter);

        if (distSq > 10.0) {
            Vec3 diff = targetCenter.subtract(this.position());
            Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.24);
            double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
            this.setDeltaMovement(dir.x, jumpY, dir.z);
        } else {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.3, delta.y, delta.z * 0.3);

            workBreakTicks++;
            if (workBreakTicks % 5 == 0) {
                this.swing(InteractionHand.MAIN_HAND, true);
            }

            if (workBreakTicks >= 20) {
                this.level().destroyBlock(targetWorkPos, true, this);
                harvestedCount++;
                targetWorkPos = null;
                workBreakTicks = 0;

                if (harvestedCount >= 16) {
                    setMode(CompanionMode.FOLLOW);
                    speakToOwner("Coletei 16 troncos de madeira e guardei na bolsa! Voltando a te seguir.");
                    harvestedCount = 0;
                }
            }
        }
    }

    private void handleMineMode(ServerPlayer owner) {
        if (targetWorkPos == null || this.level().getBlockState(targetWorkPos).isAir()) {
            targetWorkPos = findNearestOre();
            workBreakTicks = 0;
            if (targetWorkPos == null) {
                setMode(CompanionMode.FOLLOW);
                speakToOwner("Nao encontrei mais minerios expostos por perto. Voltando a te seguir!");
                harvestedCount = 0;
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);
        double distSq = this.distanceToSqr(targetCenter);

        if (distSq > 10.0) {
            Vec3 diff = targetCenter.subtract(this.position());
            Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.24);
            double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
            this.setDeltaMovement(dir.x, jumpY, dir.z);
        } else {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.3, delta.y, delta.z * 0.3);

            workBreakTicks++;
            if (workBreakTicks % 5 == 0) {
                this.swing(InteractionHand.MAIN_HAND, true);
            }

            if (workBreakTicks >= 25) {
                this.level().destroyBlock(targetWorkPos, true, this);
                harvestedCount++;
                targetWorkPos = null;
                workBreakTicks = 0;

                if (harvestedCount >= 16) {
                    setMode(CompanionMode.FOLLOW);
                    speakToOwner("Coletei minerios suficientes por agora! Voltando a te seguir.");
                    harvestedCount = 0;
                }
            }
        }
    }

    private void handleFollowMode(ServerPlayer owner) {
        if (owner != null && owner.level() == this.level()) {
            double distSq = this.distanceToSqr(owner);

            if (distSq > 1296.0) {
                this.teleportTo(owner.serverLevel(), owner.getX() + 1.0, owner.getY(), owner.getZ() + 1.0, owner.getYRot(), owner.getXRot());
                return;
            }

            if (distSq > 9.0) {
                lookAtEntity(owner);
                Vec3 diff = owner.position().subtract(this.position());
                Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.24);
                double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
                this.setDeltaMovement(dir.x, jumpY, dir.z);
            } else {
                Vec3 delta = this.getDeltaMovement();
                this.setDeltaMovement(delta.x * 0.5, delta.y, delta.z * 0.5);
                lookAtEntity(owner);
            }
        }
    }

    private BlockPos findNearestLog() {
        BlockPos currentPos = this.blockPosition();
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-12, -4, -12), currentPos.offset(12, 8, 12))) {
            BlockState state = this.level().getBlockState(candidate);
            if (!state.isAir() && (state.is(BlockTags.LOGS) || state.getBlock().getDescriptionId().contains("log"))) {
                double dist = candidate.distSqr(currentPos);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestPos = candidate.immutable();
                }
            }
        }
        return bestPos;
    }

    private BlockPos findNearestOre() {
        BlockPos currentPos = this.blockPosition();
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-10, -4, -10), currentPos.offset(10, 4, 10))) {
            BlockState state = this.level().getBlockState(candidate);
            if (!state.isAir()) {
                String id = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
                if (id.contains("ore") || id.contains("coal") || id.contains("iron") || id.contains("copper") || id.contains("gold") || id.contains("diamond")) {
                    double dist = candidate.distSqr(currentPos);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestPos = candidate.immutable();
                    }
                }
            }
        }
        return bestPos;
    }

    public int depositToNearbyChest() {
        BlockPos currentPos = this.blockPosition();
        Container targetChest = null;
        BlockPos chestPos = null;

        for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-8, -3, -8), currentPos.offset(8, 3, 8))) {
            BlockEntity be = this.level().getBlockEntity(candidate);
            if (be instanceof Container container) {
                targetChest = container;
                chestPos = candidate.immutable();
                break;
            }
        }

        if (targetChest == null || chestPos == null) {
            speakToOwner("Nao encontrei nenhum bau ou barril por perto (alcance de 8 blocos).");
            return 0;
        }

        lookAtPosition(new Vec3(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5));
        this.swing(InteractionHand.MAIN_HAND, true);

        int totalMoved = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                int count = stack.getCount();
                ItemStack remaining = addItemToContainer(targetChest, stack);
                int moved = count - remaining.getCount();
                totalMoved += moved;
                this.getInventory().setItem(i, remaining);
            }
        }

        targetChest.setChanged();
        speakToOwner("Guardei " + totalMoved + " itens no bau proximo!");
        return totalMoved;
    }

    private ItemStack addItemToContainer(Container container, ItemStack stack) {
        ItemStack itemstack = stack.copy();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.isEmpty()) {
                container.setItem(i, itemstack);
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(slotStack, itemstack)) {
                int max = Math.min(container.getMaxStackSize(), slotStack.getMaxStackSize());
                int space = max - slotStack.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, itemstack.getCount());
                    slotStack.grow(toAdd);
                    itemstack.shrink(toAdd);
                    if (itemstack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return itemstack;
    }

    public void openCompanionInventory(ServerPlayer owner) {
        if (owner == null) return;
        owner.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) ->
                        ChestMenu.threeRows(containerId, playerInventory, getMochilaContainer()),
                Component.literal("Mochila: " + this.getName().getString())
        ));
    }

    public Container getMochilaContainer() {
        return new Container() {
            @Override
            public int getContainerSize() {
                return 27;
            }

            @Override
            public boolean isEmpty() {
                for (int i = 9; i < 36; i++) {
                    if (!getInventory().getItem(i).isEmpty()) return false;
                }
                return true;
            }

            @Override
            public ItemStack getItem(int slot) {
                return getInventory().getItem(slot + 9);
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                return getInventory().removeItem(slot + 9, amount);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return getInventory().removeItemNoUpdate(slot + 9);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                getInventory().setItem(slot + 9, stack);
            }

            @Override
            public void setChanged() {
                getInventory().setChanged();
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return true;
            }

            @Override
            public void clearContent() {
                for (int i = 9; i < 36; i++) {
                    getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        };
    }

    public void lookAtEntity(net.minecraft.world.entity.Entity target) {
        if (target == null) return;
        lookAtPosition(target.getEyePosition());
    }

    public void lookAtPosition(Vec3 targetPos) {
        if (targetPos == null) return;
        Vec3 diff = targetPos.subtract(this.getEyePosition());
        double dXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float targetYaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(diff.y, dXZ) * (180.0 / Math.PI)));
        this.setYRot(targetYaw);
        this.setXRot(targetPitch);
        this.setYHeadRot(targetYaw);
    }

    public void speakToOwner(String message) {
        if (this.server == null) return;
        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("<" + this.getName().getString() + "> " + message));
        }
    }

    public void recallToOwner() {
        if (this.server == null) return;
        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);
        if (owner != null) {
            this.teleportTo(owner.serverLevel(), owner.getX() + 1.2, owner.getY(), owner.getZ() + 1.2, owner.getYRot(), owner.getXRot());
        }
    }

    public void dismiss() {
        if (this.server != null && this.server.getPlayerList() != null) {
            this.server.getPlayerList().broadcastSystemMessage(
                    Component.literal(this.getName().getString() + " saiu do jogo"),
                    false
            );
            this.server.getPlayerList().remove(this);
        }
    }

    public void dismissSilent() {
        if (this.server != null && this.server.getPlayerList() != null) {
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
