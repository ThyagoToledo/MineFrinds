package com.thyagotoledo.companions.core.model;

import java.util.Objects;

public final class ItemSlot {
    private final String itemId;
    private final int count;

    public ItemSlot(String itemId, int count) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId cannot be null or empty");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
        this.itemId = itemId.trim();
        this.count = count;
    }

    public String getItemId() {
        return itemId;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemSlot itemSlot = (ItemSlot) o;
        return count == itemSlot.count && Objects.equals(itemId, itemSlot.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, count);
    }

    @Override
    public String toString() {
        return itemId + " x" + count;
    }
}
