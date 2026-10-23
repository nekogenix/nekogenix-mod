package io.github.nekogenix.client.skin;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.github.nekogenix.skin.CatSkin;
import io.github.nekogenix.client.NekoFeaturesModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

final class CatSkinPreview {
    enum Pose {
        FOUR_PAWS, UPRIGHT, SITTING, NEKO;
        net.minecraft.network.chat.Component label() { return net.minecraft.network.chat.Component.translatable("skins.nekogenix.pose." + name().toLowerCase(java.util.Locale.ROOT)); }
        Pose next() { return values()[(ordinal() + 1) % values().length]; }
    }
    private final ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CAT);
    private final CatModel<Cat> model = new CatModel<>(root);
    private final io.github.nekogenix.client.CatBodyPose posture = new io.github.nekogenix.client.CatBodyPose(root);
    private final NekoFeaturesModel features = new NekoFeaturesModel();
    private final net.minecraft.client.resources.PlayerSkin playerSkin = Minecraft.getInstance().player == null
            ? net.minecraft.client.resources.DefaultPlayerSkin.get(Minecraft.getInstance().getUser().getProfileId())
            : Minecraft.getInstance().player.getSkin();
    private final net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> playerModel =
            new net.minecraft.client.model.PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(
                    playerSkin.model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER),
                    playerSkin.model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM);

    void render(GuiGraphics graphics, CatSkin skin, int x, int y, float scale, float angle) {
        render(graphics, skin, x, y, scale, angle, Pose.FOUR_PAWS);
    }

    void renderInArea(GuiGraphics graphics, CatSkin skin, int x, int top, int bottom, float scale, float angle, Pose selected) {
        float min = switch (selected) { case FOUR_PAWS -> -.95F; case UPRIGHT -> -2.32F; case SITTING -> -1.16F; case NEKO -> -2.35F; };
        float max = .15F;
        scale = Math.min(scale, Math.max(1, bottom - top - 8) / (max - min));
        render(graphics, skin, x, (int)((top + bottom) / 2F - (min + max) * scale / 2), scale, angle, selected);
    }

    private void render(GuiGraphics graphics, CatSkin skin, int x, int y, float scale, float angle, Pose selected) {
        var texture = CatSkins.texture(skin, ResourceLocation.withDefaultNamespace("textures/entity/cat/tabby.png"));
        root.getAllParts().forEach(ModelPart::resetPose);
        model.young = false;
        root.getChild("body").xRot = net.minecraft.util.Mth.HALF_PI;
        root.getChild("tail2").xRot = 1.65F;
        if (selected == Pose.UPRIGHT) posture.upright(false);
        else if (selected == Pose.SITTING) posture.sitting();
        var pose = graphics.pose(); pose.pushPose();
        pose.translate(x, y, 150); pose.scale(scale, scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(-12)); pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(0, -1.5, 0);
        Lighting.setupForEntityInInventory(); RenderSystem.enableDepthTest();
        if (selected == Pose.NEKO) {
            playerModel.young = false;
            playerModel.setAllVisible(true);
            playerModel.renderToBuffer(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(playerSkin.texture())), 15728880, OverlayTexture.NO_OVERLAY);
            features.animateEars((net.minecraft.Util.getMillis() % 1_000_000) / 50F, 0, 0, 0, 0);
            pose.pushPose();
            playerModel.head.translateAndRotate(pose);
            features.renderEars(pose, graphics.bufferSource(), texture, 15728880, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        } else model.renderToBuffer(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(texture)), 15728880, OverlayTexture.NO_OVERLAY);
        graphics.flush(); pose.popPose(); Lighting.setupFor3DItems();
    }
}
