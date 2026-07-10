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

/**
 * Detailed voxel MBT: tracks with road wheels, side skirts, stepped hull with
 * glacis and engine deck, elongated turret with mantlet, commander cupola,
 * coaxial MG, main gun with muzzle brake and a whip antenna.
 */
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

        // Tracks (dark band in the texture at v=0..20)
        body.addOrReplaceChild("track_left", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-18.0F, -9.0F, -21.0F, 8.0F, 9.0F, 42.0F), PartPose.ZERO);
        body.addOrReplaceChild("track_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(10.0F, -9.0F, -21.0F, 8.0F, 9.0F, 42.0F), PartPose.ZERO);

        // Road wheels poking out of the track sides (dark steel region)
        CubeListBuilder wheelsLeft = CubeListBuilder.create();
        CubeListBuilder wheelsRight = CubeListBuilder.create();
        for (int i = 0; i < 5; i++)
        {
            float z = -17.0F + i * 8.0F;
            wheelsLeft.texOffs(0, 0).addBox(-19.0F, -6.0F, z, 1.0F, 5.0F, 5.0F);
            wheelsRight.texOffs(0, 0).addBox(18.0F, -6.0F, z, 1.0F, 5.0F, 5.0F);
        }
        body.addOrReplaceChild("wheels_left", wheelsLeft, PartPose.ZERO);
        body.addOrReplaceChild("wheels_right", wheelsRight, PartPose.ZERO);

        // Side skirts above the tracks (camo region)
        body.addOrReplaceChild("skirt_left", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-18.5F, -13.0F, -19.0F, 1.0F, 5.0F, 38.0F), PartPose.ZERO);
        body.addOrReplaceChild("skirt_right", CubeListBuilder.create()
                .texOffs(0, 60).addBox(17.5F, -13.0F, -19.0F, 1.0F, 5.0F, 38.0F), PartPose.ZERO);

        // Hull: lower body, sloped-ish glacis step, engine deck, upper plate
        body.addOrReplaceChild("hull_lower", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-11.0F, -14.0F, -20.0F, 22.0F, 6.0F, 40.0F), PartPose.ZERO);
        body.addOrReplaceChild("glacis", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-10.0F, -16.5F, -23.0F, 20.0F, 4.0F, 6.0F), PartPose.ZERO);
        body.addOrReplaceChild("hull_upper", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-12.0F, -17.5F, -15.0F, 24.0F, 4.0F, 28.0F), PartPose.ZERO);
        body.addOrReplaceChild("engine_deck", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-11.0F, -16.0F, 13.0F, 22.0F, 2.5F, 8.0F), PartPose.ZERO);

        // Turret: elongated base, cupola with hatch, gun mantlet (camo region)
        body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-9.0F, -23.0F, -9.0F, 18.0F, 6.0F, 20.0F), PartPose.ZERO);
        body.addOrReplaceChild("turret_neck", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-7.0F, -18.0F, -7.0F, 14.0F, 1.0F, 16.0F), PartPose.ZERO);
        body.addOrReplaceChild("cupola", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-4.5F, -26.0F, 0.0F, 9.0F, 3.0F, 9.0F), PartPose.ZERO);
        body.addOrReplaceChild("hatch", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-2.5F, -26.8F, 2.0F, 5.0F, 1.0F, 5.0F), PartPose.ZERO);
        body.addOrReplaceChild("mantlet", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-4.0F, -22.5F, -13.0F, 8.0F, 5.0F, 4.0F), PartPose.ZERO);

        // Main gun + muzzle brake, coaxial MG, whip antenna (dark steel region)
        body.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -21.5F, -35.0F, 3.0F, 3.0F, 22.0F), PartPose.ZERO);
        body.addOrReplaceChild("muzzle_brake", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -22.5F, -39.0F, 5.0F, 5.0F, 4.0F), PartPose.ZERO);
        body.addOrReplaceChild("coax_mg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(4.5F, -20.7F, -16.0F, 1.0F, 1.0F, 7.0F), PartPose.ZERO);
        body.addOrReplaceChild("antenna", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.2F, -34.0F, 8.0F, 0.5F, 11.0F, 0.5F), PartPose.ZERO);

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
