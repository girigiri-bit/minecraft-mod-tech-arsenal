package com.girigiri.techarsenal.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Hold-to-fire flame cone: 2 dmg / 5 ticks + 5s burn, range 8, ~30 degree cone. */
public class FlamethrowerItem extends Item
{
    private static final float DAMAGE = 2.0F;
    private static final int DAMAGE_INTERVAL_TICKS = 5;
    private static final double RANGE = 8.0D;
    private static final double CONE_MIN_DOT = 0.86D; // ~30 degree cone
    private static final int BURN_SECONDS = 5;

    public FlamethrowerItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration)
    {
        if (level.isClientSide)
            return;

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 eye = entity.getEyePosition();
        Vec3 look = entity.getLookAngle();

        // Flame stream particles
        for (int i = 0; i < 3; i++)
        {
            double d = 1.0D + level.random.nextDouble() * (RANGE - 1.0D);
            Vec3 p = eye.add(look.scale(d)).add(
                    (level.random.nextDouble() - 0.5D) * d * 0.25D,
                    (level.random.nextDouble() - 0.5D) * d * 0.25D,
                    (level.random.nextDouble() - 0.5D) * d * 0.25D);
            serverLevel.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.01D);
        }

        if (remainingUseDuration % 20 == 0)
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6F, 0.7F);

        if (remainingUseDuration % DAMAGE_INTERVAL_TICKS != 0)
            return;

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(RANGE),
                target -> target != entity && target.isAlive());
        for (LivingEntity target : targets)
        {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            if (toTarget.length() > RANGE || toTarget.normalize().dot(look) < CONE_MIN_DOT)
                continue;
            target.setSecondsOnFire(BURN_SECONDS);
            target.hurt(level.damageSources().inFire(), DAMAGE);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.NONE;
    }
}
