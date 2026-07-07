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
                        output.accept(ModItems.SECURITY_CAMERA.get());
                        output.accept(ModItems.MONITOR.get());
                        output.accept(ModItems.CAMERA_MONITOR.get());
                        output.accept(ModItems.SATELLITE_REMOTE.get());
                        output.accept(ModItems.MISSILE_LAUNCHER.get());
                        output.accept(ModItems.GUIDED_MISSILE.get());
                        output.accept(ModItems.DRONE.get());
                    }).build());
}
