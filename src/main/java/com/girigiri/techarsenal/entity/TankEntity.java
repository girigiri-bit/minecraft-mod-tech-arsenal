package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ground tank: WASD driving (faces where the rider looks), climbs 1-block
 * steps. HP 100, speed 0.25 m/tick, full knockback resistance.
 */
public class TankEntity extends VehicleEntityBase
{
    public TankEntity(EntityType<? extends TankEntity> type, Level level)
    {
        super(type, level, ModItems.TANK);
        this.setMaxUpStep(1.0F);
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
