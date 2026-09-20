package com.thyagotoledo.companions.neoforge.service;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecipeCraftingServiceTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    private RecipeHolder<CraftingRecipe> recipe(String name, Item input, Item output, int count) {
        return new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("test", name),
                new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(output, count),
                        NonNullList.of(Ingredient.EMPTY, Ingredient.of(input))));
    }
    private List<ItemStack> bag(Item item, int count, int slots) {
        List<ItemStack> bag = new ArrayList<>();
        bag.add(new ItemStack(item, count));
        while (bag.size() < slots) bag.add(ItemStack.EMPTY);
        return bag;
    }
    @Test void followsLoadedRecipeChainWithoutMutatingOriginal() {
        var original = bag(Items.OAK_LOG, 1, 4);
        var recipes = List.of(recipe("planks", Items.OAK_LOG, Items.OAK_PLANKS, 4),
                recipe("sticks", Items.OAK_PLANKS, Items.STICK, 2));
        var result = RecipeCraftingService.plan(original, recipes, null, null, false, Items.STICK, 3);
        assertNotNull(result);
        assertEquals(4, result.stream().filter(s -> s.is(Items.STICK)).mapToInt(ItemStack::getCount).sum());
        assertEquals(2, result.stream().filter(s -> s.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum());
        assertEquals(1, original.get(0).getCount());
        assertTrue(original.get(0).is(Items.OAK_LOG));
    }
    @Test void missingIngredientsAndFullBagLeaveOriginalIntact() {
        var recipes = List.of(recipe("planks", Items.OAK_LOG, Items.OAK_PLANKS, 4));
        var original = bag(Items.OAK_LOG, 2, 1);
        assertNull(RecipeCraftingService.plan(original, recipes, null, null, false, Items.OAK_PLANKS, 1));
        assertEquals(2, original.get(0).getCount());
        assertNull(RecipeCraftingService.plan(original, recipes, null, null, false, Items.IRON_PICKAXE, 1));
    }
    @Test void largeRecipeNeedsWorkbench() {
        var pattern = ShapedRecipePattern.of(Map.of('p', Ingredient.of(Items.OAK_PLANKS)), "ppp");
        RecipeHolder<CraftingRecipe> recipe = new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("test", "large"),
                new ShapedRecipe("", CraftingBookCategory.MISC, pattern, new ItemStack(Items.STICK)));
        var original = bag(Items.OAK_PLANKS, 3, 2);
        assertNull(RecipeCraftingService.plan(original, List.of(recipe), null, null, false, Items.STICK, 1));
        assertNotNull(RecipeCraftingService.plan(original, List.of(recipe), null, null, true, Items.STICK, 1));
    }
    @Test void recipeRemainderIsPreserved() {
        var result = RecipeCraftingService.plan(bag(Items.MILK_BUCKET, 1, 2),
                List.of(recipe("milk", Items.MILK_BUCKET, Items.BREAD, 1)), null, null, false, Items.BREAD, 1);
        assertNotNull(result);
        assertEquals(1, result.stream().filter(s -> s.is(Items.BUCKET)).mapToInt(ItemStack::getCount).sum());
    }
    @Test void cyclicRecipeCannotFabricateMissingResources() {
        var recipes = List.of(recipe("a", Items.STICK, Items.OAK_PLANKS, 1),
                recipe("b", Items.OAK_PLANKS, Items.STICK, 1));
        assertNull(RecipeCraftingService.plan(bag(Items.DIRT, 1, 2), recipes, null, null, false, Items.STICK, 1));
    }
}
