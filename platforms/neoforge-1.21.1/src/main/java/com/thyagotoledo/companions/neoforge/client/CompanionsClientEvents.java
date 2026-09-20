package com.thyagotoledo.companions.neoforge.client;

import com.thyagotoledo.companions.neoforge.CompanionsNeoForgeMod;
import com.thyagotoledo.companions.neoforge.client.gui.CompanionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Eventos client-side para abertura do painel de controle do companheiro por tecla,
 * comando no chat e botao estilizado no inventario (estilo FTB Quests / FTB Teams).
 */
public class CompanionsClientEvents {

    @SubscribeEvent
    public static void onPlayerRender(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event) {
        com.thyagotoledo.companions.neoforge.client.skin.CompanionPresetRenderer.render(event);
    }

    public static void onAddLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        com.thyagotoledo.companions.neoforge.client.skin.CompanionPresetRenderer.create(event);
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CompanionsKeyMappings.KEY_OPEN_GUI);
    }

    /**
     * Adiciona o icone/botao no canto do inventario do jogador (Sobrevivencia e Criativo).
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof InventoryScreen) {
            int left = (screen.width - 176) / 2;
            int top = (screen.height - 166) / 2;

            Button btn = Button.builder(Component.literal("C"), b -> {
                        Minecraft.getInstance().setScreen(new CompanionScreen(screen));
                    })
                    .bounds(left - 20, top + 4, 18, 18)
                    .tooltip(Tooltip.create(Component.literal("Companions - Painel de Controle (Tecla C)")))
                    .build();
            event.addListener(btn);

        } else if (screen instanceof CreativeModeInventoryScreen) {
            int left = (screen.width - 195) / 2;
            int top = (screen.height - 136) / 2;

            Button btn = Button.builder(Component.literal("C"), b -> {
                        Minecraft.getInstance().setScreen(new CompanionScreen(screen));
                    })
                    .bounds(left - 20, top + 4, 18, 18)
                    .tooltip(Tooltip.create(Component.literal("Companions - Painel de Controle (Tecla C)")))
                    .build();
            event.addListener(btn);
        }
    }

    /**
     * Detecta o pressionamento da tecla de atalho configurada (padrao: C).
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        RemoteViewClient.tick();
        while (CompanionsKeyMappings.KEY_OPEN_GUI.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new CompanionScreen(null));
            }
        }
    }

    @SubscribeEvent
    public static void remoteInputBeforeTick(ClientTickEvent.Pre event) { RemoteViewClient.beforeTick(); }

    @SubscribeEvent
    public static void remoteScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent event) {
        if (RemoteViewClient.active()) { RemoteViewClient.scroll(event.getScrollDeltaY()); event.setCanceled(true); }
    }

    @SubscribeEvent
    public static void remoteOverlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        if (!RemoteViewClient.active()) return;
        var mc = Minecraft.getInstance();
        var graphics = event.getGuiGraphics();
        graphics.drawString(mc.font, RemoteViewClient.label(), 8, 8, 0xFFFFFF, true);
        if (mc.getCameraEntity() instanceof net.minecraft.world.entity.LivingEntity npc) {
            int x = 8;
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET}) {
                graphics.renderItem(npc.getItemBySlot(slot), x, 24); x += 20;
            }
            graphics.drawString(mc.font, "HP " + Math.round(npc.getHealth()) + "/" + Math.round(npc.getMaxHealth()), x + 4, 28, 0xFFFFFF, true);
        }
    }

    @SubscribeEvent
    public static void onMovement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        if (!RemoteViewClient.active()) return;
        event.getInput().forwardImpulse = 0;
        event.getInput().leftImpulse = 0;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteraction(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (RemoteViewClient.active()) { event.setCanceled(true); event.setSwingHand(false); }
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CompanionScreen.clearSessionState();
        RemoteViewClient.clear();
    }

    /**
     * Intercepta comandos de chat digitados pelo jogador (/companion, /companion gui, /companions).
     */
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String msg = event.getMessage();
        if (msg != null) {
            String clean = msg.trim().toLowerCase();
            if (clean.equals("/companion") || clean.equals("/companion gui") || clean.equals("/companions")) {
                event.setCanceled(true);
                Minecraft.getInstance().tell(() -> {
                    Minecraft.getInstance().setScreen(new CompanionScreen(null));
                });
            }
        }
    }
}
