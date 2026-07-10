package com.girigiri.techarsenal.registry;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.block.AuthMonitorBlock;
import com.girigiri.techarsenal.block.FaceScannerBlock;
import com.girigiri.techarsenal.block.LandmineBlock;
import com.girigiri.techarsenal.block.MonitorBlock;
import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.block.SecurityDoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TechArsenal.MODID);

    public static final RegistryObject<Block> SECURITY_CAMERA = BLOCKS.register("security_camera",
            () -> new SecurityCameraBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> MONITOR = BLOCKS.register("monitor",
            () -> new MonitorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> LANDMINE = BLOCKS.register("landmine",
            () -> new LandmineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // --- v0.5: security ---

    public static final RegistryObject<Block> FACE_SCANNER = BLOCKS.register("face_scanner",
            () -> new FaceScannerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> AUTH_MONITOR = BLOCKS.register("auth_monitor",
            () -> new AuthMonitorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.5F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> SECURITY_DOOR = BLOCKS.register("security_door",
            () -> new SecurityDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
}
