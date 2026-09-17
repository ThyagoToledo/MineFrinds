package com.thyagotoledo.companions.core.dialogue;

import java.util.Objects;

public final class Intent {
    private final IntentType type;
    private final String target;
    private final int quantity;

    public Intent(IntentType type, String target, int quantity) {
        this.type = type != null ? type : IntentType.UNKNOWN_OR_BLOCKED;
        this.target = target;
        this.quantity = Math.max(0, quantity);
    }

    public Intent(IntentType type) {
        this(type, null, 0);
    }

    public IntentType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Intent intent = (Intent) o;
        return quantity == intent.quantity && type == intent.type && Objects.equals(target, intent.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, target, quantity);
    }

    @Override
    public String toString() {
        return "Intent{" + "type=" + type + ", target=\"" + target + "\", quantity=" + quantity + "}";
    }
}
