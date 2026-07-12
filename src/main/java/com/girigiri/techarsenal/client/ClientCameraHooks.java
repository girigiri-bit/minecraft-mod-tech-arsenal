package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.client.feed.FeedManager;
import com.girigiri.techarsenal.entity.CameraEntity;
import com.girigiri.techarsenal.item.CameraMonitorItem;
import com.girigiri.techarsenal.item.SatelliteRemoteItem;
import com.girigiri.techarsenal.network.CloseCameraViewPacket;
import com.girigiri.techarsenal.network.CycleCameraViewPacket;
import com.girigiri.techarsenal.network.ModNetwork;
import com.girigiri.techarsenal.network.OpenCameraViewPacket;
import com.girigiri.techarsenal.registry.ModEntities;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;

/**
 * Client-side camera view controller.
 * <p>
 * Two independent view kinds are handled here:
 * <ul>
 *   <li><b>SAT</b> (satellite remote) — a pure client-side view: a
 *       {@link CameraEntity} is created locally and handed to
 *       {@link Minecraft#setCameraEntity}; it is never added to the world.
 *       Sneak exits.</li>
 *   <li><b>CAM</b> (camera monitor, V-key) — server-driven since v0.9. The
 *       client only sends packets; the server spectates the player onto a real
 *       server-spawned {@link CameraEntity} via {@code ServerPlayer#setCamera},
 *       which streams the remote chunks and lifts the old 64m limit. The client
 *       detects the resulting remote view by watching {@link Minecraft#getCameraEntity()}
 *       and mirrors the server's truth — it never drives the CAM view locally.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCameraHooks
{
    // --- SAT (local) view state ---
    private static boolean active;
    private static String activeLabel = "";
    @Nullable
    private static CameraEntity camera;
    @Nullable
    private static CameraType previousCameraType;

    // --- CAM (server-driven remote) view state ---
    /** True while the server has us spectating a server-spawned CameraEntity. */
    private static boolean remoteActive;
    /** Hand that opened the CAM view once confirmed; null otherwise. Display only. */
    @Nullable
    private static InteractionHand activeHand;
    /** Hand of a pending OpenCameraViewPacket, promoted to activeHand on confirm. */
    @Nullable
    private static InteractionHand pendingHand;

    // Click-cycle debounce (shared: SAT swallows clicks, CAM sends cycle packets).
    private static boolean attackLatch;
    private static boolean useLatch;
    // Sneak-to-close debounce for the remote view (send CloseCameraViewPacket once
    // per hold instead of every tick).
    private static boolean shiftCloseLatch;

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

        // A stale activeHand could let a later view incorrectly accept cycling
        // input, so clear the shared click state here too.
        activeHand = null;
        attackLatch = false;
        useLatch = false;
    }

    public static boolean isActive()
    {
        return active || remoteActive;
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
        // Already viewing? V closes it. SAT is local; CAM asks the server to end
        // (client state flips only when the server actually reverts our camera,
        // so it can never desync from server truth).
        if (active)
        {
            deactivate();
            return;
        }
        if (remoteActive)
        {
            ModNetwork.CHANNEL.sendToServer(new CloseCameraViewPacket());
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
                // CAM view is fully server-driven now: just request it. The
                // server validates the item, list, registry and block, opens
                // the spectate, and writes SelectedCameraId back to the stack.
                int selectedId = CameraMonitorItem.getSelectedId(stack);
                pendingHand = hand;
                ModNetwork.CHANNEL.sendToServer(new OpenCameraViewPacket(hand.ordinal(), selectedId));
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.techarsenal.hold_view_item"), true);
    }

    /**
     * Refreshes the action-bar hint for whichever view is active. For the CAM
     * view the label is read live from the stack's SelectedCameraId (the server
     * owns it), so no client-side id tracking is needed.
     */
    private static void refreshHint(Minecraft mc)
    {
        Component hint;
        if (remoteActive && activeHand != null)
        {
            ItemStack held = mc.player.getItemInHand(activeHand);
            String label = "CAM-" + CameraMonitorItem.getSelectedId(held);
            if (held.getItem() instanceof CameraMonitorItem
                    && CameraMonitorItem.readCameras(held).size() >= 2)
                hint = Component.translatable("message.techarsenal.camera_cycle_hint", label);
            else
                hint = Component.translatable("message.techarsenal.camera_exit_hint_labeled", label);
        }
        else
        {
            hint = Component.translatable("message.techarsenal.camera_exit_hint_labeled", activeLabel);
        }
        mc.player.displayClientMessage(hint, true);
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (!active && !remoteActive)
            return;
        event.setCanceled(true);
        event.setSwingHand(false);

        // SAT just swallows clicks (no cycling); only the remote CAM view cycles.
        if (!remoteActive)
            return;

        if (event.isAttack())
        {
            if (!attackLatch)
            {
                attackLatch = true;
                ModNetwork.CHANNEL.sendToServer(new CycleCameraViewPacket(-1)); // left = previous
            }
        }
        else if (event.isUseItem())
        {
            if (event.getHand() == InteractionHand.MAIN_HAND && !useLatch)
            {
                useLatch = true;
                ModNetwork.CHANNEL.sendToServer(new CycleCameraViewPacket(1)); // right = next
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
            // Drop any remote-view bookkeeping too; the server session is gone
            // with the connection.
            remoteActive = false;
            activeHand = null;
            pendingHand = null;
            previousCameraType = null;
            attackLatch = false;
            useLatch = false;
            shiftCloseLatch = false;
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
        if (!mc.options.keyShift.isDown())
            shiftCloseLatch = false;

        // --- remote (CAM) view edge detection ---
        // The server drives the CAM view by setting our camera entity to a
        // server-spawned CameraEntity. The local SAT anchor is the only
        // CameraEntity we ever set ourselves, so anything else that is a
        // CameraEntity is the remote view.
        boolean nowRemote = mc.getCameraEntity() instanceof CameraEntity e && e != camera;
        if (nowRemote && !remoteActive)
        {
            remoteActive = true;
            previousCameraType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            activeHand = pendingHand;
            refreshHint(mc);
        }
        else if (!nowRemote && remoteActive)
        {
            if (previousCameraType != null)
                mc.options.setCameraType(previousCameraType);
            previousCameraType = null;
            remoteActive = false;
            activeHand = null;
            pendingHand = null;
            attackLatch = false;
            useLatch = false;
        }

        // --- SAT (local) view maintenance ---
        if (active)
        {
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
        }

        // --- remote (CAM) view: sneak requests close (once per hold) ---
        if (remoteActive && mc.options.keyShift.isDown() && !shiftCloseLatch)
        {
            shiftCloseLatch = true;
            ModNetwork.CHANNEL.sendToServer(new CloseCameraViewPacket());
        }

        // Keep the exit hint visible for the whole session — the action bar
        // fades after a few seconds and players otherwise look "stuck".
        if ((active || remoteActive) && mc.level.getGameTime() % 40L == 0L)
            refreshHint(mc);
    }

    /**
     * Draws the local player into its own monitor feeds ("see yourself on the
     * security camera") while a SAT view or a wall-monitor capture is active.
     * <p>
     * NOTE: intentionally NOT extended to {@code remoteActive}. In the
     * server-driven CAM view the player's actual body stands at the camera
     * position, so vanilla already draws it there — re-drawing it here would
     * duplicate it. This hack only matters when the body stays elsewhere (SAT,
     * or an offscreen monitor capture).
     * <p>
     * This manual draw is what actually puts the body into the <b>non-shader</b>
     * feed. During {@link FeedManager}'s offscreen {@code renderLevel}, vanilla
     * rebinds the main framebuffer before the {@code AFTER_ENTITIES} stage, so
     * every entity (including the local player, whether drawn by vanilla/Forge or
     * here) lands in the main target, not the feed's — terrain shows because it
     * renders earlier while the feed target is still bound. So the draw below
     * explicitly rebinds the feed target ({@link FeedManager#captureTarget})
     * before rendering and restores the previous framebuffer afterwards. Under a
     * shader pack the feed is a full copy of the main target
     * ({@code blitMainIntoFeed}), which already contains the body, so the rebind
     * is redundant there but harmless.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            return;
        boolean cap = FeedManager.isCapturing();
        if (!active && !cap)
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

        // During a monitor feed capture, vanilla's renderLevel has already
        // switched the bound framebuffer back to the main render target by the
        // AFTER_ENTITIES stage, so drawing here would land in the main target and
        // never reach the offscreen feed texture (blocks render earlier, while
        // the feed target is still bound, which is why terrain shows but the
        // player did not). Rebind the feed target for this draw, then restore the
        // exact previous framebuffer + viewport so the rest of renderLevel is
        // undisturbed. (Under shaders the feed is a copy of the main target, so
        // this is redundant but harmless.)
        RenderTarget feedTarget = cap ? FeedManager.captureTarget : null;
        int prevFbo = 0;
        int[] prevViewport = null;
        if (feedTarget != null)
        {
            prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            prevViewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
            feedTarget.bindWrite(true);
        }

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        dispatcher.render(player, x, y, z, Mth.lerp(partialTick, player.yRotO, player.getYRot()),
                partialTick, event.getPoseStack(), buffers,
                dispatcher.getPackedLightCoords(player, partialTick));
        buffers.endBatch();

        if (feedTarget != null)
        {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        }
    }

    /**
     * Hide the local player's own body ONLY in the CAM view (remoteActive): the
     * body is teleported onto the camera, so vanilla (camera entity != player)
     * renders the model point-blank around the lens - the camera sits inside the
     * avatar and sees the dark model interior, blacking out the view.
     * <p>
     * Deliberately NOT canceled during FeedManager.isCapturing(): wall-monitor
     * feeds must show the player's third-person body ("see yourself on the
     * security camera"). The first-person hand bleed is handled separately by
     * onRenderHand + the main-target clear in FeedManager.capture(); neither of
     * those touches the body. (History: a prior build canceled here during
     * capture and the body vanished from feeds - do not re-add isCapturing.)
     * SAT view (active) is unaffected: the body stays put and should show.
     */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event)
    {
        if (remoteActive && event.getEntity() == Minecraft.getInstance().player)
            event.setCanceled(true);
    }

    /**
     * The first-person hand/held item is a separate render from the player body,
     * so hiding the body above still leaves it floating in a camera view. Cancel
     * it while any TA camera view (SAT or CAM) is active. Also cancel during feed
     * capture in case a shader pack renders a hand pass inside the offscreen
     * renderLevel (belt-and-suspenders alongside the main-target clear).
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event)
    {
        if (active || remoteActive || FeedManager.isCapturing())
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event)
    {
        if (!active && !remoteActive)
            return;

        // While remote-viewing, the body is the server's to move; suppressing
        // input here (esp. shiftKeyDown) keeps vanilla's own sneak-dismount from
        // racing our explicit CloseCameraViewPacket.
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
