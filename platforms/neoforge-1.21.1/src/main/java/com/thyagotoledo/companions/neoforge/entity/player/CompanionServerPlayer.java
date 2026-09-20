package com.thyagotoledo.companions.neoforge.entity.player;

import com.mojang.authlib.GameProfile;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.planner.RecipeCatalog;
import com.thyagotoledo.companions.neoforge.entity.CompanionManager;
import com.thyagotoledo.companions.neoforge.entity.NeoForgeCompanionEntity;
import com.thyagotoledo.companions.neoforge.service.NeoForgePermissionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
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
    private int healCooldown = 0;
    private long lastRecallGameTime = 0L;

    // Estado para tarefas ativas de trabalho (Madeira / Mineracao / Agricultura)
    private BlockPos targetWorkPos = null;
    private int workBreakTicks = 0;
    private int harvestedCount = 0;
    private long nextWorkScanTick = 0L;

    // Meta pendente de crafting autonomo
    private String pendingCraftItem = null;
    private int pendingCraftCount = 0;

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
        super.die(damageSource);
        speakToOwner("Fui derrotado em combate! Use /companion spawn para me convocar novamente.");
    }

    @Override
    public void tick() {
        super.doTick();
        tickAI();
    }

    /**
     * Executa a logica motora e tomada de decisoes da IA a cada tick do servidor.
     */
    private void tickAI() {
        if (this.server == null || this.level().isClientSide) {
            return;
        }

        // 1. Inteligencia periodica a cada 20 ticks (1 segundo)
        if (this.tickCount % 20 == 0) {
            tryAutoEat();
            smartEquipArmor();
            smartEquipTools();
        }

        ServerPlayer owner = this.server.getPlayerList().getPlayer(this.ownerUuid);

        // 2. Coleta automatica de drops do chao ao redor (raio de 3 blocos).
        // A busca de entidades e limitada a 4 Hz para evitar custo por NPC a cada tick.
        if (this.tickCount % 5 == 0) {
            pickupNearbyItems();
        }

        // 3. Execucao de acordo com o modo atual
        switch (this.mode) {
            case STAY -> handleStayMode(owner);
            case DEFEND -> handleDefendMode(owner);
            case WOOD -> handleWoodMode(owner);
            case MINE -> handleMineMode(owner);
            case FARM -> handleFarmMode(owner);
            case FOLLOW, WORK, IDLE -> handleFollowMode(owner);
        }
    }

    /**
     * Auto-alimentacao: consome alimentos da bolsa se a vida estiver abaixo de 18 coracoes.
     */
    private void tryAutoEat() {
        if (this.getHealth() >= 18.0f) {
            return;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                FoodProperties food = stack.get(DataComponents.FOOD);
                int nutrition = food != null ? food.nutrition() : 4;
                this.heal(nutrition);
                stack.shrink(1);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;
            }
        }
    }

    /**
     * Auto-equip de armadura: equipa automaticamente as pecas com maior protecao encontradas na bolsa.
     */
    private void smartEquipArmor() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armor) {
                EquipmentSlot slot = armor.getEquipmentSlot();
                ItemStack currentArmor = this.getItemBySlot(slot);
                int currentScore = getArmorScore(currentArmor);
                int newScore = getArmorScore(stack);

                if (newScore > currentScore) {
                    this.setItemSlot(slot, stack.copy());
                    stack.setCount(0);
                    if (!currentArmor.isEmpty()) {
                        this.getInventory().add(currentArmor);
                    }
                }
            }
        }
    }

    private int getArmorScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) {
            return 0;
        }
        return armor.getDefense() * 10 + (int) (armor.getToughness() * 5);
    }

    /**
     * Auto-equip de ferramentas: seleciona a ferramenta correta para o modo atual.
     */
    private void smartEquipTools() {
        switch (this.mode) {
            case WOOD -> equipBestTool(AxeItem.class);
            case MINE -> equipBestTool(PickaxeItem.class);
            case DEFEND -> equipBestWeapon();
            case FARM -> equipBestTool(HoeItem.class);
            default -> {}
        }
    }

    private <T extends Item> void equipBestTool(Class<T> toolClass) {
        int bestSlot = -1;
        int bestTierScore = -1;

        ItemStack currentMain = this.getMainHandItem();
        if (toolClass.isInstance(currentMain.getItem())) {
            bestTierScore = getTierScore(currentMain);
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && toolClass.isInstance(stack.getItem())) {
                int score = getTierScore(stack);
                if (score > bestTierScore) {
                    bestTierScore = score;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) {
            ItemStack bestItem = this.getInventory().getItem(bestSlot);
            ItemStack oldMain = this.getMainHandItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, bestItem.copy());
            bestItem.setCount(0);
            if (!oldMain.isEmpty()) {
                this.getInventory().add(oldMain);
            }
        }
    }

    private void equipBestWeapon() {
        int bestSlot = -1;
        int bestScore = -1;

        ItemStack currentMain = this.getMainHandItem();
        if (currentMain.getItem() instanceof SwordItem || currentMain.getItem() instanceof AxeItem) {
            bestScore = getTierScore(currentMain);
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem)) {
                int score = getTierScore(stack) + (stack.getItem() instanceof SwordItem ? 5 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) {
            ItemStack bestItem = this.getInventory().getItem(bestSlot);
            ItemStack oldMain = this.getMainHandItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, bestItem.copy());
            bestItem.setCount(0);
            if (!oldMain.isEmpty()) {
                this.getInventory().add(oldMain);
            }
        }
    }

    private int getTierScore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        String id = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        if (id.contains("netherite")) return 60;
        if (id.contains("diamond")) return 50;
        if (id.contains("iron")) return 40;
        if (id.contains("golden")) return 35;
        if (id.contains("stone")) return 30;
        if (id.contains("wooden")) return 20;
        return 10;
    }

    private void pickupNearbyItems() {
        AABB vacuumBox = this.getBoundingBox().inflate(3.0, 1.5, 3.0);
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

        if (priorityTarget == null && this.tickCount % 10 == 0) {
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

    /**
     * Coleta de madeira com corte em cascata do tronco (Connected Tree Felling).
     * Derruba a arvore inteira de baixo para cima, evitando travamentos olhando para cima.
     */
    private void handleWoodMode(ServerPlayer owner) {
        if (targetWorkPos == null || this.level().getBlockState(targetWorkPos).isAir()) {
            if (this.tickCount < nextWorkScanTick) return;
            nextWorkScanTick = this.tickCount + 20L;
            targetWorkPos = findNearestLog();
            workBreakTicks = 0;
            if (targetWorkPos == null) {
                checkPendingCraftFulfillment(owner);
                setMode(CompanionMode.FOLLOW);
                speakToOwner("Toda a madeira proxima foi coletada (" + harvestedCount + " blocos). Voltando a te seguir!");
                harvestedCount = 0;
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);

        Vec3 diff = targetCenter.subtract(this.position());
        double dXZ = Math.hypot(diff.x, diff.z);
        double distEyeSq = this.getEyePosition().distanceToSqr(targetCenter);

        // Se estiver distante horizontalmente e fora do alcance dos olhos
        if (dXZ > 2.2 && distEyeSq > 20.0) {
            Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.24);
            double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
            this.setDeltaMovement(dir.x, jumpY, dir.z);
        } else {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.3, delta.y, delta.z * 0.3);

            workBreakTicks++;
            if (workBreakTicks % 4 == 0) {
                this.swing(InteractionHand.MAIN_HAND, true);
            }

            if (workBreakTicks >= 20) {
                // Valida protecao do bloco base
                if (!checkCanBreakBlock(targetWorkPos)) {
                    speakToOwner("Nao tenho permissao para quebrar madeira nesta area protegida!");
                    targetWorkPos = null;
                    workBreakTicks = 0;
                    setMode(CompanionMode.FOLLOW);
                    return;
                }

                // Destroi o bloco base
                this.level().destroyBlock(targetWorkPos, true, this);
                harvestedCount++;

                // Cascata: derruba troncos conectados verticalmente para cima ate 16 blocos
                BlockPos currentAbove = targetWorkPos.above();
                for (int i = 0; i < 16; i++) {
                    BlockState stateAbove = this.level().getBlockState(currentAbove);
                    if (!stateAbove.isAir() && (stateAbove.is(BlockTags.LOGS) || stateAbove.getBlock().getDescriptionId().contains("log"))) {
                        if (!checkCanBreakBlock(currentAbove)) {
                            break;
                        }
                        this.level().destroyBlock(currentAbove, true, this);
                        harvestedCount++;
                        currentAbove = currentAbove.above();
                    } else {
                        break;
                    }
                }

                targetWorkPos = null;
                workBreakTicks = 0;

                // Verifica se completou meta de crafting pendente
                if (checkPendingCraftFulfillment(owner)) {
                    return;
                }

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
            if (this.tickCount < nextWorkScanTick) return;
            nextWorkScanTick = this.tickCount + 20L;
            targetWorkPos = findNearestOre();
            workBreakTicks = 0;
            if (targetWorkPos == null) {
                checkPendingCraftFulfillment(owner);
                setMode(CompanionMode.FOLLOW);
                speakToOwner("Nao encontrei mais minerios ou pedras expostas por perto. Voltando a te seguir!");
                harvestedCount = 0;
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);

        Vec3 diff = targetCenter.subtract(this.position());
        double dXZ = Math.hypot(diff.x, diff.z);
        double distEyeSq = this.getEyePosition().distanceToSqr(targetCenter);

        if (dXZ > 2.2 && distEyeSq > 20.0) {
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
                // Valida protecao de mineracao
                if (!checkCanBreakBlock(targetWorkPos)) {
                    speakToOwner("Nao tenho permissao para minerar nesta area protegida!");
                    targetWorkPos = null;
                    workBreakTicks = 0;
                    setMode(CompanionMode.FOLLOW);
                    return;
                }

                this.level().destroyBlock(targetWorkPos, true, this);
                harvestedCount++;
                targetWorkPos = null;
                workBreakTicks = 0;

                if (checkPendingCraftFulfillment(owner)) {
                    return;
                }

                if (harvestedCount >= 16) {
                    setMode(CompanionMode.FOLLOW);
                    speakToOwner("Coletei recursos minerados suficientes! Voltando a te seguir.");
                    harvestedCount = 0;
                }
            }
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
                setMode(CompanionMode.FOLLOW);
                speakToOwner("Trabalho na fazenda concluido! Todas as safras maduras foram colhidas e replantadas.");
                return;
            }
        }

        Vec3 targetCenter = new Vec3(targetWorkPos.getX() + 0.5, targetWorkPos.getY() + 0.5, targetWorkPos.getZ() + 0.5);
        lookAtPosition(targetCenter);

        Vec3 diff = targetCenter.subtract(this.position());
        double dXZ = Math.hypot(diff.x, diff.z);
        double distEyeSq = this.getEyePosition().distanceToSqr(targetCenter);

        if (dXZ > 2.0 && distEyeSq > 16.0) {
            Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize().scale(0.24);
            double jumpY = (this.horizontalCollision && this.onGround()) ? 0.42 : this.getDeltaMovement().y;
            this.setDeltaMovement(dir.x, jumpY, dir.z);
        } else {
            Vec3 delta = this.getDeltaMovement();
            this.setDeltaMovement(delta.x * 0.3, delta.y, delta.z * 0.3);

            BlockState state = this.level().getBlockState(targetWorkPos);

            // Se for safra madura: colhe
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
                        setMode(CompanionMode.FOLLOW);
                        return;
                    }
                    this.level().destroyBlock(targetWorkPos, true, this);
                    targetWorkPos = null;
                    workBreakTicks = 0;
                }
            }
            // Se for terra arada vazia: replanta
            else if (state.is(Blocks.FARMLAND) && this.level().getBlockState(targetWorkPos.above()).isAir()) {
                BlockPos plantPos = targetWorkPos.above();
                if (!checkCanInteractBlock(plantPos)) {
                    speakToOwner("Nao tenho permissao para plantar sementes nesta area protegida!");
                    targetWorkPos = null;
                    workBreakTicks = 0;
                    setMode(CompanionMode.FOLLOW);
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
        if (findSeedsInInventory() != null) {
            for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-10, -2, -10), currentPos.offset(10, 2, 10))) {
                BlockState state = this.level().getBlockState(candidate);
                if (state.is(Blocks.FARMLAND) && this.level().getBlockState(candidate.above()).isAir()) {
                    return candidate.immutable();
                }
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

    private BlockPos findNearestOre() {
        BlockPos currentPos = this.blockPosition();
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(currentPos.offset(-10, -4, -10), currentPos.offset(10, 4, 10))) {
            BlockState state = this.level().getBlockState(candidate);
            if (!state.isAir()) {
                String id = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
                if (id.contains("ore") || id.contains("coal") || id.contains("iron") || id.contains("copper") || id.contains("stone") || id.contains("cobblestone")) {
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
        if (owner == null) return;
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
                        if (slot >= 9 && slot < 45) {
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
                } else if (slot >= 9 && slot < 45) {
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
                } else if (slot >= 9 && slot < 45) {
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
                        if (slot >= 9 && slot < 45) {
                            getInventory().setItem(slot - 9, stack);
                        }
                    }
                }
            }

            @Override
            public void setChanged() {
                getInventory().setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return isAlive() && player.distanceToSqr(CompanionServerPlayer.this) <= 64.0;
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
     * Verifica inventario, procura no bau designado e, se faltar materiais, vai coletar na natureza.
     * Garante conservacao estrita de itens sem duplicacao.
     */
    public void executeAutonomousCraft(ServerPlayer owner, String rawItemName, int count) {
        if (count <= 0) count = 1;
        String query = rawItemName.toLowerCase(Locale.ROOT).trim();
        String targetItem = resolveRecipeName(query);

        Item itemObj = getItemByRecipeId(targetItem);
        if (itemObj == null) {
            speakToOwner("Item desconhecido ou receita nao suportada: '" + rawItemName + "'. Operacao cancelada.");
            return;
        }

        // 1. Entrega primeiro o que ja esta pronto, mantendo a quantidade pedida exata.
        // Se so houver parte disponivel, a fabricacao continua apenas para o restante.
        int remainingCount = count;
        int availableInBag = countItemInInventory(itemObj);
        if (availableInBag > 0) {
            int takenFromBag = Math.min(availableInBag, remainingCount);
            deliverItemToOwner(owner, withdrawItemFromInventory(itemObj, takenFromBag));
            remainingCount -= takenFromBag;
            if (remainingCount == 0) {
                speakToOwner("Ja tinha " + takenFromBag + " " + rawItemName + " prontos na bolsa e entreguei para voce.");
                return;
            }
        }

        // 2. Verifica no bau designado (respeitando protecao de claim)
        BlockPos chestPos = CompanionManager.getDesignatedChest(this.ownerUuid);
        if (chestPos != null) {
            if (!checkCanInteractBlock(chestPos)) {
                speakToOwner("Nao tenho permissao para acessar o bau designado nesta area protegida!");
            } else if (this.level().getBlockEntity(chestPos) instanceof Container chest) {
                int inChest = withdrawItemFromContainer(chest, itemObj, remainingCount);
                if (inChest > 0) {
                    deliverItemToOwner(owner, new ItemStack(itemObj, inChest));
                    speakToOwner("Peguei " + inChest + " " + rawItemName + " do nosso bau designado e entreguei para voce!");
                    remainingCount -= inChest;
                    if (remainingCount == 0) return;
                }
            }
        }

        // 3. Calcula ingredientes necessarios para fabricacao
        int logsNeeded = calculateLogsNeeded(targetItem, remainingCount);
        int cobbleNeeded = calculateCobbleNeeded(targetItem, remainingCount);
        int wheatNeeded = calculateWheatNeeded(targetItem, remainingCount);

        // Se faltar na bolsa, tenta retirar ingredientes do bau designado
        if (chestPos != null && checkCanInteractBlock(chestPos) && this.level().getBlockEntity(chestPos) instanceof Container chest) {
            if (logsNeeded > 0 && countLogsInInventory() < logsNeeded) {
                int missing = logsNeeded - countLogsInInventory();
                withdrawItemTagFromContainer(chest, ItemTags.LOGS, missing);
            }
            if (cobbleNeeded > 0 && countCobbleInInventory() < cobbleNeeded) {
                int missing = cobbleNeeded - countCobbleInInventory();
                withdrawItemFromContainer(chest, Items.COBBLESTONE, missing);
            }
            if (wheatNeeded > 0 && countWheatInInventory() < wheatNeeded) {
                int missing = wheatNeeded - countWheatInInventory();
                withdrawItemFromContainer(chest, Items.WHEAT, missing);
            }
        }

        boolean hasWood = logsNeeded == 0 || countLogsInInventory() >= logsNeeded;
        boolean hasStone = cobbleNeeded == 0 || countCobbleInInventory() >= cobbleNeeded;
        boolean hasWheat = wheatNeeded == 0 || countWheatInInventory() >= wheatNeeded;

        if (!hasWood) {
            this.pendingCraftItem = targetItem;
            this.pendingCraftCount = remainingCount;
            setMode(CompanionMode.WOOD);
            speakToOwner("Falta madeira (" + countLogsInInventory() + "/" + logsNeeded + " troncos). Vou coletar na floresta agora para fabricar seu " + rawItemName + "!");
            return;
        } else if (!hasStone) {
            this.pendingCraftItem = targetItem;
            this.pendingCraftCount = remainingCount;
            setMode(CompanionMode.MINE);
            speakToOwner("Falta pedra (" + countCobbleInInventory() + "/" + cobbleNeeded + " pedras). Vou minerar agora para fabricar seu " + rawItemName + "!");
            return;
        } else if (!hasWheat) {
            this.pendingCraftItem = targetItem;
            this.pendingCraftCount = remainingCount;
            setMode(CompanionMode.FARM);
            speakToOwner("Falta trigo (" + countWheatInInventory() + "/" + wheatNeeded + " trigos). Vou cultivar na fazenda agora para fabricar seu " + rawItemName + "!");
            return;
        }

        // 4. Se os materiais estao disponiveis, fabrica atomicamente
        craftAndDeliver(owner, targetItem, remainingCount);
    }

    private boolean checkPendingCraftFulfillment(ServerPlayer owner) {
        if (pendingCraftItem == null) return false;

        int count = pendingCraftCount > 0 ? pendingCraftCount : 1;
        int logsNeeded = calculateLogsNeeded(pendingCraftItem, count);
        int cobbleNeeded = calculateCobbleNeeded(pendingCraftItem, count);
        int wheatNeeded = calculateWheatNeeded(pendingCraftItem, count);

        boolean canCraft = (logsNeeded == 0 || countLogsInInventory() >= logsNeeded)
                && (cobbleNeeded == 0 || countCobbleInInventory() >= cobbleNeeded)
                && (wheatNeeded == 0 || countWheatInInventory() >= wheatNeeded);

        if (canCraft) {
            String item = pendingCraftItem;
            pendingCraftItem = null;
            pendingCraftCount = 0;
            craftAndDeliver(owner, item, count);
            setMode(CompanionMode.FOLLOW);
            return true;
        }
        return false;
    }

    private void craftAndDeliver(ServerPlayer owner, String targetItem, int count) {
        Item itemObj = getItemByRecipeId(targetItem);
        if (itemObj == null) {
            speakToOwner("Item desconhecido ou receita nao suportada: '" + targetItem + "'. Operacao cancelada.");
            return;
        }

        int logsNeeded = calculateLogsNeeded(targetItem, count);
        int cobbleNeeded = calculateCobbleNeeded(targetItem, count);
        int wheatNeeded = calculateWheatNeeded(targetItem, count);

        // Pre-flight check de atomicidade
        if (logsNeeded > 0 && countLogsInInventory() < logsNeeded) {
            speakToOwner("Materiais insuficientes para fabricar " + count + " " + targetItem + ".");
            return;
        }
        if (cobbleNeeded > 0 && countCobbleInInventory() < cobbleNeeded) {
            speakToOwner("Materiais insuficientes para fabricar " + count + " " + targetItem + ".");
            return;
        }
        if (wheatNeeded > 0 && countWheatInInventory() < wheatNeeded) {
            speakToOwner("Materiais insuficientes para fabricar " + count + " " + targetItem + ".");
            return;
        }

        // Consome ingredientes estritamente na quantidade requerida
        if (logsNeeded > 0) consumeLogs(logsNeeded);
        if (cobbleNeeded > 0) consumeCobblestone(cobbleNeeded);
        if (wheatNeeded > 0) consumeWheat(wheatNeeded);

        // Produz e entrega com destino unico: NUNCA adiciona em ambos!
        ItemStack crafted = new ItemStack(itemObj, count);
        if (owner != null && owner.isAlive() && owner.level() == this.level()) {
            deliverItemToOwner(owner, crafted);
            speakToOwner("Fabriquei " + count + " " + targetItem + " e entreguei para voce!");
        } else {
            this.getInventory().add(crafted);
            speakToOwner("Fabriquei " + count + " " + targetItem + " e guardei na minha bolsa.");
        }
    }

    private int calculateLogsNeeded(String recipeId, int count) {
        return switch (recipeId) {
            case "oak_planks", "planks" -> (count + 3) / 4;
            case "stick" -> (count + 7) / 8;
            case "crafting_table" -> count;
            case "chest" -> count * 2;
            case "wooden_pickaxe", "wooden_axe" -> count * 2;
            case "wooden_sword" -> Math.max(1, count);
            case "torch" -> Math.max(1, (count + 7) / 8);
            default -> 0;
        };
    }

    private int calculateCobbleNeeded(String recipeId, int count) {
        return switch (recipeId) {
            case "stone_pickaxe" -> count * 3;
            case "stone_sword" -> count * 2;
            case "furnace" -> count * 8;
            default -> 0;
        };
    }

    private int calculateWheatNeeded(String recipeId, int count) {
        return switch (recipeId) {
            case "bread" -> count * 3;
            default -> 0;
        };
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

    private int countCobbleInInventory() {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.COBBLESTONE) || stack.is(Items.STONE))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private int countWheatInInventory() {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Items.WHEAT)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private net.minecraft.tags.TagKey<Item> ItemTags_LOGS() {
        return net.minecraft.tags.ItemTags.LOGS;
    }

    private void consumeLogs(int amount) {
        int needed = amount;
        for (int i = 0; i < 36 && needed > 0; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.is(ItemTags_LOGS()) || stack.getItem().getDescriptionId().contains("log"))) {
                int toTake = Math.min(needed, stack.getCount());
                stack.shrink(toTake);
                needed -= toTake;
            }
        }
    }

    private void consumeCobblestone(int amount) {
        int needed = amount;
        for (int i = 0; i < 36 && needed > 0; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.COBBLESTONE) || stack.is(Items.STONE))) {
                int toTake = Math.min(needed, stack.getCount());
                stack.shrink(toTake);
                needed -= toTake;
            }
        }
    }

    private void consumeWheat(int amount) {
        int needed = amount;
        for (int i = 0; i < 36 && needed > 0; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Items.WHEAT)) {
                int toTake = Math.min(needed, stack.getCount());
                stack.shrink(toTake);
                needed -= toTake;
            }
        }
    }

    private ItemStack withdrawItemFromInventory(Item item, int count) {
        int needed = count;
        int taken = 0;
        for (int i = 0; i < 36 && needed > 0; i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int toTake = Math.min(needed, stack.getCount());
                stack.shrink(toTake);
                needed -= toTake;
                taken += toTake;
            }
        }
        return new ItemStack(item, taken);
    }

    private void deliverItemToOwner(ServerPlayer owner, ItemStack stack) {
        if (owner == null || stack == null || stack.isEmpty()) return;
        if (!owner.getInventory().add(stack)) {
            owner.drop(stack, false);
        }
    }

    private int withdrawItemFromContainer(Container container, Item item, int maxCount) {
        int retrieved = 0;
        for (int i = 0; i < container.getContainerSize() && retrieved < maxCount; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int toTake = Math.min(maxCount - retrieved, stack.getCount());
                stack.shrink(toTake);
                retrieved += toTake;
            }
        }
        if (retrieved > 0) {
            container.setChanged();
        }
        return retrieved;
    }

    private int withdrawItemTagFromContainer(Container container, net.minecraft.tags.TagKey<Item> tag, int maxCount) {
        int retrieved = 0;
        for (int i = 0; i < container.getContainerSize() && retrieved < maxCount; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.is(tag)) {
                int toTake = Math.min(maxCount - retrieved, stack.getCount());
                stack.shrink(toTake);
                retrieved += toTake;
            }
        }
        if (retrieved > 0) {
            container.setChanged();
        }
        return retrieved;
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
        this.setMode(CompanionMode.FOLLOW);
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
