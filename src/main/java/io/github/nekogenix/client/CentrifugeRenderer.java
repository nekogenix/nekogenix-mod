package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nekogenix.Content;
import io.github.nekogenix.centrifuge.CentrifugeBlock;
import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;

public final class CentrifugeRenderer implements BlockEntityRenderer<CentrifugeBlockEntity> {
    public static final ModelResourceLocation LID = model("lid");
    public static final ModelResourceLocation GLASS = model("glass");
    public static final ModelResourceLocation ROTOR = model("rotor");
    public static final ModelResourceLocation VIAL = model("vial");
    public static final ModelResourceLocation LIQUID = model("liquid");
    public static final ModelResourceLocation CORK = model("cork");

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {}
    private static ModelResourceLocation model(String part) { return ModelResourceLocation.standalone(Content.id("block/centrifuge_" + part)); }

    @Override
    public void render(CentrifugeBlockEntity machine, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - machine.getBlockState().getValue(CentrifugeBlock.FACING).toYRot()));
        pose.translate(-.5, 0, -.5);
        lid(machine, partial, LID, RenderType.cutout(), pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(machine.rotation(partial)));
        pose.translate(-.5, 0, -.5);
        draw(ROTOR, RenderType.cutout(), pose, buffers, light, overlay);
        for (int slot = 0; slot < 6; slot++) {
            if (machine.getItem(slot).isEmpty()) continue;
            double angle = slot * Math.PI / 3;
            pose.pushPose();
            pose.translate(Math.sin(angle) * 3.7 / 16, 0, -Math.cos(angle) * 3.7 / 16);
            if (machine.getItem(slot).is(Content.SALIVA_BOTTLE)) {
                pose.pushPose();
                pose.translate(0, 4.5 / 16, 0);
                pose.scale(1, Math.max(.001F, 1 - machine.fraction(partial)), 1);
                pose.translate(0, -4.5 / 16, 0);
                draw(LIQUID, RenderType.cutout(), pose, buffers, light, overlay);
                pose.popPose();
                draw(CORK, RenderType.cutout(), pose, buffers, light, overlay);
            }
            pose.popPose();
        }
        // Finish opaque contents before glass writes to the depth buffer.
        for (int slot = 0; slot < 6; slot++) {
            if (machine.getItem(slot).isEmpty()) continue;
            double angle = slot * Math.PI / 3;
            pose.pushPose();
            pose.translate(Math.sin(angle) * 3.7 / 16, 0, -Math.cos(angle) * 3.7 / 16);
            draw(VIAL, RenderType.translucent(), pose, buffers, light, overlay);
            pose.popPose();
        }
        pose.popPose();
        lid(machine, partial, GLASS, RenderType.translucent(), pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void lid(CentrifugeBlockEntity machine, float partial, ModelResourceLocation part, RenderType type,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, 10 / 16F, 14 / 16F);
        pose.mulPose(Axis.XP.rotationDegrees(machine.lid(partial) * 80));
        pose.translate(-.5, -10 / 16F, -14 / 16F);
        draw(part, type, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void draw(ModelResourceLocation part, RenderType type, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        var model = mc.getModelManager().getModel(part);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(type), null, model,
                1, 1, 1, light, overlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, type);
    }
}
