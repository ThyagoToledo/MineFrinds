package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Uses loaded item attributes/components rather than material names. */
public final class EquipmentPolicy {
    private EquipmentPolicy() { }
    private static double attribute(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute, double base) {
        double[] values = {0, 0, 1};
        stack.forEachModifier(slot, (key, modifier) -> {
            if (!key.equals(attribute)) return;
            switch (modifier.operation()) {
                case ADD_VALUE -> values[0] += modifier.amount();
                case ADD_MULTIPLIED_BASE -> values[1] += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> values[2] *= 1 + modifier.amount();
            }
        });
        return (base + values[0]) * (1 + values[1]) * values[2];
    }
    private static double condition(ItemStack stack) {
        return stack.isDamageableItem() ? Math.max(0, 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage()) : 1;
    }
    public static double weapon(ItemStack stack) {
        if (stack.isEmpty()) return 4;
        double damage = attribute(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1);
        if (damage <= 1) return 4;
        double speed = attribute(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED, 4);
        return damage * Math.max(0.1, speed) + (stack.isEnchanted() ? 0.2 : 0) + condition(stack) * 0.05;
    }
    public static double armor(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return 0;
        double armor = attribute(stack, slot, Attributes.ARMOR, 0);
        if (armor <= 0) return 0;
        return armor * 10 + attribute(stack, slot, Attributes.ARMOR_TOUGHNESS, 0) * 5
                + attribute(stack, slot, Attributes.KNOCKBACK_RESISTANCE, 0) * 10
                + (stack.isEnchanted() ? 0.5 : 0) + condition(stack) * 0.1;
    }
    public static double tool(ItemStack stack, BlockState state) {
        if (state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) return -1;
        return stack.getDestroySpeed(state) + (stack.isEnchanted() ? 0.1 : 0) + condition(stack) * 0.01;
    }
}
