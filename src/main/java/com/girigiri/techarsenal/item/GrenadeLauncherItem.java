package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.entity.GrenadeEntity;
import com.girigiri.techarsenal.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Grenade launcher: lobbed grenade, 6 direct + r2.5 explosion, cooldown 25t. */
public class GrenadeLauncherItem extends Item
{
    private static final int COOLDOWN_TICKS = 25;
    private static final float LAUNCH_SPEED = 1.2F;

    public GrenadeLauncherItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide)
        {
            if (!AmmoHelper.tryConsume(player, ModItems.GRENADE.get()))
                return InteractionResultHolder.fail(player.getItemInHand(hand));

            Vec3 look = player.getLookAngle();
            GrenadeEntity grenade = new GrenadeEntity(level, player);
            grenade.setPos(player.getEyePosition().add(look.scale(0.7D)));
            grenade.shoot(look.x, look.y + 0.1D, look.z, LAUNCH_SPEED, 1.0F);
            level.addFreshEntity(grenade);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
