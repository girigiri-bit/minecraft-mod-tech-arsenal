package com.girigiri.techarsenal.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Status display driven by a nearby face scanner: idle (blue), granted
 * (green check) or denied (red cross). The scanner updates every auth
 * monitor within 4 blocks whenever its status changes.
 */
public class AuthMonitorBlock extends HorizontalDirectionalBlock
{
    public enum Status implements StringRepresentable
    {
        IDLE("idle"),
        GRANTED("granted"),
        DENIED("denied");

        private final String name;

        Status(String name)
        {
            this.name = name;
        }

        @Override
        public String getSerializedName()
        {
            return name;
        }
    }

    public static final EnumProperty<Status> STATUS = EnumProperty.create("status", Status.class);

    public AuthMonitorBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATUS, Status.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, STATUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
