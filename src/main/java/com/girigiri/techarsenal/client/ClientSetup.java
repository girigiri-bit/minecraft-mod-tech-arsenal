package com.girigiri.techarsenal.client;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.client.renderer.DroneRenderer;
import com.girigiri.techarsenal.client.renderer.NoopRenderer;
import com.girigiri.techarsenal.registry.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TechArsenal.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup
{
    private ClientSetup()
    {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.CAMERA.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.GUIDED_MISSILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE_BOLT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
    }
}
