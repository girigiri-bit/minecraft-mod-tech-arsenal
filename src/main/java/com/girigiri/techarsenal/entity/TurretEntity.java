package com.girigiri.techarsenal.entity;

import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Stationary defense turret: auto-targets monsters within 24 blocks and fires
 * 8-damage bolts every 15 ticks (DPS ~10.7). HP 40.
 */
public class TurretEntity extends PathfinderMob implements RangedAttackMob
{
    private static final float BOLT_DAMAGE = 8.0F;
    private static final int FIRE_INTERVAL_TICKS = 15;
    private static final float ATTACK_RANGE = 24.0F;

    public TurretEntity(EntityType<? extends TurretEntity> type, Level level)
    {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, ATTACK_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 0.0D, FIRE_INTERVAL_TICKS, ATTACK_RANGE));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power)
    {
        DroneBoltEntity bolt = new DroneBoltEntity(this.level(), this, BOLT_DAMAGE);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - bolt.getY();
        double dz = target.getZ() - this.getZ();
        bolt.shoot(dx, dy, dz, 2.0F, 1.0F);
        this.playSound(SoundEvents.DISPENSER_LAUNCH, 0.8F, 1.8F);
        this.level().addFreshEntity(bolt);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty())
        {
            if (!this.level().isClientSide)
            {
                ItemStack stack = new ItemStack(ModItems.DEFENSE_TURRET.get());
                if (!player.addItem(stack))
                    player.drop(stack, false);
                this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    protected void pushEntities()
    {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source)
    {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater()
    {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.DEFENSE_TURRET.get()));
    }
}
