package com.thyagotoledo.companions.core.model;

import java.util.Objects;
import java.util.UUID;

public final class CompanionProfile {
    private final UUID id;
    private final UUID ownerId;
    private final String name;
    private final CompanionMode mode;
    private final Personality personality;

    public CompanionProfile(UUID id, UUID ownerId, String name, CompanionMode mode, Personality personality) {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (ownerId == null) throw new IllegalArgumentException("ownerId cannot be null");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name cannot be empty");
        this.id = id;
        this.ownerId = ownerId;
        this.name = name.trim();
        this.mode = mode != null ? mode : CompanionMode.FOLLOW;
        this.personality = personality != null ? personality : Personality.BALANCED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public CompanionMode getMode() {
        return mode;
    }

    public Personality getPersonality() {
        return personality;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompanionProfile that = (CompanionProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
