package com.girigiri.techarsenal.event;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Beam saber parry: while a player is mid-swing holding the saber, incoming
 * projectiles approaching from the front or sides (within ~110 degrees of the
 * look direction, 2.5 blocks) are knocked back toward their shooter. Deflected
 * shots switch owner to the deflecting player and get a glow outline plus a
 * spark burst so the parry is unmistakable. Hitscan lasers call
 * {@link #tryDeflectHitscan} on their target instead.
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaberDeflection
{
    private static final double DEFLECT_RANGE = 2.5D;
    private static final double FRONT_ARC_DOT = -0.35D;
    private static final String TAG_DEFLECTED = "techarsenal_deflected";

    private SaberDeflection()
    {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
            return;
        Player player = event.player;
        if (!isParrying(player))
            return;

        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(DEFLECT_RANGE), p -> canDeflect(player, p)))
            deflect(player, projectile);
    }

    public static boolean isParrying(Player player)
    {
        return player.swinging
                && (player.getMainHandItem().is(ModItems.BEAM_SABER.get())
                        || player.getOffhandItem().is(ModItems.BEAM_SABER.get()));
    }

    /**
     * Hitscan beams (laser gun) can't be swept as entities; the shooter calls
     * this on its target. Returns true when the beam was parried — the caller
     * then applies the reflected damage to the shooter instead.
     */
    public static boolean tryDeflectHitscan(Player target, LivingEntity shooter)
    {
        if (target.level().isClientSide || !isParrying(target))
            return false;
        Vec3 toShooter = shooter.getEyePosition().subtract(target.getEyePosition());
        if (toShooter.lengthSqr() > 1.0E-4D
                && toShooter.normalize().dot(target.getLookAngle()) < FRONT_ARC_DOT)
            return false;
        playDeflectEffects((ServerLevel) target.level(),
                target.getEyePosition().add(target.getLookAngle().scale(0.8D)));
        return true;
    }

    private static boolean canDeflect(Player player, Projectile projectile)
    {
        if (projectile.getOwner() == player)
            return false;
        Vec3 velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() < 0.01D)
            return false;
        Vec3 toProjectile = projectile.position().subtract(player.getEyePosition());
        if (toProjectile.lengthSqr() < 1.0E-4D)
            return true;
        // front/side arc only, and the projectile must be inbound
        return toProjectile.normalize().dot(player.getLookAngle()) >= FRONT_ARC_DOT
                && velocity.dot(toProjectile) < 0.0D;
    }

    private static void deflect(Player player, Projectile projectile)
    {
        Entity shooter = projectile.getOwner();
        Vec3 out = shooter != null && shooter.isAlive()
                ? shooter.getEyePosition().subtract(projectile.position()).normalize()
                : player.getLookAngle();
        double speed = Math.max(projectile.getDeltaMovement().length(), 0.8D);
        projectile.setDeltaMovement(out.scale(speed));
        projectile.hasImpulse = true;
        projectile.setOwner(player); // reflected hits count as the deflector's attack
        projectile.getPersistentData().putBoolean(TAG_DEFLECTED, true);
        projectile.setGlowingTag(true);
        playDeflectEffects((ServerLevel) player.level(), projectile.position());
    }

    private static void playDeflectEffects(ServerLevel level, Vec3 pos)
    {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 12, 0.15D, 0.15D, 0.15D, 0.3D);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 8, 0.1D, 0.1D, 0.1D, 0.4D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 1.8F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    /** White trail for deflected mod projectiles; called from their tick(). */
    public static void tickDeflectTrail(Projectile projectile)
    {
        if (projectile.level() instanceof ServerLevel level
                && projectile.getPersistentData().getBoolean(TAG_DEFLECTED))
            level.sendParticles(ParticleTypes.END_ROD,
                    projectile.getX(), projectile.getY(), projectile.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
