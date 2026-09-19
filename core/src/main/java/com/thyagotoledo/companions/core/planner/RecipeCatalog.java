package com.thyagotoledo.companions.core.planner;

import com.thyagotoledo.companions.core.model.InventorySnapshot;
import com.thyagotoledo.companions.core.model.ItemSlot;

import java.util.*;

public final class RecipeCatalog {
    private final Map<String, List<RecipeRequirement>> recipesByOutput = new HashMap<>();

    private static final RecipeCatalog DEFAULT_INSTANCE = new RecipeCatalog();

    public static RecipeCatalog getDefault() {
        return DEFAULT_INSTANCE;
    }

    public static boolean hasRecipe(String targetItemId) {
        if (targetItemId == null) return false;
        String id = targetItemId.startsWith("minecraft:") ? targetItemId : "minecraft:" + targetItemId;
        return DEFAULT_INSTANCE.recipesByOutput.containsKey(id) || DEFAULT_INSTANCE.recipesByOutput.containsKey(targetItemId);
    }

    public RecipeCatalog() {
        registerDefaultRecipes();
    }

    private void registerDefaultRecipes() {
        // Tabuas
        registerRecipe(new RecipeRequirement("planks", new ItemSlot("minecraft:oak_planks", 4),
                Collections.singletonList(new ItemSlot("minecraft:oak_log", 1))));
        // Gravetos
        registerRecipe(new RecipeRequirement("stick", new ItemSlot("minecraft:stick", 4),
                Collections.singletonList(new ItemSlot("minecraft:oak_planks", 2))));
        // Bancada de trabalho
        registerRecipe(new RecipeRequirement("crafting_table", new ItemSlot("minecraft:crafting_table", 1),
                Collections.singletonList(new ItemSlot("minecraft:oak_planks", 4))));
        // Bau
        registerRecipe(new RecipeRequirement("chest", new ItemSlot("minecraft:chest", 1),
                Collections.singletonList(new ItemSlot("minecraft:oak_planks", 8))));
        // Tochas
        registerRecipe(new RecipeRequirement("torch", new ItemSlot("minecraft:torch", 4),
                Arrays.asList(new ItemSlot("minecraft:coal", 1), new ItemSlot("minecraft:stick", 1))));
        // Picareta de madeira
        registerRecipe(new RecipeRequirement("wooden_pickaxe", new ItemSlot("minecraft:wooden_pickaxe", 1),
                Arrays.asList(new ItemSlot("minecraft:oak_planks", 3), new ItemSlot("minecraft:stick", 2))));
        // Picareta de pedra
        registerRecipe(new RecipeRequirement("stone_pickaxe", new ItemSlot("minecraft:stone_pickaxe", 1),
                Arrays.asList(new ItemSlot("minecraft:cobblestone", 3), new ItemSlot("minecraft:stick", 2))));
        // Picareta de ferro
        registerRecipe(new RecipeRequirement("iron_pickaxe", new ItemSlot("minecraft:iron_pickaxe", 1),
                Arrays.asList(new ItemSlot("minecraft:iron_ingot", 3), new ItemSlot("minecraft:stick", 2))));
        // Espada de madeira
        registerRecipe(new RecipeRequirement("wooden_sword", new ItemSlot("minecraft:wooden_sword", 1),
                Arrays.asList(new ItemSlot("minecraft:oak_planks", 2), new ItemSlot("minecraft:stick", 1))));
        // Espada de pedra
        registerRecipe(new RecipeRequirement("stone_sword", new ItemSlot("minecraft:stone_sword", 1),
                Arrays.asList(new ItemSlot("minecraft:cobblestone", 2), new ItemSlot("minecraft:stick", 1))));
        // Espada de ferro
        registerRecipe(new RecipeRequirement("iron_sword", new ItemSlot("minecraft:iron_sword", 1),
                Arrays.asList(new ItemSlot("minecraft:iron_ingot", 2), new ItemSlot("minecraft:stick", 1))));
        // Fornalha
        registerRecipe(new RecipeRequirement("furnace", new ItemSlot("minecraft:furnace", 1),
                Collections.singletonList(new ItemSlot("minecraft:cobblestone", 8))));
        // Pao
        registerRecipe(new RecipeRequirement("bread", new ItemSlot("minecraft:bread", 1),
                Collections.singletonList(new ItemSlot("minecraft:wheat", 3))));
    }

    public void registerRecipe(RecipeRequirement recipe) {
        if (recipe == null) return;
        recipesByOutput.computeIfAbsent(recipe.getOutput().getItemId(), k -> new ArrayList<>()).add(recipe);
    }

    public void clear() {
        recipesByOutput.clear();
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
