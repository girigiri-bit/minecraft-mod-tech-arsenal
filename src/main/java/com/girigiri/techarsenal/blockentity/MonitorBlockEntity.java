package com.girigiri.techarsenal.blockentity;

import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.registry.ModBlockEntities;
import com.girigiri.techarsenal.world.CameraRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MonitorBlockEntity extends BlockEntity
{
    public static final int FEED_OFF = 0;
    public static final int FEED_SAT = 1;
    public static final int FEED_CAM = 2;

    private int feedType = FEED_OFF;
    private int camId;
    private BlockPos camPos = BlockPos.ZERO;
    private float camYaw;

    public MonitorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.MONITOR.get(), pos, state);
    }

    public int getFeedType()
    {
        return feedType;
    }

    public int getCamId()
    {
        return camId;
    }

    public BlockPos getCamPos()
    {
        return camPos;
    }

    public float getCamYaw()
    {
        return camYaw;
    }

    /** Cycles OFF -> SAT -> CAM-1 -> CAM-2 -> ... and returns the new feed's display name. */
    public Component cycleFeed(ServerLevel level, int direction)
    {
        CameraRegistry registry = CameraRegistry.get(level);
        List<Map.Entry<Integer, BlockPos>> cameras = new ArrayList<>();
        for (Map.Entry<Integer, BlockPos> entry : registry.all().entrySet())
        {
            BlockState state = level.getBlockState(entry.getValue());
            if (state.getBlock() instanceof SecurityCameraBlock)
                cameras.add(entry);
            else
                registry.unregister(entry.getValue());
        }

        int feedCount = 2 + cameras.size(); // OFF, SAT, cams...
        int current = switch (feedType)
        {
            case FEED_SAT -> 1;
            case FEED_CAM -> {
                int index = 2;
                for (int i = 0; i < cameras.size(); i++)
                {
                    if (cameras.get(i).getKey() == camId)
                    {
                        index = 2 + i;
                        break;
                    }
                }
                yield index;
            }
            default -> 0;
        };

        int next = Math.floorMod(current + direction, feedCount);
        if (next == 0)
        {
            feedType = FEED_OFF;
            camId = 0;
        }
        else if (next == 1)
        {
            feedType = FEED_SAT;
            camId = 0;
        }
        else
        {
            Map.Entry<Integer, BlockPos> camera = cameras.get(next - 2);
            feedType = FEED_CAM;
            camId = camera.getKey();
            camPos = camera.getValue();
            camYaw = level.getBlockState(camPos).getValue(HorizontalDirectionalBlock.FACING).toYRot();
        }

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return getFeedName();
    }

    public Component getFeedName()
    {
        return switch (feedType)
        {
            case FEED_SAT -> Component.literal("SAT");
            case FEED_CAM -> Component.literal("CAM-" + camId);
            default -> Component.translatable("message.techarsenal.feed_off");
        };
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        feedType = tag.getInt("FeedType");
        camId = tag.getInt("CamId");
        camPos = BlockPos.of(tag.getLong("CamPos"));
        camYaw = tag.getFloat("CamYaw");
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("FeedType", feedType);
        tag.putInt("CamId", camId);
        tag.putLong("CamPos", camPos.asLong());
        tag.putFloat("CamYaw", camYaw);
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox()
    {
        // Screens can span up to 5x5 blocks from the controller
        return new AABB(worldPosition).inflate(MonitorScreenBounds.INFLATE);
    }

    private static final class MonitorScreenBounds
    {
        static final double INFLATE = 6.0D;
    }
}
