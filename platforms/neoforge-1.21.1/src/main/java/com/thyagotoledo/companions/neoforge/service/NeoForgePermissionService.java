package com.thyagotoledo.companions.neoforge.service;

import com.thyagotoledo.companions.core.permissions.DefaultPermissionService;
import com.thyagotoledo.companions.core.permissions.PermissionService;

import java.util.UUID;

/**
 * Adaptador de servico de permissoes para NeoForge 21.1.248 (Minecraft 1.21.1).
 * Respeita protecoes de terreno de FTB Chunks NeoForge e OPAC (Open Parties and Claims).
 */
public class NeoForgePermissionService implements PermissionService {

    private final DefaultPermissionService fallbackService;

    public NeoForgePermissionService() {
        this(new DefaultPermissionService());
    }

    public NeoForgePermissionService(DefaultPermissionService fallbackService) {
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
