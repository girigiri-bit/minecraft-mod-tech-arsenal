package com.girigiri.techarsenal.client.renderer;

import com.girigiri.techarsenal.TechArsenal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders the beam saber as a 3D energy blade: a dark grip and emitter drawn
 * with normal lighting, plus a full-bright white core wrapped in a pulsing
 * translucent cyan glow. Model space matches block space (0..1), blade up +Y.
 */
public final class BeamSaberRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation BLADE_TEXTURE =
            new ResourceLocation(TechArsenal.MODID, "textures/item/beam_blade.png");
    private static final ResourceLocation SPRITE_TEXTURE =
            new ResourceLocation(TechArsenal.MODID, "textures/item/beam_saber.png");

    private static BeamSaberRenderer instance;

    public static BeamSaberRenderer instance()
    {
        if (instance == null)
        {
            Minecraft mc = Minecraft.getInstance();
            instance = new BeamSaberRenderer(mc);
        }
        return instance;
    }

    private BeamSaberRenderer(Minecraft mc)
    {
        super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay)
    {
        // The 3D blade is only used in first person, where it reads as a real
        // beam. Everywhere else (GUI, frames, drops, third person — where the
        // arm pose buries a 3D model inside the body) the flat sprite wins.
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
        {
            drawSprite(poseStack, buffers, packedLight, packedOverlay);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float time = mc.level != null ? mc.level.getGameTime() + mc.getFrameTime() : 0.0F;
        float pulse = 1.0F + 0.12F * Mth.sin(time * 0.45F);

        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutoutNoCull(BLADE_TEXTURE));
        // Grip and emitter: metallic grays under normal lighting
        drawBox(poseStack, solid, 0.5F, 0.5F, 0.035F, 0.02F, 0.30F,
                60, 60, 72, 255, packedLight, packedOverlay);
        drawBox(poseStack, solid, 0.5F, 0.5F, 0.055F, 0.30F, 0.37F,
                150, 152, 165, 255, packedLight, packedOverlay);

        VertexConsumer emissive = buffers.getBuffer(RenderType.entityTranslucentEmissive(BLADE_TEXTURE));
        int fullBright = LightTexture.FULL_BRIGHT;
        // Blade core: solid white, always full-bright
        drawBox(poseStack, emissive, 0.5F, 0.5F, 0.02F, 0.37F, 1.32F,
                255, 255, 255, 255, fullBright, packedOverlay);
        // Outer glow: translucent cyan, pulsing width
        float glow = 0.045F * pulse;
        drawBox(poseStack, emissive, 0.5F, 0.5F, glow, 0.35F, 1.36F,
                90, 200, 255, 110, fullBright, packedOverlay);
    }

    /**
     * Flat item-icon sprite used for GUI slots, item frames, drops and third
     * person. Drawn as two perpendicular quads (a cross) so it stays visible
     * from every angle; in the GUI the second quad is edge-on and invisible.
     */
    private static void drawSprite(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay)
    {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(SPRITE_TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        // XY-plane quad
        vertex(buffer, pose, normal, 0.0F, 0.0F, 0.5F, 0.0F, 1.0F, 255, 255, 255, 255, light, overlay, 0.0F, 0.0F, 1.0F);
        vertex(buffer, pose, normal, 1.0F, 0.0F, 0.5F, 1.0F, 1.0F, 255, 255, 255, 255, light, overlay, 0.0F, 0.0F, 1.0F);
        vertex(buffer, pose, normal, 1.0F, 1.0F, 0.5F, 1.0F, 0.0F, 255, 255, 255, 255, light, overlay, 0.0F, 0.0F, 1.0F);
        vertex(buffer, pose, normal, 0.0F, 1.0F, 0.5F, 0.0F, 0.0F, 255, 255, 255, 255, light, overlay, 0.0F, 0.0F, 1.0F);
        // ZY-plane quad
        vertex(buffer, pose, normal, 0.5F, 0.0F, 1.0F, 0.0F, 1.0F, 255, 255, 255, 255, light, overlay, 1.0F, 0.0F, 0.0F);
        vertex(buffer, pose, normal, 0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 255, 255, 255, 255, light, overlay, 1.0F, 0.0F, 0.0F);
        vertex(buffer, pose, normal, 0.5F, 1.0F, 0.0F, 1.0F, 0.0F, 255, 255, 255, 255, light, overlay, 1.0F, 0.0F, 0.0F);
        vertex(buffer, pose, normal, 0.5F, 1.0F, 1.0F, 0.0F, 0.0F, 255, 255, 255, 255, light, overlay, 1.0F, 0.0F, 0.0F);
    }

    /** Axis-aligned box centered on (cx, cz) with half-width r, from y0 to y1. */
    private static void drawBox(PoseStack poseStack, VertexConsumer buffer,
                                float cx, float cz, float r, float y0, float y1,
                                int red, int green, int blue, int alpha, int light, int overlay)
    {
        float x0 = cx - r;
        float x1 = cx + r;
        float z0 = cz - r;
        float z1 = cz + r;
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // -Z / +Z / -X / +X sides, then bottom and top
        quad(buffer, pose, normal, light, overlay, red, green, blue, alpha,
                x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1);
        quad(buffer, pose, normal, light, overlay, red, green, blue, alpha,
                x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1);
        quad(buffer, pose, normal, light, overlay, red, green, blue, alpha,
                x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0);
        quad(buffer, pose, normal, light, overlay, red, green, blue, alpha,
                x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0);
        quad(buffer, pose, normal, light, overlay, red, green, blue, alpha,
                x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0);
        quad(buffer, pose, normal, light, overlay, red, green, blue, alpha,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                             int light, int overlay, int red, int green, int blue, int alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz)
    {
        vertex(buffer, pose, normal, ax, ay, az, 0.0F, 1.0F, red, green, blue, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, pose, normal, bx, by, bz, 1.0F, 1.0F, red, green, blue, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, pose, normal, cx, cy, cz, 1.0F, 0.0F, red, green, blue, alpha, light, overlay, nx, ny, nz);
        vertex(buffer, pose, normal, dx, dy, dz, 0.0F, 0.0F, red, green, blue, alpha, light, overlay, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               int red, int green, int blue, int alpha, int light, int overlay,
                               float nx, float ny, float nz)
    {
        buffer.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}
