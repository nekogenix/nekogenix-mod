package io.github.nekogenix.client.skin;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.github.nekogenix.skin.CatSkin;
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
    private final ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CAT);
    private final CatModel<Cat> model = new CatModel<>(root);

    void render(GuiGraphics graphics, CatSkin skin, int x, int y, float scale, float angle) {
        var texture = CatSkins.texture(skin, ResourceLocation.withDefaultNamespace("textures/entity/cat/tabby.png"));
        root.getAllParts().forEach(ModelPart::resetPose);
        root.getChild("tail2").xRot = 1.65F;
        var pose = graphics.pose(); pose.pushPose();
        pose.translate(x, y, 150); pose.scale(scale, scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(-12)); pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(0, -1.5, 0);
        Lighting.setupForEntityInInventory(); RenderSystem.enableDepthTest();
        model.renderToBuffer(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(texture)), 15728880, OverlayTexture.NO_OVERLAY);
        graphics.flush(); pose.popPose(); Lighting.setupFor3DItems();
    }
}
