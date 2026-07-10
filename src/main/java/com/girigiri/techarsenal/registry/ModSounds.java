package com.girigiri.techarsenal.registry;

import com.girigiri.techarsenal.TechArsenal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Synthesized custom sounds (see tools/generate_sounds.ps1). */
public class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TechArsenal.MODID);

    public static final RegistryObject<SoundEvent> MACHINE_GUN_FIRE = register("machine_gun_fire");
    public static final RegistryObject<SoundEvent> RIFLE_FIRE = register("rifle_fire");
    public static final RegistryObject<SoundEvent> TANK_CANNON = register("tank_cannon");
    public static final RegistryObject<SoundEvent> SABER_SWING = register("saber_swing");
    public static final RegistryObject<SoundEvent> SABER_SPECIAL = register("saber_special");

    private static RegistryObject<SoundEvent> register(String name)
    {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TechArsenal.MODID, name)));
    }
}
