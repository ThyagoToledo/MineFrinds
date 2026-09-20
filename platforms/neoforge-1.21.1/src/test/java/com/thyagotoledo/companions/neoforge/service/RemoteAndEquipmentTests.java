package com.thyagotoledo.companions.neoforge.service;

import com.thyagotoledo.companions.neoforge.entity.CompanionSavedData;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RemoteAndEquipmentTests {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void weaponAndArmorUseActualAttributes() {
        assertTrue(EquipmentPolicy.weapon(new ItemStack(Items.DIAMOND_SWORD)) > EquipmentPolicy.weapon(new ItemStack(Items.WOODEN_SWORD)));
        assertTrue(EquipmentPolicy.armor(new ItemStack(Items.DIAMOND_CHESTPLATE), EquipmentSlot.CHEST)
                > EquipmentPolicy.armor(new ItemStack(Items.LEATHER_CHESTPLATE), EquipmentSlot.CHEST));
        assertEquals(0, EquipmentPolicy.armor(new ItemStack(Items.DIAMOND_SWORD), EquipmentSlot.HEAD));
    }
    @Test void equipmentCheckpointSurvivesNbtAndDoesNotAliasCallerData() {
        CompanionSavedData data = new CompanionSavedData();
        UUID owner = UUID.randomUUID();
        CompoundTag equipment = new CompoundTag(); equipment.putInt("level", 12);
        data.saveEquipment(owner, equipment); equipment.putInt("level", 99);
        CompanionSavedData loaded = CompanionSavedData.load(data.save(new CompoundTag(), null), null);
        assertEquals(12, loaded.getEquipment(owner).getInt("level"));
        assertNull(loaded.getEquipment(UUID.randomUUID()));
        CompoundTag returned = loaded.getEquipment(owner); returned.putInt("level", 1);
        assertEquals(12, loaded.getEquipment(owner).getInt("level"));
    }
    @Test void remoteInputRoundTripAndBounds() {
        UUID token = UUID.randomUUID();
        var input = new RemoteViewService.Input(token, 3, 1, -1, 80, -20, true, true, false, false, 4);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RemoteViewService.Input.CODEC.encode(buffer, input);
            assertEquals(input, RemoteViewService.Input.CODEC.decode(buffer));
        } finally { buffer.release(); }
        assertTrue(input.valid());
        assertFalse(new RemoteViewService.Input(token, 0, Float.NaN, 0, 0, 0, false, false, false, false, 0).valid());
        assertFalse(new RemoteViewService.Input(token, 0, 2, 0, 0, 0, false, false, false, false, 0).valid());
        assertFalse(new RemoteViewService.Input(token, 0, 0, 0, 0, 0, false, false, false, false, 9).valid());
    }
}
