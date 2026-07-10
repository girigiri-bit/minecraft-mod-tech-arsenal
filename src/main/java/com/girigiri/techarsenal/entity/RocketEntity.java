package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModEntities;
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

/**
 * Straight-flying rocket. The handheld launcher fires the standard version
 * (no block damage); helicopter pods fire the heavy version — double damage
 * and a small terrain-breaking blast.
 */
public class RocketEntity extends ThrowableItemProjectile
{
    private static final float DIRECT_DAMAGE = 12.0F;
    private static final float EXPLOSION_RADIUS = 3.5F;
    private static final float HEAVY_DIRECT_DAMAGE = 24.0F;
    private static final float HEAVY_EXPLOSION_RADIUS = 3.0F;
    private static final int MAX_LIFETIME_TICKS = 60;

    private boolean heavy;

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level)
    {
        super(type, level);
    }

    public RocketEntity(Level level, LivingEntity shooter)
    {
        super(ModEntities.ROCKET.get(), shooter, level);
    }

    public RocketEntity(Level level, LivingEntity shooter, boolean heavy)
    {
        this(level, shooter);
        this.heavy = heavy;
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.ROCKET.get();
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
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(), 2, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);
        if (!this.level().isClientSide)
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()),
                    heavy ? HEAVY_DIRECT_DAMAGE : DIRECT_DAMAGE);
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
        if (heavy)
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    HEAVY_EXPLOSION_RADIUS, Level.ExplosionInteraction.BLOCK);
        else
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
        this.discard();
    }
}
