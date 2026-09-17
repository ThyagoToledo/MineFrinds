package com.thyagotoledo.companions.forge.registry;

import com.thyagotoledo.companions.forge.CompanionsForgeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CompanionsForgeMod.MODID);

    public static final RegistryObject<CreativeModeTab> COMPANIONS_TAB =
            REGISTRY.register("companions_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COMPANION_CONTRACT.get()))
                    .title(Component.translatable("itemGroup.companions"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COMPANION_CONTRACT.get());
                        output.accept(ModItems.COMPANION_SPAWN_EGG.get());
                    })
                    .build()
            );

    private ModCreativeTabs() {
    }
}
