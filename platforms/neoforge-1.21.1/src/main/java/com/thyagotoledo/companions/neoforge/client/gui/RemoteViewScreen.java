package com.thyagotoledo.companions.neoforge.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RemoteViewScreen extends Screen {
    private final Screen parent;
    public RemoteViewScreen(Screen parent) { super(Component.literal("Visao do companheiro / Companion view")); this.parent = parent; }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Observar / Observe"), button -> start("observe"))
                .bounds(width / 2 - 105, height / 2 - 30, 210, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Controlar / Control"), button -> start("control"))
                .bounds(width / 2 - 105, height / 2, 210, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voltar / Back"), button -> onClose())
                .bounds(width / 2 - 105, height / 2 + 55, 210, 20).build());
    }
    private void start(String mode) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.connection.sendCommand("companion view " + mode);
            minecraft.setScreen(null);
        }
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 62, 0xFFFFFF);
        graphics.drawCenteredString(font, "Shift: sair e retornar ao seu local", width / 2, height / 2 + 30, 0xBBBBBB);
    }
}
