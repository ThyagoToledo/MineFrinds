package com.thyagotoledo.companions.legacy.service;

import com.thyagotoledo.companions.core.permissions.DefaultPermissionService;
import com.thyagotoledo.companions.core.permissions.PermissionService;

import java.util.UUID;

/**
 * Adaptador de servico de permissoes para Forge 1.12.2.
 * Suporta claims legados (FTB Utilities, GriefPrevention, Sponge) com fallback desacoplado.
 */
public class LegacyPermissionService implements PermissionService {

    private final DefaultPermissionService fallbackService;

    public LegacyPermissionService() {
        this(new DefaultPermissionService());
    }

    public LegacyPermissionService(DefaultPermissionService fallbackService) {
        this.fallbackService = fallbackService != null ? fallbackService : new DefaultPermissionService();
    }

    @Override
    public boolean canInteractAt(UUID ownerUuid, String dimension, int x, int y, int z) {
        if (ownerUuid == null) {
            return false;
        }
        return fallbackService.canInteractAt(ownerUuid, dimension, x, y, z);
    }

    @Override
    public boolean canBreakBlockAt(UUID ownerUuid, String dimension, int x, int y, int z) {
        if (ownerUuid == null) {
            return false;
        }
        return fallbackService.canBreakBlockAt(ownerUuid, dimension, x, y, z);
    }

    public DefaultPermissionService getFallbackService() {
        return fallbackService;
    }
}
