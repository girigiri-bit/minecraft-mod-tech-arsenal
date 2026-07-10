package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * HP 60, top speed ~1.2 m/tick. Fire key launches rockets from alternating
 * side pods (CD 20t).
 */
public class HelicopterEntity extends VehicleEntityBase implements ArmedVehicle
{
    private static final double ACCELERATION = 0.12D;
    private static final double DRAG = 0.90D;
    private static final int ROCKET_COOLDOWN_TICKS = 20;
    private static final double POD_OFFSET = 1.1D;

    private long lastFireGameTime = -1_000_000L;
    private boolean leftPod;

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
    public void fireWeapon(ServerPlayer rider)
    {
        long now = this.level().getGameTime();
        if (now - lastFireGameTime < ROCKET_COOLDOWN_TICKS)
            return;
        if (!com.girigiri.techarsenal.item.AmmoHelper.tryConsume(rider, ModItems.ROCKET.get()))
            return;
        lastFireGameTime = now;

        Vec3 look = rider.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        leftPod = !leftPod;
        Vec3 muzzle = this.position()
                .add(0.0D, 0.9D, 0.0D)
                .add(right.scale(leftPod ? -POD_OFFSET : POD_OFFSET))
                .add(look.scale(1.5D));

        RocketEntity rocket = new RocketEntity(this.level(), rider, true);
        rocket.setPos(muzzle);
        rocket.shoot(look.x, look.y, look.z, 2.5F, 0.0F);
        this.level().addFreshEntity(rocket);
        this.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 0.8F);
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
