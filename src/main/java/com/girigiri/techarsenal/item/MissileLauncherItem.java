package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.entity.GuidedMissileEntity;
import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MissileLauncherItem extends Item
{
    private static final double TARGET_SEARCH_RANGE = 48.0D;
    private static final int COOLDOWN_TICKS = 30;

    public MissileLauncherItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide)
        {
            if (!player.getAbilities().instabuild && !consumeAmmo(player))
            {
                player.displayClientMessage(Component.translatable("message.techarsenal.no_ammo"), true);
                return InteractionResultHolder.fail(stack);
            }

            Vec3 look = player.getLookAngle();
            GuidedMissileEntity missile = new GuidedMissileEntity(ModEntities.GUIDED_MISSILE.get(), player, level);
            missile.setPos(player.getEyePosition().add(look.scale(1.0D)));
            missile.shoot(look.x, look.y, look.z, 1.5F, 0.0F);
            missile.setHomingTarget(findTarget(level, player));
            level.addFreshEntity(missile);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.7F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static boolean consumeAmmo(Player player)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.is(ModItems.GUIDED_MISSILE.get()))
            {
                slot.shrink(1);
                return true;
            }
        }
        return false;
    }

    // Pick the monster closest to the player's crosshair direction within range
    private static LivingEntity findTarget(Level level, Player player)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<Monster> candidates = level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(TARGET_SEARCH_RANGE),
                monster -> monster.isAlive() && player.hasLineOfSight(monster));

        Monster best = null;
        double bestDot = 0.5D;
        for (Monster monster : candidates)
        {
            Vec3 toTarget = monster.getEyePosition().subtract(eye).normalize();
            double dot = toTarget.dot(look);
            if (dot > bestDot)
            {
                bestDot = dot;
                best = monster;
            }
        }
        return best;
    }
}
