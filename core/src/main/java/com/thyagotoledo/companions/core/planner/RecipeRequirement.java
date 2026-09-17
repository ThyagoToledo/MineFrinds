package com.thyagotoledo.companions.core.planner;

import com.thyagotoledo.companions.core.model.ItemSlot;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RecipeRequirement {
    private final String recipeId;
    private final ItemSlot output;
    private final List<ItemSlot> inputs;

    public RecipeRequirement(String recipeId, ItemSlot output, List<ItemSlot> inputs) {
        if (recipeId == null || output == null || inputs == null) {
            throw new IllegalArgumentException("RecipeRequirement fields cannot be null");
        }
        this.recipeId = recipeId;
        this.output = output;
        this.inputs = Collections.unmodifiableList(inputs);
    }

    public String getRecipeId() {
        return recipeId;
    }

    public ItemSlot getOutput() {
        return output;
    }

    public List<ItemSlot> getInputs() {
        return inputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeRequirement that = (RecipeRequirement) o;
        return Objects.equals(recipeId, that.recipeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipeId);
    }
}
