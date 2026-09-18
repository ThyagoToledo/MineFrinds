package com.thyagotoledo.companions.forge;

import com.thyagotoledo.companions.forge.entity.CompanionEntity;
import com.thyagotoledo.companions.forge.network.CompanionsNetwork;
import com.thyagotoledo.companions.forge.registry.ModCreativeTabs;
import com.thyagotoledo.companions.forge.registry.ModEntities;
import com.thyagotoledo.companions.forge.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CompanionsForgeMod.MODID)
public final class CompanionsForgeMod {
    public static final String MODID = "companions";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public CompanionsForgeMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModEntities.REGISTRY.register(modEventBus);
        ModItems.REGISTRY.register(modEventBus);
        ModCreativeTabs.REGISTRY.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerAttributes);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CompanionsNetwork::register);
        LOGGER.info("Companions Forge 1.20.1 common setup inicializado com sucesso.");
    }

    private void registerAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPANION.get(), CompanionEntity.createAttributes().build());
    }
}
