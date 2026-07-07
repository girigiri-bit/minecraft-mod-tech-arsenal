package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.client.ClientCameraHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class SatelliteRemoteItem extends Item
{
    private static final double SATELLITE_HEIGHT = 80.0D;

    public SatelliteRemoteItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (level.isClientSide)
        {
            Vec3 viewPos = new Vec3(player.getX(), player.getEyeY() + SATELLITE_HEIGHT, player.getZ());
            float yaw = player.getYRot();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCameraHooks.activate(viewPos, yaw, 90.0F, "SAT"));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
