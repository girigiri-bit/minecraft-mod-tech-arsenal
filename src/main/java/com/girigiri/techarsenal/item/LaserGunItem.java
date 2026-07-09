package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.event.SaberDeflection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Hitscan laser: 7 dmg instantly out to 64 blocks, cooldown 10t (DPS 14). */
public class LaserGunItem extends Item
{
    private static final float DAMAGE = 7.0F;
    private static final int COOLDOWN_TICKS = 10;
    private static final double RANGE = 64.0D;

    public LaserGunItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide)
        {
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = start.add(look.scale(RANGE));

            BlockHitResult blockHit = level.clip(new ClipContext(start, end,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            Vec3 beamEnd = blockHit.getLocation();

            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, beamEnd,
                    player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D),
                    e -> e instanceof LivingEntity && e.isAlive() && e != player);
            if (entityHit != null)
            {
                beamEnd = entityHit.getLocation();
                Entity target = entityHit.getEntity();
                if (target instanceof Player targetPlayer
                        && SaberDeflection.tryDeflectHitscan(targetPlayer, player))
                {
                    // Parried: the beam comes back and burns the shooter
                    player.hurt(player.damageSources().playerAttack(targetPlayer), DAMAGE);
                }
                else
                {
                    target.hurt(player.damageSources().playerAttack(player), DAMAGE);
                }
            }

            // Beam particle trail
            ServerLevel serverLevel = (ServerLevel) level;
            double length = beamEnd.distanceTo(start);
            for (double d = 1.0D; d < length; d += 0.8D)
            {
                Vec3 p = start.add(look.scale(d));
                serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.7F, 1.8F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
