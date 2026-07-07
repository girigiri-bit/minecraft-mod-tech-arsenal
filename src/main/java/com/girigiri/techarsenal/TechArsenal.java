package com.girigiri.techarsenal;

import com.girigiri.techarsenal.registry.ModBlockEntities;
import com.girigiri.techarsenal.registry.ModBlocks;
import com.girigiri.techarsenal.registry.ModCreativeTabs;
import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TechArsenal.MODID)
public class TechArsenal
{
    public static final String MODID = "techarsenal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TechArsenal(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        LOGGER.info("Tech Arsenal loaded");
    }
}
