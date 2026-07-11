package com.girigiri.techarsenal.item;

import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.client.ClientCameraHooks;
import com.girigiri.techarsenal.world.CameraRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CameraMonitorItem extends Item
{
    public static final String TAG_POS = "CameraPos";
    public static final String TAG_YAW = "CameraYaw";
    public static final String TAG_ID = "CameraId";
    public static final String TAG_CAMERAS = "Cameras";
    public static final String TAG_SELECTED_ID = "SelectedCameraId";
    public static final int MAX_CAMERAS = 24;

    public CameraMonitorItem(Properties properties)
    {
        super(properties);
    }

    /** One linked camera entry. Immutable snapshot of a "Cameras" list compound. */
    public record CameraLink(BlockPos pos, float yaw, int id) {}

    /**
     * Reads the linked-camera list. Client-safe: never mutates the stack.
     * If the new-format "Cameras" list is absent but a legacy root-level
     * CameraPos tag is present, synthesizes a 1-entry list from it in memory
     * so old items keep working before the server-side migration runs.
     * Result is sorted ascending by camera id.
     */
    public static List<CameraLink> readCameras(ItemStack stack)
    {
        List<CameraLink> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null)
            return result;

        if (tag.contains(TAG_CAMERAS, Tag.TAG_LIST))
        {
            ListTag list = tag.getList(TAG_CAMERAS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag entry = list.getCompound(i);
                result.add(new CameraLink(BlockPos.of(entry.getLong(TAG_POS)), entry.getFloat(TAG_YAW), entry.getInt(TAG_ID)));
            }
        }
        else if (tag.contains(TAG_POS))
        {
            result.add(new CameraLink(BlockPos.of(tag.getLong(TAG_POS)), tag.getFloat(TAG_YAW),
                    tag.contains(TAG_ID) ? tag.getInt(TAG_ID) : 0));
        }

        result.sort(Comparator.comparingInt(CameraLink::id));
        return result;
    }

    /**
     * One-time server-side migration: converts a legacy root-scalar item
     * (CameraPos/CameraYaw/CameraId) into the new "Cameras" list format and
     * removes the old scalar tags. No-op if there's nothing to migrate.
     * Only ever called from server-side write paths.
     */
    public static void migrateLegacyTag(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_POS) || tag.contains(TAG_CAMERAS, Tag.TAG_LIST))
            return;

        int id = tag.contains(TAG_ID) ? tag.getInt(TAG_ID) : 0;
        CompoundTag entry = new CompoundTag();
        entry.putLong(TAG_POS, tag.getLong(TAG_POS));
        entry.putFloat(TAG_YAW, tag.getFloat(TAG_YAW));
        entry.putInt(TAG_ID, id);

        ListTag list = new ListTag();
        list.add(entry);

        tag.remove(TAG_POS);
        tag.remove(TAG_YAW);
        tag.remove(TAG_ID);
        tag.put(TAG_CAMERAS, list);
        tag.putInt(TAG_SELECTED_ID, id);
    }

    public static int getSelectedId(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_SELECTED_ID) ? tag.getInt(TAG_SELECTED_ID) : -1;
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
            if (!level.isClientSide && level instanceof ServerLevel serverLevel)
            {
                ItemStack stack = context.getItemInHand();
                migrateLegacyTag(stack);

                if (player.isShiftKeyDown())
                    removeCamera(player, stack, pos);
                else
                    addCamera(player, serverLevel, stack, pos, state);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void addCamera(Player player, ServerLevel serverLevel, ItemStack stack, BlockPos pos, BlockState state)
    {
        List<CameraLink> cameras = readCameras(stack);
        for (CameraLink link : cameras)
        {
            if (link.pos().equals(pos))
            {
                player.displayClientMessage(Component.translatable("message.techarsenal.camera_already_linked", link.id()), true);
                return;
            }
        }
        if (cameras.size() >= MAX_CAMERAS)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_list_full"), true);
            return;
        }

        int id = CameraRegistry.get(serverLevel).getOrAssign(pos);
        float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();

        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.contains(TAG_CAMERAS, Tag.TAG_LIST) ? tag.getList(TAG_CAMERAS, Tag.TAG_COMPOUND) : new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putLong(TAG_POS, pos.asLong());
        entry.putFloat(TAG_YAW, yaw);
        entry.putInt(TAG_ID, id);
        list.add(entry);
        tag.put(TAG_CAMERAS, list);

        if (!tag.contains(TAG_SELECTED_ID))
            tag.putInt(TAG_SELECTED_ID, id);

        player.displayClientMessage(Component.translatable("message.techarsenal.camera_added",
                id, pos.getX(), pos.getY(), pos.getZ(), list.size()), true);
    }

    private static void removeCamera(Player player, ItemStack stack, BlockPos pos)
    {
        List<CameraLink> cameras = readCameras(stack);
        CameraLink toRemove = null;
        for (CameraLink link : cameras)
        {
            if (link.pos().equals(pos))
            {
                toRemove = link;
                break;
            }
        }
        if (toRemove == null)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_not_registered"), true);
            return;
        }

        cameras.remove(toRemove);

        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = new ListTag();
        for (CameraLink link : cameras)
        {
            CompoundTag entry = new CompoundTag();
            entry.putLong(TAG_POS, link.pos().asLong());
            entry.putFloat(TAG_YAW, link.yaw());
            entry.putInt(TAG_ID, link.id());
            list.add(entry);
        }
        tag.put(TAG_CAMERAS, list);

        if (getSelectedId(stack) == toRemove.id())
        {
            if (cameras.isEmpty())
                tag.remove(TAG_SELECTED_ID);
            else
                tag.putInt(TAG_SELECTED_ID, cameras.get(0).id());
        }

        player.displayClientMessage(Component.translatable("message.techarsenal.camera_unlinked_one",
                toRemove.id(), cameras.size()), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        // Viewing is bound to a dedicated key so right-click can't lock the
        // player into camera view by accident — just point at the keybind
        if (level.isClientSide)
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientCameraHooks::showViewKeyHint);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        List<CameraLink> cameras = readCameras(stack);
        if (!cameras.isEmpty())
        {
            tooltip.add(Component.translatable("tooltip.techarsenal.camera_monitor.linked_count",
                    cameras.size(), getSelectedId(stack)).withStyle(ChatFormatting.GRAY));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.techarsenal.camera_monitor.unlinked").withStyle(ChatFormatting.GRAY));
        }
    }
}
