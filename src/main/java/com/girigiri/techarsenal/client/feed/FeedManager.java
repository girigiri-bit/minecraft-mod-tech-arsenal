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
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FeedManager
{
    // One feed capture per frame keeps the feed real-time; with several
    // screens visible the frames are shared round-robin. On failure a feed
    // backs off so a broken capture can't spam the log every frame.
    private static final long ERROR_BACKOFF_FRAMES = 40;
    private static final long EVICT_AFTER_FRAMES = 600;
    private static final double MAX_CAMERA_DISTANCE = 64.0D;

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
            feed.nextCaptureFrame = frameCounter + 1;
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
    }
}
