package com.girigiri.techarsenal.registry;

import com.girigiri.techarsenal.TechArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TechArsenal.MODID);

    public static final RegistryObject<CreativeModeTab> TECH_ARSENAL_TAB = CREATIVE_MODE_TABS.register("tech_arsenal",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .title(Component.translatable("itemGroup.techarsenal"))
                    .icon(() -> ModItems.MISSILE_LAUNCHER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FIELD_MANUAL.get());
                        output.accept(ModItems.SECURITY_CAMERA.get());
                        output.accept(ModItems.MONITOR.get());
                        output.accept(ModItems.FACE_SCANNER.get());
                        output.accept(ModItems.AUTH_MONITOR.get());
                        output.accept(ModItems.SECURITY_DOOR.get());
                        output.accept(ModItems.DOOR_KEY.get());
                        output.accept(ModItems.CAMERA_MONITOR.get());
                        output.accept(ModItems.SATELLITE_REMOTE.get());
                        output.accept(ModItems.MISSILE_LAUNCHER.get());
                        output.accept(ModItems.GUIDED_MISSILE.get());
                        output.accept(ModItems.LASER_DESIGNATOR.get());
                        output.accept(ModItems.DRONE.get());
                        output.accept(ModItems.DRONE_UPGRADE_DAMAGE.get());
                        output.accept(ModItems.DRONE_UPGRADE_ARMOR.get());
                        output.accept(ModItems.RIFLE.get());
                        output.accept(ModItems.MACHINE_GUN.get());
                        output.accept(ModItems.GRENADE_LAUNCHER.get());
                        output.accept(ModItems.ROCKET_LAUNCHER.get());
                        output.accept(ModItems.LASER_GUN.get());
                        output.accept(ModItems.BEAM_SABER.get());
                        output.accept(ModItems.FLAMETHROWER.get());
                        output.accept(ModItems.BULLET.get());
                        output.accept(ModItems.GRENADE.get());
                        output.accept(ModItems.ROCKET.get());
                        output.accept(ModItems.SHELL.get());
                        output.accept(ModItems.LANDMINE.get());
                        output.accept(ModItems.DEFENSE_TURRET.get());
                        output.accept(ModItems.ATTACK_HELICOPTER.get());
                        output.accept(ModItems.TANK.get());
                    }).build());
}
