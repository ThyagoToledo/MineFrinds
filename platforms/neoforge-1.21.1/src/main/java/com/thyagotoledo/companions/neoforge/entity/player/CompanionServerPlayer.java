package com.thyagotoledo.companions.neoforge.entity.player;

import com.mojang.authlib.GameProfile;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.neoforge.service.EquipmentPolicy;
import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Entidade ServerPlayer fake para o companheiro (estilo Carpet Mod).
 * Atua oficialmente como jogador com modelo 3D, inventario completo com armaduras,
 * inteligencia de combate, agricultura, felling de arvores em cascata e auto-cura.
 */
public class CompanionServerPlayer extends ServerPlayer {

    private final UUID ownerUuid;
    private final NeoForgeCompanionEntity dataEntity;
    private NeoForgePermissionService permissionService;
    private CompanionMode mode = CompanionMode.FOLLOW;
    private int perceptionTicks;
    private int stairSteps;
    private int stairApproachTicks;
    private BlockPos stairDestination;
    private Direction miningDirection;
    private BlockState blockedOre;
    private String miningPriority = "all";
    private BlockPos targetVeinPos = null;
    private BlockPos miningObjectivePos = null;
    private boolean farmTillEnabled = false;
    private long nextGearCraftTick;
    private BlockPos remoteBreakTarget;
    private float remoteBreakProgress;
    private long nextRemoteUseTick;
    private LivingEntity combatTarget;
    private int bowChargeTicks = 0;
    private boolean autonomous;
    private long nextAutonomyTick;
    private int healCooldown = 0;
    private long lastRecallGameTime = 0L;

    // Estado para tarefas ativas de trabalho (Madeira / Mineracao / Agricultura)
    private BlockPos targetWorkPos = null;
    private int workBreakTicks = 0;
    private float miningProgress;
    private int harvestedCount = 0;
    private long nextWorkScanTick = 0L;
    private BlockPos observedWorkTarget;
    private Vec3 lastWorkPosition;
    private int stalledWorkTicks;
    private int consecutiveStalls = 0;
    private java.util.List<BlockPos> localPath = java.util.Collections.emptyList();
    private long nextPathTick;
    private Vec3 lastPathTarget;
    private Vec3 idleLookTarget;
    private long nextIdleLookTick;
    private AABB pendingDropCollectionBox = null;
    private long pendingDropCollectionUntilTick = 0L;

    // Meta pendente de crafting autonomo
    private Item pendingCraftItem;
    private int pendingCraftCount;
    private long pendingCraftUntil;
    private long nextCraftRetry;

    public CompanionServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile,
                                ClientInformation clientInfo, UUID ownerUuid,
                                NeoForgeCompanionEntity dataEntity) {
        super(server, level, profile, clientInfo);
        this.ownerUuid = ownerUuid;
        this.dataEntity = dataEntity;
        if (dataEntity != null) {
            this.mode = dataEntity.getMode();
            this.permissionService = dataEntity.getPermissionService();
        }
        if (this.permissionService == null) {
            this.permissionService = new NeoForgePermissionService();
        }

        // Modo Sobrevivencia oficial e subida fluida de blocos (passo de 1 bloco)
        this.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        if (this.getAttribute(Attributes.STEP_HEIGHT) != null) {
            this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.06);
        }
    }

    public NeoForgePermissionService getPermissionService() {
        return permissionService;
    }

    public void setPermissionService(NeoForgePermissionService permissionService) {
        this.permissionService = permissionService != null ? permissionService : new NeoForgePermissionService();
    }

    private boolean canUsePlayerBreak(BlockPos pos) {
        if (pos == null || !level().hasChunkAt(pos)) return false;
        return (permissionService == null || permissionService.canBreakBlockAt(ownerUuid,
                level().dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ()))
                && serverLevel().mayInteract(this, pos);
    }

    public boolean checkCanBreakBlock(BlockPos pos) {
        if (pos == null || this.level() == null) return false;
        String dimension = this.level().dimension().location().toString();
        if (this.permissionService != null && !this.permissionService.canBreakBlockAt(this.ownerUuid, dimension, pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }
        if (this.serverLevel() != null && !this.serverLevel().mayInteract(this, pos)) {
            return false;
        }
        // Publicar o evento antes da mutação permite que FTB Chunks, OPAC e
        // outros mods de proteção neguem a ação usando o mesmo contrato de
        // quebra de um jogador real.
        if (this.serverLevel() != null) {
            BlockState state = this.level().getBlockState(pos);
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(this.level(), pos, state, this);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return false;
            }
        }
        return true;
    }

    public boolean checkCanInteractBlock(BlockPos pos) {
        if (pos == null || this.level() == null) return false;
        String dimension = this.level().dimension().location().toString();
        if (this.permissionService != null && !this.permissionService.canInteractAt(this.ownerUuid, dimension, pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }
        if (this.serverLevel() != null && !this.serverLevel().mayInteract(this, pos)) {
            return false;
        }
        if (this.serverLevel() != null) {
            BlockSnapshot snapshot = BlockSnapshot.create(this.level().dimension(), this.level(), pos);
            BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(
                    snapshot, this.level().getBlockState(pos), this);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource damageSource) {
        saveEquipmentCheckpoint();
        super.die(damageSource);
        speakToOwner("Fui derrotado em combate! Use /companion spawn para me convocar novamente.");
    }

    @Override
    protected void dropEquipment() { /* NPC-only keep inventory; player gamerules are unchanged. */ }

    @Override
    protected void dropExperience(net.minecraft.world.entity.Entity killer) { }

    public void saveEquipmentCheckpoint() {
        if (server == null) return;
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.put("items", getInventory().save(new net.minecraft.nbt.ListTag()));
        tag.putInt("selected", getInventory().selected);
        tag.putInt("level", experienceLevel);
        tag.putInt("total", totalExperience);
        tag.putFloat("progress", experienceProgress);
        com.thyagotoledo.companions.neoforge.entity.CompanionSavedData.get(server.overworld()).saveEquipment(ownerUuid, tag);
    }

    public void restoreEquipmentCheckpoint() {
        var tag = com.thyagotoledo.companions.neoforge.entity.CompanionSavedData.get(server.overworld()).getEquipment(ownerUuid);
        if (tag != null) {
            getInventory().clearContent();
            getInventory().load(tag.getList("items", 10));
            getInventory().selected = Math.max(0, Math.min(8, tag.getInt("selected")));
            experienceLevel = tag.getInt("level");
            totalExperience = tag.getInt("total");
            experienceProgress = tag.getFloat("progress");
        }
        gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        if (getHealth() <= 0) setHealth(getMaxHealth());
    }

    @Override
    public void tick() {
        super.doTick();
        if (isAlive()) tickAI();
    }

    /**
     * Executa a logica motora e tomada de decisoes da IA a cada tick do servidor.
     */
    private void tickAI() {
        if (this.server == null || this.level().isClientSide) {
            return;
        }

        if (this.tickCount % 100 == 0) saveEquipmentCheckpoint();

        if (com.thyagotoledo.companions.neoforge.service.RemoteViewService.tickControlled(this)) {
            if (tickCount % 5 == 0) { pickupNearbyItems(); collectPendingDrops(); }
            return;
        }

        // 1. Inteligencia periodica a cada 20 ticks (1 segundo)
        if (this.tickCount % 20 == 0) {
            tryAutoEat();
            smartEquipArmor();
            smartEquipTools();
            if (dataEntity != null) {
                java.util.List<com.thyagotoledo.companions.core.model.ItemSlot> slots = new java.util.ArrayList<>();
                for (int slot = 0; slot < 36; slot++) {
                    ItemStack stack = getInventory().getItem(slot);
                    if (!stack.isEmpty()) slots.add(new com.thyagotoledo.companions.core.model.ItemSlot(
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount()));
                }
                dataEntity.setInventory(new com.thyagotoledo.companions.core.model.InventorySnapshot(slots, 36));
            }
        }

        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);

        if (owner == null || owner.level() != this.level()) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            return;
        }
        if (this.mode == CompanionMode.WOOD || this.mode == CompanionMode.MINE || this.mode == CompanionMode.FARM) {
            boolean capacity = false;
            for (int slot = 0; slot < 36; slot++) {
                ItemStack stack = getInventory().getItem(slot);
                if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) { capacity = true; break; }
            }
            if (!capacity) {
                if (tickCount % 200 == 0) {
                    speakToOwner("Mochila cheia! Por favor recolha meus itens ou defina um bau com /companion chest.");
                }
                return;
            }
            if (targetWorkPos != null && targetWorkPos.equals(observedWorkTarget)
                    && lastWorkPosition != null && position().distanceToSqr(lastWorkPosition) < 0.01 && workBreakTicks == 0) {
                if (++stalledWorkTicks >= 80) {
                    targetWorkPos = null;
                    stalledWorkTicks = 0;
                    nextWorkScanTick = this.tickCount + 15;
                    consecutiveStalls = 0;
                }
            } else {
                stalledWorkTicks = 0;
                if (targetWorkPos == null || !targetWorkPos.equals(observedWorkTarget)) {
                    consecutiveStalls = 0;
                }
            }
            observedWorkTarget = targetWorkPos;
            lastWorkPosition = position();
        }

        // 2. Coleta automatica de drops do chao ao redor (raio de 3 blocos) e
        // das areas onde uma tarefa acabou de quebrar blocos. A janela extra
        // cobre o pickup delay normal dos ItemEntity e drops espalhados por
        // arvores, minerios e safras.
        if (this.tickCount % 5 == 0) {
            pickupNearbyItems();
            collectPendingDrops();
            checkPendingCraftFulfillment(owner);
        }

        if (handleCombatPriority(owner)) return;
        if (isUsingItem()) { setDeltaMovement(0, getDeltaMovement().y, 0); return; }

        // 3. Execucao de acordo com o modo atual
        switch (this.mode) {
            case STAY -> handleStayMode(owner);
            case DEFEND -> handleDefendMode(owner);
            case WOOD -> handleWoodMode(owner);
            case MINE -> handleMineMode(owner);
            case FARM -> handleFarmMode(owner);
            case WORK -> handleAutonomousMode(owner);
            case FOLLOW, IDLE -> handleFollowMode(owner);
        }
    }

    /**
     * Auto-alimentacao: consome alimentos da bolsa se a vida estiver abaixo de 18 coracoes.
     */
    private void tryAutoEat() {
        if (!this.getFoodData().needsFood() || isUsingItem() || combatTarget != null) {
            return;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                FoodProperties food = stack.get(DataComponents.FOOD);
                if (food != null) { swapToHand(i); startUsingItem(InteractionHand.MAIN_HAND); }
                break;
            }
        }
    }

    /**
     * Auto-equip de armadura: equipa automaticamente as pecas com maior protecao encontradas na bolsa.
     */
    private void smartEquipArmor() {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack current = getItemBySlot(slot);
            if (net.minecraft.world.item.enchantment.EnchantmentHelper.has(current, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) continue;
            double best = EquipmentPolicy.armor(current, slot);
            int chosen = -1;
            for (int i = 0; i < 36; i++) {
                double score = EquipmentPolicy.armor(getInventory().getItem(i), slot);
                if (score > best) { best = score; chosen = i; }
            }
            if (chosen >= 0) {
                ItemStack replacement = getInventory().getItem(chosen);
                setItemSlot(slot, replacement);
                getInventory().setItem(chosen, current);
            }
        }
    }

    private void smartEquipTools() {
        if (combatTarget != null || isUsingItem()) return;
        switch (mode) {
            case WOOD -> equipForBlock(Blocks.OAK_LOG.defaultBlockState());
            case MINE -> equipForBlock(targetWorkPos == null ? Blocks.STONE.defaultBlockState() : level().getBlockState(targetWorkPos));
            case FARM -> {
                if (targetWorkPos != null && isTillableDirt(level().getBlockState(targetWorkPos))) {
                    equipBestHoe();
                }
            }
            case DEFEND -> equipBestWeapon();
            default -> { }
        }
    }

    private void equipBestTool(Class<? extends Item> toolClass) {
        if (toolClass == HoeItem.class) {
            equipBestHoe();
        } else {
            equipForBlock(Blocks.STONE.defaultBlockState());
        }
    }

    private void equipBestHoe() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof HoeItem) {
                swapToHand(i);
                return;
            }
        }
    }

    private boolean equipForBlock(BlockState state) {
        int chosen = getInventory().selected;
        double best = EquipmentPolicy.tool(getMainHandItem(), state);
        for (int i = 0; i < 36; i++) {
            double score = EquipmentPolicy.tool(getInventory().getItem(i), state);
            if (score > best) { best = score; chosen = i; }
        }
        swapToHand(chosen);
        return best >= 0;
    }

    private boolean hasToolFor(BlockState state) {
        if (!state.requiresCorrectToolForDrops()) return true;
        for (int i = 0; i < 36; i++) if (getInventory().getItem(i).isCorrectToolForDrops(state)) return true;
        return false;
    }

    private void swapToHand(int slot) {
        int selected = getInventory().selected;
        if (slot == selected) return;
        ItemStack held = getInventory().getItem(selected);
        getInventory().setItem(selected, getInventory().getItem(slot));
        getInventory().setItem(slot, held);
    }

    private void equipBestWeapon() {
        double best = EquipmentPolicy.weapon(getMainHandItem());
        int chosen = getInventory().selected;
        for (int i = 0; i < 36; i++) {
            double score = EquipmentPolicy.weapon(getInventory().getItem(i));
            if (score > best) { best = score; chosen = i; }
        }
        swapToHand(chosen);
    }

    private void pickupNearbyItems() {
        pickupItemsInArea(this.getBoundingBox().inflate(3.0, 1.5, 3.0));
    }

    /** Mantem a coleta ativa por alguns segundos depois de uma quebra. */
    private void requestDropCollection(BlockPos center) {
        if (center == null) return;
        AABB area = new AABB(center).inflate(3.0, 2.0, 3.0);
        if (pendingDropCollectionBox == null || pendingDropCollectionBox.getCenter().distanceToSqr(area.getCenter()) > 1024) {
            pendingDropCollectionBox = area;
        } else {
            pendingDropCollectionBox = union(pendingDropCollectionBox, area);
        }
        pendingDropCollectionUntilTick = Math.max(pendingDropCollectionUntilTick, this.tickCount + 100L);
    }

    private void collectPendingDrops() {
        if (pendingDropCollectionBox == null) return;
        if (this.tickCount > pendingDropCollectionUntilTick) {
            pendingDropCollectionBox = null;
            pendingDropCollectionUntilTick = 0L;
            return;
        }
        pickupItemsInArea(pendingDropCollectionBox);
    }

    private void pickupItemsInArea(AABB collectionBox) {
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, collectionBox);
        for (ItemEntity itemEntity : items) {
            if (itemEntity.isAlive() && !itemEntity.hasPickUpDelay()) {
                ItemStack stack = itemEntity.getItem();
                if (!stack.isEmpty()) {
                    int originalCount = stack.getCount();
                    // Inventory.add pode mover apenas parte da pilha quando
                    // o inventario esta quase cheio. Sempre contabilize o
                    // delta, mesmo quando o metodo retorna false.
                    this.getInventory().add(stack);
                    int moved = originalCount - stack.getCount();
                    if (moved > 0) {
                        this.take(itemEntity, moved);
                    }
                    if (stack.isEmpty()) {
                        itemEntity.discard();
                    }
                }
            }
        }
    }

    private static AABB union(AABB first, AABB second) {
        return new AABB(
                Math.min(first.minX, second.minX),
                Math.min(first.minY, second.minY),
                Math.min(first.minZ, second.minZ),
                Math.max(first.maxX, second.maxX),
                Math.max(first.maxY, second.maxY),
                Math.max(first.maxZ, second.maxZ)
        );
    }

    public void applyRemoteInput(float forward, float sideways, float yaw, float pitch,
                                 boolean jump, boolean attackHeld, boolean useHeld, int slot) {
        gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        getInventory().selected = Math.max(0, Math.min(8, slot));
        setYRot(yaw); setXRot(pitch); setYHeadRot(yaw);
        double angle = Math.toRadians(yaw);
        Vec3 direction = new Vec3(sideways * Math.cos(angle) - forward * Math.sin(angle), 0,
                forward * Math.cos(angle) + sideways * Math.sin(angle));
        if (direction.lengthSqr() > 1) direction = direction.normalize();
        setDeltaMovement(direction.x * 0.215, jump && onGround() ? 0.42 : getDeltaMovement().y, direction.z * 0.215);
        Vec3 eye = getEyePosition();
        Vec3 end = eye.add(getLookAngle().scale(4.5));
        var hit = level().clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (attackHeld) {
            LivingEntity victim = null;
            double nearest = Math.min(9, eye.distanceToSqr(hit.getLocation()));
            for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().expandTowards(getLookAngle().scale(3)).inflate(1))) {
                if (entity == this || entity instanceof Player || !entity.isAlive() || isAlliedTo(entity)) continue;
                var intersection = entity.getBoundingBox().inflate(0.1).clip(eye, end);
                if (intersection.isPresent() && eye.distanceToSqr(intersection.get()) < nearest) {
                    nearest = eye.distanceToSqr(intersection.get()); victim = entity;
                }
            }
            if (victim != null) {
                remoteBreakTarget = null; remoteBreakProgress = 0;
                if (getAttackStrengthScale(0.5f) >= 1) { attack(victim); swing(InteractionHand.MAIN_HAND, true); resetAttackStrengthTicker(); }
            } else if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                if (!pos.equals(remoteBreakTarget)) { remoteBreakTarget = pos; remoteBreakProgress = 0; }
                BlockState state = level().getBlockState(pos);
                remoteBreakProgress += state.getDestroyProgress(this, level(), pos);
                if (tickCount % 4 == 0) swing(InteractionHand.MAIN_HAND, true);
                if (remoteBreakProgress >= 1 && canUsePlayerBreak(pos)) {
                    if (gameMode.destroyBlock(pos)) requestDropCollection(pos);
                    remoteBreakTarget = null; remoteBreakProgress = 0;
                }
            }
        } else { remoteBreakTarget = null; remoteBreakProgress = 0; }
        if (useHeld && tickCount >= nextRemoteUseTick) {
            nextRemoteUseTick = tickCount + 5;
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos interaction = hit.getBlockPos();
                if (permissionService != null && !permissionService.canInteractAt(ownerUuid,
                        level().dimension().location().toString(), interaction.getX(), interaction.getY(), interaction.getZ())) return;
                gameMode.useItemOn(this, level(), getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            } else gameMode.useItem(this, level(), getMainHandItem(), InteractionHand.MAIN_HAND);
        }
        if (!useHeld && isUsingItem()) releaseUsingItem();
    }

    private boolean canReachWork(BlockPos target) {
        if (target == null) return false;
        Vec3 end = target.getCenter();
        double distSq = this.getEyePosition().distanceToSqr(end);
        if (distSq > 20.25) return false;
        if (distSq <= 9.0) return true;
        BlockState state = this.level().getBlockState(target);
        if (state.getCollisionShape(this.level(), target).isEmpty()) {
            return true;
        }
        var hit = this.level().clip(new net.minecraft.world.level.ClipContext(this.getEyePosition(), end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(target);
    }

    private void finishWork() {
        if (autonomous) {
            setMode(CompanionMode.WORK);
        }
    }

    private void handleAutonomousMode(ServerPlayer owner) {
        handleFollowMode(owner);
        if (this.tickCount < nextAutonomyTick || owner == null || distanceToSqr(owner) > 144) return;
        nextAutonomyTick = this.tickCount + 100;
        if (tickCount % 400 < 100 && tryCraftUpgrade(Blocks.STONE.defaultBlockState(), true)) return;
        // Bounded starter loop: no exploration, tunnelling or repeated model calls.
        boolean pickaxe = false;
        for (int i = 0; i < 36; i++) if (getInventory().getItem(i).isCorrectToolForDrops(Blocks.STONE.defaultBlockState())) pickaxe = true;
        if (!pickaxe) {
            prepareWorkbench();
            if (com.thyagotoledo.companions.neoforge.service.RecipeCraftingService.craft(this, Items.WOODEN_PICKAXE, 1)) {
                smartEquipTools();
                speakToOwner("Preparei uma picareta / I crafted a pickaxe.");
                return;
            }
            if (countLogsInInventory() < 4) {
                this.mode = CompanionMode.WOOD;
                if (dataEntity != null) dataEntity.setMode(mode);
            } else {
                speakToOwner("Preciso de uma bancada proxima e ingredientes para a picareta / I need a nearby crafting table and pickaxe ingredients.");
                nextAutonomyTick = this.tickCount + 600;
            }
        } else if (countItemInInventory(Items.COBBLESTONE) < 16) {
            tryCraftUpgrade(Blocks.IRON_ORE.defaultBlockState(), false);
            equipBestTool(PickaxeItem.class);
            this.mode = CompanionMode.MINE;
            perceptionTicks = 0; stairSteps = 0; stairApproachTicks = 0; stairDestination = null; miningDirection = null;
            if (dataEntity != null) dataEntity.setMode(mode);
        }
    }

    private void prepareWorkbench() {
        for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-3, -2, -3), blockPosition().offset(3, 2, 3))) {
            if (level().hasChunkAt(pos) && level().getBlockState(pos).is(Blocks.CRAFTING_TABLE)) return;
        }
        if (countItemInInventory(Items.CRAFTING_TABLE) == 0
                && !com.thyagotoledo.companions.neoforge.service.RecipeCraftingService.craft(this, Items.CRAFTING_TABLE, 1)) return;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos pos = blockPosition().relative(direction, 2);
            if (!com.thyagotoledo.companions.neoforge.service.LocalNavigation.safe(this, pos) || !checkCanInteractBlock(pos)) continue;
            if (level().setBlockAndUpdate(pos, Blocks.CRAFTING_TABLE.defaultBlockState())) {
                for (int slot = 0; slot < 36; slot++) {
                    ItemStack stack = getInventory().getItem(slot);
                    if (stack.is(Items.CRAFTING_TABLE)) { stack.shrink(1); break; }
                }
                swing(InteractionHand.MAIN_HAND, true);
            }
            return;
        }
    }

    private void handleStayMode(ServerPlayer owner) {
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(delta.x * 0.5, delta.y, delta.z * 0.5);
        if (owner != null && this.distanceToSqr(owner) <= 64.0) {
            lookAround(owner);
        }
    }

    private void moveToward(Vec3 target, double speed) {
        Vec3 diff = target.subtract(this.position());
        double horizDistSq = diff.x * diff.x + diff.z * diff.z;

        if (horizDistSq < 0.16) {
            this.setDeltaMovement(this.getDeltaMovement().x * 0.5, this.getDeltaMovement().y, this.getDeltaMovement().z * 0.5);
            return;
        }

        if (this.tickCount >= nextPathTick || lastPathTarget == null || lastPathTarget.distanceToSqr(target) > 9.0) {
            nextPathTick = this.tickCount + 15;
            lastPathTarget = target;
            localPath = com.thyagotoledo.companions.neoforge.service.LocalNavigation.plan(this, target);
        }

        while (!localPath.isEmpty()) {
            Vec3 waypoint = Vec3.atBottomCenterOf(localPath.get(0));
            double wpDistSq = (this.getX() - waypoint.x) * (this.getX() - waypoint.x)
                            + (this.getZ() - waypoint.z) * (this.getZ() - waypoint.z);
            if (wpDistSq < 0.5 && Math.abs(this.getY() - waypoint.y) < 1.2) {
                localPath.remove(0);
            } else {
                break;
            }
        }

        Vec3 steerTarget = !localPath.isEmpty() ? Vec3.atBottomCenterOf(localPath.get(0)) : target;
        Vec3 step = steerTarget.subtract(this.position());
        Vec3 direction = new Vec3(step.x, 0, step.z);
        double len = direction.length();
        if (len > 1.0e-4) {
            direction = direction.scale(speed / len);
            float moveYaw = (float) (Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
            float yaw = Mth.approachDegrees(this.getYRot(), moveYaw, 35.0f);
            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.setYBodyRot(yaw);
        } else {
            direction = Vec3.ZERO;
        }

        boolean shouldJump = (this.onGround() || this.isInWater()) && (this.horizontalCollision || step.y > 0.45);
        double vertical = shouldJump ? 0.42 : this.getDeltaMovement().y;
        if (this.isInWater() && (target.y > this.getY() || shouldJump)) {
            vertical = 0.15;
        }

        this.setDeltaMovement(direction.x, vertical, direction.z);
    }

    private void lookAround(ServerPlayer owner) {
        if (idleLookTarget == null || this.tickCount >= nextIdleLookTick) {
            nextIdleLookTick = this.tickCount + 50 + this.random.nextInt(60);
            idleLookTarget = owner != null && this.random.nextInt(3) != 0
                    ? owner.getEyePosition()
                    : this.position().add(this.random.nextDouble() * 8 - 4,
                            1.0 + this.random.nextDouble() * 1.5, this.random.nextDouble() * 8 - 4);
        }
        lookAtPosition(idleLookTarget);
    }

    private boolean handleCombatPriority(ServerPlayer owner) {
        if (owner == null) return false;
        if (combatTarget != null && (!combatTarget.isAlive() || combatTarget.level() != level()
                || (combatTarget.distanceToSqr(owner) > 324 && combatTarget.distanceToSqr(this) > 324)
                || combatTarget.isAlliedTo(owner) || combatTarget.isAlliedTo(this))) {
            if (isUsingItem() && getMainHandItem().getItem() instanceof BowItem) {
                stopUsingItem();
            }
            combatTarget = null;
            bowChargeTicks = 0;
            nextPathTick = 0;
            smartEquipTools();
        }
        if (combatTarget == null && tickCount % 5 == 0) {
            double closest = Double.MAX_VALUE;
            AABB searchBox = owner.getBoundingBox().minmax(this.getBoundingBox()).inflate(16, 8, 16);
            for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, searchBox)) {
                boolean hostile = entity instanceof net.minecraft.world.entity.monster.Enemy
                        || entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER
                        || (entity instanceof net.minecraft.world.entity.Mob mob && (mob.getTarget() == owner || mob.getTarget() == this));
                if (!hostile || !entity.isAlive() || entity.isAlliedTo(owner) || entity.isAlliedTo(this)) continue;
                if (entity instanceof net.minecraft.world.entity.NeutralMob neutral
                        && !neutral.isAngryAt(owner) && !neutral.isAngryAt(this)) continue;

                double dMeSq = entity.distanceToSqr(this);
                double dOwnerSq = entity.distanceToSqr(owner);
                boolean closeThreat = dMeSq <= 4.0 || dOwnerSq <= 4.0; // Raio de seguranca de 2 blocos

                Vec3 toEntity = entity.getEyePosition().subtract(this.getEyePosition());
                double distToEnt = toEntity.length();
                boolean inFov = false;
                if (distToEnt > 1.0e-3) {
                    Vec3 dir = toEntity.scale(1.0 / distToEnt);
                    inFov = dir.dot(this.getLookAngle()) > 0.1; // Campo de visao frontal (~168 graus)
                }

                // Alvo valido: perigo imediato (<= 2 blocos) OU no campo de visao com visada desimpedida
                if (!closeThreat && (!inFov || !hasLineOfSight(entity))) continue;
                if (!closeThreat && (dMeSq > 324 && dOwnerSq > 324)) continue;

                double score = Math.min(dOwnerSq, dMeSq);
                if (score < closest) { closest = score; combatTarget = entity; }
            }
        }
        if (combatTarget == null) {
            if (isUsingItem() && getMainHandItem().getItem() instanceof BowItem) {
                stopUsingItem();
                bowChargeTicks = 0;
            }
            return false;
        }

        double distSq = distanceToSqr(combatTarget);
        int bowSlot = findBowInInventory();
        boolean hasAmmo = hasArrowsOrCreative();

        // Modo Arqueria: se tiver arco e flechas no inventario e o monstro estiver a distancia (> 4 blocos)
        if (bowSlot >= 0 && hasAmmo && distSq > 16.0) {
            if (getInventory().selected != bowSlot) {
                if (isUsingItem()) stopUsingItem();
                swapToHand(bowSlot);
            }

            Vec3 targetEye = combatTarget.getEyePosition();
            double horizDist = Math.sqrt(Math.pow(targetEye.x - getX(), 2) + Math.pow(targetEye.z - getZ(), 2));
            double pitchCompensation = horizDist * 0.05; // Ajuste balistico para gravidade do projétil
            lookAtPosition(targetEye.add(0, pitchCompensation, 0));

            if (!isUsingItem()) {
                startUsingItem(InteractionHand.MAIN_HAND);
                bowChargeTicks = 0;
            }
            bowChargeTicks++;
            if (bowChargeTicks >= 20) {
                releaseUsingItem();
                bowChargeTicks = 0;
            }
            setDeltaMovement(0, getDeltaMovement().y, 0);
            return true;
        }

        // Modo Corpo a Corpo: para curta distancia (<= 4 blocos) ou sem arco/flechas
        if (isUsingItem() && getMainHandItem().getItem() instanceof BowItem) {
            stopUsingItem();
            bowChargeTicks = 0;
        }
        equipBestWeapon();
        lookAtEntity(combatTarget);
        if (distSq <= 6.25 && hasLineOfSight(combatTarget)) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (getAttackStrengthScale(0.5f) >= 1.0f) {
                attack(combatTarget);
                swing(InteractionHand.MAIN_HAND, true);
                resetAttackStrengthTicker();
            }
        } else if (mode != CompanionMode.STAY) moveToward(combatTarget.position(), 0.26);
        return true;
    }

    private int findBowInInventory() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BowItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasArrowsOrCreative() {
        if (isCreative()) return true;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Items.ARROW)) {
                return true;
            }
        }
        return false;
    }

    private void handleDefendMode(ServerPlayer owner) {
        handleFollowMode(owner);
    }

    /**
     * Coleta de madeira com corte em cascata do tronco e desintegracao da copa (TreeCapitator).
     * Quebra folhas obstrutoras de visada e derruba a arvore inteira de baixo para cima com drops naturais.
     */
    private void handleWoodMode(ServerPlayer owner) {
        if (targetWorkPos == null || this.level().getBlockState(targetWorkPos).isAir()) {
            if (this.tickCount < nextWorkScanTick) return;
            nextWorkScanTick = this.tickCount + 20L;
            targetWorkPos = findNearestLog();
            workBreakTicks = 0;
            if (targetWorkPos == null) {
                checkPendingCraftFulfillment(owner);
                nextWorkScanTick = this.tickCount + 40L;
                if (this.tickCount % 200 == 0) {
                    speakToOwner("Procurando mais arvores por perto... / Looking for more trees nearby...");
                }
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);

        // Se houver folhas obstruindo a linha de visada ate o tronco alvo, quebra a folha impeditiva
        Vec3 eyePos = this.getEyePosition();
        var rayHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                eyePos, targetCenter,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this
        ));
        if (rayHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos hitPos = rayHit.getBlockPos();
            if (!hitPos.equals(targetWorkPos) && isLeafBlock(this.level().getBlockState(hitPos))) {
                if (canUsePlayerBreak(hitPos)) {
                    lookAtPosition(hitPos.getCenter());
                    this.swing(InteractionHand.MAIN_HAND, true);
                    this.level().destroyBlock(hitPos, true, this);
                    requestDropCollection(hitPos);
                }
            }
        }

        Vec3 diff = targetCenter.subtract(this.position());
        double dXZ = Math.hypot(diff.x, diff.z);
        double distEyeSq = this.getEyePosition().distanceToSqr(targetCenter);

        // Se estiver distante horizontalmente e fora do alcance dos olhos
        if (!canReachWork(targetWorkPos)) {
            moveToward(targetCenter, 0.24);
        } else {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.3, delta.y, delta.z * 0.3);

            workBreakTicks++;
            if (workBreakTicks % 4 == 0) {
                this.swing(InteractionHand.MAIN_HAND, true);
            }

            if (workBreakTicks >= 20) {
                // Valida protecao do bloco base
                if (!canUsePlayerBreak(targetWorkPos)) {
                    speakToOwner("Nao tenho permissao para quebrar madeira nesta area protegida!");
                    targetWorkPos = null;
                    workBreakTicks = 0;
                    return;
                }

                // Destroi o bloco base
                int maxY = targetWorkPos.getY();
                if (this.gameMode.destroyBlock(targetWorkPos)) {
                    requestDropCollection(targetWorkPos);
                    harvestedCount++;
                }

                // Cascata: derruba troncos conectados verticalmente para cima ate 24 blocos
                BlockPos currentAbove = targetWorkPos.above();
                for (int i = 0; i < 24; i++) {
                    BlockState stateAbove = this.level().getBlockState(currentAbove);
                    if (!stateAbove.isAir() && (stateAbove.is(BlockTags.LOGS) || stateAbove.getBlock().getDescriptionId().contains("log"))) {
                        if (!canUsePlayerBreak(currentAbove)) {
                            break;
                        }
                        if (this.gameMode.destroyBlock(currentAbove)) {
                            requestDropCollection(currentAbove);
                            harvestedCount++;
                            maxY = currentAbove.getY();
                        } else break;
                        currentAbove = currentAbove.above();
                    } else {
                        break;
                    }
                }

                // Remove as folhas ao redor da copa da arvore derrubada com drops naturais (TreeCapitator / Timber)
                clearTreeLeaves(targetWorkPos, maxY);

                targetWorkPos = null;
                workBreakTicks = 0;

                // Verifica se completou meta de crafting pendente
                if (checkPendingCraftFulfillment(owner)) {
                    return;
                }
            }
        }
    }

    private void clearTreeLeaves(BlockPos basePos, int maxY) {
        if (basePos == null) return;
        int minY = Math.max(this.level().getMinBuildHeight(), basePos.getY());
        int topY = Math.min(this.level().getMaxBuildHeight(), maxY + 4);
        int radius = 5;

        for (BlockPos pos : BlockPos.betweenClosed(
                basePos.offset(-radius, minY - basePos.getY(), -radius),
                basePos.offset(radius, topY - basePos.getY(), radius))) {
            if (!this.level().hasChunkAt(pos)) continue;
            BlockState state = this.level().getBlockState(pos);
            if (isLeafBlock(state) && canUsePlayerBreak(pos)) {
                this.level().destroyBlock(pos, true, this);
            }
        }
        requestDropCollection(new BlockPos(basePos.getX(), (minY + topY) / 2, basePos.getZ()));
    }

    private boolean isLeafBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        return state.is(BlockTags.LEAVES) || state.getBlock().getDescriptionId().contains("leaves");
    }

    private void handleMineMode(ServerPlayer owner) {
        if (miningDirection == null) miningDirection = Direction.fromYRot(getYRot());

        // 1. Vein Mining: se acabou de minerar um minerio, prioriza minerar blocos contiguos do mesmo veio
        if (targetVeinPos != null) {
            BlockPos adjacent = findAdjacentOre(targetVeinPos);
            if (adjacent != null) {
                targetWorkPos = adjacent;
            } else {
                targetVeinPos = null;
            }
        }

        // 2. Se nao temos alvo de mineracao ativo ou o alvo virou ar
        if (targetWorkPos == null || this.level().getBlockState(targetWorkPos).isAir()) {
            if (this.tickCount < nextWorkScanTick) return;
            nextWorkScanTick = this.tickCount + 10L;

            // Modo Caverna: se estiver em caverna aberta, procura minerios expostos na parede/chao/teto
            if (isInCave()) {
                BlockPos exposed = findExposedOreInCave();
                if (exposed != null) {
                    miningObjectivePos = exposed;
                    targetWorkPos = exposed;
                } else {
                    BlockPos explorePos = findCaveExplorationTarget();
                    if (explorePos != null) {
                        moveToward(Vec3.atBottomCenterOf(explorePos), 0.22);
                        nextWorkScanTick = this.tickCount + 20L;
                        return;
                    }
                }
            }

            // Se nao encontrou em caverna, escaneia subsolo por veios do filtro selecionado
            if (targetWorkPos == null) {
                if (miningObjectivePos == null || this.level().getBlockState(miningObjectivePos).isAir()
                        || !isTargetOre(this.level().getBlockState(miningObjectivePos))) {
                    miningObjectivePos = findSubterraneanOreTarget();
                }

                if (miningObjectivePos != null) {
                    targetWorkPos = planExcavationStep(miningObjectivePos);
                    if (targetWorkPos == null) return;
                } else {
                    targetWorkPos = staircaseTarget();
                }
            }

            if (blockedOre != null && tryCraftUpgrade(blockedOre, false)) {
                targetWorkPos = null;
                return;
            }

            workBreakTicks = 0;
            if (targetWorkPos == null) {
                checkPendingCraftFulfillment(owner);
                miningDirection = miningDirection.getClockWise();
                perceptionTicks = 0;
                nextWorkScanTick = this.tickCount + 20L;
                if (this.tickCount % 200 == 0) {
                    speakToOwner("Procurando novos veios de mineracao... / Searching for new mining veins...");
                }
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);

        if (!canReachWork(targetWorkPos)) {
            moveToward(targetCenter, 0.24);
        } else {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.3, delta.y, delta.z * 0.3);

            workBreakTicks++;
            if (workBreakTicks % 5 == 0) {
                this.swing(InteractionHand.MAIN_HAND, true);
            }

            BlockState miningState = this.level().getBlockState(targetWorkPos);
            equipForBlock(miningState);
            if (!this.hasCorrectToolForDrops(miningState)) {
                if (tickCount % 100 == 0) {
                    speakToOwner("Preciso de uma picareta adequada / I need a suitable pickaxe.");
                }
                targetWorkPos = null;
                workBreakTicks = 0;
                return;
            }

            if (workBreakTicks == 1) miningProgress = 0;
            miningProgress += miningState.getDestroyProgress(this, this.level(), targetWorkPos);

            if (miningProgress >= 1.0f) {
                if (!canUsePlayerBreak(targetWorkPos)) {
                    speakToOwner("Nao tenho permissao para minerar nesta area protegida!");
                    targetWorkPos = null;
                    workBreakTicks = 0;
                    return;
                }

                boolean wasOre = isTargetOre(miningState);
                BlockPos brokenPos = targetWorkPos.immutable();

                if (this.gameMode.destroyBlock(targetWorkPos)) {
                    requestDropCollection(brokenPos);
                    harvestedCount++;

                    if (wasOre) {
                        mineVeinCascade(brokenPos, miningState);
                        targetVeinPos = null;
                    }
                }

                targetWorkPos = null;
                workBreakTicks = 0;

                if (checkPendingCraftFulfillment(owner)) {
                    return;
                }
            }
        }
    }

    /**
     * Vein Miner: escavacao em cascata de veios de minerios contiguos (estilo FTB Ultimine / Ore Excavation).
     * Quebra todos os blocos conectados do mesmo tipo de minerio com drops normais e consumo de durabilidade.
     */
    private void mineVeinCascade(BlockPos originPos, BlockState originState) {
        if (originPos == null || originState == null) return;

        java.util.Queue<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();

        queue.add(originPos);
        visited.add(originPos);

        int minedCount = 0;
        int maxVeinBlocks = 48;

        while (!queue.isEmpty() && minedCount < maxVeinBlocks) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);

                        if (neighbor.equals(blockPosition().below())) continue;
                        if (!visited.add(neighbor)) continue;

                        if (!level().hasChunkAt(neighbor)) continue;

                        BlockState state = level().getBlockState(neighbor);
                        if (state.isAir()) continue;

                        if (isMatchingVeinOre(originState, state)) {
                            boolean nearLava = false;
                            for (Direction dir : Direction.values()) {
                                if (level().getBlockState(neighbor.relative(dir)).is(Blocks.LAVA)) {
                                    nearLava = true;
                                    break;
                                }
                            }
                            if (nearLava) continue;

                            if (!hasCorrectToolForDrops(state)) {
                                equipForBlock(state);
                                if (!hasCorrectToolForDrops(state)) break;
                            }

                            if (!canUsePlayerBreak(neighbor)) continue;

                            if (this.gameMode.destroyBlock(neighbor)) {
                                requestDropCollection(neighbor);
                                harvestedCount++;
                                minedCount++;
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (minedCount > 0) {
            requestDropCollection(originPos);
            this.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private boolean isMatchingVeinOre(BlockState originState, BlockState candidateState) {
        if (originState == null || candidateState == null) return false;
        if (candidateState.is(originState.getBlock())) return true;

        String originDesc = originState.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
        String candidateDesc = candidateState.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);

        for (String oreKeyword : new String[]{"diamond", "iron", "coal", "gold", "redstone", "lapis", "copper", "emerald", "debris", "nether_gold", "quartz"}) {
            if (originDesc.contains(oreKeyword) && candidateDesc.contains(oreKeyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInCave() {
        BlockPos pos = blockPosition();
        if (level().canSeeSky(pos)) return false;
        int skyLight = level().getBrightness(LightLayer.SKY, pos);
        if (skyLight >= 4) return false;

        int airCount = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 2, 3))) {
            if (level().getBlockState(p).isAir()) {
                airCount++;
                if (airCount >= 14) return true;
            }
        }
        return false;
    }

    private boolean isTargetOre(BlockState state) {
        if (state == null || state.isAir()) return false;
        boolean isOre = state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
        if (!isOre) {
            String desc = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
            if (desc.contains("ore") || desc.contains("debris")) isOre = true;
        }
        if (!isOre) return false;
        return matchesOreFilter(state, this.miningPriority);
    }

    private boolean matchesOreFilter(BlockState state, String filter) {
        if (filter == null || filter.equalsIgnoreCase("all") || filter.equalsIgnoreCase("qualquer")) {
            return true;
        }
        String desc = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
        String name = filter.toLowerCase(Locale.ROOT);
        return switch (name) {
            case "diamante", "diamond" -> desc.contains("diamond");
            case "ferro", "iron" -> desc.contains("iron");
            case "carvao", "coal" -> desc.contains("coal");
            case "ouro", "gold" -> desc.contains("gold");
            case "redstone" -> desc.contains("redstone");
            case "lapis" -> desc.contains("lapis");
            case "netherite", "debris" -> desc.contains("debris") || desc.contains("netherite");
            case "cobre", "copper" -> desc.contains("copper");
            default -> desc.contains(name);
        };
    }

    private double getOreWeight(BlockState state) {
        String desc = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
        if (desc.contains("diamond")) return 100.0;
        if (desc.contains("debris") || desc.contains("netherite")) return 95.0;
        if (desc.contains("gold")) return 80.0;
        if (desc.contains("iron")) return 70.0;
        if (desc.contains("redstone")) return 60.0;
        if (desc.contains("lapis")) return 50.0;
        if (desc.contains("copper")) return 40.0;
        if (desc.contains("coal")) return 30.0;
        return 20.0;
    }

    private BlockPos findExposedOreInCave() {
        BlockPos current = blockPosition();
        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;
        blockedOre = null;

        for (BlockPos candidate : BlockPos.betweenClosed(current.offset(-16, -6, -16), current.offset(16, 6, 16))) {
            if (!level().hasChunkAt(candidate) || candidate.equals(current.below())) continue;
            BlockState state = level().getBlockState(candidate);
            if (!isTargetOre(state)) continue;
            if (state.getDestroySpeed(level(), candidate) < 0) continue;

            boolean exposed = false;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = candidate.relative(dir);
                if (level().getBlockState(neighbor).isAir()) {
                    exposed = true;
                    break;
                }
            }
            if (!exposed) continue;

            if (!hasToolFor(state)) {
                blockedOre = state;
                continue;
            }

            double distSq = candidate.distSqr(current);
            double score = getOreWeight(state) * 20.0 - distSq;
            if (score > bestScore) {
                bestScore = score;
                best = candidate.immutable();
            }
        }
        return best;
    }

    private BlockPos findCaveExplorationTarget() {
        BlockPos current = blockPosition();
        BlockPos best = null;
        double bestDist = 0;

        for (BlockPos candidate : BlockPos.betweenClosed(current.offset(-8, -2, -8), current.offset(8, 2, 8))) {
            if (level().getBlockState(candidate).isAir()
                    && level().getBlockState(candidate.above()).isAir()
                    && isSafeDigFloor(candidate.below())) {
                double dist = candidate.distSqr(current);
                if (dist > 16.0 && dist > bestDist) {
                    bestDist = dist;
                    best = candidate.immutable();
                }
            }
        }
        return best;
    }

    private BlockPos findSubterraneanOreTarget() {
        BlockPos current = blockPosition();
        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;
        blockedOre = null;

        for (BlockPos candidate : BlockPos.betweenClosed(current.offset(-18, -32, -18), current.offset(18, 12, 18))) {
            if (!level().hasChunkAt(candidate) || candidate.equals(current.below())) continue;
            BlockState state = level().getBlockState(candidate);
            if (!isTargetOre(state)) continue;
            if (state.getDestroySpeed(level(), candidate) < 0) continue;

            if (!hasToolFor(state)) {
                blockedOre = state;
                continue;
            }

            double distSq = candidate.distSqr(current);
            double score = getOreWeight(state) * 25.0 - distSq;
            if (score > bestScore) {
                bestScore = score;
                best = candidate.immutable();
            }
        }
        return best;
    }

    private BlockPos findAdjacentOre(BlockPos origin) {
        if (origin == null) return null;
        BlockState originState = level().getBlockState(origin);
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            if (neighbor.equals(blockPosition().below())) continue;
            if (!level().hasChunkAt(neighbor)) continue;
            BlockState neighborState = level().getBlockState(neighbor);
            if (!neighborState.isAir() && (neighborState.is(originState.getBlock()) || isTargetOre(neighborState))) {
                if (hasToolFor(neighborState) && canUsePlayerBreak(neighbor)) {
                    return neighbor.immutable();
                }
            }
        }
        return null;
    }

    private boolean isSafeDigFloor(BlockPos floor) {
        if (!level().hasChunkAt(floor)) return false;
        BlockState state = level().getBlockState(floor);
        if (state.isAir() || !state.isSolidRender(level(), floor)) return false;
        if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.LAVA) || state.is(Blocks.FIRE)) return false;
        if (!level().getFluidState(floor).isEmpty()) return false;
        return true;
    }

    private boolean isDangerousBlock(BlockPos pos) {
        if (!level().hasChunkAt(pos)) return true;
        if (!level().getFluidState(pos).isEmpty()) return true;
        BlockState state = level().getBlockState(pos);
        return state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.FIRE) || state.is(Blocks.LAVA);
    }

    private BlockPos planExcavationStep(BlockPos objective) {
        if (objective == null) return null;

        if (canReachWork(objective)) {
            return objective;
        }

        BlockPos current = blockPosition();
        int dx = objective.getX() - current.getX();
        int dy = objective.getY() - current.getY();
        int dz = objective.getZ() - current.getZ();

        Direction primaryDir;
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
            primaryDir = dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (dz != 0) {
            primaryDir = dz > 0 ? Direction.SOUTH : Direction.NORTH;
        } else {
            primaryDir = miningDirection != null ? miningDirection : Direction.fromYRot(getYRot());
        }
        miningDirection = primaryDir;

        // Caso 1: Minerio esta significativamente abaixo (dy <= -2) -> Escada Descendente 1x2
        if (dy <= -2) {
            BlockPos stepFloor = current.relative(primaryDir).below();
            if (isSafeDigFloor(stepFloor.below())) {
                for (BlockPos p : new BlockPos[]{stepFloor.above(2), stepFloor.above(), stepFloor}) {
                    BlockState s = level().getBlockState(p);
                    if (!s.isAir() && s.getDestroySpeed(level(), p) >= 0 && hasToolFor(s) && !isDangerousBlock(p)) {
                        lookAtPosition(p.getCenter());
                        return p;
                    }
                }
                moveToward(Vec3.atBottomCenterOf(stepFloor), 0.20);
                return null;
            } else {
                miningDirection = primaryDir.getClockWise();
                return staircaseTarget();
            }
        }

        // Caso 2: Minerio esta significativamente acima (dy >= 2) -> Escada Ascendente 1x2
        if (dy >= 2) {
            BlockPos stepFloor = current.relative(primaryDir).above();
            for (BlockPos p : new BlockPos[]{stepFloor.above(2), stepFloor.above(), stepFloor}) {
                BlockState s = level().getBlockState(p);
                if (!s.isAir() && s.getDestroySpeed(level(), p) >= 0 && hasToolFor(s) && !isDangerousBlock(p)) {
                    lookAtPosition(p.getCenter());
                    return p;
                }
            }
            moveToward(Vec3.atBottomCenterOf(stepFloor), 0.20);
            return null;
        }

        // Caso 3: Minerio esta na mesma altura (-1 <= dy <= 1) -> Tunel Reto 1x2
        BlockPos tunnelStep = current.relative(primaryDir);
        if (isSafeDigFloor(tunnelStep.below())) {
            for (BlockPos p : new BlockPos[]{tunnelStep.above(), tunnelStep}) {
                BlockState s = level().getBlockState(p);
                if (!s.isAir() && s.getDestroySpeed(level(), p) >= 0 && hasToolFor(s) && !isDangerousBlock(p)) {
                    lookAtPosition(p.getCenter());
                    return p;
                }
            }
            moveToward(Vec3.atBottomCenterOf(tunnelStep), 0.20);
            return null;
        } else {
            miningDirection = primaryDir.getClockWise();
            return staircaseTarget();
        }
    }

    /**
     * Modo Agricultura (FARM): colhe safras maduras e replanta sementes em blocos de terra arada.
     */
    private void handleFarmMode(ServerPlayer owner) {
        if (targetWorkPos == null) {
            if (this.tickCount < nextWorkScanTick) return;
            nextWorkScanTick = this.tickCount + 20L;
            targetWorkPos = findFarmTarget();
            workBreakTicks = 0;
            if (targetWorkPos == null) {
                nextWorkScanTick = this.tickCount + 40L;
                if (this.tickCount % 200 == 0) {
                    speakToOwner("Aguardando crescimento das safras na plantacao... / Waiting for crops to grow...");
                }
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);

        if (!canReachWork(targetWorkPos)) {
            moveToward(targetCenter, 0.24);
        } else {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.3, delta.y, delta.z * 0.3);

            BlockState state = this.level().getBlockState(targetWorkPos);

            // 1. Safra madura: colhe
            if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                workBreakTicks++;
                if (workBreakTicks % 4 == 0) {
                    this.swing(InteractionHand.MAIN_HAND, true);
                }
                if (workBreakTicks >= 12) {
                    if (!checkCanBreakBlock(targetWorkPos)) {
                        speakToOwner("Nao tenho permissao para colher safras nesta area protegida!");
                        targetWorkPos = null;
                        workBreakTicks = 0;
                        return;
                    }
                    this.level().destroyBlock(targetWorkPos, true, this);
                    requestDropCollection(targetWorkPos);
                    targetWorkPos = null;
                    workBreakTicks = 0;
                }
            }
            // 2. Terra arada vazia: replanta
            else if (state.is(Blocks.FARMLAND) && this.level().getBlockState(targetWorkPos.above()).isAir()) {
                BlockPos plantPos = targetWorkPos.above();
                if (!checkCanInteractBlock(plantPos)) {
                    speakToOwner("Nao tenho permissao para plantar sementes nesta area protegida!");
                    targetWorkPos = null;
                    workBreakTicks = 0;
                    return;
                }
                ItemStack seedStack = findSeedsInInventory();
                if (seedStack != null && !seedStack.isEmpty()) {
                    BlockState cropState = getCropStateForSeed(seedStack.getItem());
                    if (cropState != null) {
                        this.level().setBlockAndUpdate(plantPos, cropState);
                        seedStack.shrink(1);
                        this.swing(InteractionHand.MAIN_HAND, true);
                        this.level().playSound(null, plantPos.getX(), plantPos.getY(), plantPos.getZ(),
                                SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                }
                targetWorkPos = null;
                workBreakTicks = 0;
            }
            // 3. Terra aravel com permissao de arar e enxada disponivel
            else if (this.farmTillEnabled && isTillableDirt(state) && this.level().getBlockState(targetWorkPos.above()).isAir()) {
                if (!checkCanInteractBlock(targetWorkPos)) {
                    speakToOwner("Nao tenho permissao para arar a terra nesta area protegida!");
                    targetWorkPos = null;
                    workBreakTicks = 0;
                    return;
                }
                ItemStack hoe = findHoeInInventory();
                if (hoe != null) {
                    equipBestHoe();
                    this.swing(InteractionHand.MAIN_HAND, true);
                    this.level().setBlockAndUpdate(targetWorkPos, Blocks.FARMLAND.defaultBlockState());
                    this.level().playSound(null, targetWorkPos.getX(), targetWorkPos.getY(), targetWorkPos.getZ(),
                            SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                    hoe.hurtAndBreak(1, serverLevel(), this, item -> {});

                    // Planta imediatamente sobre o novo bloco de terra arada se tiver semente
                    ItemStack seedStack = findSeedsInInventory();
                    if (seedStack != null && !seedStack.isEmpty()) {
                        BlockPos plantPos = targetWorkPos.above();
                        BlockState cropState = getCropStateForSeed(seedStack.getItem());
                        if (cropState != null) {
                            this.level().setBlockAndUpdate(plantPos, cropState);
                            seedStack.shrink(1);
                            this.level().playSound(null, plantPos.getX(), plantPos.getY(), plantPos.getZ(),
                                    SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
                        }
                    }
                }
                targetWorkPos = null;
                workBreakTicks = 0;
            } else {
                targetWorkPos = null;
                workBreakTicks = 0;
            }
        }
    }

    private BlockPos findFarmTarget() {
        BlockPos currentPos = this.blockPosition();

        // 1. Procura primeiro safras maduras para colher
        for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-10, -2, -10), currentPos.offset(10, 2, 10))) {
            BlockState state = this.level().getBlockState(candidate);
            if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                return candidate.immutable();
            }
        }

        // 2. Se tem sementes, procura terra arada vazia para plantar
        ItemStack seeds = findSeedsInInventory();
        if (seeds != null) {
            for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-10, -2, -10), currentPos.offset(10, 2, 10))) {
                BlockState state = this.level().getBlockState(candidate);
                if (state.is(Blocks.FARMLAND) && this.level().getBlockState(candidate.above()).isAir()) {
                    return candidate.immutable();
                }
            }

            // 3. Se arado estiver habilitado e possuir enxada: procura terra/grama proxima a agua
            if (this.farmTillEnabled && findHoeInInventory() != null) {
                for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-8, -2, -8), currentPos.offset(8, 2, 8))) {
                    BlockState state = this.level().getBlockState(candidate);
                    if (isTillableDirt(state) && this.level().getBlockState(candidate.above()).isAir() && isNearWater(candidate)) {
                        return candidate.immutable();
                    }
                }
            }
        }

        return null;
    }

    private boolean isTillableDirt(BlockState state) {
        if (state == null || state.isAir()) return false;
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT);
    }

    private boolean isNearWater(BlockPos pos) {
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (this.level().getFluidState(candidate).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack findHoeInInventory() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof HoeItem) {
                return stack;
            }
        }
        return null;
    }

    private ItemStack findSeedsInInventory() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (item == Items.WHEAT_SEEDS || item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT_SEEDS) {
                    return stack;
                }
            }
        }
        return null;
    }

    private BlockState getCropStateForSeed(Item item) {
        if (item == Items.WHEAT_SEEDS) return Blocks.WHEAT.defaultBlockState();
        if (item == Items.CARROT) return Blocks.CARROTS.defaultBlockState();
        if (item == Items.POTATO) return Blocks.POTATOES.defaultBlockState();
        if (item == Items.BEETROOT_SEEDS) return Blocks.BEETROOTS.defaultBlockState();
        return null;
    }

    private void handleFollowMode(ServerPlayer owner) {
        if (owner != null && owner.level() == this.level()) {
            double distSq = this.distanceToSqr(owner);

            if (distSq > 1296.0) {
                if (this.tickCount % 100 == 0) recallToOwner();
                return;
            }

            if (distSq > 9.0) {
                moveToward(owner.position(), 0.24);
            } else {
                Vec3 delta = this.getDeltaMovement();
                this.setDeltaMovement(delta.x * 0.5, delta.y, delta.z * 0.5);
                lookAtEntity(owner);
            }
        }
    }

    /**
     * Prioriza troncos com o menor Y (base da arvore) e menor distancia horizontal.
     */
    private BlockPos findNearestLog() {
        BlockPos currentPos = this.blockPosition();
        BlockPos bestPos = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-12, -4, -12), currentPos.offset(12, 12, 12))) {
            BlockState state = this.level().getBlockState(candidate);
            if (!state.isAir() && (state.is(BlockTags.LOGS) || state.getBlock().getDescriptionId().contains("log"))) {
                int yDiff = candidate.getY() - currentPos.getY();
                double horizDist = Math.hypot(candidate.getX() - currentPos.getX(), candidate.getZ() - currentPos.getZ());
                // Peso altissimo para Y baixo (cortar a base primeiro)
                double score = (yDiff * 500.0) + horizDist;
                if (score < bestScore) {
                    bestScore = score;
                    bestPos = candidate.immutable();
                }
            }
        }
        return bestPos;
    }

    private boolean visibleBlock(BlockPos pos) {
        Vec3 direction = pos.getCenter().subtract(getEyePosition());
        if (direction.normalize().dot(getLookAngle()) < 0.35) return false;
        var hit = level().clip(new net.minecraft.world.level.ClipContext(getEyePosition(), pos.getCenter(),
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    private BlockPos findNearestOre() {
        BlockPos current = blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        blockedOre = null;
        for (BlockPos candidate : BlockPos.betweenClosed(current.offset(-8, -2, -8), current.offset(8, 4, 8))) {
            if (!level().hasChunkAt(candidate) || candidate.equals(current.below())) continue;
            BlockState state = level().getBlockState(candidate);
            boolean ore = isTargetOre(state);
            if (!ore && !state.is(net.neoforged.neoforge.common.Tags.Blocks.STONES) && !state.is(Blocks.COBBLESTONE)) continue;
            if (state.getDestroySpeed(level(), candidate) < 0 || !visibleBlock(candidate)) continue;
            if (!hasToolFor(state)) { if (ore) blockedOre = state; continue; }
            double score = candidate.distSqr(current) + (ore ? 0 : 400);
            if (score < bestScore) { bestScore = score; best = candidate.immutable(); }
        }
        return best;
    }

    private BlockPos staircaseTarget() {
        if (stairDestination == null) stairDestination = blockPosition().relative(miningDirection).below();
        BlockPos floor = stairDestination.below();
        if (!level().hasChunkAt(floor) || !level().getBlockState(floor).isSolidRender(level(), floor)
                || level().getBlockState(floor).is(Blocks.MAGMA_BLOCK)) {
            miningDirection = miningDirection.getClockWise();
            stairDestination = null;
            return null;
        }
        for (BlockPos pos : BlockPos.betweenClosed(stairDestination.offset(-1, -1, -1), stairDestination.offset(1, 2, 1))) {
            if (!level().hasChunkAt(pos) || !level().getFluidState(pos).isEmpty()) {
                miningDirection = miningDirection.getClockWise();
                stairDestination = null;
                return null;
            }
        }
        for (BlockPos pos : new BlockPos[]{stairDestination.above(2), stairDestination.above(), stairDestination}) {
            BlockState state = level().getBlockState(pos);
            if (state.isAir()) continue;
            if (state.hasBlockEntity() || state.getDestroySpeed(level(), pos) < 0 || !hasToolFor(state)) {
                miningDirection = miningDirection.getClockWise();
                stairDestination = null;
                return null;
            }
            lookAtPosition(pos.getCenter());
            return pos;
        }
        return null;
    }

    private boolean tryCraftUpgrade(BlockState block, boolean weapon) {
        if (tickCount < nextGearCraftTick) return false;
        nextGearCraftTick = tickCount + 200;
        double current = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = getInventory().getItem(i);
            current = Math.max(current, weapon ? EquipmentPolicy.weapon(stack) : EquipmentPolicy.tool(stack, block));
        }
        java.util.Map<Item, ItemStack> unique = new java.util.HashMap<>();
        for (var holder : serverLevel().getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (holder.value().isSpecial() || (!(holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe)
                    && !(holder.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe))) continue;
            ItemStack output = holder.value().getResultItem(registryAccess());
            double score = weapon ? EquipmentPolicy.weapon(output) : EquipmentPolicy.tool(output, block);
            if (score > current + 0.2 && (weapon || output.getDestroySpeed(block) > 1)) unique.put(output.getItem(), output);
        }
        java.util.List<ItemStack> choices = new java.util.ArrayList<>(unique.values());
        choices.sort(java.util.Comparator.comparingDouble(stack -> weapon ? EquipmentPolicy.weapon(stack) : EquipmentPolicy.tool(stack, block)));
        for (int index = 0; index < Math.min(4, choices.size()); index++) {
            ItemStack choice = choices.get(index);
            if (com.thyagotoledo.companions.neoforge.service.RecipeCraftingService.craft(this, choice.getItem(), 1)) {
                if (weapon) equipBestWeapon(); else equipForBlock(block);
                speakToOwner("Preparei equipamento melhor / Crafted better equipment: " + choice.getHoverName().getString());
                return true;
            }
        }
        return false;
    }

    /**
     * Guarda itens coletados no bau designado ou no bau mais proximo.
     */
    public int depositToNearbyChest() {
        BlockPos chestPos = CompanionManager.getDesignatedChest(this.ownerUuid);
        Container targetChest = null;

        if (chestPos != null && this.level().getBlockEntity(chestPos) instanceof Container designatedContainer) {
            targetChest = designatedContainer;
        } else {
            BlockPos currentPos = this.blockPosition();
            for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-8, -3, -8), currentPos.offset(8, 3, 8))) {
                BlockEntity be = this.level().getBlockEntity(candidate);
                if (be instanceof Container container) {
                    targetChest = container;
                    chestPos = candidate.immutable();
                    break;
                }
            }
        }

        if (targetChest == null || chestPos == null) {
            speakToOwner("Nao encontrei nenhum bau por perto (alcance de 8 blocos). Marque um com /companion chest.");
            return 0;
        }

        if (!checkCanInteractBlock(chestPos)) {
            speakToOwner("Nao tenho permissao para acessar o bau nesta area protegida!");
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
        speakToOwner("Guardei " + totalMoved + " itens no bau em [" + chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ() + "]!");
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

    /**
     * Abre a interface completa de 54 slots com visualizacao e equipacao de armaduras,
     * offhand, mao principal e toda a mochila do companheiro.
     */
    public void openCompanionInventory(ServerPlayer owner) {
        if (owner == null || !owner.getUUID().equals(ownerUuid)) return;
        owner.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) ->
                        ChestMenu.sixRows(containerId, playerInventory, getFullInventoryContainer()),
                Component.literal("Equipamentos e Mochila: " + this.getName().getString())
        ));
    }

    public Container getFullInventoryContainer() {
        return new Container() {
            @Override
            public int getContainerSize() {
                return 54;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public ItemStack getItem(int slot) {
                return switch (slot) {
                    case 0 -> getItemBySlot(EquipmentSlot.HEAD);
                    case 1 -> getItemBySlot(EquipmentSlot.CHEST);
                    case 2 -> getItemBySlot(EquipmentSlot.LEGS);
                    case 3 -> getItemBySlot(EquipmentSlot.FEET);
                    case 4 -> getItemBySlot(EquipmentSlot.OFFHAND);
                    case 5 -> getItemBySlot(EquipmentSlot.MAINHAND);
                    default -> {
                        if (slot >= 9 && slot < 45 && slot - 9 != getInventory().selected) {
                            yield getInventory().getItem(slot - 9);
                        }
                        yield ItemStack.EMPTY;
                    }
                };
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                if (slot < 6) {
                    ItemStack current = getItem(slot);
                    if (current.isEmpty()) return ItemStack.EMPTY;
                    ItemStack split = current.split(amount);
                    setItem(slot, current);
                    return split;
                } else if (slot >= 9 && slot < 45 && slot - 9 != getInventory().selected) {
                    return getInventory().removeItem(slot - 9, amount);
                }
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                if (slot < 6) {
                    ItemStack current = getItem(slot);
                    setItem(slot, ItemStack.EMPTY);
                    return current;
                } else if (slot >= 9 && slot < 45 && slot - 9 != getInventory().selected) {
                    return getInventory().removeItemNoUpdate(slot - 9);
                }
                return ItemStack.EMPTY;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                switch (slot) {
                    case 0 -> setItemSlot(EquipmentSlot.HEAD, stack);
                    case 1 -> setItemSlot(EquipmentSlot.CHEST, stack);
                    case 2 -> setItemSlot(EquipmentSlot.LEGS, stack);
                    case 3 -> setItemSlot(EquipmentSlot.FEET, stack);
                    case 4 -> setItemSlot(EquipmentSlot.OFFHAND, stack);
                    case 5 -> setItemSlot(EquipmentSlot.MAINHAND, stack);
                    default -> {
                        if (slot >= 9 && slot < 45 && slot - 9 != getInventory().selected) {
                            getInventory().setItem(slot - 9, stack);
                        }
                    }
                }
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                if (slot >= 9 && slot < 45) return slot - 9 != getInventory().selected;
                if (slot == 4 || slot == 5) return true;
                if (slot < 0 || slot > 3) return false;
                EquipmentSlot armorSlot = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}[slot];
                return EquipmentPolicy.armor(stack, armorSlot) > 0
                        || CompanionServerPlayer.this.getEquipmentSlotForItem(stack) == armorSlot;
            }

            @Override
            public void setChanged() {
                getInventory().setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return isAlive() && player.getUUID().equals(ownerUuid) && player.distanceToSqr(CompanionServerPlayer.this) <= 64.0;
            }

            @Override
            public void clearContent() {
                for (EquipmentSlot s : EquipmentSlot.values()) {
                    setItemSlot(s, ItemStack.EMPTY);
                }
                for (int i = 0; i < 36; i++) {
                    getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        };
    }

    /**
     * Sistema de Crafting Autonomo Transacional:
     * Planeja receitas reais sobre uma copia da mochila e tenta novamente por ate 60 segundos.
     * Nao retira itens remotamente de baus.
     */
    public void executeAutonomousCraft(ServerPlayer owner, String rawItemName, int count) {
        if (owner == null || !owner.getUUID().equals(ownerUuid) || rawItemName == null) return;
        String name = resolveRecipeName(rawItemName.toLowerCase(Locale.ROOT).trim());
        Item item = getItemByRecipeId(name);
        if (item == null) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(name);
            if (id != null) item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        }
        if (item == null || item == Items.AIR || count < 1 || count > 64) {
            speakToOwner("Receita ou quantidade invalida / Invalid recipe or quantity (1-64).");
            return;
        }
        boolean crafted = com.thyagotoledo.companions.neoforge.service.RecipeCraftingService.craft(this, item, count);
        pendingCraftItem = crafted ? null : item;
        pendingCraftCount = count;
        pendingCraftUntil = this.tickCount + 1200;
        nextCraftRetry = this.tickCount + 40;
        speakToOwner(crafted
                ? "Fabricado e guardado na mochila / Crafted and stored: " + name
                : "Faltam ingredientes, espaco ou receita suportada / Missing ingredients, space or supported recipe: " + name);
    }

    private boolean checkPendingCraftFulfillment(ServerPlayer owner) {
        if (pendingCraftItem == null || this.tickCount < nextCraftRetry) return false;
        nextCraftRetry = this.tickCount + 40;
        if (this.tickCount > pendingCraftUntil) {
            pendingCraftItem = null;
            speakToOwner("Fabricacao pausada: confira bancada e materiais / Craft paused: check workbench and materials.");
            return false;
        }
        if (com.thyagotoledo.companions.neoforge.service.RecipeCraftingService.craft(this, pendingCraftItem, pendingCraftCount)) {
            pendingCraftItem = null;
            finishWork();
            speakToOwner("Fabricado e guardado na mochila / Crafted and stored in my bag.");
            return true;
        }
        return false;
    }

    private String resolveRecipeName(String query) {
        if (query.contains("picareta de ferro")) return "iron_pickaxe";
        if (query.contains("picareta de pedra")) return "stone_pickaxe";
        if (query.contains("picareta")) return "wooden_pickaxe";
        if (query.contains("espada de ferro")) return "iron_sword";
        if (query.contains("espada de pedra")) return "stone_sword";
        if (query.contains("espada")) return "wooden_sword";
        if (query.contains("machado")) return "wooden_axe";
        if (query.contains("bancada") || query.contains("mesa")) return "crafting_table";
        if (query.contains("bau")) return "chest";
        if (query.contains("fornalha")) return "furnace";
        if (query.contains("tocha")) return "torch";
        if (query.contains("tabua") || query.contains("madeira")) return "oak_planks";
        if (query.contains("graveto")) return "stick";
        if (query.contains("pao")) return "bread";
        return query;
    }

    private Item getItemByRecipeId(String recipeId) {
        String clean = recipeId.startsWith("minecraft:") ? recipeId.substring("minecraft:".length()) : recipeId;
        return switch (clean) {
            case "oak_planks", "planks" -> Items.OAK_PLANKS;
            case "stick" -> Items.STICK;
            case "crafting_table" -> Items.CRAFTING_TABLE;
            case "chest" -> Items.CHEST;
            case "furnace" -> Items.FURNACE;
            case "torch" -> Items.TORCH;
            case "wooden_pickaxe" -> Items.WOODEN_PICKAXE;
            case "stone_pickaxe" -> Items.STONE_PICKAXE;
            case "iron_pickaxe" -> Items.IRON_PICKAXE;
            case "wooden_sword" -> Items.WOODEN_SWORD;
            case "stone_sword" -> Items.STONE_SWORD;
            case "iron_sword" -> Items.IRON_SWORD;
            case "wooden_axe" -> Items.WOODEN_AXE;
            case "bread" -> Items.BREAD;
            default -> null; // Elimina fallback espúrio de tabuas!
        };
    }

    private int countItemInInventory(Item item) {
        if (item == null) return 0;
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private int countLogsInInventory() {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.is(ItemTags_LOGS()) || stack.getItem().getDescriptionId().contains("log"))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private net.minecraft.tags.TagKey<Item> ItemTags_LOGS() { return ItemTags.LOGS; }

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
        float yaw = Mth.approachDegrees(this.getYRot(), targetYaw, 35.0f);
        this.setYRot(yaw);
        this.setXRot(Mth.approachDegrees(this.getXRot(), targetPitch, 20.0f));
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    public void speakToOwner(String message) {
        if (this.server == null) return;
        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("<" + this.getName().getString() + "> " + message));
        }
    }

    public boolean tryRecallToOwner(ServerPlayer owner) {
        if (owner == null || this.server == null) {
            return false;
        }

        // 1. Verificacao de dimensao
        if (this.level() != owner.level()) {
            speakToOwner("Estamos em dimensoes diferentes, nao consigo alcancar voce.");
            return false;
        }

        // 2. Verificacao de combate ativo
        if (this.mode == CompanionMode.DEFEND) {
            AABB searchBox = this.getBoundingBox().inflate(10.0, 4.0, 10.0);
            List<Monster> hostiles = this.level().getEntitiesOfClass(Monster.class, searchBox, LivingEntity::isAlive);
            if (!hostiles.isEmpty()) {
                speakToOwner("Nao posso ir ate voce agora: estou em combate com monstros proximos!");
                return false;
            }
        }

        // 3. Verificacao de cooldown (minimo 60 ticks = 3 segundos)
        long currentGameTime = this.level().getGameTime();
        if (currentGameTime - this.lastRecallGameTime < 60L && this.lastRecallGameTime > 0) {
            speakToOwner("Aguarde alguns instantes antes de me chamar novamente.");
            return false;
        }

        // 4. Busca de bloco seguro ao redor do jogador
        BlockPos ownerPos = owner.blockPosition();
        BlockPos safePos = findSafeRecallPosition(owner.serverLevel(), ownerPos);

        if (safePos == null) {
            speakToOwner("O local de destino e perigoso ou nao tem chao seguro.");
            return false;
        }

        // 5. Teleporte seguro com conservacao de movimento e atualizacao de estado
        this.teleportTo(owner.serverLevel(), safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D, owner.getYRot(), owner.getXRot());
        this.setDeltaMovement(0, 0, 0);
        this.finishWork();
        this.targetWorkPos = null;
        this.workBreakTicks = 0;
        this.lastRecallGameTime = currentGameTime;

        speakToOwner("Estou a caminho, me aproximei com seguranca de voce!");
        return true;
    }

    public BlockPos findSafeRecallPosition(ServerLevel level, BlockPos center) {
        if (level == null || center == null) return null;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    BlockState floor = level.getBlockState(candidate);
                    BlockPos feetPos = candidate.above();
                    BlockPos headPos = candidate.above(2);
                    BlockState feet = level.getBlockState(feetPos);
                    BlockState head = level.getBlockState(headPos);

                    boolean floorSafe = (floor.isSolidRender(level, candidate) || floor.isSolid())
                            && !floor.is(Blocks.LAVA)
                            && !floor.is(Blocks.FIRE)
                            && !floor.is(Blocks.SOUL_FIRE)
                            && !floor.is(Blocks.MAGMA_BLOCK);

                    boolean feetSafe = (feet.isAir() || feet.canBeReplaced()) && feet.getFluidState().isEmpty();
                    boolean headSafe = (head.isAir() || head.canBeReplaced()) && head.getFluidState().isEmpty();

                    if (floorSafe && feetSafe && headSafe) {
                        return feetPos;
                    }
                }
            }
        }
        return null;
    }

    public void recallToOwner() {
        if (this.server == null) return;
        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);
        if (owner != null) {
            tryRecallToOwner(owner);
        }
    }

    public void dismiss() {
        if (isAlive()) saveEquipmentCheckpoint();
        if (this.server != null && this.server.getPlayerList() != null) {
            this.server.getPlayerList().broadcastSystemMessage(
                    Component.literal(this.getName().getString() + " saiu do jogo"),
                    false
            );
            this.server.getPlayerList().remove(this);
        }
    }

    public void dismissSilent() {
        if (isAlive()) saveEquipmentCheckpoint();
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
            this.autonomous = mode == CompanionMode.WORK;
            if (mode == CompanionMode.STAY) this.pendingCraftItem = null;
            this.targetWorkPos = null;
            this.targetVeinPos = null;
            this.miningObjectivePos = null;
            this.workBreakTicks = 0;
            this.localPath = java.util.Collections.emptyList();
            this.nextPathTick = 0;
            this.perceptionTicks = 0;
            this.stairSteps = 0;
            this.stairApproachTicks = 0;
            this.stairDestination = null;
            this.miningDirection = null;
            if (dataEntity != null) {
                dataEntity.setMode(mode);
            }
        }
    }

    public String getMiningPriority() {
        return miningPriority;
    }

    public void setMiningPriority(String priority) {
        this.miningPriority = (priority == null || priority.trim().isEmpty()) ? "all" : priority.trim().toLowerCase(Locale.ROOT);
        this.targetWorkPos = null;
        this.targetVeinPos = null;
        this.miningObjectivePos = null;
    }

    public boolean isFarmTillEnabled() {
        return farmTillEnabled;
    }

    public void setFarmTillEnabled(boolean enabled) {
        this.farmTillEnabled = enabled;
        this.targetWorkPos = null;
    }

    public NeoForgeCompanionEntity getDataEntity() {
        return dataEntity;
    }
}
