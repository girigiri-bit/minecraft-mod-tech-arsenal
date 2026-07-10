package com.girigiri.techarsenal.event;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.item.BeamSaberItem;
import com.girigiri.techarsenal.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Beam saber R-key special: a three-strike combo in Star Wars fashion —
 * kesa-giri (diagonal cut), horizontal sweep, then a 360-degree spin slash.
 * Strikes land 7 ticks apart; the later stages are scheduled on the server
 * tick and cancel if the player dies or puts the saber away.
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaberComboManager
{
    private static final int STRIKE_INTERVAL_TICKS = 7;
    private static final double RANGE = 3.2D;
    private static final float SLASH_DAMAGE = 8.0F;
    private static final float SPIN_DAMAGE = 12.0F;

    private record PendingStrike(ServerPlayer player, int stage, long executeAt)
    {
    }

    private static final List<PendingStrike> PENDING = new ArrayList<>();

    private SaberComboManager()
    {
    }

    public static void startCombo(ServerPlayer player)
    {
        long now = player.serverLevel().getGameTime();
        executeStrike(player, 0);
        PENDING.add(new PendingStrike(player, 1, now + STRIKE_INTERVAL_TICKS));
        PENDING.add(new PendingStrike(player, 2, now + STRIKE_INTERVAL_TICKS * 2L));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty())
            return;
        Iterator<PendingStrike> it = PENDING.iterator();
        while (it.hasNext())
        {
            PendingStrike strike = it.next();
            if (strike.player.isRemoved() || !strike.player.isAlive()
                    || !(strike.player.getMainHandItem().getItem() instanceof BeamSaberItem))
            {
                it.remove();
                continue;
            }
            if (strike.player.serverLevel().getGameTime() >= strike.executeAt)
            {
                executeStrike(strike.player, strike.stage);
                it.remove();
            }
        }
    }

    private static void executeStrike(ServerPlayer player, int stage)
    {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        player.swing(InteractionHand.MAIN_HAND, true);

        if (stage < 2)
        {
            // Kesa-giri / horizontal sweep: everything in the frontal arc
            for (LivingEntity target : frontalTargets(level, player, look))
            {
                target.invulnerableTime = 0; // combo hits land in rapid succession
                target.hurt(player.damageSources().playerAttack(player), SLASH_DAMAGE);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SABER_SWING.get(), SoundSource.PLAYERS, 0.9F,
                    stage == 0 ? 1.15F : 0.85F);

            if (stage == 0)
            {
                // Diagonal slash trail: upper-right down to lower-left
                Vec3 top = eye.add(look.scale(1.6D)).add(right.scale(0.9D)).add(0.0D, 0.7D, 0.0D);
                Vec3 bottom = eye.add(look.scale(1.6D)).subtract(right.scale(0.9D)).add(0.0D, -1.1D, 0.0D);
                for (int i = 0; i <= 10; i++)
                {
                    Vec3 p = top.lerp(bottom, i / 10.0D);
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
                Vec3 mid = top.lerp(bottom, 0.5D);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y, mid.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            else
            {
                // Horizontal sweep trail: arc across the front
                float baseYaw = player.getYRot();
                for (int i = -5; i <= 5; i++)
                {
                    float yaw = (float) Math.toRadians(baseYaw + i * 14.0F);
                    Vec3 dir = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
                    Vec3 p = eye.add(dir.scale(2.2D)).add(0.0D, -0.2D, 0.0D);
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                    if (i % 5 == 0)
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            return;
        }

        // Final stage: 360-degree spin slash with knockback
        Vec3 center = player.position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE),
                t -> t != player && t.isAlive() && player.distanceTo(t) <= RANGE + 0.5D))
        {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), SPIN_DAMAGE);
            Vec3 push = target.position().subtract(center).normalize();
            target.push(push.x * 0.8D, 0.3D, push.z * 0.8D);
        }
        double y = player.getY() + 1.1D;
        for (int i = 0; i < 24; i++)
        {
            float angle = (float) (i * Math.PI * 2.0D / 24.0D);
            double px = center.x + Mth.cos(angle) * 2.2D;
            double pz = center.z + Mth.sin(angle) * 2.2D;
            if (i % 2 == 0)
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, px, y, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, px, y, pz, 1, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SABER_SPECIAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static List<LivingEntity> frontalTargets(ServerLevel level, ServerPlayer player, Vec3 look)
    {
        return level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE),
                t -> {
                    if (t == player || !t.isAlive() || player.distanceTo(t) > RANGE + 0.5D)
                        return false;
                    Vec3 to = t.position().subtract(player.position());
                    return to.lengthSqr() < 1.0E-4D || to.normalize().dot(look) > 0.25D;
                });
    }
}
