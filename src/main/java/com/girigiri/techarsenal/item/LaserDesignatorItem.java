package com.girigiri.techarsenal.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Marks the living entity under the crosshair (64m) as a missile target for
 * 60 seconds; guided missiles home on the mark with priority and without
 * needing line of sight. Sneak-use clears the mark.
 */
public class LaserDesignatorItem extends Item
{
    public static final String TAG_TARGET_UUID = "TargetUUID";
    public static final String TAG_TARGET_TIME = "TargetTime";
    public static final int VALID_TICKS = 20 * 60;
    private static final double MARK_RANGE = 64.0D;
    private static final DustParticleOptions RED_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.0F);

    public LaserDesignatorItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.sidedSuccess(stack, true);

        if (player.isShiftKeyDown())
        {
            if (stack.getTag() != null)
            {
                stack.getTag().remove(TAG_TARGET_UUID);
                stack.getTag().remove(TAG_TARGET_TIME);
            }
            player.displayClientMessage(Component.translatable("message.techarsenal.designator_cleared"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(MARK_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 beamEnd = blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, start, beamEnd,
                player.getBoundingBox().expandTowards(look.scale(MARK_RANGE)).inflate(1.0D),
                e -> e instanceof LivingEntity && e.isAlive() && e != player);

        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity target))
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.designator_no_target"), true);
            return InteractionResultHolder.fail(stack);
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_TARGET_UUID, target.getUUID());
        tag.putLong(TAG_TARGET_TIME, level.getGameTime());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, VALID_TICKS, 0));

        ServerLevel serverLevel = (ServerLevel) level;
        double length = entityHit.getLocation().distanceTo(start);
        for (double d = 1.0D; d < length; d += 0.8D)
        {
            Vec3 p = start.add(look.scale(d));
            serverLevel.sendParticles(RED_DUST, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        player.displayClientMessage(Component.translatable("message.techarsenal.designator_set",
                target.getDisplayName()), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 2.0F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** Resolves the marked target if it is still alive and the mark hasn't expired. */
    @Nullable
    public static LivingEntity getDesignatedTarget(ServerLevel level, ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_TARGET_UUID))
            return null;
        if (level.getGameTime() - tag.getLong(TAG_TARGET_TIME) > VALID_TICKS)
            return null;
        return level.getEntity(tag.getUUID(TAG_TARGET_UUID)) instanceof LivingEntity target
                && target.isAlive() ? target : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(TAG_TARGET_UUID))
            tooltip.add(Component.translatable("tooltip.techarsenal.laser_designator.set")
                    .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.techarsenal.laser_designator.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
