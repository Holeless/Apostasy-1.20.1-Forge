package com.vrilmod.vril.client;

import com.vrilmod.vril.VrilMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Random;
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
    private static final float LEG_SWING_TRIM = 0.88F;

    // Tail cubes are grown slightly so rotating joints never open hairline gaps.
    private static final CubeDeformation TAIL_GROW = new CubeDeformation(0.15F);

    // ---- physics tuning ----
    private static final int JOINTS = 5;
    // tail: each joint's relative bend is a damped spring; bends add up along the chain
    private static final float TAIL_K = 0.20F;
    private static final float TAIL_DAMP = 0.16F;
    private static final float TAIL_WAG = 0.016F;        // rad/tick^2, travelling wag while moving
    private static final float TAIL_LAT_DRAG = 0.05F;    // sideways speed pushes the tail the other way
    private static final float TAIL_VERT_DRAG = 0.035F;  // jump/fall speed bends it continuously
    private static final float TAIL_FWD_DRAG = 0.030F;   // running streams it backward
    private static final float TAIL_ACC_GAIN = 0.9F;     // inertia: tail lags behind body acceleration
    private static final float TAIL_TURN_GAIN = 0.0035F; // per deg/tick of body turn
    private static final float JUMP_WHIP = 0.050F;       // rad/tick^2 kick when you leave the ground
    private static final float LAND_WHIP = 0.060F;       // rad/tick^2 kick when you land (scaled by impact)
    private static final float AIR_FLUTTER = 0.012F;     // rad/tick^2 wobble while airborne
    private static final float TAIL_MAX_YAW = 0.11F;     // soft limit per joint (rad)
    private static final float TAIL_MAX_PITCH = 0.14F;
    // flop-out when the pants come off: one smooth spring from "tucked" (1) to rest (0), overshooting once
    private static final float FLOP_ANGLE = 0.6F;        // rad, total curl of the tucked tail
    private static final float FLOP_K = 0.18F;
    private static final float FLOP_DAMP = 0.28F;        // ~0.33 damping ratio: a visible overshoot, then settles
    private static final float FLOP_START_SCALE = 0.5F;  // tail grows out from this size as it appears
    private static final float[] FLOP_WEIGHT = {0.5F, 0.2F, 0.15F, 0.1F, 0.05F};
    private static final float CROUCH_TAIL_LIFT = 0.5F;  // rad, whole tail lifts off the legs when sneaking (tune by eye)
    // cheeks: hang off the hips, follow the legs with lag/overshoot, and squash on impact
    private static final float HIP_FOLLOW = 0.55F;       // fraction of the leg's swing the cheek follows
    private static final float HIP_K = 0.32F;
    private static final float HIP_DAMP = 0.45F;   // ~0.4 damping ratio: one soft overshoot, then settles (0.11 rang for ages)
    private static final float HIP_MAX_OVERSHOOT = 0.22F; // rad
    private static final float HIP_MAX_SWING = 0.22F;    // rad, cheeks never rotate further than this with the leg (keeps the butt shape when running)
    private static final float CHEEK_K = 0.35F;
    private static final float CHEEK_DAMP = 0.60F;
    private static final float CHEEK_LIFT = 0.55F;       // px per radian of leg swing (cheek rises as its leg goes back)
    private static final float CHEEK_CROUCH_GAIN = 2.5F; // sneaking barely swings the legs, so exaggerate it
    private static final float CHEEK_MAX = 0.8F;         // px

    private final ModelPart[] tail = new ModelPart[JOINTS];
    private final float[] tailRestXRot = new float[JOINTS];
    private final ModelPart hipL, hipR, cheekL, cheekR;
    private final float cheekRestL, cheekRestR;
    private boolean pants;   // wearing leggings this frame (set in setupAnim before simulate)

    public Char1Model(ModelPart root) {
        super(root);
        ModelPart b = root.getChild("body");
        hipL = b.getChild("hip_left");
        hipR = b.getChild("hip_right");
        cheekL = hipL.getChild("cheek_left");
        cheekR = hipR.getChild("cheek_right");
        cheekRestL = cheekL.y;
        cheekRestR = cheekR.y;
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
        PartDefinition hip_left = body.addOrReplaceChild("hip_left", CubeListBuilder.create(),
                PartPose.offset(2.0F, 12.0F, 0.0F));
        hip_left.addOrReplaceChild("cheek_left",
                CubeListBuilder.create()
                        .texOffs(38, 38).addBox(-3.0F, -3.0F, -1.0F, 4.0F, 4.0F, 4.0F)
                , PartPose.offsetAndRotation(1.0F, 2.0F, 1.0F, (float) Math.toRadians(-35.0), 0.0F, 0.0F));
        PartDefinition hip_right = body.addOrReplaceChild("hip_right", CubeListBuilder.create(),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        hip_right.addOrReplaceChild("cheek_right",
                CubeListBuilder.create()
                        .texOffs(38, 38).addBox(-3.0F, -3.0F, -1.0F, 4.0F, 4.0F, 4.0F)
                , PartPose.offsetAndRotation(1.0F, 2.0F, 1.0F, (float) Math.toRadians(-35.0), 0.0F, 0.0F));
        PartDefinition tail_j0 = body.addOrReplaceChild("tail_j0", CubeListBuilder.create(),
                PartPose.offset(0.0F, 15.0F, -2.0F));
        tail_j0.addOrReplaceChild("tail_c10",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, TAIL_GROW)
                , PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, (float) Math.toRadians(25.0), 0.0F, 0.0F));
        tail_j0.addOrReplaceChild("tail_c11",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, TAIL_GROW)
                , PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, (float) Math.toRadians(25.0), 0.0F, 0.0F));
        tail_j0.addOrReplaceChild("tail_c12",
                CubeListBuilder.create()
                        .texOffs(19, 41).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F, TAIL_GROW)
                , PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, (float) Math.toRadians(25.0), 0.0F, 0.0F));
        PartDefinition tail_j1 = tail_j0.addOrReplaceChild("tail_j1", CubeListBuilder.create(),
                PartPose.offset(0.0F, 1.0F, -2.0F));
        tail_j1.addOrReplaceChild("tail_c13",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.218F, -0.8F, 2.0F, 2.0F, 2.0F, TAIL_GROW)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.toRadians(45.0), 0.0F, 0.0F));
        PartDefinition tail_j2 = tail_j1.addOrReplaceChild("tail_j2", CubeListBuilder.create(),
                PartPose.offset(0.0F, 1.0F, -1.0F));
        tail_j2.addOrReplaceChild("tail_c14",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -2.009F, -0.85F, 2.0F, 2.0F, 2.0F, TAIL_GROW)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.toRadians(58.5), 0.0F, 0.0F));
        PartDefinition tail_j3 = tail_j2.addOrReplaceChild("tail_j3", CubeListBuilder.create(),
                PartPose.offset(0.0F, 1.7F, -1.169F));
        tail_j3.addOrReplaceChild("tail_c15",
                CubeListBuilder.create()
                        .texOffs(20, 42).addBox(-1.0F, -1.709F, -0.331F, 2.0F, 2.0F, 2.0F, TAIL_GROW)
                , PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) Math.toRadians(66.1), 0.0F, 0.0F));
        PartDefinition tail_tip = tail_j3.addOrReplaceChild("tail_tip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.3F, -0.831F, (float) Math.toRadians(-2.5), 0.0F, 0.0F));
        tail_tip.addOrReplaceChild("tail_c16",
                CubeListBuilder.create()
                        .texOffs(31, 43).addBox(-1.0F, -1.459F, 0.7F, 2.0F, 2.0F, 1.0F, TAIL_GROW)
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
        pants = p.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ArmorItem;
        simulate(p, limbSwing, limbSwingAmount, ageInTicks);

        // cheeks and tail poke through the leggings' armor layer, so hide them while pants are worn
        hipL.visible = !pants;
        hipR.visible = !pants;
        tail[0].visible = !pants;   // hides the whole tail chain

        head.xRot += HEAD_DROOP;
        rightArm.zRot += RIGHT_SHOULDER_DROOP;
        leftArm.zRot += LEFT_SHOULDER_DROOP;
    }

    // ---- per-player physics (the model instance is shared between players) ----

    private static final class Phys {
        final float[] yaw = new float[JOINTS], yawV = new float[JOINTS];
        final float[] pitch = new float[JOINTS], pitchV = new float[JOINTS];
        final float[] hip = new float[2], hipV = new float[2];     // 0 = left, 1 = right
        final float[] dy = new float[2], dyV = new float[2];
        final Random rnd = new Random();
        final float ph1 = rnd.nextFloat() * 6.28F, ph2 = rnd.nextFloat() * 6.28F;
        // smoothed body motion (raw per-tick velocity is noisy: gravity makes it flicker on the ground)
        float vfL, vfF, vfV, lastVfL, lastVfV, turnF, ampF, airF;
        float lastAge = -1.0F, lastYaw, air, fallMax, jumpKick, landKick;
        float flop, flopV;   // flop-out spring
        float crouchF;   // 0..1, eased so the tail lifts smoothly when sneaking starts/stops
        boolean init, wasGround = true, airborne, wasPants;
    }

    private final Map<Player, Phys> states = new WeakHashMap<>();

    private static float soft(float v, float max) {
        return max * (float) Math.tanh(v / max);
    }

    private void simulate(AbstractClientPlayer p, float limbSwing, float amount, float age) {
        if (age <= 0.0F) {
            return; // first-person hand pass: don't step the simulation
        }
        Phys st = states.computeIfAbsent(p, k -> new Phys());
        float phase = limbSwing * 0.6662F;
        float partial = Mth.clamp(age - p.tickCount, 0.0F, 1.0F);

        float dt = age - st.lastAge;
        float bodyYaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        if (!st.init || dt < 0.0F || dt > 10.0F) {
            st.init = true;
            st.lastYaw = bodyYaw;
            st.hip[0] = soft(leftLeg.xRot * HIP_FOLLOW, HIP_MAX_SWING);
            st.hip[1] = soft(rightLeg.xRot * HIP_FOLLOW, HIP_MAX_SWING);
            st.ampF = amount;
            st.crouchF = this.crouching ? 1.0F : 0.0F;
            st.wasPants = pants;
            dt = 0.0F;
        }
        float[] target = {soft(leftLeg.xRot * HIP_FOLLOW, HIP_MAX_SWING),
                soft(rightLeg.xRot * HIP_FOLLOW, HIP_MAX_SWING)};

        if (dt > 0.0F) {
            Random r = st.rnd;
            boolean ground = p.onGround();
            double yawRad = Math.toRadians(bodyYaw);
            Vec3 mv = p.getDeltaMovementLerped(partial);
            float lat = (float) (mv.x * Math.cos(yawRad) + mv.z * Math.sin(yawRad));   // + = character's left
            float fwd = (float) (-mv.x * Math.sin(yawRad) + mv.z * Math.cos(yawRad));
            float vert = ground ? 0.0F : (float) Mth.clamp(mv.y, -0.9D, 0.9D);          // ground gravity is just noise

            // low-pass everything so nothing steps from one frame to the next
            float f = Mth.clamp(dt / 2.5F, 0.0F, 1.0F);
            st.lastVfL = st.vfL; st.lastVfV = st.vfV;
            st.vfL += (lat - st.vfL) * f;
            st.vfF += (fwd - st.vfF) * f;
            st.vfV += (vert - st.vfV) * f;
            float turnRate = Mth.wrapDegrees(bodyYaw - st.lastYaw) / dt;
            st.lastYaw = bodyYaw;
            st.turnF += (Mth.clamp(turnRate, -30.0F, 30.0F) - st.turnF) * f;
            st.ampF += (Math.min(amount, 1.0F) - st.ampF) * f;
            float a = st.ampF;
            float accL = Mth.clamp((st.vfL - st.lastVfL) / dt, -0.15F, 0.15F);
            float accV = Mth.clamp((st.vfV - st.lastVfV) / dt, -0.15F, 0.15F);

            // ---- jump / landing events (debounced; each play-out gets a random strength) ----
            if (ground) {
                if (st.airborne) {   // landed
                    float impact = Math.min(1.0F, st.fallMax * 1.6F);
                    st.landKick = impact * (0.7F + r.nextFloat() * 0.6F);
                    for (int c = 0; c < 2; c++) {
                        st.dyV[c] += impact * (1.0F + r.nextFloat() * 0.8F);   // squash
                    }
                }
                st.air = 0.0F; st.airborne = false; st.fallMax = 0.0F;
            } else {
                if (st.wasGround && mv.y > 0.15D) {   // took off upward
                    st.jumpKick = (0.7F + r.nextFloat() * 0.6F) * (0.6F + Math.min(0.8F, (float) mv.y));
                    for (int c = 0; c < 2; c++) {
                        st.dyV[c] -= 0.5F + r.nextFloat() * 0.4F;              // stretch
                    }
                }
                st.air += dt;
                if (st.air > 2.5F) {
                    st.airborne = true;
                }
                st.fallMax = Math.max(st.fallMax, -vert);
            }
            st.wasGround = ground;

            // taking the pants off lets the tail flop out: it starts tucked and springs out
            if (st.wasPants && !pants) {
                st.flop = 1.0F;
                st.flopV = 0.0F;
                for (int c = 0; c < 2; c++) {
                    st.dyV[c] += 0.6F * (0.8F + 0.4F * r.nextFloat());   // cheeks pop out too
                }
            }
            st.wasPants = pants;

            float decay = (float) Math.exp(-dt / 6.0F);
            float jump = st.jumpKick, land = st.landKick;
            st.jumpKick *= decay;
            st.landKick *= decay;
            st.airF += ((ground ? 0.0F : 1.0F) - st.airF) * f;
            st.crouchF += ((this.crouching ? 1.0F : 0.0F) - st.crouchF) * Mth.clamp(dt / 4.0F, 0.0F, 1.0F);

            int steps = Math.min(8, Math.max(1, (int) Math.ceil(dt / 0.5F)));
            float h = dt / steps;
            for (int n = 0; n < steps; n++) {
                float t = st.lastAge + n * h;
                for (int i = 0; i < JOINTS; i++) {
                    float seg = (i + 1) / (float) JOINTS;
                    float wag = TAIL_WAG * a * (float) Math.cos(phase - i * 0.55F);
                    float wander = 0.0025F * ((float) Math.sin(t * 0.05F + st.ph1 - i * 0.5F)
                            + 0.6F * (float) Math.sin(t * 0.131F + st.ph2 - i * 0.9F));
                    float fy = wag + wander - st.vfL * TAIL_LAT_DRAG - accL * TAIL_ACC_GAIN * 0.1F
                            - st.turnF * TAIL_TURN_GAIN;
                    float fp = st.vfV * TAIL_VERT_DRAG + st.vfF * TAIL_FWD_DRAG
                            + accV * TAIL_ACC_GAIN * 0.3F
                            + (jump * JUMP_WHIP + land * LAND_WHIP) * (0.5F + 0.5F * seg)
                            + AIR_FLUTTER * st.airF * Math.min(1.0F, Math.abs(st.vfV) * 4.0F + 0.2F)
                            * (float) Math.sin(t * 0.45F - i * 0.8F)
                            + 0.010F * a * (float) Math.cos(phase * 2.0F - i * 0.5F);
                    st.yawV[i] += (-TAIL_K * st.yaw[i] - TAIL_DAMP * st.yawV[i] + fy) * h;
                    st.pitchV[i] += (-TAIL_K * st.pitch[i] - TAIL_DAMP * st.pitchV[i] + fp) * h;
                }
                st.flopV += (-FLOP_K * st.flop - FLOP_DAMP * st.flopV) * h;
                st.flop += st.flopV * h;
                for (int i = 0; i < JOINTS; i++) {
                    st.yaw[i] += st.yawV[i] * h;
                    st.pitch[i] += st.pitchV[i] * h;
                }
                for (int c = 0; c < 2; c++) {
                    st.hipV[c] += (-HIP_K * (st.hip[c] - target[c]) - HIP_DAMP * st.hipV[c]) * h;
                    st.hip[c] += st.hipV[c] * h;
                    // follows its leg's swing (so it settles when you stop) instead of being
                    // driven at the stride frequency, which rang near the spring's resonance
                    float gain = this.crouching ? CHEEK_CROUCH_GAIN : 1.0F;
                    float dyTarget = -CHEEK_LIFT * gain * (c == 0 ? leftLeg.xRot : rightLeg.xRot);
                    st.dyV[c] += (-CHEEK_K * (st.dy[c] - dyTarget) - CHEEK_DAMP * st.dyV[c]) * h;
                    st.dy[c] += st.dyV[c] * h;
                }
            }
        }
        st.lastAge = age;

        for (int i = 0; i < JOINTS; i++) {
            tail[i].yRot = soft(st.yaw[i], TAIL_MAX_YAW);
            tail[i].xRot = tailRestXRot[i] + soft(st.pitch[i], TAIL_MAX_PITCH);
        }
        // sneaking tilts the body into the tail: lift the whole tail from its base, as one piece
        tail[0].xRot -= CROUCH_TAIL_LIFT * st.crouchF;
        // flop-out: curl the whole chain by the spring value (negative values overshoot outward),
        // while the tail grows out from the base so it doesn't pop into view
        for (int i = 0; i < JOINTS; i++) {
            tail[i].xRot -= FLOP_ANGLE * FLOP_WEIGHT[i] * st.flop;
        }
        float grow = 1.0F - (1.0F - FLOP_START_SCALE) * Mth.clamp(st.flop, 0.0F, 1.0F);
        tail[0].xScale = grow;
        tail[0].yScale = grow;
        tail[0].zScale = grow;
        // cheeks ride on the hip joints (the legs' pivots), so the legs can't swing into them
        hipL.xRot = target[0] + soft(st.hip[0] - target[0], HIP_MAX_OVERSHOOT);
        hipR.xRot = target[1] + soft(st.hip[1] - target[1], HIP_MAX_OVERSHOOT);
        cheekL.y = cheekRestL + soft(st.dy[0], CHEEK_MAX);
        cheekR.y = cheekRestR + soft(st.dy[1], CHEEK_MAX);
    }
}