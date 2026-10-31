package com.vrilmod.vril.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vrilmod.vril.VrilMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.mojang.math.Axis;

/** Renders players who picked Character 1 with {@link Char1Model}. */
public class Char1Renderer extends LivingEntityRenderer<AbstractClientPlayer, Char1Model> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(VrilMod.MOD_ID, "textures/entity/character_1.png");

    /** Visual width relative to height scale: <1 makes the character lanky. */
    private static final float LANKY_WIDTH = 0.88F;
    private static final float VANILLA_PLAYER_HEIGHT = 1.8F;

    public Char1Renderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Char1Model(ctx.bakeLayer(Char1Model.LAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<AbstractClientPlayer>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<AbstractClientPlayer>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, ctx.getModelSet(), ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        return TEXTURE;
    }

    @Override
    public void render(AbstractClientPlayer player, float yaw, float partial, PoseStack ps,
                       MultiBufferSource buf, int light) {
        setModelProperties(player);
        super.render(player, yaw, partial, ps, buf, light);
    }

    /** Fits the model to the (taller) hitbox, a bit narrower than a straight scale-up. */
    @Override
    protected void scale(AbstractClientPlayer player, PoseStack ps, float partial) {
        Pose pose = player.getPose();
        boolean tall = pose == Pose.STANDING || pose == Pose.CROUCHING;
        float targetHeight = tall ? player.getBbHeight() + (pose == Pose.CROUCHING ? 0.3F : 0.0F)
                                  : VANILLA_PLAYER_HEIGHT;
        float sy = targetHeight / (Char1Model.MODEL_HEIGHT_PX / 16.0F);
        float sxz = sy * LANKY_WIDTH;
        ps.scale(sxz, sy, sxz);
    }

    @Override
    public Vec3 getRenderOffset(AbstractClientPlayer player, float partial) {
        return player.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : super.getRenderOffset(player, partial);
    }

    @Override
    protected void setupRotations(AbstractClientPlayer p, PoseStack ps, float age, float yaw, float partial) {
        float swim = p.getSwimAmount(partial);
        float pitch = p.getViewXRot(partial);
        if (p.isFallFlying()) {
            super.setupRotations(p, ps, age, yaw, partial);
            float ticks = (float) p.getFallFlyingTicks() + partial;
            float f = Mth.clamp(ticks * ticks / 100.0F, 0.0F, 1.0F);
            if (!p.isAutoSpinAttack()) {
                ps.mulPose(Axis.XP.rotationDegrees(f * (-90.0F - pitch)));
            }
            Vec3 view = p.getViewVector(partial);
            Vec3 motion = p.getDeltaMovementLerped(partial);
            double md = motion.horizontalDistanceSqr();
            double vd = view.horizontalDistanceSqr();
            if (md > 0.0D && vd > 0.0D) {
                double dot = (motion.x * view.x + motion.z * view.z) / Math.sqrt(md * vd);
                double cross = motion.x * view.z - motion.z * view.x;
                ps.mulPose(Axis.YP.rotation((float) (Math.signum(cross) * Math.acos(dot))));
            }
        } else if (swim > 0.0F) {
            super.setupRotations(p, ps, age, yaw, partial);
            float target = p.isInWater() ? -90.0F - pitch : -90.0F;
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swim, 0.0F, target)));
            if (p.isVisuallySwimming()) {
                ps.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            super.setupRotations(p, ps, age, yaw, partial);
        }
    }

    // ---- first person arm ----

    public void renderHand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, HumanoidArm arm) {
        Char1Model m = this.getModel();
        setModelProperties(p);
        m.attackTime = 0.0F;
        m.crouching = false;
        m.swimAmount = 0.0F;
        m.setupAnim(p, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ModelPart part = arm == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
        // The first-person pose stack assumes vanilla pivots.
        part.x = arm == HumanoidArm.RIGHT ? -5.0F : 5.0F;
        part.y = 2.0F;
        part.xRot = 0.0F;
        part.zRot = 0.0F;
        part.render(ps, buf.getBuffer(RenderType.entitySolid(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
    }

    // ---- model state (mirrors PlayerRenderer) ----

    private void setModelProperties(AbstractClientPlayer player) {
        Char1Model m = this.getModel();
        if (player.isSpectator()) {
            m.setAllVisible(false);
            m.head.visible = true;
            m.hat.visible = true;
            return;
        }
        m.setAllVisible(true);
        m.crouching = player.isCrouching();
        HumanoidModel.ArmPose main = getArmPose(player, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose off = getArmPose(player, InteractionHand.OFF_HAND);
        if (main.isTwoHanded()) {
            off = player.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            m.rightArmPose = main;
            m.leftArmPose = off;
        } else {
            m.rightArmPose = off;
            m.leftArmPose = main;
        }
    }

    private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (stack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (p.getUsedItemHand() == hand && p.getUseItemRemainingTicks() > 0) {
            UseAnim anim = stack.getUseAnimation();
            if (anim == UseAnim.BLOCK) return HumanoidModel.ArmPose.BLOCK;
            if (anim == UseAnim.BOW) return HumanoidModel.ArmPose.BOW_AND_ARROW;
            if (anim == UseAnim.SPEAR) return HumanoidModel.ArmPose.THROW_SPEAR;
            if (anim == UseAnim.CROSSBOW) return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            if (anim == UseAnim.SPYGLASS) return HumanoidModel.ArmPose.SPYGLASS;
            if (anim == UseAnim.TOOT_HORN) return HumanoidModel.ArmPose.TOOT_HORN;
            if (anim == UseAnim.BRUSH) return HumanoidModel.ArmPose.BRUSH;
        } else if (!p.swinging && stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        HumanoidModel.ArmPose forge = IClientItemExtensions.of(stack).getArmPose(p, hand, stack);
        return forge != null ? forge : HumanoidModel.ArmPose.ITEM;
    }
}
