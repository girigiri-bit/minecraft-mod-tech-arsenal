package com.girigiri.techarsenal.registry;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.item.BeamSaberItem;
import com.girigiri.techarsenal.item.CameraMonitorItem;
import com.girigiri.techarsenal.item.DeployableItem;
import com.girigiri.techarsenal.item.DoorKeyItem;
import com.girigiri.techarsenal.item.DroneItem;
import com.girigiri.techarsenal.item.FieldManualItem;
import com.girigiri.techarsenal.item.FlamethrowerItem;
import com.girigiri.techarsenal.item.GrenadeLauncherItem;
import com.girigiri.techarsenal.item.LaserDesignatorItem;
import com.girigiri.techarsenal.item.LaserGunItem;
import com.girigiri.techarsenal.item.MachineGunItem;
import com.girigiri.techarsenal.item.MissileLauncherItem;
import com.girigiri.techarsenal.item.RifleItem;
import com.girigiri.techarsenal.item.RocketLauncherItem;
import com.girigiri.techarsenal.item.SatelliteRemoteItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TechArsenal.MODID);

    public static final RegistryObject<Item> FIELD_MANUAL = ITEMS.register("field_manual",
            () -> new FieldManualItem(new Item.Properties().stacksTo(1)));

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

    // --- v0.3: weapons ---

    public static final RegistryObject<Item> RIFLE = ITEMS.register("rifle",
            () -> new RifleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MACHINE_GUN = ITEMS.register("machine_gun",
            () -> new MachineGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRENADE_LAUNCHER = ITEMS.register("grenade_launcher",
            () -> new GrenadeLauncherItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ROCKET_LAUNCHER = ITEMS.register("rocket_launcher",
            () -> new RocketLauncherItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LASER_GUN = ITEMS.register("laser_gun",
            () -> new LaserGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BEAM_SABER = ITEMS.register("beam_saber",
            () -> new BeamSaberItem(new Item.Properties()));

    public static final RegistryObject<Item> FLAMETHROWER = ITEMS.register("flamethrower",
            () -> new FlamethrowerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LANDMINE = ITEMS.register("landmine",
            () -> new BlockItem(ModBlocks.LANDMINE.get(), new Item.Properties().stacksTo(16)));

    // Ammo (also used by the projectile renderers)
    public static final RegistryObject<Item> BULLET = ITEMS.register("bullet",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GRENADE = ITEMS.register("grenade",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ROCKET = ITEMS.register("rocket",
            () -> new Item(new Item.Properties()));

    // Tank cannon shell (renderer display only; the cannon needs no ammo)
    public static final RegistryObject<Item> SHELL = ITEMS.register("shell",
            () -> new Item(new Item.Properties()));

    // --- v0.5: security ---

    public static final RegistryObject<Item> FACE_SCANNER = ITEMS.register("face_scanner",
            () -> new BlockItem(ModBlocks.FACE_SCANNER.get(), new Item.Properties()));

    public static final RegistryObject<Item> AUTH_MONITOR = ITEMS.register("auth_monitor",
            () -> new BlockItem(ModBlocks.AUTH_MONITOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> SECURITY_DOOR = ITEMS.register("security_door",
            () -> new DoubleHighBlockItem(ModBlocks.SECURITY_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> DOOR_KEY = ITEMS.register("door_key",
            () -> new DoorKeyItem(new Item.Properties().stacksTo(1)));

    // --- v0.4: support gear ---

    public static final RegistryObject<Item> LASER_DESIGNATOR = ITEMS.register("laser_designator",
            () -> new LaserDesignatorItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DRONE_UPGRADE_DAMAGE = ITEMS.register("drone_upgrade_damage",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> DRONE_UPGRADE_ARMOR = ITEMS.register("drone_upgrade_armor",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // --- v0.3: machines & vehicles ---

    public static final RegistryObject<Item> DEFENSE_TURRET = ITEMS.register("defense_turret",
            () -> new DeployableItem(ModEntities.TURRET, new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ATTACK_HELICOPTER = ITEMS.register("attack_helicopter",
            () -> new DeployableItem(ModEntities.HELICOPTER, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> TANK = ITEMS.register("tank",
            () -> new DeployableItem(ModEntities.TANK, new Item.Properties().stacksTo(1)));
}
