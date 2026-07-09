package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Attack helicopter: W flies toward the rider's look direction (pitch controls
 * altitude), S flies backward. Hovers while ridden, descends gently when parked.
 * HP 60, top speed ~1.2 m/tick.
 */
public class HelicopterEntity extends VehicleEntityBase
{
    private static final double ACCELERATION = 0.12D;
    private static final double DRAG = 0.90D;

    public HelicopterEntity(EntityType<? extends HelicopterEntity> type, Level level)
    {
        super(type, level, ModItems.ATTACK_HELICOPTER);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input)
    {
        super.tickRidden(rider, input);
        this.setRot(rider.getYRot(), rider.getXRot() * 0.5F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        this.setNoGravity(true);
    }

    @Override
    public void travel(Vec3 input)
    {
        if (this.isAlive() && this.isVehicle() && this.getControllingPassenger() instanceof Player rider)
        {
            // Custom flight: W flies toward the rider's look direction (pitch
            // controls climb/descent), S flies backward. Hovers otherwise.
            Vec3 look = rider.getLookAngle();
            Vec3 delta = this.getDeltaMovement().scale(DRAG);
            if (rider.zza != 0.0F)
                delta = delta.add(look.scale(rider.zza * ACCELERATION));

            this.setDeltaMovement(delta);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.isVehicle())
        {
            this.setNoGravity(false);
            // Autorotation: parked helicopters descend gently instead of dropping
            Vec3 delta = this.getDeltaMovement();
            if (!this.onGround() && delta.y < -0.1D)
                this.setDeltaMovement(delta.x, -0.1D, delta.z);
        }
    }
}
