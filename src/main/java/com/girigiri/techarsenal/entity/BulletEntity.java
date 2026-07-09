package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Fast gun bullet used by the rifle and machine gun. Damage set by the weapon. */
public class BulletEntity extends ThrowableItemProjectile
{
    private static final int MAX_LIFETIME_TICKS = 40;

    private float damage = 5.0F;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level)
    {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity shooter, float damage)
    {
        super(ModEntities.BULLET.get(), shooter, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.BULLET.get();
    }

    @Override
    protected float getGravity()
    {
        return 0.0F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);
        if (!this.level().isClientSide)
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), damage);
    }

    @Override
    protected void onHit(HitResult result)
    {
        super.onHit(result);
        if (!this.level().isClientSide)
        {
            this.discard();
        }
        else
        {
            for (int i = 0; i < 3; i++)
                this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.tickCount > MAX_LIFETIME_TICKS)
            this.discard();
    }
}
