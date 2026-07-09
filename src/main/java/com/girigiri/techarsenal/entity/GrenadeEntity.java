package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Lobbed grenade: arcs with gravity, explodes on impact (no block damage). */
public class GrenadeEntity extends ThrowableItemProjectile
{
    private static final float DIRECT_DAMAGE = 6.0F;
    private static final float EXPLOSION_RADIUS = 2.5F;
    private static final int MAX_LIFETIME_TICKS = 100;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level)
    {
        super(type, level);
    }

    public GrenadeEntity(Level level, LivingEntity shooter)
    {
        super(ModEntities.GRENADE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.GRENADE.get();
    }

    @Override
    protected float getGravity()
    {
        return 0.05F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);
        if (!this.level().isClientSide)
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), DIRECT_DAMAGE);
    }

    @Override
    protected void onHit(HitResult result)
    {
        super.onHit(result);
        if (!this.level().isClientSide)
            explode();
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFETIME_TICKS)
            explode();
    }

    private void explode()
    {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
        this.discard();
    }
}
