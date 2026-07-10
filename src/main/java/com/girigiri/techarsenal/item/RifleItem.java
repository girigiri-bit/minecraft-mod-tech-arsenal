package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.entity.BulletEntity;
import com.girigiri.techarsenal.registry.ModItems;
import com.girigiri.techarsenal.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Semi-auto rifle: 5 dmg, cooldown 8t (2.5 rps, DPS 12.5), bullet speed 4.0, high accuracy. */
public class RifleItem extends Item
{
    private static final float DAMAGE = 5.0F;
    private static final int COOLDOWN_TICKS = 8;
    private static final float BULLET_SPEED = 4.0F;
    private static final float INACCURACY = 0.5F;

    public RifleItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide)
        {
            if (!AmmoHelper.tryConsume(player, ModItems.BULLET.get()))
                return InteractionResultHolder.fail(player.getItemInHand(hand));

            Vec3 look = player.getLookAngle();
            BulletEntity bullet = new BulletEntity(level, player, DAMAGE);
            bullet.setPos(player.getEyePosition().add(look.scale(0.5D)));
            bullet.shoot(look.x, look.y, look.z, BULLET_SPEED, INACCURACY);
            level.addFreshEntity(bullet);
            GunEffects.muzzleFlashAndCasing((ServerLevel) level, player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.RIFLE_FIRE.get(), SoundSource.PLAYERS, 1.0F,
                    0.95F + level.random.nextFloat() * 0.1F);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
