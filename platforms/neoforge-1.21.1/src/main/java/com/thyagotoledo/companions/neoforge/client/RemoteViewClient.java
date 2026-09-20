package com.thyagotoledo.companions.neoforge.client;

import com.thyagotoledo.companions.neoforge.service.RemoteViewService;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RemoteViewClient {
    private static RemoteViewService.State state;
    private static long sequence;
    private static int selectedSlot;
    public static void accept(RemoteViewService.State incoming) { state = incoming.active() ? incoming : null; sequence = 0; selectedSlot = 0; }
    public static boolean active() { return state != null; }
    public static String label() {
        return state == null ? "" : state.control() ? "Controlando NPC | WASD + mouse | Slot " + (selectedSlot + 1) + " | Shift: sair" : "Observando NPC | Shift: sair";
    }
    public static void scroll(double delta) { if (state != null && state.control()) selectedSlot = Math.floorMod(selectedSlot - (int)Math.signum(delta), 9); }
    public static void beforeTick() {
        var mc = Minecraft.getInstance();
        if (state == null || mc.player == null || mc.screen != null) return;
        for (int slot = 0; slot < 9; slot++) {
            while (mc.options.keyHotbarSlots[slot].consumeClick()) if (state.control()) selectedSlot = slot;
        }
        while (mc.options.keyInventory.consumeClick()) mc.player.connection.sendCommand("companion inventory");
    }
    public static void clear() { state = null; sequence = 0; }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (state == null || mc.player == null) return;
        if (mc.level != null) {
            var npc = mc.level.getEntity(state.entityId());
            if (npc != null && mc.getCameraEntity() != npc) mc.setCameraEntity(npc);
        }
        if (mc.player.tickCount % 2 != 0) return;
        boolean exit = mc.options.keyShift.isDown();
        boolean enabled = state.control() && mc.screen == null;
        float forward = enabled ? (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0) : 0;
        float sideways = enabled ? (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0) : 0;
        PacketDistributor.sendToServer(new RemoteViewService.Input(state.token(), sequence++, forward, sideways,
                net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()), mc.player.getXRot(), enabled && mc.options.keyJump.isDown(),
                enabled && mc.options.keyAttack.isDown(), enabled && mc.options.keyUse.isDown(), exit, selectedSlot));
    }
}
