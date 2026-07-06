package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.entity.DroneEntity;
import com.girigiri.techarsenal.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DroneItem extends Item
{
    public DroneItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        if (!level.isClientSide)
        {
            BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
            DroneEntity drone = new DroneEntity(ModEntities.DRONE.get(), level);
            drone.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D,
                    player.getYRot(), 0.0F);
            drone.setOwner(player.getUUID());
            drone.setPersistenceRequired();
            level.addFreshEntity(drone);

            level.playSound(null, spawnPos, SoundEvents.BEEHIVE_EXIT, SoundSource.NEUTRAL, 1.0F, 1.2F);
            if (!player.getAbilities().instabuild)
                context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
