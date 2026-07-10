package com.girigiri.techarsenal.item;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Shared firing effects: muzzle flash, powder smoke and an ejected brass casing. */
public final class GunEffects
{
    private GunEffects()
    {
    }

    public static void muzzleFlashAndCasing(ServerLevel level, LivingEntity shooter)
    {
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();

        // Muzzle flash + powder smoke just past the barrel
        Vec3 muzzle = eye.add(look.scale(1.1D)).add(right.scale(0.15D)).add(0.0D, -0.1D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.03D, 0.03D, 0.03D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.04D, 0.04D, 0.04D, 0.015D);

        // Brass casing kicked out of the ejection port to the shooter's right
        Vec3 port = eye.add(look.scale(0.35D)).add(right.scale(0.3D)).add(0.0D, -0.15D, 0.0D);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)),
                port.x, port.y, port.z, 1,
                right.x * 0.12D, 0.1D, right.z * 0.12D, 0.35D);
    }
}
