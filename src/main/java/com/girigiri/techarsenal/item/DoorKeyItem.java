package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.block.SecurityDoorBlock;
import com.girigiri.techarsenal.world.LockRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Key for security doors. Gets a unique id engraved the first time it locks a
 * door; afterwards only keys with the same id can open that door.
 */
public class DoorKeyItem extends Item
{
    public static final String TAG_LOCK_ID = "LockId";

    public DoorKeyItem(Properties properties)
    {
        super(properties);
    }

    public static UUID getOrCreateId(ItemStack stack)
    {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(TAG_LOCK_ID))
            tag.putUUID(TAG_LOCK_ID, UUID.randomUUID());
        return tag.getUUID(TAG_LOCK_ID);
    }

    public static boolean hasId(ItemStack stack)
    {
        return stack.getTag() != null && stack.getTag().hasUUID(TAG_LOCK_ID);
    }

    /**
     * Sneak+use unlock lives here: while sneaking with an item in hand,
     * vanilla skips the block's use() entirely, so the block-side branch
     * never runs and the item hook has to handle it.
     */
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown())
            return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SecurityDoorBlock))
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos lockPos = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        LockRegistry locks = LockRegistry.get(serverLevel);
        UUID lock = locks.getLock(lockPos);
        if (lock == null)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.door_unlocked"), true);
            return InteractionResult.CONSUME;
        }
        if (lock.equals(getOrCreateId(context.getItemInHand())))
        {
            locks.clearLock(lockPos);
            player.displayClientMessage(Component.translatable("message.techarsenal.door_unlocked"), true);
            level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(Component.translatable("message.techarsenal.door_wrong_key"), true);
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8F, 0.9F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        if (hasId(stack))
            tooltip.add(Component.translatable("tooltip.techarsenal.door_key.bound")
                    .withStyle(ChatFormatting.AQUA));
        else
            tooltip.add(Component.translatable("tooltip.techarsenal.door_key.unbound")
                    .withStyle(ChatFormatting.GRAY));
    }
}
