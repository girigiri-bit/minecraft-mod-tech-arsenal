package com.girigiri.techarsenal.registry;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.entity.BulletEntity;
import com.girigiri.techarsenal.entity.CameraEntity;
import com.girigiri.techarsenal.entity.DroneBoltEntity;
import com.girigiri.techarsenal.entity.DroneEntity;
import com.girigiri.techarsenal.entity.GrenadeEntity;
import com.girigiri.techarsenal.entity.GuidedMissileEntity;
import com.girigiri.techarsenal.entity.HelicopterEntity;
import com.girigiri.techarsenal.entity.RocketEntity;
import com.girigiri.techarsenal.entity.TankEntity;
import com.girigiri.techarsenal.entity.TurretEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = TechArsenal.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TechArsenal.MODID);

    public static final RegistryObject<EntityType<CameraEntity>> CAMERA = ENTITY_TYPES.register("camera",
            () -> EntityType.Builder.<CameraEntity>of(CameraEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .noSave()
                    .clientTrackingRange(4)
                    .build("camera"));

    public static final RegistryObject<EntityType<GuidedMissileEntity>> GUIDED_MISSILE = ENTITY_TYPES.register("guided_missile",
            () -> EntityType.Builder.<GuidedMissileEntity>of(GuidedMissileEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("guided_missile"));

    public static final RegistryObject<EntityType<DroneEntity>> DRONE = ENTITY_TYPES.register("drone",
            () -> EntityType.Builder.of(DroneEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.4F)
                    .clientTrackingRange(10)
                    .build("drone"));

    public static final RegistryObject<EntityType<DroneBoltEntity>> DRONE_BOLT = ENTITY_TYPES.register("drone_bolt",
            () -> EntityType.Builder.<DroneBoltEntity>of(DroneBoltEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(4)
                    .build("drone_bolt"));

    // --- v0.3 ---

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITY_TYPES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(6)
                    .updateInterval(4)
                    .build("bullet"));

    public static final RegistryObject<EntityType<GrenadeEntity>> GRENADE = ENTITY_TYPES.register("grenade",
            () -> EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(6)
                    .updateInterval(4)
                    .build("grenade"));

    public static final RegistryObject<EntityType<RocketEntity>> ROCKET = ENTITY_TYPES.register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("rocket"));

    public static final RegistryObject<EntityType<TurretEntity>> TURRET = ENTITY_TYPES.register("defense_turret",
            () -> EntityType.Builder.of(TurretEntity::new, MobCategory.MISC)
                    .sized(0.9F, 1.4F)
                    .clientTrackingRange(10)
                    .build("defense_turret"));

    public static final RegistryObject<EntityType<HelicopterEntity>> HELICOPTER = ENTITY_TYPES.register("attack_helicopter",
            () -> EntityType.Builder.of(HelicopterEntity::new, MobCategory.MISC)
                    .sized(2.4F, 1.3F)
                    .clientTrackingRange(10)
                    .build("attack_helicopter"));

    public static final RegistryObject<EntityType<TankEntity>> TANK = ENTITY_TYPES.register("tank",
            () -> EntityType.Builder.of(TankEntity::new, MobCategory.MISC)
                    .sized(2.2F, 1.3F)
                    .clientTrackingRange(10)
                    .build("tank"));

    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event)
    {
        event.put(DRONE.get(), DroneEntity.createAttributes().build());
        event.put(TURRET.get(), TurretEntity.createAttributes().build());
        event.put(HELICOPTER.get(), HelicopterEntity.createAttributes().build());
        event.put(TANK.get(), TankEntity.createAttributes().build());
    }
}
