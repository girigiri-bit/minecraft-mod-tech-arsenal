package com.girigiri.techarsenal.client.renderer;

import com.girigiri.techarsenal.TechArsenal;
import com.girigiri.techarsenal.client.model.HelicopterModel;
import com.girigiri.techarsenal.client.model.TankModel;
import com.girigiri.techarsenal.client.model.TurretModel;
import com.girigiri.techarsenal.entity.HelicopterEntity;
import com.girigiri.techarsenal.entity.TankEntity;
import com.girigiri.techarsenal.entity.TurretEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** MobRenderers for the voxel-model vehicles and machines. */
public final class VehicleRenderers
{
    public static final ModelLayerLocation TANK_LAYER =
            new ModelLayerLocation(new ResourceLocation(TechArsenal.MODID, "tank"), "main");
    public static final ModelLayerLocation HELICOPTER_LAYER =
            new ModelLayerLocation(new ResourceLocation(TechArsenal.MODID, "attack_helicopter"), "main");
    public static final ModelLayerLocation TURRET_LAYER =
            new ModelLayerLocation(new ResourceLocation(TechArsenal.MODID, "defense_turret"), "main");

    private static final ResourceLocation TANK_TEXTURE =
            new ResourceLocation(TechArsenal.MODID, "textures/entity/tank.png");
    private static final ResourceLocation HELICOPTER_TEXTURE =
            new ResourceLocation(TechArsenal.MODID, "textures/entity/attack_helicopter.png");
    private static final ResourceLocation TURRET_TEXTURE =
            new ResourceLocation(TechArsenal.MODID, "textures/entity/defense_turret.png");

    private VehicleRenderers()
    {
    }

    public static class Tank extends MobRenderer<TankEntity, TankModel>
    {
        public Tank(EntityRendererProvider.Context context)
        {
            super(context, new TankModel(context.bakeLayer(TANK_LAYER)), 1.1F);
        }

        @Override
        public ResourceLocation getTextureLocation(TankEntity entity)
        {
            return TANK_TEXTURE;
        }
    }

    public static class Helicopter extends MobRenderer<HelicopterEntity, HelicopterModel>
    {
        public Helicopter(EntityRendererProvider.Context context)
        {
            super(context, new HelicopterModel(context.bakeLayer(HELICOPTER_LAYER)), 1.1F);
        }

        @Override
        public ResourceLocation getTextureLocation(HelicopterEntity entity)
        {
            return HELICOPTER_TEXTURE;
        }
    }

    public static class Turret extends MobRenderer<TurretEntity, TurretModel>
    {
        public Turret(EntityRendererProvider.Context context)
        {
            super(context, new TurretModel(context.bakeLayer(TURRET_LAYER)), 0.6F);
        }

        @Override
        public ResourceLocation getTextureLocation(TurretEntity entity)
        {
            return TURRET_TEXTURE;
        }
    }
}
