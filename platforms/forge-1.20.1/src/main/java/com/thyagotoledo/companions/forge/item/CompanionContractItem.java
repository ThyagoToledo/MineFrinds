package com.thyagotoledo.companions.forge.item;

import com.thyagotoledo.companions.core.model.CompanionMode;
import com.thyagotoledo.companions.forge.entity.CompanionEntity;
import com.thyagotoledo.companions.forge.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class CompanionContractItem extends Item {
    public CompanionContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
        CompanionEntity companion = ModEntities.COMPANION.get().create(level);
        if (companion != null) {
            companion.moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, player.getYRot(), 0.0F);
            companion.tame(player);
            companion.setMode(CompanionMode.FOLLOW);
            level.addFreshEntity(companion);

            String message = companion.getLocaleService().translate(companion.getPreferredLocale(), "message.companions.recruited");
            player.sendSystemMessage(Component.literal(message));

            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof CompanionEntity companion) {
            if (!player.level().isClientSide) {
                String locale = companion.getPreferredLocale();
                String modeName = companion.getMode().name();
                String info = "Companion [Mode: " + modeName + ", Locale: " + locale + ", Health: " + (int) companion.getHealth() + "/" + (int) companion.getMaxHealth() + "]";
                player.sendSystemMessage(Component.literal(info));
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }
}
