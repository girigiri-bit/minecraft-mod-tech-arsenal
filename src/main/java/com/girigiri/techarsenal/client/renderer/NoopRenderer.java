package com.girigiri.techarsenal.client.renderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class NoopRenderer<T extends Entity> extends EntityRenderer<T>
{
    public NoopRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z)
    {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
