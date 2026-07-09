package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.entity.BulletEntity;
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

/** Hold-to-fire machine gun: 2 dmg every 2 ticks (10 rps, DPS 20), 6 deg spread. */
public class MachineGunItem extends Item
{
    private static final float DAMAGE = 2.0F;
    private static final int FIRE_INTERVAL_TICKS = 2;
    private static final float BULLET_SPEED = 3.5F;
    private static final float INACCURACY = 6.0F;

    public MachineGunItem(Properties properties)
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
        if (level.isClientSide || remainingUseDuration % FIRE_INTERVAL_TICKS != 0)
            return;

        Vec3 look = entity.getLookAngle();
        BulletEntity bullet = new BulletEntity(level, entity, DAMAGE);
        bullet.setPos(entity.getEyePosition().add(look.scale(0.5D)));
        bullet.shoot(look.x, look.y, look.z, BULLET_SPEED, INACCURACY);
        level.addFreshEntity(bullet);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.5F, 1.8F);
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
