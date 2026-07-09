package com.girigiri.techarsenal.client.model;

import com.girigiri.techarsenal.entity.HelicopterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Voxel attack helicopter: fuselage, tail boom + fin, skids, spinning main rotor. */
public class HelicopterModel extends EntityModel<HelicopterEntity>
{
    private final ModelPart root;
    private final ModelPart rotor;

    public HelicopterModel(ModelPart root)
    {
        this.root = root;
        this.rotor = root.getChild("body").getChild("rotor");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        body.addOrReplaceChild("fuselage", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -18.0F, -14.0F, 12.0F, 11.0F, 28.0F), PartPose.ZERO);
        body.addOrReplaceChild("tail_boom", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-2.0F, -16.0F, 14.0F, 4.0F, 4.0F, 18.0F), PartPose.ZERO);
        body.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-1.0F, -24.0F, 28.0F, 2.0F, 10.0F, 6.0F), PartPose.ZERO);
        body.addOrReplaceChild("skid_left", CubeListBuilder.create()
                .texOffs(48, 40).addBox(-9.0F, -3.0F, -10.0F, 2.0F, 3.0F, 22.0F), PartPose.ZERO);
        body.addOrReplaceChild("skid_right", CubeListBuilder.create()
                .texOffs(48, 40).addBox(7.0F, -3.0F, -10.0F, 2.0F, 3.0F, 22.0F), PartPose.ZERO);
        body.addOrReplaceChild("rotor", CubeListBuilder.create()
                        .texOffs(0, 84).addBox(-28.0F, -1.0F, -2.0F, 56.0F, 1.0F, 4.0F)
                        .texOffs(0, 84).addBox(-2.0F, -1.0F, -28.0F, 4.0F, 1.0F, 56.0F),
                PartPose.offset(0.0F, -19.5F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HelicopterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch)
    {
        // Rotor spins faster while ridden
        float speed = entity.isVehicle() ? 1.6F : 0.4F;
        rotor.yRot = ageInTicks * speed;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
