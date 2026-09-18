package com.thyagotoledo.companions.core.planner;

import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;

import java.util.Collections;
import java.util.List;

public class CraftingPlanner {
    private final RecipeCatalog recipeCatalog;

    public CraftingPlanner(RecipeCatalog recipeCatalog) {
        this.recipeCatalog = recipeCatalog != null ? recipeCatalog : new RecipeCatalog();
    }

    public boolean canCraft(String targetItemId, int quantity, InventorySnapshot inventory) {
        List<ItemSlot> missing = recipeCatalog.calculateMissingIngredients(targetItemId, quantity, inventory);
        return missing.isEmpty();
    }

    public List<ItemSlot> planCraftingChain(String targetItemId, int quantity, InventorySnapshot currentInventory) {
        RecipeRequirement recipe = recipeCatalog.getRecipe(targetItemId);
        if (recipe == null) {
            return Collections.emptyList();
        }
        return recipeCatalog.calculateMissingIngredients(targetItemId, quantity, currentInventory);
    }

    public RecipeCatalog getRecipeCatalog() {
        return recipeCatalog;
    }
}
