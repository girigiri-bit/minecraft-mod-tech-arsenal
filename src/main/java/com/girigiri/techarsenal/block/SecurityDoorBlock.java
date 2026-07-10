package com.girigiri.techarsenal.block;

import com.girigiri.techarsenal.item.DoorKeyItem;
import com.girigiri.techarsenal.registry.ModItems;
import com.girigiri.techarsenal.world.LockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Lockable metal door. Unlocked it opens like a wooden door; lock it by using
 * a key (the key gets a unique id engraved) and it only opens for keys with
 * that id. Ignores redstone while locked. Face scanners bypass the lock.
 */
public class SecurityDoorBlock extends DoorBlock
{
    private static final DustParticleOptions RED_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.0F);

    public SecurityDoorBlock(Properties properties)
    {
        super(properties, BlockSetType.IRON);
    }

    private static BlockPos lowerPos(BlockState state, BlockPos pos)
    {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        ServerLevel serverLevel = (ServerLevel) level;
        LockRegistry locks = LockRegistry.get(serverLevel);
        BlockPos lockPos = lowerPos(state, pos);
        UUID lock = locks.getLock(lockPos);
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.DOOR_KEY.get()))
        {
            if (lock == null)
            {
                locks.setLock(lockPos, DoorKeyItem.getOrCreateId(held));
                this.setOpen(player, level, state, pos, false);
                player.displayClientMessage(Component.translatable("message.techarsenal.door_locked"), true);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.4F);
                return InteractionResult.CONSUME;
            }
            // Sneak+key unlocking is handled in DoorKeyItem#useOn — while
            // sneaking with an item in hand, vanilla never calls block use()
            if (lock.equals(DoorKeyItem.getOrCreateId(held)))
            {
                this.setOpen(player, level, state, pos, !state.getValue(OPEN));
                return InteractionResult.CONSUME;
            }
            deny(serverLevel, pos, player, "message.techarsenal.door_wrong_key");
            return InteractionResult.CONSUME;
        }

        if (lock == null)
        {
            this.setOpen(player, level, state, pos, !state.getValue(OPEN));
            return InteractionResult.CONSUME;
        }

        deny(serverLevel, pos, player, "message.techarsenal.door_locked_denied");
        return InteractionResult.CONSUME;
    }

    private static void deny(ServerLevel level, BlockPos pos, Player player, String messageKey)
    {
        player.displayClientMessage(Component.translatable(messageKey), true);
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(RED_DUST, center.x, center.y + 0.3D, center.z, 8, 0.25D, 0.4D, 0.25D, 0.0D);
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8F, 0.9F);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 1.8F);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving)
    {
        // Locked doors ignore redstone; unlocked ones behave like iron doors
        if (level instanceof ServerLevel serverLevel
                && LockRegistry.get(serverLevel).getLock(lowerPos(state, pos)) != null)
            return;
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel)
            LockRegistry.get(serverLevel).clearLock(lowerPos(state, pos));
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
