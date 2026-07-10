package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.event.SaberDeflection;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
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
    // Solid cyan beam with a bright core, like a coherent laser
    private static final DustParticleOptions BEAM_DUST =
            new DustParticleOptions(new Vector3f(0.25F, 0.9F, 1.0F), 0.8F);

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

            // Continuous beam: dense cyan line with a sparse bright core
            ServerLevel serverLevel = (ServerLevel) level;
            double length = beamEnd.distanceTo(start);
            Vec3 beamStart = start.add(look.scale(0.8D)).add(0.0D, -0.1D, 0.0D);
            for (double d = 0.0D; d < length - 0.8D; d += 0.15D)
            {
                Vec3 p = beamStart.add(look.scale(d));
                serverLevel.sendParticles(BEAM_DUST, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (d % 1.05D < 0.15D)
                    serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            // Impact burst where the beam lands
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    beamEnd.x, beamEnd.y, beamEnd.z, 8, 0.08D, 0.08D, 0.08D, 0.12D);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.05D, 0.05D, 0.05D, 0.01D);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.7F, 1.8F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        // consume (not sidedSuccess): no arm-swing animation, the gun stays steady
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
}
