package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.client.ClientCameraHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public class CameraMonitorItem extends Item
{
    private static final String TAG_POS = "CameraPos";
    private static final String TAG_YAW = "CameraYaw";
    private static final double MAX_VIEW_DISTANCE = 64.0D;

    public CameraMonitorItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.getBlock() instanceof SecurityCameraBlock && player != null)
        {
            if (!level.isClientSide)
            {
                CompoundTag tag = context.getItemInHand().getOrCreateTag();
                tag.putLong(TAG_POS, pos.asLong());
                tag.putFloat(TAG_YAW, state.getValue(HorizontalDirectionalBlock.FACING).toYRot());
                player.displayClientMessage(Component.translatable("message.techarsenal.camera_linked",
                        pos.getX(), pos.getY(), pos.getZ()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains(TAG_POS))
        {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.techarsenal.camera_not_linked"), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos cameraPos = BlockPos.of(tag.getLong(TAG_POS));
        float yaw = tag.getFloat(TAG_YAW);

        if (!(level.getBlockState(cameraPos).getBlock() instanceof SecurityCameraBlock))
        {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.techarsenal.camera_missing"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (player.distanceToSqr(Vec3.atCenterOf(cameraPos)) > MAX_VIEW_DISTANCE * MAX_VIEW_DISTANCE)
        {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.techarsenal.camera_out_of_range"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide)
        {
            Vec3 viewPos = Vec3.atCenterOf(cameraPos).add(0.0D, 0.15D, 0.0D);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCameraHooks.activate(viewPos, yaw, 15.0F));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_POS))
        {
            BlockPos pos = BlockPos.of(tag.getLong(TAG_POS));
            tooltip.add(Component.translatable("tooltip.techarsenal.camera_monitor.linked",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GRAY));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.techarsenal.camera_monitor.unlinked").withStyle(ChatFormatting.GRAY));
        }
    }
}
