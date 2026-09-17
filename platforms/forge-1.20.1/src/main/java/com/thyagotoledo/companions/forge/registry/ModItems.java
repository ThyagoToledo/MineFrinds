package com.thyagotoledo.companions.forge.registry;

import com.thyagotoledo.companions.forge.CompanionsForgeMod;
import com.thyagotoledo.companions.forge.item.CompanionContractItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, CompanionsForgeMod.MODID);

    public static final RegistryObject<Item> COMPANION_CONTRACT =
            REGISTRY.register("companion_contract", () -> new CompanionContractItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> COMPANION_SPAWN_EGG =
            REGISTRY.register("companion_spawn_egg", () -> new ForgeSpawnEggItem(
                    ModEntities.COMPANION,
                    0x3c44aa,
                    0xd8833b,
                    new Item.Properties()
            ));

    private ModItems() {
    }
}
