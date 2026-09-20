package com.thyagotoledo.companions.forge.entity;

import com.thyagotoledo.companions.core.dialogue.DeterministicDialogueProvider;
import com.thyagotoledo.companions.core.locale.LocaleService;
import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.core.model.CompanionProfile;
import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;
import com.thyagotoledo.companions.core.model.Personality;
import com.thyagotoledo.companions.forge.network.ClientboundFeedbackPacket;
import com.thyagotoledo.companions.forge.network.CompanionsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import com.thyagotoledo.companions.core.work.ToolType;
import com.thyagotoledo.companions.core.work.WorkArea;
import com.thyagotoledo.companions.core.work.WorkTask;
import com.thyagotoledo.companions.forge.entity.ai.CompanionHarvestGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.thyagotoledo.companions.core.ai.ConversationMemory;
import com.thyagotoledo.companions.core.ai.InferenceSupervisor;
import com.thyagotoledo.companions.core.dialogue.HybridDialogueProvider;
import com.thyagotoledo.companions.core.planner.RecipeCatalog;
import com.thyagotoledo.companions.core.quest.Quest;
import com.thyagotoledo.companions.core.quest.QuestPlanner;
import com.thyagotoledo.companions.core.quest.QuestPlanResult;
import com.thyagotoledo.companions.forge.service.ForgeQuestService;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CompanionEntity extends TamableAnimal {
    private static final InferenceSupervisor INFERENCE_SUPERVISOR = new InferenceSupervisor(
            Boolean.parseBoolean(System.getProperty("companions.ai.enabled", "false")),
            System.getProperty("companions.ai.endpoint", "http://127.0.0.1:8080/v1/chat/completions"),
            1500,
            8
    );
    private final SimpleContainer inventory = new SimpleContainer(27);
    private CompanionMode mode = CompanionMode.FOLLOW;
    private String preferredLocale = LocaleService.PT_BR;
    private final LocaleService localeService = new LocaleService();
    private final DeterministicDialogueProvider dialogueProvider = new DeterministicDialogueProvider(localeService);
    private final ConversationMemory conversationMemory = new ConversationMemory(6);
    private final HybridDialogueProvider hybridDialogueProvider = new HybridDialogueProvider(
            dialogueProvider,
            INFERENCE_SUPERVISOR,
            conversationMemory,
            localeService
    );
    private final ForgeQuestService questService = new ForgeQuestService();
    private final RecipeCatalog recipeCatalog = new RecipeCatalog();
    private final QuestPlanner questPlanner = new QuestPlanner(questService, recipeCatalog);
    private long lastRecallGameTime = -100L;
    private WorkTask activeWorkTask = null;

    public static void shutdownInference() {
        INFERENCE_SUPERVISOR.shutdown();
    }

    public CompanionEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new CompanionHarvestGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1D, 6.0F, 2.0F, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    public CompanionMode getMode() {
        return this.mode;
    }

    public void setMode(CompanionMode mode) {
        if (mode == null) return;
        this.mode = mode;
        if (mode == CompanionMode.STAY) {
            this.setOrderedToSit(true);
        } else {
            this.setOrderedToSit(false);
        }
    }

    public String getPreferredLocale() {
        return this.preferredLocale;
    }

    public void setPreferredLocale(String locale) {
        this.preferredLocale = localeService.normalizeLocale(locale);
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public LocaleService getLocaleService() {
        return this.localeService;
    }

    public DeterministicDialogueProvider getDialogueProvider() {
        return this.dialogueProvider;
    }

    public WorkTask getActiveWorkTask() {
        return this.activeWorkTask;
    }

    public void setActiveWorkTask(WorkTask activeWorkTask) {
        this.activeWorkTask = activeWorkTask;
    }

    public void speakKey(String key, Object... args) {
        LivingEntity owner = this.getOwner();
        if (owner instanceof Player player) {
            String text = this.localeService.translate(this.preferredLocale, key, args);
            player.sendSystemMessage(Component.literal(text));
        }
    }

    public ForgeQuestService getQuestService() {
        return this.questService;
    }

    public QuestPlanner getQuestPlanner() {
        return this.questPlanner;
    }

    public RecipeCatalog getRecipeCatalog() {
        return this.recipeCatalog;
    }

    public HybridDialogueProvider getHybridDialogueProvider() {
        return this.hybridDialogueProvider;
    }

    public ConversationMemory getConversationMemory() {
        return this.conversationMemory;
    }

    public InventorySnapshot createPlayerInventorySnapshot(Player player) {
        if (player == null) return new InventorySnapshot(new ArrayList<>(), 0);
        List<ItemSlot> slots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                slots.add(new ItemSlot(
                        ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(),
                        stack.getCount()
                ));
            }
        }
        return new InventorySnapshot(slots, player.getInventory().getContainerSize());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CompanionMode", this.mode.name());
        tag.putString("PreferredLocale", this.preferredLocale);
        tag.put("Inventory", this.inventory.createTag());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CompanionMode", Tag.TAG_STRING)) {
            try {
                this.mode = CompanionMode.valueOf(tag.getString("CompanionMode"));
            } catch (IllegalArgumentException e) {
                this.mode = CompanionMode.FOLLOW;
            }
        }
        if (tag.contains("PreferredLocale", Tag.TAG_STRING)) {
            this.preferredLocale = localeService.normalizeLocale(tag.getString("PreferredLocale"));
        }
        if (tag.contains("Inventory", Tag.TAG_LIST)) {
            this.inventory.fromTag(tag.getList("Inventory", Tag.TAG_COMPOUND));
        }
        if (this.mode == CompanionMode.STAY) {
            this.setOrderedToSit(true);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        Level level = this.level();

        if (level.isClientSide) {
            boolean canInteract = this.isOwnedBy(player) || !this.isTame();
            return canInteract ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        if (!this.isTame()) {
            this.tame(player);
            this.setMode(CompanionMode.FOLLOW);
            String message = localeService.translate(preferredLocale, "message.companions.recruited");
            player.sendSystemMessage(Component.literal(message));
            return InteractionResult.SUCCESS;
        }

        if (this.isOwnedBy(player)) {
            if (player.isShiftKeyDown()) {
                switch (this.mode) {
                    case FOLLOW:
                        setMode(CompanionMode.STAY);
                        player.sendSystemMessage(Component.literal(localeService.translate(preferredLocale, "message.companions.mode_stay")));
                        break;
                    case STAY:
                        setMode(CompanionMode.DEFEND);
                        player.sendSystemMessage(Component.literal(localeService.translate(preferredLocale, "message.companions.mode_defend")));
                        break;
                    default:
                        setMode(CompanionMode.FOLLOW);
                        player.sendSystemMessage(Component.literal(localeService.translate(preferredLocale, "message.companions.mode_follow")));
                        break;
                }
                return InteractionResult.SUCCESS;
            }

            if (itemInHand.isEdible() && this.getHealth() < this.getMaxHealth()) {
                var foodProperties = itemInHand.getFoodProperties(this);
                if (foodProperties != null) {
                    this.heal((float) foodProperties.getNutrition());
                    if (!player.getAbilities().instabuild) {
                        itemInHand.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            String statusMessage = localeService.translate(preferredLocale, "dialogue.follow_ack");
            if (this.mode == CompanionMode.STAY) {
                statusMessage = localeService.translate(preferredLocale, "dialogue.stay_ack");
            }
            player.sendSystemMessage(Component.literal(statusMessage));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                ItemStack stack = this.inventory.getItem(i);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack);
                }
            }
            this.inventory.clearContent();
        }
        super.die(damageSource);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Player && target.getUUID().equals(owner.getUUID())) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isOwnedBy(owner)) {
            return false;
        }
        return super.wantsToAttack(target, owner);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob mate) {
        return null;
    }

    public InventorySnapshot createInventorySnapshot() {
        List<ItemSlot> slots = new ArrayList<>();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                slots.add(new ItemSlot(
                        ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(),
                        stack.getCount()
                ));
            }
        }
        return new InventorySnapshot(slots, this.inventory.getContainerSize());
    }

    public boolean tryRecall(Player owner) {
        if (owner == null) return false;
        Level currentLevel = this.level();

        if (currentLevel != owner.level()) {
            String msg = localeService.translate(preferredLocale, "dialogue.recall_blocked_dimension");
            owner.sendSystemMessage(Component.literal(msg));
            return false;
        }

        if (this.getTarget() != null && this.getTarget().isAlive()) {
            String msg = localeService.translate(preferredLocale, "dialogue.recall_blocked_combat");
            owner.sendSystemMessage(Component.literal(msg));
            return false;
        }

        long gameTime = currentLevel.getGameTime();
        if (gameTime - lastRecallGameTime < 60L) {
            return false;
        }

        BlockPos ownerPos = owner.blockPosition();
        BlockPos safePos = null;

        for (int dx = -2; dx <= 2 && safePos == null; dx++) {
            for (int dz = -2; dz <= 2 && safePos == null; dz++) {
                for (int dy = -1; dy <= 2 && safePos == null; dy++) {
                    BlockPos candidate = ownerPos.offset(dx, dy, dz);
                    BlockState floor = currentLevel.getBlockState(candidate);
                    BlockState feet = currentLevel.getBlockState(candidate.above());
                    BlockState head = currentLevel.getBlockState(candidate.above(2));

                    if (floor.isSolidRender(currentLevel, candidate) && feet.isAir() && head.isAir()) {
                        safePos = candidate.above();
                    }
                }
            }
        }

        if (safePos == null) {
            String msg = localeService.translate(preferredLocale, "dialogue.recall_blocked_hazard");
            owner.sendSystemMessage(Component.literal(msg));
            return false;
        }

        this.teleportTo(safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D);
        this.getNavigation().stop();
        this.setOrderedToSit(false);
        this.setMode(CompanionMode.FOLLOW);
        this.lastRecallGameTime = gameTime;

        String ack = localeService.translate(preferredLocale, "dialogue.recall_ack");
        owner.sendSystemMessage(Component.literal(ack));
        return true;
    }

    public void handleCommand(String command, Player sender) {
        handleCommandAsync(command, sender);
    }

    public void handleCommandAsync(String command, Player sender) {
        if (command == null || command.trim().isEmpty() || !this.isOwnedBy(sender)) return;

        CompanionProfile coreProfile = new CompanionProfile(
                this.getUUID(),
                this.getOwnerUUID(),
                this.getName().getString(),
                this.mode,
                Personality.BALANCED
        );

        this.hybridDialogueProvider.processAsync(command, this.preferredLocale, coreProfile, createInventorySnapshot())
                .thenAccept(response -> {
                    Runnable apply = () -> applyDialogueResponse(response, sender);
                    if (sender.getServer() != null) {
                        sender.getServer().execute(apply);
                    } else {
                        apply.run();
                    }
                });
    }

    private void applyDialogueResponse(com.thyagotoledo.companions.core.dialogue.DialogueResponse response, Player sender) {
        if (response == null || response.getIntent() == null) return;
        var intent = response.getIntent();

        switch (intent.getType()) {
            case FOLLOW_OWNER:
                this.activeWorkTask = null;
                setMode(CompanionMode.FOLLOW);
                break;
            case STAY:
                this.activeWorkTask = null;
                setMode(CompanionMode.STAY);
                break;
            case DEFEND:
                this.activeWorkTask = null;
                setMode(CompanionMode.DEFEND);
                break;
            case RECALL:
                this.activeWorkTask = null;
                tryRecall(sender);
                return;
            case REMOTE_VIEW:
                if (sender instanceof ServerPlayer sp) {
                    CompanionsNetwork.sendToPlayer(
                            sp,
                            new ClientboundFeedbackPacket(this.getUUID(), response.getSpeech(), true)
                    );
                }
                return;
            case REPORT_STATUS:
                String statusMsg = localeService.translate(
                        preferredLocale,
                        "dialogue.status_report",
                        String.valueOf((int) this.getHealth()),
                        String.valueOf((int) this.getMaxHealth()),
                        this.mode.name()
                );
                sender.sendSystemMessage(Component.literal(statusMsg));
                return;
            case CHOP_WOOD:
                int woodCount = intent.getQuantity() > 0 ? intent.getQuantity() : 16;
                String woodTarget = intent.getTarget() != null ? intent.getTarget() : "minecraft:oak_log";
                String dim = this.level().dimension().location().toString();
                WorkArea woodArea = new WorkArea(dim, this.blockPosition().getX(), this.blockPosition().getY(), this.blockPosition().getZ(), 16);
                this.setActiveWorkTask(new WorkTask("chop_wood", woodTarget, woodCount, ToolType.AXE, woodArea));
                setMode(CompanionMode.WORK);
                setOrderedToSit(false);
                break;
            case MINE_BLOCK:
                int mineCount = intent.getQuantity() > 0 ? intent.getQuantity() : 16;
                String mineTarget = intent.getTarget() != null ? intent.getTarget() : "minecraft:stone";
                String mdim = this.level().dimension().location().toString();
                WorkArea mineArea = new WorkArea(mdim, this.blockPosition().getX(), this.blockPosition().getY(), this.blockPosition().getZ(), 16);
                this.setActiveWorkTask(new WorkTask("mine_block", mineTarget, mineCount, ToolType.PICKAXE, mineArea));
                setMode(CompanionMode.WORK);
                setOrderedToSit(false);
                break;
            case ASSIST_SELECTED_QUEST:
                List<Quest> available = this.questService.getAvailableQuests(sender.getUUID());
                if (available.isEmpty()) {
                    String msg = localeService.translate(preferredLocale, "quest.not_found");
                    sender.sendSystemMessage(Component.literal(msg));
                    return;
                }
                Quest targetQuest = available.get(0);
                QuestPlanResult planResult = this.questPlanner.planQuest(
                        sender.getUUID(),
                        targetQuest.getId(),
                        createPlayerInventorySnapshot(sender),
                        createInventorySnapshot()
                );
                String questResponse;
                switch (planResult.getStatus()) {
                    case READY_TO_SUBMIT:
                        questResponse = localeService.translate(preferredLocale, "quest.ready", targetQuest.getTitle());
                        break;
                    case BLOCKED_DEPENDENCY:
                        questResponse = localeService.translate(preferredLocale, "quest.blocked.dependency", targetQuest.getTitle());
                        break;
                    case UNKNOWN_RECIPE_OR_MACHINE:
                        questResponse = localeService.translate(preferredLocale, "quest.unknown_process");
                        break;
                    case MISSING_ITEMS:
                    default:
                        StringBuilder missingStr = new StringBuilder();
                        for (var entry : planResult.getMissingItems().entrySet()) {
                            missingStr.append(entry.getValue()).append("x ").append(entry.getKey()).append(", ");
                        }
                        if (missingStr.length() > 2) missingStr.setLength(missingStr.length() - 2);
                        questResponse = localeService.translate(preferredLocale, "quest.missing", targetQuest.getTitle(), missingStr.toString());
                        break;
                }
                sender.sendSystemMessage(Component.literal(questResponse));
                return;
            default:
                break;
        }

        if (sender instanceof ServerPlayer sp) {
            CompanionsNetwork.sendToPlayer(
                    sp,
                    new ClientboundFeedbackPacket(this.getUUID(), response.getSpeech(), false)
            );
        } else {
            sender.sendSystemMessage(Component.literal(response.getSpeech()));
        }
    }
}
