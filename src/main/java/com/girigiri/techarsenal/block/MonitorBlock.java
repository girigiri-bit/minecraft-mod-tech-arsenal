package com.girigiri.techarsenal.block;

import com.girigiri.techarsenal.blockentity.MonitorBlockEntity;
import com.girigiri.techarsenal.util.MonitorScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MonitorBlock extends BaseEntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MonitorBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new MonitorBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        // Holding any block means the player is building (extending the wall,
        // placing cameras next to it, ...) — let placement happen instead of
        // cycling the feed. Cycling works with an empty hand or non-block items.
        if (player.getItemInHand(hand).getItem() instanceof BlockItem)
            return InteractionResult.PASS;

        if (!level.isClientSide && level instanceof ServerLevel serverLevel)
        {
            MonitorScreen.Screen screen = MonitorScreen.resolve(level, pos, state);
            if (level.getBlockEntity(screen.origin()) instanceof MonitorBlockEntity controller)
            {
                Component feedName = controller.cycleFeed(serverLevel, player.isShiftKeyDown() ? -1 : 1);
                player.displayClientMessage(Component.translatable("message.techarsenal.monitor_feed", feedName), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
