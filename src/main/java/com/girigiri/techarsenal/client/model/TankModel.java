package com.girigiri.techarsenal.client.model;

import com.girigiri.techarsenal.entity.TankEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Voxel tank: two tracks, hull, rotating-with-body turret, forward barrel. */
public class TankModel extends EntityModel<TankEntity>
{
    private final ModelPart root;

    public TankModel(ModelPart root)
    {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        body.addOrReplaceChild("track_left", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.0F, -8.0F, -20.0F, 8.0F, 8.0F, 40.0F), PartPose.ZERO);
        body.addOrReplaceChild("track_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(10.0F, -8.0F, -20.0F, 8.0F, 8.0F, 40.0F), PartPose.ZERO);
        body.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-12.0F, -14.0F, -18.0F, 24.0F, 8.0F, 36.0F), PartPose.ZERO);
        body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 92).addBox(-8.0F, -21.0F, -8.0F, 16.0F, 7.0F, 16.0F), PartPose.ZERO);
        body.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(64, 92).addBox(-1.5F, -19.0F, -32.0F, 3.0F, 3.0F, 24.0F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(TankEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch)
    {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
