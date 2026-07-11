package com.girigiri.techarsenal.client.feed;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.blockentity.MonitorBlockEntity;
import com.girigiri.techarsenal.entity.CameraEntity;
import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.util.MonitorScreen;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-side live feed capture. Each active monitor screen gets an offscreen
 * RenderTarget; the level is re-rendered from the camera's viewpoint into it
 * every frame (one capture per frame, shared round-robin between screens),
 * and the target's color texture is drawn onto the monitor by the block
 * entity renderer.
 * <p>
 * While a shader pack (OptiFine/Iris) is active, an extra {@code renderLevel}
 * per frame corrupts the shader pack's global cross-frame state (frame parity,
 * temporal buffers, weather uniforms). Commit d30a27a established empirically
 * that this corruption only affects the single displayed frame immediately
 * following the extra render call - the pipeline self-recovers after it. So
 * instead of suspending capture forever, we keep the feed live via a
 * "masked capture frame": a real capture is allowed only roughly every
 * {@link #SHADER_REFRESH_INTERVAL_FRAMES} frames (~0.5s at 60fps), and the one
 * polluted frame it produces is hidden from the player by re-presenting the
 * previous frame. One frame BEFORE a scheduled capture, the fully-composited
 * main framebuffer is snapshotted into {@link #frameBackup} (Phase.END). On
 * the capture frame the normal capture runs at Phase.START (polluting shader
 * state for that frame only); then at Phase.END, before the frame is presented,
 * the backup is blitted back over the main render target, so the user sees a
 * repeated frame instead of the polluted one. Net cost is one duplicated frame
 * (indistinguishable from a dropped frame) roughly every 1.7s per on-screen
 * monitor.
 * <p>
 * Fallback safety nets: {@link #SHADER_LIVE_FEED} is a compile-time kill switch
 * - set it to {@code false} and rebuild to reproduce exactly the earlier
 * shipped behavior (commit 78493d4) where capture is fully suspended and feeds
 * freeze on their last frame while shaders are active. {@link #shaderLiveBroken}
 * is a runtime kill switch: any throwable in the masking path sets it and
 * permanently degrades to that same frozen-frame behavior for the rest of the
 * session, never crashing and never spamming the log.
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FeedManager
{
    // One feed capture per frame keeps the feed real-time; with several
    // screens visible the frames are shared round-robin. On failure a feed
    // backs off so a broken capture can't spam the log every frame. While a
    // shader pack is active this every-frame path is gated off (see
    // shadersActive() and the START gate in onRenderTick); capture is instead
    // driven by the masked-capture scheduler (see onFrameEnd / the class
    // javadoc), which allows one real capture every SHADER_REFRESH_INTERVAL_FRAMES
    // frames and masks the single frame it pollutes. This is safe because the
    // shader-state corruption from an extra renderLevel only affects that one
    // following frame (empirically established in commit d30a27a); it was only
    // continuous flicker back when capture ran every frame.
    private static final long ERROR_BACKOFF_FRAMES = 40;
    private static final long EVICT_AFTER_FRAMES = 600;
    private static final double MAX_CAMERA_DISTANCE = 64.0D;
    // Under a shader pack, allow a real (masked) capture only this often.
    private static final long SHADER_REFRESH_INTERVAL_FRAMES = 30;

    // Compile-time kill switch: false reproduces exactly the 78493d4 shipped
    // behavior (freeze feeds while shaders active, no masking attempted).
    private static final boolean SHADER_LIVE_FEED = true;
    // Large negative sentinel (same overflow-safety style as the Feed frame
    // fields) meaning "no masked capture is currently scheduled".
    private static final long MASKED_SENTINEL = -1_000_000L;

    public record FeedView(@Nullable ResourceLocation texture, String label, float aspect)
    {
        public boolean hasSignal()
        {
            return texture != null;
        }
    }

    private static final class Feed
    {
        TextureTarget target;
        ResourceLocation textureLocation;
        float aspect = 16.0F / 9.0F;
        CameraEntity camera;
        Vec3 capturePos = Vec3.ZERO;
        float captureYaw;
        float capturePitch;
        // Large negative sentinel; must stay far from Long.MIN_VALUE so
        // (frameCounter - nextCaptureFrame) can't overflow
        long nextCaptureFrame = -1_000_000L;
        long lastCaptureFrame = -1_000_000L;
        long lastRequestFrame;
        boolean everCaptured;
    }

    private static final Map<BlockPos, Feed> FEEDS = new HashMap<>();
    private static long frameCounter;
    private static boolean capturing;

    // Masked-capture state (only used while a shader pack is active). Full-
    // resolution backup of the composited main framebuffer, snapshotted one
    // frame before a scheduled capture and blitted back over the polluted
    // capture frame before it is presented. Lazily created, resized with the
    // window, destroyed with the feeds.
    private static TextureTarget frameBackup;
    // frameCounter value of the scheduled masked capture frame (the frame on
    // which the real capture runs and whose polluted output is then masked).
    private static long maskedCaptureFrame = MASKED_SENTINEL;
    // Runtime kill switch: set once (never reset) if the masking path throws;
    // afterwards behaves exactly like the 78493d4 gate (freeze under shaders).
    private static boolean shaderLiveBroken;

    // Shader mods (OptiFine, Iris/Oculus) composite the level into the main
    // render target regardless of which framebuffer is bound, so the feed
    // capture has to copy the result out of the main target afterwards.
    private static Method shaderCheck;
    private static Object shaderCheckReceiver;
    private static boolean shaderCheckResolved;

    private static boolean shadersActive()
    {
        if (!shaderCheckResolved)
        {
            shaderCheckResolved = true;
            try
            {
                // OptiFine: static boolean net.optifine.Config.isShaders()
                shaderCheck = Class.forName("net.optifine.Config").getMethod("isShaders");
            }
            catch (Throwable optifineAbsent)
            {
                try
                {
                    // Iris/Oculus: IrisApi.getInstance().isShaderPackInUse()
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    shaderCheckReceiver = api.getMethod("getInstance").invoke(null);
                    shaderCheck = api.getMethod("isShaderPackInUse");
                }
                catch (Throwable irisAbsent)
                {
                    shaderCheck = null;
                }
            }
        }
        if (shaderCheck == null)
            return false;
        try
        {
            return (Boolean) shaderCheck.invoke(shaderCheckReceiver);
        }
        catch (Throwable t)
        {
            shaderCheck = null;
            return false;
        }
    }

    public static boolean isCapturing()
    {
        return capturing;
    }

    private FeedManager()
    {
    }

    /**
     * Called from the monitor BER each frame for visible controller screens.
     * Registers/refreshes the feed and returns what to draw.
     */
    public static FeedView requestFeed(MonitorBlockEntity be, MonitorScreen.Screen screen)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return new FeedView(null, "NO SIGNAL", 1.0F);

        int type = be.getFeedType();
        if (type == MonitorBlockEntity.FEED_OFF)
            return new FeedView(null, "OFF", 1.0F);

        String label;
        Vec3 pos;
        float yaw;
        float pitch;

        if (type == MonitorBlockEntity.FEED_SAT)
        {
            label = "SAT";
            Direction right = screen.facing().getCounterClockWise();
            Vec3 screenCenter = Vec3.atCenterOf(screen.origin())
                    .add(Vec3.atLowerCornerOf(right.getNormal()).scale((screen.width() - 1) / 2.0D));
            pos = screenCenter.add(0.0D, 80.0D, 0.0D);
            yaw = screen.facing().toYRot();
            pitch = 90.0F;
        }
        else
        {
            label = "CAM-" + be.getCamId();
            BlockPos camPos = be.getCamPos();
            if (!mc.level.isLoaded(camPos)
                    || !(mc.level.getBlockState(camPos).getBlock() instanceof SecurityCameraBlock)
                    || !camPos.closerThan(be.getBlockPos(), MAX_CAMERA_DISTANCE))
            {
                return new FeedView(null, label, 1.0F);
            }
            yaw = be.getCamYaw();
            pitch = 15.0F;
            pos = Vec3.atCenterOf(camPos).add(Vec3.directionFromRotation(0.0F, yaw).scale(0.6D));
        }

        Feed feed = FEEDS.computeIfAbsent(be.getBlockPos().immutable(), FeedManager::createFeed);
        feed.capturePos = pos;
        feed.captureYaw = yaw;
        feed.capturePitch = pitch;
        feed.lastRequestFrame = frameCounter;
        // Under shaders, mark the label so the player can tell the feed's mode
        // apart: [SLOW] = live but refreshing only every ~1.7s via masked
        // capture; [SHADERS] = frozen on last frame (masking disabled/broken).
        if (shadersActive())
            label = label + ((SHADER_LIVE_FEED && !shaderLiveBroken) ? " [SLOW]" : " [SHADERS]");
        return new FeedView(feed.everCaptured ? feed.textureLocation : null, label, feed.aspect);
    }

    private static Feed createFeed(BlockPos controllerPos)
    {
        Minecraft mc = Minecraft.getInstance();
        Feed feed = new Feed();
        int width = Math.max(mc.getWindow().getWidth() / 2, 320);
        int height = Math.max(mc.getWindow().getHeight() / 2, 180);
        feed.target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        feed.aspect = (float) width / (float) height;
        feed.textureLocation = new ResourceLocation(TechArsenal.MODID,
                "feeds/" + controllerPos.getX() + "_" + controllerPos.getY() + "_" + controllerPos.getZ());
        mc.getTextureManager().register(feed.textureLocation, new RenderTargetTexture(feed.target));
        return feed;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        // TickEvent.Phase is START/END only. Phase.END fires after the world +
        // HUD have been rendered into the main render target but BEFORE it is
        // presented (Forge Minecraft.runTick: onRenderTickEnd is posted between
        // gameRenderer.render(...) and mainRenderTarget.blitToScreen(...) /
        // window.updateDisplay()), so it is the correct hook for both the
        // pre-capture backup snapshot and the post-capture restore blit.
        if (event.phase == TickEvent.Phase.END)
        {
            onFrameEnd();
            return;
        }
        if (event.phase != TickEvent.Phase.START)
            return;

        frameCounter++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        evictStale(mc);

        // Post-processing transparency chains rebind the main target mid-render
        if (mc.options.graphicsMode().get() == GraphicsStatus.FABULOUS)
            return;

        // A shader pack (OptiFine/Iris) keeps global cross-frame state (frame
        // parity, temporal buffers, weather uniforms); a second renderLevel
        // here pollutes that state for the one following frame. So while
        // shaders are active the every-frame path is gated off - EXCEPT on a
        // frame the masked-capture scheduler (onFrameEnd) has specifically
        // marked, where we do let the normal selection + capture run because
        // its polluted output will be masked at Phase.END. capture()'s own
        // shader branch (blitMainIntoFeed) still handles the copy-out.
        if (shadersActive()
                && !(SHADER_LIVE_FEED && !shaderLiveBroken && frameCounter == maskedCaptureFrame))
            return;

        // Capture at most one feed per frame, picking the most out-of-date one
        Feed due = null;
        for (Feed feed : FEEDS.values())
        {
            if (frameCounter - feed.lastRequestFrame > 2)
                continue;
            if (frameCounter < feed.nextCaptureFrame)
                continue;
            if (due == null || feed.lastCaptureFrame < due.lastCaptureFrame)
                due = feed;
        }
        if (due != null)
            capture(mc, due);
    }

    private static void capture(Minecraft mc, Feed feed)
    {
        if (capturing)
            return;
        capturing = true;
        Entity previousCamera = mc.getCameraEntity();
        try
        {
            if (feed.camera == null || feed.camera.level() != mc.level)
                feed.camera = new CameraEntity(ModEntities.CAMERA.get(), mc.level);
            CameraEntity camera = feed.camera;
            camera.moveTo(feed.capturePos.x, feed.capturePos.y, feed.capturePos.z, feed.captureYaw, feed.capturePitch);
            camera.xo = feed.capturePos.x;
            camera.yo = feed.capturePos.y;
            camera.zo = feed.capturePos.z;
            camera.yRotO = feed.captureYaw;
            camera.xRotO = feed.capturePitch;

            boolean shaders = shadersActive();
            mc.setCameraEntity(camera);
            feed.target.clear(Minecraft.ON_OSX);
            feed.target.bindWrite(true);
            mc.gameRenderer.renderLevel(1.0F, Util.getNanos(), new PoseStack());
            if (shaders)
                blitMainIntoFeed(mc, feed);
            feed.lastCaptureFrame = frameCounter;
            // Under shaders the frame cadence is decided by onFrameEnd, not by
            // this field; keep it consistent so that if shaders toggle off
            // right after, the every-frame path resumes from a sane baseline.
            feed.nextCaptureFrame = frameCounter + (shaders ? SHADER_REFRESH_INTERVAL_FRAMES : 1);
            feed.everCaptured = true;
        }
        catch (Exception e)
        {
            TechArsenal.LOGGER.error("Monitor feed capture failed", e);
            feed.lastCaptureFrame = frameCounter;
            feed.nextCaptureFrame = frameCounter + ERROR_BACKOFF_FRAMES;
        }
        finally
        {
            mc.getMainRenderTarget().bindWrite(true);
            mc.setCameraEntity(previousCamera != null ? previousCamera : mc.player);
            capturing = false;
        }
    }

    /**
     * With a shader pack active the composited camera view ends up in the main
     * render target (the capture happens before this frame's own level render,
     * which clears and redraws it, so nothing user-visible is disturbed).
     */
    private static void blitMainIntoFeed(Minecraft mc, Feed feed)
    {
        RenderTarget main = mc.getMainRenderTarget();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, feed.target.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height,
                0, 0, feed.target.width, feed.target.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /**
     * End-of-frame masked-capture bookkeeping, run at RenderTickEvent Phase.END
     * (after world + HUD render, before present). While a shader pack is
     * active this either (a) restores the previous frame over the just-rendered
     * polluted capture frame, or (b) snapshots the current frame and schedules
     * the next frame to be a masked capture. Any throwable here permanently and
     * safely degrades to the frozen-frame fallback (shaderLiveBroken).
     */
    private static void onFrameEnd()
    {
        if (!SHADER_LIVE_FEED || shaderLiveBroken)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        // Match the START gate: never touch the composited frame under FABULOUS.
        if (mc.options.graphicsMode().get() == GraphicsStatus.FABULOUS)
            return;
        if (!shadersActive())
        {
            // Shaders just turned off: drop any pending schedule so a stale
            // mask can never be applied to a real (non-shader) frame.
            maskedCaptureFrame = MASKED_SENTINEL;
            return;
        }

        try
        {
            if (frameCounter == maskedCaptureFrame)
            {
                // This is the polluted capture frame that was just rendered.
                // Hide it by re-presenting the frame we backed up last frame.
                RenderTarget main = mc.getMainRenderTarget();
                if (frameBackup != null
                        && frameBackup.width == main.width
                        && frameBackup.height == main.height)
                {
                    restoreMaskedFrame(mc);
                }
                // else: window resized in the single frame between backup and
                // mask (rare). Let the one polluted frame through rather than
                // blit a mismatched size - this is an accepted rare edge case.
                maskedCaptureFrame = MASKED_SENTINEL;
            }
            else if (maskedCaptureFrame < frameCounter)
            {
                // No capture pending: decide whether the NEXT frame should be a
                // masked capture. A feed qualifies if it is currently on-screen
                // (same freshness window as the START selection loop) and it is
                // due for a refresh. Only one masked capture is ever pending at
                // a time; different feeds take turns across successive windows.
                Feed due = null;
                for (Feed feed : FEEDS.values())
                {
                    if (frameCounter - feed.lastRequestFrame > 2)
                        continue;
                    if (frameCounter - feed.lastCaptureFrame < SHADER_REFRESH_INTERVAL_FRAMES)
                        continue;
                    if (due == null || feed.lastCaptureFrame < due.lastCaptureFrame)
                        due = feed;
                }
                if (due != null)
                {
                    ensureFrameBackup(mc);
                    backupMainFrame(mc);
                    // START of the next frame increments frameCounter to this
                    // value; the START gate then lets that one capture through.
                    maskedCaptureFrame = frameCounter + 1;
                }
            }
        }
        catch (Throwable t)
        {
            TechArsenal.LOGGER.error(
                    "Masked shader capture failed; falling back to frozen feed under shaders", t);
            shaderLiveBroken = true;
            maskedCaptureFrame = MASKED_SENTINEL;
        }
    }

    private static void ensureFrameBackup(Minecraft mc)
    {
        RenderTarget main = mc.getMainRenderTarget();
        if (frameBackup == null || frameBackup.width != main.width || frameBackup.height != main.height)
        {
            if (frameBackup != null)
                frameBackup.destroyBuffers();
            frameBackup = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        }
    }

    // Snapshot the composited main framebuffer into the backup (same size, so
    // GL_NEAREST is exact and cheap). Opposite direction from restore.
    private static void backupMainFrame(Minecraft mc)
    {
        RenderTarget main = mc.getMainRenderTarget();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, frameBackup.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height,
                0, 0, frameBackup.width, frameBackup.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    // Blit the backup back over the main render target, replacing the polluted
    // capture frame just before it is presented. Caller has verified the sizes
    // match, so GL_NEAREST is exact.
    private static void restoreMaskedFrame(Minecraft mc)
    {
        RenderTarget main = mc.getMainRenderTarget();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frameBackup.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, frameBackup.width, frameBackup.height,
                0, 0, main.width, main.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void destroyFrameBackup()
    {
        if (frameBackup != null)
        {
            frameBackup.destroyBuffers();
            frameBackup = null;
        }
    }

    private static void evictStale(Minecraft mc)
    {
        Iterator<Map.Entry<BlockPos, Feed>> it = FEEDS.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<BlockPos, Feed> entry = it.next();
            if (frameCounter - entry.getValue().lastRequestFrame > EVICT_AFTER_FRAMES)
            {
                destroy(mc, entry.getValue());
                it.remove();
            }
        }
        // No feeds left: release the full-resolution masked-capture backup too.
        if (FEEDS.isEmpty())
            destroyFrameBackup();
    }

    private static void destroy(Minecraft mc, Feed feed)
    {
        mc.getTextureManager().release(feed.textureLocation);
        feed.target.destroyBuffers();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event)
    {
        if (!event.getLevel().isClientSide())
            return;
        Minecraft mc = Minecraft.getInstance();
        FEEDS.values().forEach(feed -> destroy(mc, feed));
        FEEDS.clear();
        destroyFrameBackup();
    }
}
