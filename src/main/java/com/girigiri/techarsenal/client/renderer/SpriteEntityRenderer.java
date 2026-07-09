package com.girigiri.techarsenal.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Renders an entity as its item sprite, either lying flat (vehicles seen from
 * above) or standing upright (turrets), matching the mod's pixel-art style.
 */
public class SpriteEntityRenderer<T extends LivingEntity> extends EntityRenderer<T>
{
    private final Supplier<? extends Item> item;
    private final float scale;
    private final boolean flat;
    private final float yOffset;

    public SpriteEntityRenderer(EntityRendererProvider.Context context, Supplier<? extends Item> item,
                                float scale, boolean flat, float yOffset)
    {
        super(context);
        this.item = item;
        this.scale = scale;
        this.flat = flat;
        this.yOffset = yOffset;
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.translate(0.0F, yOffset, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        if (flat)
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(item.get()), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
