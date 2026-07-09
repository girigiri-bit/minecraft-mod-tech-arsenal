package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.client.model.HelicopterModel;
import com.girigiri.techarsenal.client.model.TankModel;
import com.girigiri.techarsenal.client.model.TurretModel;
import com.girigiri.techarsenal.client.renderer.DroneRenderer;
import com.girigiri.techarsenal.client.renderer.MonitorBlockEntityRenderer;
import com.girigiri.techarsenal.client.renderer.NoopRenderer;
import com.girigiri.techarsenal.client.renderer.VehicleRenderers;
import com.girigiri.techarsenal.registry.ModBlockEntities;
import com.girigiri.techarsenal.registry.ModEntities;
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
        event.registerEntityRenderer(ModEntities.TURRET.get(), VehicleRenderers.Turret::new);
        event.registerEntityRenderer(ModEntities.HELICOPTER.get(), VehicleRenderers.Helicopter::new);
        event.registerEntityRenderer(ModEntities.TANK.get(), VehicleRenderers.Tank::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(VehicleRenderers.TANK_LAYER, TankModel::createBodyLayer);
        event.registerLayerDefinition(VehicleRenderers.HELICOPTER_LAYER, HelicopterModel::createBodyLayer);
        event.registerLayerDefinition(VehicleRenderers.TURRET_LAYER, TurretModel::createBodyLayer);
    }
}
