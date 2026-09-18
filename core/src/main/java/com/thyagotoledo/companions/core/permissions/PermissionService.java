package com.thyagotoledo.companions.core.permissions;

import java.util.UUID;

public interface PermissionService {
    boolean canInteractAt(UUID ownerUuid, String dimension, int x, int y, int z);
    boolean canBreakBlockAt(UUID ownerUuid, String dimension, int x, int y, int z);
}
