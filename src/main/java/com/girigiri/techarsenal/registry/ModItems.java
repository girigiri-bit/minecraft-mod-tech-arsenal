package com.girigiri.techarsenal.registry;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.item.CameraMonitorItem;
import com.girigiri.techarsenal.item.DroneItem;
import com.girigiri.techarsenal.item.MissileLauncherItem;
import com.girigiri.techarsenal.item.SatelliteRemoteItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TechArsenal.MODID);

    public static final RegistryObject<Item> SECURITY_CAMERA = ITEMS.register("security_camera",
            () -> new BlockItem(ModBlocks.SECURITY_CAMERA.get(), new Item.Properties()));

    public static final RegistryObject<Item> MONITOR = ITEMS.register("monitor",
            () -> new BlockItem(ModBlocks.MONITOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAMERA_MONITOR = ITEMS.register("camera_monitor",
            () -> new CameraMonitorItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SATELLITE_REMOTE = ITEMS.register("satellite_remote",
            () -> new SatelliteRemoteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MISSILE_LAUNCHER = ITEMS.register("missile_launcher",
            () -> new MissileLauncherItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GUIDED_MISSILE = ITEMS.register("guided_missile",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> DRONE = ITEMS.register("drone",
            () -> new DroneItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> DRONE_BOLT = ITEMS.register("drone_bolt",
            () -> new Item(new Item.Properties()));
}
