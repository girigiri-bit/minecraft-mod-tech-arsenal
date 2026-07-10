package com.girigiri.techarsenal.network;

import com.girigiri.techarsenal.item.BeamSaberItem;
import com.girigiri.techarsenal.registry.ModItems;
import com.girigiri.techarsenal.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Client -> server: the player pressed the action key while holding the beam
 * saber. Performs the spin slash: 12 damage + knockback to everything within
 * 3 blocks, a sweeping particle ring, the "vwoon" sound and a 5s cooldown.
 */
public class SaberSpecialPacket
{
    private static final double RANGE = 3.0D;
    private static final float DAMAGE = 12.0F;
    private static final int COOLDOWN_TICKS = 100;

    public static void encode(SaberSpecialPacket msg, FriendlyByteBuf buf)
    {
    }

    public static SaberSpecialPacket decode(FriendlyByteBuf buf)
    {
        return new SaberSpecialPacket();
    }

    public static void handle(SaberSpecialPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null
                    || !(player.getMainHandItem().getItem() instanceof BeamSaberItem)
                    || player.getCooldowns().isOnCooldown(ModItems.BEAM_SABER.get()))
                return;

            ServerLevel level = player.serverLevel();
            Vec3 center = player.position();

            // 360-degree spin slash
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(RANGE),
                    t -> t != player && t.isAlive() && player.distanceTo(t) <= RANGE + 0.5D);
            for (LivingEntity target : targets)
            {
                target.hurt(player.damageSources().playerAttack(player), DAMAGE);
                Vec3 push = target.position().subtract(center).normalize();
                target.push(push.x * 0.8D, 0.3D, push.z * 0.8D);
            }

            // Sweeping ring of particles around the player
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
            player.swing(InteractionHand.MAIN_HAND, true);
            player.getCooldowns().addCooldown(ModItems.BEAM_SABER.get(), COOLDOWN_TICKS);
        });
        ctx.get().setPacketHandled(true);
    }
}
