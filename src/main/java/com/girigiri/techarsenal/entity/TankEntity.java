package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ground tank: WASD driving (faces where the rider looks), climbs 1-block
 * steps. HP 100, speed 0.25 m/tick, full knockback resistance. Fire key
 * shoots the main cannon (shell, CD 60t).
 */
public class TankEntity extends VehicleEntityBase implements ArmedVehicle
{
    private static final int CANNON_COOLDOWN_TICKS = 60;

    private long lastFireGameTime = -1_000_000L;

    public TankEntity(EntityType<? extends TankEntity> type, Level level)
    {
        super(type, level, ModItems.TANK);
        this.setMaxUpStep(1.0F);
    }

    @Override
    public void fireWeapon(ServerPlayer rider)
    {
        long now = this.level().getGameTime();
        if (now - lastFireGameTime < CANNON_COOLDOWN_TICKS)
            return;
        if (!com.girigiri.techarsenal.item.AmmoHelper.tryConsume(rider, ModItems.SHELL.get()))
            return;
        lastFireGameTime = now;

        Vec3 look = rider.getLookAngle();
        // The barrel tracks the hull yaw, so the shell leaves the barrel tip
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        Vec3 muzzle = this.position().add(forward.scale(2.4D)).add(0.0D, 1.15D, 0.0D);

        ShellEntity shell = new ShellEntity(this.level(), rider);
        shell.setPos(muzzle);
        shell.shoot(look.x, look.y, look.z, 3.0F, 0.0F);
        this.level().addFreshEntity(shell);

        ((ServerLevel) this.level()).sendParticles(ParticleTypes.LARGE_SMOKE,
                muzzle.x, muzzle.y, muzzle.z, 8, 0.15D, 0.15D, 0.15D, 0.02D);
        this.playSound(SoundEvents.GENERIC_EXPLODE, 0.6F, 1.6F);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input)
    {
        super.tickRidden(rider, input);
        this.setRot(rider.getYRot(), 0.0F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 input)
    {
        return new Vec3(rider.xxa * 0.4F, 0.0D, rider.zza);
    }

    @Override
    protected float getRiddenSpeed(Player rider)
    {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }
}
