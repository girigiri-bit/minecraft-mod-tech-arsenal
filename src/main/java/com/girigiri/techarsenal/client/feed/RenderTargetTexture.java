package com.girigiri.techarsenal.client.feed;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Exposes a RenderTarget's color attachment through the TextureManager so it
 * can be used with regular RenderTypes. The GL texture is owned by the
 * RenderTarget, so releasing/closing here is a no-op.
 */
public class RenderTargetTexture extends AbstractTexture
{
    private final RenderTarget target;

    public RenderTargetTexture(RenderTarget target)
    {
        this.target = target;
    }

    @Override
    public int getId()
    {
        return target.getColorTextureId();
    }

    @Override
    public void releaseId()
    {
    }

    @Override
    public void load(ResourceManager resourceManager)
    {
    }

    @Override
    public void close()
    {
    }
}
