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

/**
 * Detailed voxel attack helicopter: stepped nose with chin gun, glass canopy,
 * engine housing, stub wings with rocket pods, tail boom with spinning tail
 * rotor, skids on struts and a 4-blade main rotor with a hub.
 */
public class HelicopterModel extends EntityModel<HelicopterEntity>
{
    // Canopy cubes point at the glass-blue region painted at (96, 0) in the texture
    private static final int GLASS_U = 96;
    private static final int GLASS_V = 0;

    private final ModelPart root;
    private final ModelPart rotor;
    private final ModelPart tailRotor;

    public HelicopterModel(ModelPart root)
    {
        this.root = root;
        this.rotor = root.getChild("body").getChild("rotor");
        this.tailRotor = root.getChild("body").getChild("tail_rotor");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        // Stepped nose + chin gun
        body.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -15.0F, -22.0F, 8.0F, 7.0F, 9.0F), PartPose.ZERO);
        body.addOrReplaceChild("chin_gun", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-1.0F, -9.5F, -24.0F, 2.0F, 2.0F, 8.0F), PartPose.ZERO);

        // Glass cockpit canopy behind the nose
        body.addOrReplaceChild("canopy", CubeListBuilder.create()
                .texOffs(GLASS_U, GLASS_V).addBox(-3.0F, -19.5F, -16.0F, 6.0F, 5.0F, 10.0F), PartPose.ZERO);

        // Main fuselage + engine housing
        body.addOrReplaceChild("fuselage", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -17.0F, -8.0F, 10.0F, 10.0F, 20.0F), PartPose.ZERO);
        body.addOrReplaceChild("engine", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -20.0F, -5.0F, 12.0F, 3.5F, 13.0F), PartPose.ZERO);

        // Stub wings with rocket pods
        body.addOrReplaceChild("wing_left", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-13.0F, -15.5F, -3.0F, 8.0F, 2.0F, 7.0F), PartPose.ZERO);
        body.addOrReplaceChild("wing_right", CubeListBuilder.create()
                .texOffs(0, 40).addBox(5.0F, -15.5F, -3.0F, 8.0F, 2.0F, 7.0F), PartPose.ZERO);
        body.addOrReplaceChild("pod_left", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-12.5F, -13.5F, -6.0F, 4.0F, 4.0F, 11.0F), PartPose.ZERO);
        body.addOrReplaceChild("pod_right", CubeListBuilder.create()
                .texOffs(0, 50).addBox(8.5F, -13.5F, -6.0F, 4.0F, 4.0F, 11.0F), PartPose.ZERO);

        // Tail boom, fin and spinning tail rotor
        body.addOrReplaceChild("tail_boom", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -15.5F, 12.0F, 4.0F, 4.0F, 18.0F), PartPose.ZERO);
        body.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, -24.0F, 26.0F, 2.0F, 10.0F, 5.0F), PartPose.ZERO);
        body.addOrReplaceChild("tail_rotor", CubeListBuilder.create()
                        .texOffs(0, 90).addBox(0.0F, -7.0F, -1.5F, 0.5F, 14.0F, 3.0F)
                        .texOffs(0, 90).addBox(0.0F, -1.5F, -7.0F, 0.5F, 3.0F, 14.0F),
                PartPose.offset(1.5F, -19.0F, 28.5F));

        // Skids on struts (dark steel region)
        body.addOrReplaceChild("skid_left", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-9.0F, -2.5F, -12.0F, 2.0F, 2.5F, 26.0F)
                .texOffs(0, 50).addBox(-8.5F, -8.0F, -8.0F, 1.5F, 6.0F, 1.5F)
                .texOffs(0, 50).addBox(-8.5F, -8.0F, 8.0F, 1.5F, 6.0F, 1.5F), PartPose.ZERO);
        body.addOrReplaceChild("skid_right", CubeListBuilder.create()
                .texOffs(0, 50).addBox(7.0F, -2.5F, -12.0F, 2.0F, 2.5F, 26.0F)
                .texOffs(0, 50).addBox(7.0F, -8.0F, -8.0F, 1.5F, 6.0F, 1.5F)
                .texOffs(0, 50).addBox(7.0F, -8.0F, 8.0F, 1.5F, 6.0F, 1.5F), PartPose.ZERO);

        // Main rotor: mast, hub and 4 blades (two crossed strips)
        body.addOrReplaceChild("mast", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-1.5F, -22.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
        body.addOrReplaceChild("rotor", CubeListBuilder.create()
                        .texOffs(0, 100).addBox(-30.0F, -1.0F, -2.0F, 60.0F, 1.0F, 4.0F)
                        .texOffs(0, 100).addBox(-2.0F, -1.0F, -30.0F, 4.0F, 1.0F, 60.0F)
                        .texOffs(0, 50).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -22.5F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HelicopterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch)
    {
        // Rotors spin faster while ridden
        float speed = entity.isVehicle() ? 1.6F : 0.4F;
        rotor.yRot = ageInTicks * speed;
        tailRotor.xRot = ageInTicks * speed * 2.5F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha)
    {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
