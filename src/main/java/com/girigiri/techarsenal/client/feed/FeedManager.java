package com.girigiri.techarsenal.client.feed;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.block.SecurityCameraBlock;
import com.girigiri.techarsenal.blockentity.MonitorBlockEntity;
import com.girigiri.techarsenal.entity.CameraEntity;
import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.util.MonitorScreen;
import com.mojang.blaze3d.pipeline.TextureTarget;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-side live feed capture. Each active monitor screen gets an offscreen
 * RenderTarget; the level is re-rendered from the camera's viewpoint into it
 * at a fixed interval (one capture per frame at most), and the target's color
 * texture is drawn onto the monitor by the block entity renderer.
 */
@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FeedManager
{
    private static final int CAPTURE_INTERVAL_TICKS = 10;
    private static final long EVICT_AFTER_FRAMES = 600;
    private static final double MAX_CAMERA_DISTANCE = 64.0D;

    public record FeedView(@Nullable ResourceLocation texture, String label)
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
        CameraEntity camera;
        Vec3 capturePos = Vec3.ZERO;
        float captureYaw;
        float capturePitch;
        // Large negative sentinel; must stay far from Long.MIN_VALUE so
        // (gameTime - lastCaptureGameTime) can't overflow
        long lastCaptureGameTime = -1_000_000L;
        long lastRequestFrame;
        boolean everCaptured;
    }

    private static final Map<BlockPos, Feed> FEEDS = new HashMap<>();
    private static long frameCounter;
    private static boolean capturing;

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
            return new FeedView(null, "NO SIGNAL");

        int type = be.getFeedType();
        if (type == MonitorBlockEntity.FEED_OFF)
            return new FeedView(null, "OFF");

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
                return new FeedView(null, label);
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
        return new FeedView(feed.everCaptured ? feed.textureLocation : null, label);
    }

    private static Feed createFeed(BlockPos controllerPos)
    {
        Minecraft mc = Minecraft.getInstance();
        Feed feed = new Feed();
        int width = Math.max(mc.getWindow().getWidth() / 2, 320);
        int height = Math.max(mc.getWindow().getHeight() / 2, 180);
        feed.target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
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
        long gameTime = mc.level.getGameTime();
        Feed due = null;
        for (Feed feed : FEEDS.values())
        {
            if (frameCounter - feed.lastRequestFrame > 2)
                continue;
            if (gameTime - feed.lastCaptureGameTime < CAPTURE_INTERVAL_TICKS)
                continue;
            if (due == null || feed.lastCaptureGameTime < due.lastCaptureGameTime)
                due = feed;
        }
        if (due != null)
            capture(mc, due, gameTime);
    }

    private static void capture(Minecraft mc, Feed feed, long gameTime)
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

            mc.setCameraEntity(camera);
            feed.target.clear(Minecraft.ON_OSX);
            feed.target.bindWrite(true);
            mc.gameRenderer.renderLevel(1.0F, Util.getNanos(), new PoseStack());
            feed.lastCaptureGameTime = gameTime;
            feed.everCaptured = true;
            TechArsenal.LOGGER.debug("Captured monitor feed at {} from {}", feed.textureLocation, feed.capturePos);
        }
        catch (Exception e)
        {
            TechArsenal.LOGGER.error("Monitor feed capture failed", e);
            feed.lastCaptureGameTime = gameTime; // don't retry every frame
        }
        finally
        {
            mc.getMainRenderTarget().bindWrite(true);
            mc.setCameraEntity(previousCamera != null ? previousCamera : mc.player);
            capturing = false;
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
