package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Retarget the equipment skeleton, leaving materials and item rendering to vanilla and NeoForge. */
final class CatEquipment implements RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final PlayerModel<AbstractClientPlayer> model;
    private final HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, HumanoidArmorModel<AbstractClientPlayer>> armor;
    private final CustomHeadLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> head;
    private final ElytraLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> wings;
    private final CapeLayer cape;
    private final ModelPart catHead, catBody, frontRight, frontLeft, hindRight, hindLeft;

    CatEquipment(EntityRendererProvider.Context context, ModelPart cat) {
        model = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        armor = new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager());
        head = new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer());
        wings = new ElytraLayer<>(this, context.getModelSet());
        cape = new CapeLayer(this);
        catHead = cat.getChild("head");
        catBody = cat.getChild("body");
        frontRight = cat.getChild("right_front_leg");
        frontLeft = cat.getChild("left_front_leg");
        hindRight = cat.getChild("right_hind_leg");
        hindLeft = cat.getChild("left_hind_leg");
    }

    void render(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light,
            float walk, float speed, float partial, float age, float yaw, float pitch) {
        model.young = model.riding = false;
        model.crouching = player.isCrouching();
        model.attackTime = player.getAttackAnim(partial);
        model.leftArmPose = model.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        fit(model.head, catHead, 0, 2, -.5F, .625F, .5F, .625F);
        model.hat.copyFrom(model.head);
        fit(model.body, catBody, 0, 3.5F, -5, .5F, 1.25F, 1.25F);
        fit(model.rightArm, frontRight, .5F, 5 / 3F, 1, .5F, 5 / 6F, .5F);
        fit(model.leftArm, frontLeft, -.5F, 5 / 3F, 1, .5F, 5 / 6F, .5F);
        fit(model.rightLeg, hindRight, 0, 0, 2, .5F, .5F, .5F);
        fit(model.leftLeg, hindLeft, 0, 0, 2, .5F, .5F, .5F);
        // The native armor layer preserves custom models, texture hooks, dye layers, trims and glint.
        armor.render(pose, buffers, light, player, walk, speed, partial, age, yaw, pitch);
        head.render(pose, buffers, light, player, walk, speed, partial, age, yaw, pitch);
        pose.pushPose();
        catBody.translateAndRotate(pose);
        pose.translate(0, 3 / 16F, -2.2F / 16F);
        pose.scale(.55F, .75F, .65F);
        wings.render(pose, buffers, light, player, walk, speed, partial, age, yaw, pitch);
        cape.render(pose, buffers, light, player, walk, speed, partial, age, yaw, pitch);
        pose.popPose();
    }

    private static void fit(ModelPart part, ModelPart bone, float x, float y, float z, float sx, float sy, float sz) {
        part.copyFrom(bone);
        Vector3f offset = new Vector3f(x, y, z).rotate(new Quaternionf().rotationZYX(bone.zRot, bone.yRot, bone.xRot));
        part.x += offset.x; part.y += offset.y; part.z += offset.z;
        part.xScale *= sx; part.yScale *= sy; part.zScale *= sz;
    }

    @Override public PlayerModel<AbstractClientPlayer> getModel() { return model; }
    @Override public ResourceLocation getTextureLocation(AbstractClientPlayer player) { return player.getSkin().texture(); }
}
