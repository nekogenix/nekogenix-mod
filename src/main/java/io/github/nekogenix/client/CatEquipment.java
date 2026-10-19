package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class CatEquipment implements RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final PlayerModel<AbstractClientPlayer> model;
    private final HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, HumanoidArmorModel<AbstractClientPlayer>> armor;
    private final CustomHeadLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> head;
    private final ElytraLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> wings;
    private final ModelPart catHead, catBody, frontRight, frontLeft, hindRight, hindLeft;

    CatEquipment(EntityRendererProvider.Context context, ModelPart cat) {
        model = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        armor = new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager());
        head = new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer());
        wings = new ElytraLayer<>(this, context.getModelSet());
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
        armor.render(pose, buffers, light, player, walk, speed, partial, age, yaw, pitch);
        head.render(pose, buffers, light, player, walk, speed, partial, age, yaw, pitch);
        pose.pushPose();
        catBody.translateAndRotate(pose);
        pose.translate(0, 4 / 16F, -3.2F / 16F);
        pose.pushPose();
        pose.scale(.55F, .75F, .65F);
        wings.render(pose, buffers, light, player, walk, speed, partial, age, yaw, pitch);
        pose.popPose();
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem) pose.translate(0, 0, 1 / 16F);
        pose.scale(.55F, .75F, .65F);
        cape(player, pose, buffers, light, partial);
        pose.popPose();
    }

    private void cape(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light, float partial) {
        var texture = player.getSkin().capeTexture();
        if (texture == null || player.isInvisible() || !player.isModelPartShown(PlayerModelPart.CAPE)
                || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        double dx = Mth.lerp(partial, player.xCloakO, player.xCloak) - Mth.lerp(partial, player.xo, player.getX());
        double dy = Mth.lerp(partial, player.yCloakO, player.yCloak) - Mth.lerp(partial, player.yo, player.getY());
        double dz = Mth.lerp(partial, player.zCloakO, player.zCloak) - Mth.lerp(partial, player.zo, player.getZ());
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        float lift = Mth.clamp((float)(dx * Mth.sin(yaw) - dz * Mth.cos(yaw)) * 18, 0, 16);
        float vertical = Mth.clamp((float)dy * 2, -2, 6);
        float bob = Mth.sin(Mth.lerp(partial, player.walkDistO, player.walkDist) * 6)
                * Mth.lerp(partial, player.oBob, player.bob) * 5;
        float sideways = Mth.clamp((float)(-dx * Mth.cos(yaw) - dz * Mth.sin(yaw)) * 10, -6, 6);
        float tilt = Mth.clamp(6 + lift + vertical + bob + (player.isCrouching() ? 4 : 0), 2, 28);
        pose.pushPose();
        pose.translate(0, 0, .125);
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.mulPose(Axis.ZP.rotationDegrees(sideways));
        pose.mulPose(Axis.YP.rotationDegrees(180 - sideways));
        model.renderCloak(pose, buffers.getBuffer(RenderType.entitySolid(texture)), light, OverlayTexture.NO_OVERLAY);
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
