package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class DroneBoltEntity extends ThrowableItemProjectile
{
    private static final float DAMAGE = 5.0F;

    public DroneBoltEntity(EntityType<? extends DroneBoltEntity> type, Level level)
    {
        super(type, level);
    }

    public DroneBoltEntity(Level level, LivingEntity shooter)
    {
        super(ModEntities.DRONE_BOLT.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.DRONE_BOLT.get();
    }

    @Override
    protected float getGravity()
    {
        return 0.01F;
    }

    @Override
    protected boolean canHitEntity(Entity entity)
    {
        // Drone bolts never hit players — drones are friendly hardware
        return super.canHitEntity(entity) && !(entity instanceof Player);
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);
        if (!this.level().isClientSide)
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
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
            for (int i = 0; i < 4; i++)
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        if (this.tickCount > 60)
            this.discard();
    }
}
