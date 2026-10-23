package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

public final class NekoFeaturesModel {
    private final ModelPart left, right;
    private final List<FurQuad> leftFur, rightFur;

    public NekoFeaturesModel() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("left", CubeListBuilder.create(),
                PartPose.offset(2.8F, -8, .2F));
        root.addOrReplaceChild("right", CubeListBuilder.create(),
                PartPose.offset(-2.8F, -8, .2F));
        var baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        left = baked.getChild("left"); right = baked.getChild("right");
        leftFur = earShape(1, 6); rightFur = earShape(-1, 0);
    }

    private static List<FurQuad> earShape(int side, int u) {
        int inner = u + (side > 0 ? 3 : 0);
        int outer = u + (side > 0 ? 0 : 3);
        float centerX = .7F, centerY = -.9F;
        float[][] outline = {{-2, .2F}, {2, .2F}, {2, -1.1F}, {1.7F, -1.1F},
                {1.7F, -2.4F}, {1.35F, -2.4F}, {1.35F, -4}, {.45F, -3.4F},
                {.45F, -2.9F}, {-.45F, -2.9F}, {-.45F, -2.1F}, {-1.2F, -2.1F},
                {-1.2F, -1.2F}, {-2, -1.2F}};
        var quads = new ArrayList<FurQuad>();
        for (int i = 0; i < outline.length; i++) {
            float[] a = outline[i], b = outline[(i + 1) % outline.length];
            float az = -1 + -a[1] * .1F, bz = -1 + -b[1] * .1F;
            float ax = centerX + (a[0] - centerX) * .68F, ay = centerY + (a[1] - centerY) * .7F;
            float bx = centerX + (b[0] - centerX) * .68F, by = centerY + (b[1] - centerY) * .7F;
            float innerA = -.84F - ay * .1F, innerB = -.84F - by * .1F;
            addQuad(quads, side, u + 2, 12, -1,
                    a[0], a[1], az, b[0], b[1], bz, bx, by, innerB, ax, ay, innerA);
            addQuad(quads, side, inner + (i < 6 ? 0 : 1), 12, -1,
                    ax, ay, innerA, bx, by, innerB,
                    centerX, centerY, -.84F - centerY * .1F, centerX, centerY, -.84F - centerY * .1F);
            float backA = .9F + a[1] * .13F, backB = .9F + b[1] * .13F;
            addQuad(quads, side, outer, 12, -1, true,
                    b[0], b[1], bz, a[0], a[1], az,
                    a[0], a[1], backA, b[0], b[1], backB);
            addQuad(quads, side, u + 5, 12, -1, true,
                    b[0], b[1], backB, a[0], a[1], backA,
                    centerX, centerY, .9F + centerY * .13F, centerX, centerY, .9F + centerY * .13F);
        }
        for (int tuft = 0; tuft < 3; tuft++) {
            float innerX = -.7F + tuft * .65F;
            float top = -1.1F - tuft * .1F;
            addQuad(quads, side, inner + 1, 12, 0xffeeeeee,
                    innerX - .32F, -.25F, -.885F, innerX + .4F, -.25F, -.885F,
                    innerX + .18F, top, -.91F - top * .1F,
                    innerX - .14F, top + .14F, -.91F - (top + .14F) * .1F);
        }
        return List.copyOf(quads);
    }

    private static void addQuad(List<FurQuad> quads, int side, int u, int v, int color, float... points) {
        addQuad(quads, side, u, v, color, false, points);
    }

    private static void addQuad(List<FurQuad> quads, int side, int u, int v, int color, boolean detail, float... points) {
        var mirrored = new float[12];
        for (int i = 0; i < 4; i++) {
            int source = (side < 0 ? 3 - i : i) * 3;
            mirrored[i * 3] = points[source] * side;
            mirrored[i * 3 + 1] = points[source + 1];
            mirrored[i * 3 + 2] = points[source + 2];
        }
        float ax = mirrored[3] - mirrored[0], ay = mirrored[4] - mirrored[1], az = mirrored[5] - mirrored[2];
        float bx = mirrored[6] - mirrored[0], by = mirrored[7] - mirrored[1], bz = mirrored[8] - mirrored[2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        if (nx * nx + ny * ny + nz * nz < .000001F) {
            ax = mirrored[6] - mirrored[0]; ay = mirrored[7] - mirrored[1]; az = mirrored[8] - mirrored[2];
            bx = mirrored[9] - mirrored[0]; by = mirrored[10] - mirrored[1]; bz = mirrored[11] - mirrored[2];
            nx = ay * bz - az * by; ny = az * bx - ax * bz; nz = ax * by - ay * bx;
        }
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > .00001F) quads.add(new FurQuad(mirrored, u + .2F, v + .2F, .6F, .6F,
                nx / length, ny / length, nz / length, color, detail));
    }

    public void animateEars(float age, float forward, float sideways, float vertical, float headTurn) {
        animateEars(age, forward, sideways, vertical, headTurn, 0, 0, 0);
    }

    public void animateEars(float age, float forward, float sideways, float vertical, float headTurn,
            float pitchSway, float yawSway, float rollSway) {
        float calm = 1 - .55F * Mth.clamp(Math.abs(forward) + Math.abs(sideways) + Math.abs(vertical) * .35F, 0, 1);
        float earForward = forward * Mth.cos(headTurn) - sideways * Mth.sin(headTurn);
        float earSideways = sideways * Mth.cos(headTurn) + forward * Mth.sin(headTurn);
        float leftFlick = flick(age, 173, 21) * calm, rightFlick = flick(age + 63, 211, 17) * calm;
        float pitch = -.06F - earForward * .32F + vertical * .13F + pitchSway;
        float roll = -earSideways * .2F + rollSway;
        float yaw = -earSideways * .14F + yawSway;
        left.xRot = Mth.clamp(pitch + Mth.sin(age * .057F) * .045F * calm - leftFlick * .17F, -.6F, .35F);
        right.xRot = Mth.clamp(pitch + Mth.sin(age * .049F + 1.1F) * .045F * calm - rightFlick * .17F, -.6F, .35F);
        left.zRot = Mth.clamp(.18F + roll + Mth.sin(age * .041F) * .05F * calm + leftFlick * .13F, -.04F, .5F);
        right.zRot = Mth.clamp(-.18F + roll - Mth.sin(age * .037F + .7F) * .05F * calm - rightFlick * .13F, -.5F, .04F);
        left.yRot = Mth.clamp(yaw + Mth.sin(age * .029F + .8F) * .055F * calm - leftFlick * .14F, -.38F, .38F);
        right.yRot = Mth.clamp(yaw - Mth.sin(age * .033F + 2.3F) * .055F * calm + rightFlick * .14F, -.38F, .38F);
    }

    public void shakeEars(float progress) {
        if (progress < 0 || progress > 1) return;
        float envelope = Mth.sin(progress * Mth.PI);
        envelope *= envelope;
        float phase = progress * Mth.PI * 15;
        float leftShake = Mth.sin(phase) * envelope, rightShake = Mth.sin(phase + .5F) * envelope;
        left.yRot = Mth.clamp(left.yRot + leftShake * .46F, -.58F, .58F);
        right.yRot = Mth.clamp(right.yRot + rightShake * .46F, -.58F, .58F);
        left.zRot = Mth.clamp(left.zRot + leftShake * .18F, -.08F, .55F);
        right.zRot = Mth.clamp(right.zRot - rightShake * .18F, -.55F, .08F);
        float pitch = Mth.sin(phase * .5F) * envelope * .1F;
        left.xRot = Mth.clamp(left.xRot + pitch, -.65F, .4F);
        right.xRot = Mth.clamp(right.xRot - pitch, -.65F, .4F);
    }

    private static float flick(float age, float period, float duration) {
        float time = Mth.positiveModulo(age, period);
        if (time >= duration) return 0;
        float pulse = Mth.sin(time * Mth.PI / duration);
        return pulse * pulse;
    }

    public void renderEars(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        renderFur(pose, vertices, light, overlay, left, leftFur, false);
        renderFur(pose, vertices, light, overlay, right, rightFur, false);
    }

    public void renderEars(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
            net.minecraft.resources.ResourceLocation texture, int light, int overlay) {
        renderEars(pose, buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texture)), light, overlay);
        var vertices = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(
                io.github.nekogenix.Content.id("textures/entity/neko/ear_detail.png")));
        renderFur(pose, vertices, light, overlay, left, leftFur, true);
        renderFur(pose, vertices, light, overlay, right, rightFur, true);
    }

    private static void renderFur(PoseStack pose, VertexConsumer vertices, int light, int overlay,
            ModelPart part, List<FurQuad> quads, boolean detail) {
        pose.pushPose(); part.translateAndRotate(pose);
        for (var quad : quads) {
            if (!detail) quad.render(pose, vertices, light, overlay);
            else if (quad.detail) quad.renderDetail(pose, vertices, light, overlay);
        }
        pose.popPose();
    }

    private record FurQuad(float[] points, float u, float v, float width, float height,
            float nx, float ny, float nz, int color, boolean detail) {
        void renderDetail(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
            for (int i = 0; i < 4; i++) {
                float x = points[i * 3], y = points[i * 3 + 1], z = points[i * 3 + 2];
                float u = Math.abs(nz) > .7F ? x : z;
                vertices.addVertex(pose.last(), (x + nx * .015625F) / 16,
                                (y + ny * .015625F) / 16, (z + nz * .015625F) / 16)
                        .setColor(0x2effffff).setUv(.5F + u / 8, .55F + y / 8)
                        .setOverlay(overlay).setLight(light).setNormal(pose.last(), nx, ny, nz);
            }
        }
        void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
            for (int i = 0; i < 4; i++) vertex(pose, vertices, light, overlay, i,
                    points[i * 3], points[i * 3 + 1], points[i * 3 + 2], nx, ny, nz);
        }
        void vertex(PoseStack pose, VertexConsumer vertices, int light, int overlay, int i,
                float x, float y, float z, float normalX, float normalY, float normalZ) {
            vertices.addVertex(pose.last(), x / 16, y / 16, z / 16)
                    .setColor(color).setUv((u + (i == 1 || i == 2 ? width : 0)) / 64, (v + (i >= 2 ? height : 0)) / 32)
                    .setOverlay(overlay).setLight(light).setNormal(pose.last(), normalX, normalY, normalZ);
        }
    }

}
