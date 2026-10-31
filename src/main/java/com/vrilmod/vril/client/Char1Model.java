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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

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

    // Butt bounce while moving. Cheeks only ever lift from their rest pose (never sink
    // into the legs) and the legs' swing is trimmed slightly so they don't clip them.
    private static final float BOUNCE_LIFT = 0.6F;     // px, at full stride
    private static final float BOUNCE_SPREAD = 0.0F;   // px outward, so cheeks don't merge
    private static final float BOUNCE_TILT = 0.09F;    // rad
    private static final float LEG_SWING_TRIM = 0.88F;

    // Tail physics: a spring chain (see swayTail). The tail is independent cubes, so each
    // one is moved by its segment's simulated offset.
    private static final int JOINTS = 5;
    private final ModelPart[] tail = new ModelPart[JOINTS];
    private final float[] tailRestXRot = new float[JOINTS];

    private final ModelPart buttRight;
    private final ModelPart buttLeft;
    private final float restRX, restRY, restRZ, restLX, restLY, restLZ, restRRot, restLRot;

    public Char1Model(ModelPart root) {
        super(root);
        ModelPart b = root.getChild("body");
        buttRight = b.getChild("butt_right");
        buttLeft = b.getChild("butt_left");
        restRX = buttRight.x; restRY = buttRight.y; restRZ = buttRight.z; restRRot = buttRight.xRot;
        restLX = buttLeft.x;  restLY = buttLeft.y;  restLZ = buttLeft.z;  restLRot = buttLeft.xRot;
        ModelPart j = b;
        for (int i = 0; i < JOINTS; i++) {
            j = j.getChild(i == JOINTS - 1 ? "tail_tip" : "tail_j" + i);
            tail[i] = j;
            tailRestXRot[i] = j.xRot;
        }
        tail[JOINTS - 1].xScale = 0.9F;   // baked from the "ButtFix" animation in the bbmodel
        tail[JOINTS - 1].zScale = 0.9F;
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
        PartDefinition tail_j0 = body.addOrReplaceChild("tail_j0", CubeListBuilder.create(),
                PartPose.offset(0.0F, 15.0F, -2.0F));
        tail_j0.addOrReplaceChild("tail_c10",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, (float) Math.toRadians(25.0), 0.0F, 0.0F));
        tail_j0.addOrReplaceChild("tail_c11",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, (float) Math.toRadians(25.0), 0.0F, 0.0F));
        tail_j0.addOrReplaceChild("tail_c12",
                CubeListBuilder.create()
                        .texOffs(19, 41).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F)
                , PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, (float) Math.toRadians(25.0), 0.0F, 0.0F));
        PartDefinition tail_j1 = tail_j0.addOrReplaceChild("tail_j1", CubeListBuilder.create(),
                PartPose.offset(0.0F, 1.0F, -2.0F));
        tail_j1.addOrReplaceChild("tail_c13",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.218F, -0.8F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.toRadians(45.0), 0.0F, 0.0F));
        PartDefinition tail_j2 = tail_j1.addOrReplaceChild("tail_j2", CubeListBuilder.create(),
                PartPose.offset(0.0F, 1.0F, -1.0F));
        tail_j2.addOrReplaceChild("tail_c14",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.009F, -0.85F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.toRadians(58.5), 0.0F, 0.0F));
        PartDefinition tail_j3 = tail_j2.addOrReplaceChild("tail_j3", CubeListBuilder.create(),
                PartPose.offset(0.0F, 1.7F, -1.169F));
        tail_j3.addOrReplaceChild("tail_c15",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -1.709F, -0.331F, 2.0F, 2.0F, 2.0F)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.toRadians(66.1), 0.0F, 0.0F));
        PartDefinition tail_tip = tail_j3.addOrReplaceChild("tail_tip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.3F, -0.831F, (float) Math.toRadians(-2.5), 0.0F, 0.0F));
        tail_tip.addOrReplaceChild("tail_c16",
                CubeListBuilder.create()
                        .texOffs(31, 43).addBox(-1.0F, -1.459F, 0.7F, 2.0F, 2.0F, 1.0F)
                , PartPose.offsetAndRotation(0.0F, 1.45F, -0.2F, (float) Math.toRadians(66.0), 0.0F, 0.0F));
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

        rightLeg.xRot *= LEG_SWING_TRIM;
        leftLeg.xRot *= LEG_SWING_TRIM;
        bounceButt(limbSwing, limbSwingAmount);
        swayTail(p, limbSwing, limbSwingAmount, ageInTicks);

        head.xRot += HEAD_DROOP;
        rightArm.zRot += RIGHT_SHOULDER_DROOP;
        leftArm.zRot += LEFT_SHOULDER_DROOP;
    }

    /** Each cheek follows its own leg (same phase/frequency as the vanilla walk cycle). */
    private void bounceButt(float limbSwing, float amount) {
        float a = Math.min(amount, 1.0F);
        float phase = limbSwing * 0.6662F;
        // right leg swings with cos(phase), left with cos(phase + PI)
        float r = 0.5F + 0.5F * (float) Math.cos(phase * 2.0F);          // 0..1, once per step
        float l = 0.5F + 0.5F * (float) Math.cos(phase * 2.0F + Math.PI);  // alternate step
        buttRight.x = restRX - BOUNCE_SPREAD * a;   // right is -x
        buttLeft.x = restLX + BOUNCE_SPREAD * a;
        buttRight.y = restRY - BOUNCE_LIFT * r * a;  // -y is up: lift only
        buttLeft.y = restLY - BOUNCE_LIFT * l * a;
        buttRight.z = restRZ + 0.0F * r * a;
        buttLeft.z = restLZ + 0.0F * l * a;
        buttRight.xRot = restRRot - BOUNCE_TILT * r * a;
        buttLeft.xRot = restLRot - BOUNCE_TILT * l * a;
    }

    // ---- tail spring simulation (state is per player; the model instance is shared) ----
    // The tail is a rigid joint hierarchy: every joint rotates all joints after it, so the
    // tail bends as one piece. Each joint's *relative* angle is a damped spring.

    private static final float SPRING_K = 0.22F;       // stiffness (per tick^2)
    private static final float SPRING_DAMP = 0.20F;    // damping (per tick)
    private static final float STRIDE_SWAY = 0.020F;   // rad/tick^2 side push per stride
    private static final float STRIDE_HOP = 0.012F;    // rad/tick^2 up/down push per step
    private static final float MAX_YAW = 0.22F;        // rad per joint
    private static final float MAX_PITCH = 0.16F;      // rad per joint

    private static final class TailState {
        final float[] yaw = new float[JOINTS], yawV = new float[JOINTS];
        final float[] pitch = new float[JOINTS], pitchV = new float[JOINTS];
        float lastAge = -1.0F, lastLat, lastVert, lastYaw;
        boolean init;
    }

    private final Map<Player, TailState> tailStates = new WeakHashMap<>();

    private void swayTail(AbstractClientPlayer p, float limbSwing, float amount, float age) {
        if (age <= 0.0F) {
            return; // first-person hand pass: don't step the simulation
        }
        TailState st = tailStates.computeIfAbsent(p, k -> new TailState());
        float a = Math.min(amount, 1.0F);
        float phase = limbSwing * 0.6662F;

        double yawRad = Math.toRadians(p.yBodyRot);
        Vec3 mv = p.getDeltaMovement();
        float lat = (float) (mv.x * Math.cos(yawRad) + mv.z * Math.sin(yawRad));
        float vert = (float) Mth.clamp(mv.y, -0.8D, 0.8D);

        float dt = age - st.lastAge;
        if (!st.init || dt < 0.0F || dt > 10.0F) {
            st.init = true;
            st.lastLat = lat; st.lastVert = vert; st.lastYaw = p.yBodyRot;
            dt = 0.0F;
        }
        if (dt > 0.0F) {
            float dLat = lat - st.lastLat;
            float dVert = vert - st.lastVert;
            float turn = Mth.clamp(Mth.wrapDegrees(p.yBodyRot - st.lastYaw), -30.0F, 30.0F);
            // inertia kicks, spread evenly over the joints
            for (int i = 0; i < JOINTS; i++) {
                st.yawV[i] += (-dLat * 0.9F - turn * 0.004F);
                st.pitchV[i] += dVert * 0.6F;
            }
            st.lastLat = lat; st.lastVert = vert; st.lastYaw = p.yBodyRot;

            int steps = Math.min(8, Math.max(1, (int) Math.ceil(dt / 0.5F)));
            float h = dt / steps;
            for (int n = 0; n < steps; n++) {
                float t = st.lastAge + n * h;
                float pushYaw = STRIDE_SWAY * a * (float) Math.cos(phase) + 0.0015F * (float) Math.sin(t * 0.09F);
                float pushPitch = STRIDE_HOP * a * (float) Math.cos(phase * 2.0F);
                for (int i = 0; i < JOINTS; i++) {
                    st.yawV[i] += (-SPRING_K * st.yaw[i] - SPRING_DAMP * st.yawV[i] + pushYaw) * h;
                    st.pitchV[i] += (-SPRING_K * st.pitch[i] - SPRING_DAMP * st.pitchV[i] + pushPitch) * h;
                }
                for (int i = 0; i < JOINTS; i++) {
                    st.yaw[i] = Mth.clamp(st.yaw[i] + st.yawV[i] * h, -MAX_YAW, MAX_YAW);
                    st.pitch[i] = Mth.clamp(st.pitch[i] + st.pitchV[i] * h, -MAX_PITCH, MAX_PITCH);
                }
            }
        }
        st.lastAge = age;

        for (int i = 0; i < JOINTS; i++) {
            tail[i].yRot = st.yaw[i];
            tail[i].xRot = tailRestXRot[i] + st.pitch[i];
        }
    }
}
