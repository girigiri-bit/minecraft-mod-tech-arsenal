package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.client.renderer.DroneRenderer;
import com.girigiri.techarsenal.client.renderer.MonitorBlockEntityRenderer;
import com.girigiri.techarsenal.client.renderer.NoopRenderer;
import com.girigiri.techarsenal.client.renderer.SpriteEntityRenderer;
import com.girigiri.techarsenal.registry.ModBlockEntities;
import com.girigiri.techarsenal.registry.ModEntities;
import com.girigiri.techarsenal.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup
{
    public static final KeyMapping CAMERA_VIEW_KEY = new KeyMapping(
            "key.techarsenal.camera_view", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.techarsenal");

    private ClientSetup()
    {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(CAMERA_VIEW_KEY);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.CAMERA.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GUIDED_MISSILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE_BOLT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MONITOR.get(), MonitorBlockEntityRenderer::new);

        // v0.3
        event.registerEntityRenderer(ModEntities.BULLET.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.ROCKET.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.TURRET.get(),
                ctx -> new SpriteEntityRenderer<>(ctx, ModItems.DEFENSE_TURRET, 1.8F, false, 0.7F));
        event.registerEntityRenderer(ModEntities.HELICOPTER.get(),
                ctx -> new SpriteEntityRenderer<>(ctx, ModItems.ATTACK_HELICOPTER, 3.2F, true, 0.8F));
        event.registerEntityRenderer(ModEntities.TANK.get(),
                ctx -> new SpriteEntityRenderer<>(ctx, ModItems.TANK, 3.0F, true, 0.7F));
    }
}
