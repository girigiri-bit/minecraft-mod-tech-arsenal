package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.entity.CameraEntity;
import com.girigiri.techarsenal.item.CameraMonitorItem;
import com.girigiri.techarsenal.item.SatelliteRemoteItem;
import com.girigiri.techarsenal.registry.ModEntities;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Client-only camera view controller. The camera entity is never added to the
 * world — it only exists as the render viewpoint. Sneak exits the view.
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCameraHooks
{
    private static boolean active;
    private static String activeLabel = "";
    @Nullable
    private static CameraEntity camera;
    @Nullable
    private static CameraType previousCameraType;

    private ClientCameraHooks()
    {
    }

    public static void activate(Vec3 pos, float yaw, float pitch, String label)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || active)
            return;

        CameraEntity entity = new CameraEntity(ModEntities.CAMERA.get(), mc.level);
        entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
        entity.xo = pos.x;
        entity.yo = pos.y;
        entity.zo = pos.z;
        entity.yRotO = yaw;
        entity.xRotO = pitch;

        camera = entity;
        activeLabel = label;
        previousCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(entity);
        active = true;

        mc.player.displayClientMessage(Component.translatable("message.techarsenal.camera_exit_hint_labeled", label), true);
    }

    public static void deactivate()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.setCameraEntity(mc.player);
        if (previousCameraType != null)
            mc.options.setCameraType(previousCameraType);
        camera = null;
        previousCameraType = null;
        active = false;
    }

    public static boolean isActive()
    {
        return active;
    }

    /** Shown when right-clicking a view item — the view itself is on the keybind. */
    public static void showViewKeyHint()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.player.displayClientMessage(Component.translatable("message.techarsenal.press_view_key",
                    ClientSetup.CAMERA_VIEW_KEY.getTranslatedKeyMessage()), true);
    }

    /** Keybind handler: toggles the camera view based on the held item. */
    private static void toggleFromHeldItem(Minecraft mc)
    {
        if (active)
        {
            deactivate();
            return;
        }

        Player player = mc.player;
        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof SatelliteRemoteItem)
            {
                Vec3 viewPos = new Vec3(player.getX(),
                        player.getEyeY() + SatelliteRemoteItem.SATELLITE_HEIGHT, player.getZ());
                activate(viewPos, player.getYRot(), 90.0F, "SAT");
                return;
            }
            if (stack.getItem() instanceof CameraMonitorItem)
            {
                openLinkedCamera(mc, player, stack);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.techarsenal.hold_view_item"), true);
    }

    private static void openLinkedCamera(Minecraft mc, Player player, ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CameraMonitorItem.TAG_POS))
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_not_linked"), true);
            return;
        }

        BlockPos cameraPos = BlockPos.of(tag.getLong(CameraMonitorItem.TAG_POS));
        float yaw = tag.getFloat(CameraMonitorItem.TAG_YAW);

        if (!(mc.level.getBlockState(cameraPos).getBlock() instanceof SecurityCameraBlock))
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_missing"), true);
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(cameraPos)) > CameraMonitorItem.MAX_VIEW_DISTANCE * CameraMonitorItem.MAX_VIEW_DISTANCE)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_out_of_range"), true);
            return;
        }

        Vec3 facing = Vec3.directionFromRotation(0.0F, yaw);
        Vec3 viewPos = Vec3.atCenterOf(cameraPos).add(facing.scale(0.6D));
        String label = tag.contains(CameraMonitorItem.TAG_ID) ? "CAM-" + tag.getInt(CameraMonitorItem.TAG_ID) : "CAM";
        activate(viewPos, yaw, 15.0F, label);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            if (active)
                deactivate();
            return;
        }

        while (ClientSetup.CAMERA_VIEW_KEY.consumeClick())
            toggleFromHeldItem(mc);

        if (!active)
            return;

        if (camera == null || camera.level() != mc.level)
        {
            deactivate();
            return;
        }

        if (mc.options.keyShift.isDown())
        {
            deactivate();
            return;
        }

        // Keep the exit hint visible for the whole camera session — the action
        // bar fades after a few seconds and players otherwise look "stuck"
        if (mc.level.getGameTime() % 40L == 0L)
            mc.player.displayClientMessage(
                    Component.translatable("message.techarsenal.camera_exit_hint_labeled", activeLabel), true);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event)
    {
        if (!active)
            return;

        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }
}
