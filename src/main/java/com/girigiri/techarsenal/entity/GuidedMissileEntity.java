package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class GuidedMissileEntity extends ThrowableItemProjectile
{
    private static final float SPEED = 1.5F;
    private static final int MAX_LIFETIME_TICKS = 200;
    private static final float EXPLOSION_RADIUS = 2.0F;

    @Nullable
    private LivingEntity homingTarget;

    public GuidedMissileEntity(EntityType<? extends GuidedMissileEntity> type, Level level)
    {
        super(type, level);
    }

    public GuidedMissileEntity(EntityType<? extends GuidedMissileEntity> type, LivingEntity shooter, Level level)
    {
        super(type, shooter, level);
    }

    public void setHomingTarget(@Nullable LivingEntity target)
    {
        this.homingTarget = target;
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.GUIDED_MISSILE.get();
    }

    @Override
    protected float getGravity()
    {
        return 0.0F;
    }

    @Override
    public void tick()
    {
        if (!this.level().isClientSide)
        {
            if (this.tickCount > MAX_LIFETIME_TICKS)
            {
                explode();
                return;
            }

            if (this.homingTarget != null && this.homingTarget.isAlive())
            {
                Vec3 desired = this.homingTarget.getEyePosition()
                        .subtract(this.position())
                        .normalize()
                        .scale(SPEED);
                this.setDeltaMovement(this.getDeltaMovement().lerp(desired, 0.25D).normalize().scale(SPEED));
            }

            ((ServerLevel) this.level()).sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(), 2, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);
        if (!this.level().isClientSide)
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 8.0F);
    }

    @Override
    protected void onHit(HitResult result)
    {
        super.onHit(result);
        if (!this.level().isClientSide)
            explode();
    }

    private void explode()
    {
        // ExplosionInteraction.NONE: damages entities but never breaks blocks
        this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
        this.discard();
    }
}
