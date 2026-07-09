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

/** Tank cannon shell: 15 direct + r3.0 explosion, slight ballistic drop (no block damage). */
public class ShellEntity extends ThrowableItemProjectile
{
    private static final float DIRECT_DAMAGE = 15.0F;
    private static final float EXPLOSION_RADIUS = 3.0F;
    private static final int MAX_LIFETIME_TICKS = 100;

    public ShellEntity(EntityType<? extends ShellEntity> type, Level level)
    {
        super(type, level);
    }

    public ShellEntity(Level level, LivingEntity shooter)
    {
        super(ModEntities.SHELL.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.SHELL.get();
    }

    @Override
    protected float getGravity()
    {
        return 0.02F;
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
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), DIRECT_DAMAGE);
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
        this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
        this.discard();
    }
}
