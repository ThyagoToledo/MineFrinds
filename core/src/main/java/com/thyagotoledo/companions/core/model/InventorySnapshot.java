package com.thyagotoledo.companions.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class InventorySnapshot {
    private final List<ItemSlot> slots;
    private final int capacity;

    public InventorySnapshot(List<ItemSlot> slots, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        if (slots == null) {
            this.slots = Collections.emptyList();
        } else {
            this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        }
    }

    public List<ItemSlot> getSlots() {
        return slots;
    }

    public int getCapacity() {
        return capacity;
    }

    public int countItem(String itemId) {
        if (itemId == null) return 0;
        int total = 0;
        for (ItemSlot slot : slots) {
            if (Objects.equals(slot.getItemId(), itemId)) {
                total += slot.getCount();
            }
        }
        return total;
    }

    public boolean hasItem(String itemId, int minCount) {
        return countItem(itemId) >= minCount;
    }
}
