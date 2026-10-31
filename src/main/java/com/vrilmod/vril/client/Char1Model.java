package com.vrilmod.vril.client;

import com.vrilmod.vril.VrilMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Character 1's model, converted from Char1.bbmodel. Pivots are kept
 * vanilla-compatible (head/body/arms/legs) so HumanoidModel animation, armor and
 * held items work; the model is 3 px higher than vanilla (longer legs), which
 * {@link #setupAnim} compensates for after the vanilla animation has run.
 */
public class Char1Model extends HumanoidModel<AbstractClientPlayer> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(VrilMod.MOD_ID, "character_1"), "main");

    /** Native model height in pixels (head top). */
    public static final float MODEL_HEIGHT_PX = 35.0F;
    /** Everything sits this many px higher than vanilla's rest pose. */
    private static final float REST_SHIFT = -3.0F;

    // Deliberately imperfect: a slight forward head, uneven shoulders.
    private static final float HEAD_DROOP = 0.07F;
    private static final float RIGHT_SHOULDER_DROOP = 0.04F;
    private static final float LEFT_SHOULDER_DROOP = -0.07F;

    public Char1Model(ModelPart root) {
        super(root);
        ModelPart tip = root.getChild("body").getChild("tail_tip");
        tip.xScale = 0.9F;   // baked from the "ButtFix" animation in the bbmodel
        tip.zScale = 0.9F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(4, 43).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 2.0F, 1.0F)
                        .texOffs(42, 40).addBox(-1.0F, -5.0F, -5.0F, 2.0F, 2.0F, 1.0F)
                        .texOffs(42, 40).addBox(-2.0F, -4.0F, -5.0F, 1.0F, 1.0F, 1.0F)
                        .texOffs(42, 40).addBox(1.0F, -4.0F, -5.0F, 1.0F, 1.0F, 1.0F)
                , PartPose.offset(0.0F, -3.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                        .texOffs(10, 14).addBox(2.0F, 2.0F, -2.4F, 1.0F, 1.0F, 1.0F)
                        .texOffs(9, 13).addBox(-3.0F, 2.0F, -2.4F, 1.0F, 1.0F, 2.0F)
                , PartPose.offset(0.0F, -3.0F, 0.0F));
        body.addOrReplaceChild("butt_right",
                CubeListBuilder.create()
                        .texOffs(38, 38).addBox(-3.0F, -3.0F, -1.0F, 4.0F, 4.0F, 4.0F)
                , PartPose.offsetAndRotation(3.0F, 14.0F, 1.0F, (float) Math.toRadians(-35.0), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        body.addOrReplaceChild("butt_left",
                CubeListBuilder.create()
                        .texOffs(38, 38).addBox(-3.0F, -3.0F, -1.0F, 4.0F, 4.0F, 4.0F)
                , PartPose.offsetAndRotation(-1.0F, 14.0F, 1.0F, (float) Math.toRadians(-35.0), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        body.addOrReplaceChild("tail_0",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(-1.0F, 15.0F, -2.0F, (float) Math.toRadians(25.0), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        body.addOrReplaceChild("tail_1",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(1.0F, 15.0F, -2.0F, (float) Math.toRadians(25.0), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        body.addOrReplaceChild("tail_2",
                CubeListBuilder.create()
                        .texOffs(19, 41).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F)
                , PartPose.offsetAndRotation(0.0F, 15.0F, -3.0F, (float) Math.toRadians(25.0), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        body.addOrReplaceChild("tail_3",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.218F, -0.8F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(0.0F, 16.0F, -4.0F, (float) Math.toRadians(45.0), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        body.addOrReplaceChild("tail_4",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.009F, -0.85F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(0.0F, 17.0F, -5.0F, (float) Math.toRadians(58.5), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        body.addOrReplaceChild("tail_5",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -1.709F, -0.331F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(0.0F, 18.7F, -6.169F, (float) Math.toRadians(66.1), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        PartDefinition tailTip = body.addOrReplaceChild("tail_tip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 19.0F, -7.0F, (float) Math.toRadians(2.5), 0.0F, 0.0F));
        tailTip.addOrReplaceChild("tail_tip_cube",
                CubeListBuilder.create()
                        .texOffs(31, 43).addBox(-1.0F, -1.459F, 0.7F, 2.0F, 2.0F, 1.0F)
                , PartPose.offsetAndRotation(0.0F, 1.45F, -0.2F, (float) Math.toRadians(66.0), (float) Math.toRadians(0.0), (float) Math.toRadians(0.0)));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(32, 46).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F)
                , PartPose.offset(-6.0F, -1.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(32, 46).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F)
                , PartPose.offset(6.0F, -1.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(32, 45).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F)
                , PartPose.offset(-2.0F, 9.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(32, 45).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F)
                , PartPose.offset(2.0F, 9.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(p, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        head.y += REST_SHIFT;
        body.y += REST_SHIFT;
        rightArm.y += REST_SHIFT;
        leftArm.y += REST_SHIFT;
        rightLeg.y += REST_SHIFT;
        leftLeg.y += REST_SHIFT;

        head.xRot += HEAD_DROOP;
        rightArm.zRot += RIGHT_SHOULDER_DROOP;
        leftArm.zRot += LEFT_SHOULDER_DROOP;
    }
}
