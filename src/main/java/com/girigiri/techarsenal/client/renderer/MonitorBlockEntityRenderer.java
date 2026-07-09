package com.girigiri.techarsenal.client.renderer;

import com.girigiri.techarsenal.block.MonitorBlock;
import com.girigiri.techarsenal.blockentity.MonitorBlockEntity;
import com.girigiri.techarsenal.client.feed.FeedManager;
import com.girigiri.techarsenal.util.MonitorScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity>
{
    private static final ResourceLocation DARK_SCREEN =
            new ResourceLocation("minecraft", "textures/block/black_concrete.png");
    private static final float FACE_OFFSET = 0.005F;
    private static final float LABEL_OFFSET = 0.02F;
    private static final float MARGIN = 0.12F;

    private final Font font;

    public MonitorBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {
        this.font = context.getFont();
    }

    @Override
    public void render(MonitorBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay)
    {
        Level level = be.getLevel();
        if (level == null)
            return;
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof MonitorBlock))
            return;

        MonitorScreen.Screen screen = MonitorScreen.resolve(level, be.getBlockPos(), state);
        if (!screen.origin().equals(be.getBlockPos()))
            return; // only the controller draws the combined screen

        FeedManager.FeedView view = FeedManager.requestFeed(be, screen);
        ResourceLocation texture = view.hasSignal() ? view.texture() : DARK_SCREEN;

        Direction facing = screen.facing();
        int w = screen.width();
        int h = screen.height();
        float e = FACE_OFFSET;

        // Quad corners in controller-local space: bl = viewer's bottom-left
        float[] bl;
        float[] br;
        float[] tr;
        float[] tl;
        switch (facing)
        {
            case SOUTH -> {
                bl = new float[]{0, 0, 1 + e};
                br = new float[]{w, 0, 1 + e};
                tr = new float[]{w, h, 1 + e};
                tl = new float[]{0, h, 1 + e};
            }
            case WEST -> {
                bl = new float[]{-e, 0, 0};
                br = new float[]{-e, 0, w};
                tr = new float[]{-e, h, w};
                tl = new float[]{-e, h, 0};
            }
            case EAST -> {
                bl = new float[]{1 + e, 0, 1};
                br = new float[]{1 + e, 0, 1 - w};
                tr = new float[]{1 + e, h, 1 - w};
                tl = new float[]{1 + e, h, 1};
            }
            default -> { // NORTH
                bl = new float[]{1, 0, -e};
                br = new float[]{1 - w, 0, -e};
                tr = new float[]{1 - w, h, -e};
                tl = new float[]{1, h, -e};
            }
        }

        // Center-crop the feed so the screen shows the correct aspect ratio
        float u0 = 0.0F;
        float u1 = 1.0F;
        float v0 = 0.0F;
        float v1 = 1.0F;
        if (view.hasSignal())
        {
            float texAspect = view.aspect();
            float screenAspect = (float) w / (float) h;
            if (texAspect > screenAspect)
            {
                float span = screenAspect / texAspect;
                u0 = (1.0F - span) / 2.0F;
                u1 = 1.0F - u0;
            }
            else if (texAspect < screenAspect)
            {
                float span = texAspect / screenAspect;
                v0 = (1.0F - span) / 2.0F;
                v1 = 1.0F - v0;
            }
        }

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer buffer = buffers.getBuffer(RenderType.text(texture));
        int light = LightTexture.FULL_BRIGHT;
        // FBO textures have row 0 at the image bottom, so v0 goes on the bottom edge
        vertex(buffer, mat, bl, u0, v0, light);
        vertex(buffer, mat, br, u1, v0, light);
        vertex(buffer, mat, tr, u1, v1, light);
        vertex(buffer, mat, tl, u0, v1, light);

        renderLabel(poseStack, buffers, facing, w, h, view);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f mat, float[] pos, float u, float v, int light)
    {
        buffer.vertex(mat, pos[0], pos[1], pos[2])
                .color(255, 255, 255, 255)
                .uv(u, v)
                .uv2(light)
                .endVertex();
    }

    private void renderLabel(PoseStack poseStack, MultiBufferSource buffers, Direction facing,
                             int w, int h, FeedManager.FeedView view)
    {
        String label = view.label();
        int color;
        if (view.hasSignal())
        {
            color = 0x55FF55;
        }
        else if ("OFF".equals(label))
        {
            color = 0xAAAAAA;
        }
        else
        {
            label = label + " - NO SIGNAL";
            color = 0xFF5555;
        }

        float e2 = FACE_OFFSET + LABEL_OFFSET;
        float x;
        float z;
        switch (facing)
        {
            case SOUTH -> {
                x = MARGIN;
                z = 1 + e2;
            }
            case WEST -> {
                x = -e2;
                z = MARGIN;
            }
            case EAST -> {
                x = 1 + e2;
                z = 1 - MARGIN;
            }
            default -> { // NORTH
                x = 1 - MARGIN;
                z = -e2;
            }
        }

        poseStack.pushPose();
        poseStack.translate(x, h - MARGIN, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        float scale = 0.025F;
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(label, 0.0F, 0.0F, color, false, poseStack.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
