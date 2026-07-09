package com.girigiri.techarsenal.client.model;

import com.girigiri.techarsenal.entity.TurretEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Voxel defense turret: base, column, and a gun head that tracks the target. */
public class TurretModel extends EntityModel<TurretEntity>
{
    private final ModelPart root;
    private final ModelPart head;

    public TurretModel(ModelPart root)
    {
        this.root = root;
        this.head = root.getChild("body").getChild("head");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        body.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -4.0F, -7.0F, 14.0F, 4.0F, 14.0F), PartPose.ZERO);
        body.addOrReplaceChild("column", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-3.0F, -12.0F, -3.0F, 6.0F, 8.0F, 6.0F), PartPose.ZERO);
        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 36).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 6.0F, 12.0F)
                        .texOffs(28, 36).addBox(-1.0F, -2.5F, -14.0F, 2.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, -14.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TurretEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch)
    {
        head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        head.xRot = headPitch * ((float) Math.PI / 180.0F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
