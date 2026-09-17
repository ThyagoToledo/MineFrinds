package com.thyagotoledo.companions.forge.registry;

import com.thyagotoledo.companions.forge.CompanionsForgeMod;
import com.thyagotoledo.companions.forge.entity.CompanionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CompanionsForgeMod.MODID);

    public static final RegistryObject<EntityType<CompanionEntity>> COMPANION =
            REGISTRY.register("companion", () ->
                    EntityType.Builder.<CompanionEntity>of(CompanionEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("companion")
            );

    private ModEntities() {
    }
}
