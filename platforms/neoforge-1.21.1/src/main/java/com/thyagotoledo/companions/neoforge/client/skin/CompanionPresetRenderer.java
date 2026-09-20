package com.thyagotoledo.companions.neoforge.client.skin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** Preset identity comes from server GameProfile properties, never the player's display name. */
public final class CompanionPresetRenderer {
    public static final ResourceLocation RIMURU_TEXTURE = ResourceLocation.fromNamespaceAndPath("companions", "textures/entity/rimuru_demonlord_v1.png");
    private static PlayerRenderer rimuruRenderer;
    private static boolean rendering;

    public static void create(EntityRenderersEvent.AddLayers event) {
        rimuruRenderer = new PlayerRenderer(event.getContext(), false) {
            @Override
            public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
                var skin = SkinCacheManager.getLoadedSkin("rimuru");
                return skin != null ? skin.getTextureLocation() : RIMURU_TEXTURE;
            }
        };
    }

    public static void render(RenderPlayerEvent.Pre event) {
        if (rendering || rimuruRenderer == null || !(event.getEntity() instanceof AbstractClientPlayer player)) return;
        var properties = player.getGameProfile().getProperties().get("companions_preset");
        boolean isRimuru = (properties != null && properties.stream().anyMatch(property -> "rimuru".equalsIgnoreCase(property.value())))
                || "Rimuru".equalsIgnoreCase(player.getGameProfile().getName());
        if (!isRimuru) return;
        if (player.getSkin().secure()) return;

        event.setCanceled(true);
        rendering = true;
        try {
            rimuruRenderer.render(player, player.getYRot(), event.getPartialTick(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            rendering = false;
        }
    }
}
