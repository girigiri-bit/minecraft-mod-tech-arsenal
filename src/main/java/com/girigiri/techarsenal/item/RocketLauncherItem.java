package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.entity.RocketEntity;
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

/** Rocket launcher: straight rocket, 12 direct + r3.5 explosion, cooldown 50t. */
public class RocketLauncherItem extends Item
{
    private static final int COOLDOWN_TICKS = 50;
    private static final float LAUNCH_SPEED = 2.0F;

    public RocketLauncherItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide)
        {
            if (!AmmoHelper.tryConsume(player, ModItems.ROCKET.get()))
                return InteractionResultHolder.fail(player.getItemInHand(hand));

            Vec3 look = player.getLookAngle();
            RocketEntity rocket = new RocketEntity(level, player);
            rocket.setPos(player.getEyePosition().add(look.scale(1.0D)));
            rocket.shoot(look.x, look.y, look.z, LAUNCH_SPEED, 0.0F);
            level.addFreshEntity(rocket);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2F, 0.6F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
