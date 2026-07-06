package com.girigiri.techarsenal.client.renderer;

import com.girigiri.techarsenal.entity.DroneEntity;
import com.girigiri.techarsenal.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the drone as its item sprite hovering with a slight bob,
 * lying flat like a quadcopter viewed from above.
 */
public class DroneRenderer extends EntityRenderer<DroneEntity>
{
    public DroneRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(DroneEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        float bob = Mth.sin((entity.tickCount + partialTick) * 0.15F) * 0.05F;
        poseStack.translate(0.0F, 0.25F + bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(1.4F, 1.4F, 1.4F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(ModItems.DRONE.get()), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
