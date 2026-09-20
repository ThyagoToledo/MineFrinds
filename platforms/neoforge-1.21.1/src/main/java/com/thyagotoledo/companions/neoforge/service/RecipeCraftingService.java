package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.util.ArrayList;
import java.util.List;

/** Plans on copies; only a complete recipe chain is written back to the bag. */
public final class RecipeCraftingService {
    private RecipeCraftingService() { }

    public static boolean craft(ServerPlayer player, Item target, int count) {
        if (count < 1 || count > 64) return false;
        List<ItemStack> bag = new ArrayList<>();
        for (int i = 0; i < 36; i++) bag.add(player.getInventory().getItem(i).copy());
        List<RecipeHolder<CraftingRecipe>> recipes = player.serverLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING);
        boolean workbench = false;
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                player.blockPosition().offset(-3, -2, -3), player.blockPosition().offset(3, 2, 3))) {
            if (player.level().hasChunkAt(pos) && player.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)) {
                workbench = true;
                break;
            }
        }
        List<ItemStack> result = plan(bag, recipes, player.registryAccess(), player.level(), workbench, target, count);
        if (result == null) return false;
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, result.get(i));
        player.getInventory().setChanged();
        return true;
    }

    static List<ItemStack> plan(List<ItemStack> original, List<RecipeHolder<CraftingRecipe>> recipes,
            net.minecraft.core.HolderLookup.Provider registries, net.minecraft.world.level.Level level,
            boolean workbench, Item target, int amount) {
        if (amount < 1 || amount > 64) return null;
        List<ItemStack> bag = copy(original);
        java.util.Map<Item, List<RecipeHolder<CraftingRecipe>>> indexed = new java.util.HashMap<>();
        for (RecipeHolder<CraftingRecipe> holder : recipes) {
            CraftingRecipe recipe = holder.value();
            if ((recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) && !recipe.isSpecial()) {
                indexed.computeIfAbsent(recipe.getResultItem(registries).getItem(), key -> new ArrayList<>()).add(holder);
            }
        }
        int[] budget = {256};
        int wanted = count(bag, target) + amount;
        while (count(bag, target) < wanted) {
            if (!produce(registries, level, workbench, bag, target, indexed, 0, budget)) return null;
        }
        return bag;
    }

    private static boolean produce(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.world.level.Level level,
                                   boolean workbench, List<ItemStack> bag, Item target,
                                   java.util.Map<Item, List<RecipeHolder<CraftingRecipe>>> recipes, int depth, int[] budget) {
        if (depth > 4 || --budget[0] < 0) return false;
        for (RecipeHolder<CraftingRecipe> holder : recipes.getOrDefault(target, java.util.Collections.emptyList())) {
            CraftingRecipe recipe = holder.value();
            if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;
            if (!workbench && !recipe.canCraftInDimensions(2, 2)) continue;
            if (recipe.isSpecial() || !recipe.getResultItem(registries).is(target)) continue;
            if (--budget[0] < 0) return false;
            List<ItemStack> trial = copy(bag);
            List<ItemStack> grid = new ArrayList<>();
            boolean valid = true;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) { grid.add(ItemStack.EMPTY); continue; }
                int slot = find(trial, ingredient);
                if (slot < 0) {
                    ItemStack[] alternatives = ingredient.getItems();
                    for (int a = 0; a < Math.min(8, alternatives.length) && slot < 0; a++) {
                        List<ItemStack> attempt = copy(trial);
                        if (produce(registries, level, workbench, attempt, alternatives[a].getItem(), recipes, depth + 1, budget)) {
                            trial = attempt;
                            slot = find(trial, ingredient);
                        }
                    }
                }
                if (slot < 0) { valid = false; break; }
                grid.add(trial.get(slot).split(1));
            }
            if (!valid || grid.isEmpty()) continue;
            int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            int height = recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : 3;
            while (grid.size() < width * height) grid.add(ItemStack.EMPTY);
            CraftingInput input = CraftingInput.of(width, height, grid);
            if (!recipe.matches(input, level)) continue;
            ItemStack output = recipe.assemble(input, registries);
            if (output.isEmpty() || !output.is(target) || !insert(trial, output.copy())) continue;
            for (ItemStack remainder : recipe.getRemainingItems(input)) {
                if (!insert(trial, remainder.copy())) { valid = false; break; }
            }
            if (valid) { bag.clear(); bag.addAll(trial); return true; }
        }
        return false;
    }

    private static List<ItemStack> copy(List<ItemStack> bag) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : bag) result.add(stack.copy());
        return result;
    }

    private static int find(List<ItemStack> bag, Ingredient ingredient) {
        for (int i = 0; i < bag.size(); i++) if (!bag.get(i).isEmpty() && ingredient.test(bag.get(i))) return i;
        return -1;
    }

    private static int count(List<ItemStack> bag, Item item) {
        int count = 0;
        for (ItemStack stack : bag) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    private static boolean insert(List<ItemStack> bag, ItemStack remaining) {
        if (remaining.isEmpty()) return true;
        for (ItemStack stack : bag) {
            if (ItemStack.isSameItemSameComponents(stack, remaining)) {
                int moved = Math.min(remaining.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved); remaining.shrink(moved);
                if (remaining.isEmpty()) return true;
            }
        }
        for (int i = 0; i < bag.size(); i++) {
            if (bag.get(i).isEmpty()) {
                bag.set(i, remaining.split(Math.min(remaining.getCount(), remaining.getMaxStackSize())));
                if (remaining.isEmpty()) return true;
            }
        }
        return false;
    }
}
