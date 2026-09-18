package com.thyagotoledo.companions.core.planner;

import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;

import java.util.*;

public final class RecipeCatalog {
    private final Map<String, List<RecipeRequirement>> recipesByOutput = new HashMap<>();

    public void registerRecipe(RecipeRequirement recipe) {
        if (recipe == null) return;
        recipesByOutput.computeIfAbsent(recipe.getOutput().getItemId(), k -> new ArrayList<>()).add(recipe);
    }

    public RecipeRequirement getRecipe(String targetItemId) {
        List<RecipeRequirement> list = recipesByOutput.get(targetItemId);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public List<RecipeRequirement> getRecipes(String targetItemId) {
        List<RecipeRequirement> list = recipesByOutput.get(targetItemId);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<ItemSlot> calculateMissingIngredients(String targetItemId, int requiredCount, InventorySnapshot inventory) {
        int inInventory = inventory.countItem(targetItemId);
        int remainingNeeded = requiredCount - inInventory;
        if (remainingNeeded <= 0) {
            return Collections.emptyList();
        }

        List<RecipeRequirement> options = recipesByOutput.get(targetItemId);
        if (options == null || options.isEmpty()) {
            return Collections.singletonList(new ItemSlot(targetItemId, remainingNeeded));
        }

        RecipeRequirement chosen = options.get(0);
        int outputPerCraft = Math.max(1, chosen.getOutput().getCount());
        int craftOperations = (remainingNeeded + outputPerCraft - 1) / outputPerCraft;

        List<ItemSlot> missing = new ArrayList<>();
        for (ItemSlot input : chosen.getInputs()) {
            int totalNeeded = input.getCount() * craftOperations;
            int available = inventory.countItem(input.getItemId());
            int diff = totalNeeded - available;
            if (diff > 0) {
                missing.add(new ItemSlot(input.getItemId(), diff));
            }
        }
        return missing;
    }
}
