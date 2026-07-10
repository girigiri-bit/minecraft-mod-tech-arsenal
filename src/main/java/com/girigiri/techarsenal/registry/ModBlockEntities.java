package com.girigiri.techarsenal.registry;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.blockentity.FaceScannerBlockEntity;
import com.girigiri.techarsenal.blockentity.MonitorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TechArsenal.MODID);

    public static final RegistryObject<BlockEntityType<MonitorBlockEntity>> MONITOR =
            BLOCK_ENTITY_TYPES.register("monitor",
                    () -> BlockEntityType.Builder.of(MonitorBlockEntity::new, ModBlocks.MONITOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<FaceScannerBlockEntity>> FACE_SCANNER =
            BLOCK_ENTITY_TYPES.register("face_scanner",
                    () -> BlockEntityType.Builder.of(FaceScannerBlockEntity::new, ModBlocks.FACE_SCANNER.get()).build(null));
}
