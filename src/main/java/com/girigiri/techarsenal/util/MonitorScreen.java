package com.girigiri.techarsenal.util;

import com.girigiri.techarsenal.block.MonitorBlock;
import com.girigiri.techarsenal.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resolves the rectangle of connected monitor blocks (same facing) that a
 * monitor belongs to. The origin (controller) is the bottom, viewer-left
 * block; only the controller stores the feed and renders the screen.
 */
public final class MonitorScreen
{
    public static final int MAX_SIZE = 5;

    public record Screen(BlockPos origin, int width, int height, Direction facing) {}

    private MonitorScreen()
    {
    }

    public static Screen resolve(BlockGetter level, BlockPos pos, BlockState state)
    {
        Direction facing = state.getValue(MonitorBlock.FACING);
        // "right" as seen by a viewer standing in front of the screen
        Direction right = facing.getCounterClockWise();
        Direction left = right.getOpposite();

        BlockPos origin = pos;
        for (int i = 0; i < MAX_SIZE - 1 && isMonitor(level, origin.relative(left), facing); i++)
            origin = origin.relative(left);
        for (int i = 0; i < MAX_SIZE - 1 && isMonitor(level, origin.below(), facing); i++)
            origin = origin.below();

        int width = 1;
        while (width < MAX_SIZE && isMonitor(level, origin.relative(right, width), facing))
            width++;

        int height = 1;
        outer:
        while (height < MAX_SIZE)
        {
            for (int x = 0; x < width; x++)
            {
                if (!isMonitor(level, origin.above(height).relative(right, x), facing))
                    break outer;
            }
            height++;
        }
        return new Screen(origin.immutable(), width, height, facing);
    }

    private static boolean isMonitor(BlockGetter level, BlockPos pos, Direction facing)
    {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.MONITOR.get()) && state.getValue(MonitorBlock.FACING) == facing;
    }
}
