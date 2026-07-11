package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.client.feed.FeedManager;
import com.girigiri.techarsenal.entity.CameraEntity;
import com.girigiri.techarsenal.item.CameraMonitorItem;
import com.girigiri.techarsenal.item.SatelliteRemoteItem;
import com.girigiri.techarsenal.network.ModNetwork;
import com.girigiri.techarsenal.network.SelectCameraPacket;
import com.girigiri.techarsenal.registry.ModEntities;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

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

    /** Hand that opened the current CameraMonitorItem view; null for SAT view or no view. */
    @Nullable
    private static InteractionHand activeHand;
    private static int activeCameraId = -1;
    private static boolean attackLatch;
    private static boolean useLatch;

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

        // A stale activeHand could let a later SAT view incorrectly accept
        // cycling input, so clear all of the click-cycling state here too.
        activeHand = null;
        activeCameraId = -1;
        attackLatch = false;
        useLatch = false;
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
                openLinkedCamera(mc, player, stack, hand);
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.techarsenal.hold_view_item"), true);
    }

    private static boolean isCameraValid(Minecraft mc, Player player, BlockPos pos)
    {
        return mc.level.getBlockState(pos).getBlock() instanceof SecurityCameraBlock
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= CameraMonitorItem.MAX_VIEW_DISTANCE * CameraMonitorItem.MAX_VIEW_DISTANCE;
    }

    private static Vec3 viewPosFor(BlockPos cameraPos, float yaw)
    {
        Vec3 facing = Vec3.directionFromRotation(0.0F, yaw);
        return Vec3.atCenterOf(cameraPos).add(facing.scale(0.6D));
    }

    private static void openLinkedCamera(Minecraft mc, Player player, ItemStack stack, InteractionHand hand)
    {
        List<CameraMonitorItem.CameraLink> cameras = CameraMonitorItem.readCameras(stack);
        if (cameras.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_not_linked"), true);
            return;
        }

        int selectedId = CameraMonitorItem.getSelectedId(stack);
        CameraMonitorItem.CameraLink chosen = null;
        for (CameraMonitorItem.CameraLink link : cameras)
        {
            if (link.id() == selectedId && isCameraValid(mc, player, link.pos()))
            {
                chosen = link;
                break;
            }
        }
        if (chosen == null)
        {
            for (CameraMonitorItem.CameraLink link : cameras)
            {
                if (isCameraValid(mc, player, link.pos()))
                {
                    chosen = link;
                    break;
                }
            }
        }
        if (chosen == null)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_none_valid"), true);
            return;
        }

        Vec3 viewPos = viewPosFor(chosen.pos(), chosen.yaw());
        String label = "CAM-" + chosen.id();
        activate(viewPos, chosen.yaw(), 15.0F, label);
        activeHand = hand;
        activeCameraId = chosen.id();

        if (chosen.id() != selectedId)
            ModNetwork.CHANNEL.sendToServer(new SelectCameraPacket(hand.ordinal(), chosen.id()));
    }

    /**
     * Left/right-click while a camera view (not SAT) is active: step to the
     * next lower/higher registered camera id, wrapping, skipping any entry
     * whose block is gone or now out of range of the player's live position.
     * Bounded to list.size() attempts so it can never hang.
     */
    private static void cycleCamera(Minecraft mc, int direction)
    {
        if (activeHand == null)
            return;
        Player player = mc.player;
        if (player == null)
            return;

        ItemStack stack = player.getItemInHand(activeHand);
        if (!(stack.getItem() instanceof CameraMonitorItem))
        {
            deactivate();
            return;
        }

        List<CameraMonitorItem.CameraLink> cameras = CameraMonitorItem.readCameras(stack);
        if (cameras.isEmpty() || camera == null)
        {
            deactivate();
            return;
        }

        int size = cameras.size();
        int currentIndex = -1;
        for (int i = 0; i < size; i++)
        {
            if (cameras.get(i).id() == activeCameraId)
            {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1)
            currentIndex = direction > 0 ? size - 1 : 0;

        CameraMonitorItem.CameraLink chosen = null;
        int index = currentIndex;
        for (int attempt = 0; attempt < size; attempt++)
        {
            index = Math.floorMod(index + direction, size);
            CameraMonitorItem.CameraLink candidate = cameras.get(index);
            if (candidate.id() == activeCameraId && size > 1)
                continue;
            if (isCameraValid(mc, player, candidate.pos()))
            {
                chosen = candidate;
                break;
            }
        }

        if (chosen == null)
        {
            player.displayClientMessage(Component.translatable("message.techarsenal.camera_no_switch_target"), true);
            return;
        }

        Vec3 viewPos = viewPosFor(chosen.pos(), chosen.yaw());
        camera.moveTo(viewPos.x, viewPos.y, viewPos.z, chosen.yaw(), 15.0F);
        camera.xo = viewPos.x;
        camera.yo = viewPos.y;
        camera.zo = viewPos.z;
        camera.yRotO = chosen.yaw();
        camera.xRotO = 15.0F;

        activeCameraId = chosen.id();
        activeLabel = "CAM-" + chosen.id();
        player.displayClientMessage(Component.translatable("message.techarsenal.camera_exit_hint_labeled", activeLabel), true);

        ModNetwork.CHANNEL.sendToServer(new SelectCameraPacket(activeHand.ordinal(), chosen.id()));
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (!active)
            return;
        event.setCanceled(true);
        event.setSwingHand(false);
        Minecraft mc = Minecraft.getInstance();
        if (event.isAttack())
        {
            if (!attackLatch)
            {
                attackLatch = true;
                cycleCamera(mc, -1);
            }
        }
        else if (event.isUseItem())
        {
            if (event.getHand() == InteractionHand.MAIN_HAND && !useLatch)
            {
                useLatch = true;
                cycleCamera(mc, 1);
            }
        }
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

        // Debounce latch release: InteractionKeyMappingTriggered re-fires every
        // tick (attack) / every few ticks (use item) while a button is held, so
        // the latches only clear once the underlying vanilla key is released.
        // Runs every tick regardless of view-active state so they can't get stuck.
        if (!mc.options.keyAttack.isDown())
            attackLatch = false;
        if (!mc.options.keyUse.isDown())
            useLatch = false;

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
        {
            Component hint = Component.translatable("message.techarsenal.camera_exit_hint_labeled", activeLabel);
            if (activeHand != null)
            {
                ItemStack heldStack = mc.player.getItemInHand(activeHand);
                if (heldStack.getItem() instanceof CameraMonitorItem
                        && CameraMonitorItem.readCameras(heldStack).size() >= 2)
                    hint = Component.translatable("message.techarsenal.camera_cycle_hint", activeLabel);
            }
            mc.player.displayClientMessage(hint, true);
        }
    }

    /**
     * Vanilla skips rendering the local player whenever the camera entity is
     * not the player, so they would be invisible in their own camera feeds.
     * Render them manually while a camera view or a monitor capture is active.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            return;
        if (!active && !FeedManager.isCapturing())
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isRemoved() || player.isSpectator())
            return;

        float partialTick = event.getPartialTick();
        Vec3 camPos = event.getCamera().getPosition();
        double x = Mth.lerp(partialTick, player.xOld, player.getX()) - camPos.x;
        double y = Mth.lerp(partialTick, player.yOld, player.getY()) - camPos.y;
        double z = Mth.lerp(partialTick, player.zOld, player.getZ()) - camPos.z;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        dispatcher.render(player, x, y, z, Mth.lerp(partialTick, player.yRotO, player.getYRot()),
                partialTick, event.getPoseStack(), buffers,
                dispatcher.getPackedLightCoords(player, partialTick));
        buffers.endBatch();
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
