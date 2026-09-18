package com.thyagotoledo.companions.core.permissions;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DefaultPermissionService implements PermissionService {
    private final Set<String> restrictedAreas = new HashSet<>();

    public void addRestrictedArea(String dimension, int x, int y, int z) {
        restrictedAreas.add(key(dimension, x, y, z));
    }

    public void removeRestrictedArea(String dimension, int x, int y, int z) {
        restrictedAreas.remove(key(dimension, x, y, z));
    }

    @Override
    public boolean canInteractAt(UUID ownerUuid, String dimension, int x, int y, int z) {
        return !restrictedAreas.contains(key(dimension, x, y, z));
    }

    @Override
    public boolean canBreakBlockAt(UUID ownerUuid, String dimension, int x, int y, int z) {
        return !restrictedAreas.contains(key(dimension, x, y, z));
    }

    private String key(String dimension, int x, int y, int z) {
        return (dimension != null ? dimension : "overworld") + ":" + x + "," + y + "," + z;
    }
}
