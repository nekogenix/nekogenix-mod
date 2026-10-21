package io.github.nekogenix;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SyringeGeometryChecks {
    private static List<String> names() {
        var names = new ArrayList<>(List.of("syringe", "used_syringe", "neko_injection", "stabilizing_injection"));
        for (String prefix : List.of("syringe_pressed_", "neko_injection_pressed_", "stabilizing_injection_pressed_"))
            for (int i = 1; i <= 3; i++) names.add(prefix + i);
        for (String prefix : List.of("syringe_filling_", "stabilizing_syringe_filling_"))
            for (int i = 1; i <= 7; i++) names.add(prefix + i);
        return names;
    }

    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        if (Boolean.getBoolean("nekogenix.injectionHandsSmokeTest"))
            for (String name : names()) event.register(ModelResourceLocation.standalone(Content.id("item/" + name)));
    }

    static void verify() {
        var mc = Minecraft.getInstance(); int reproduced = 0;
        for (String name : names()) {
            var model = mc.getModelManager().getModel(ModelResourceLocation.standalone(Content.id("item/" + name)));
            if (model == mc.getModelManager().getMissingModel()) throw new IllegalStateException("Missing syringe model " + name);
            var sprite = model.getParticleIcon();
            var quads = model.getQuads(null, null, net.minecraft.util.RandomSource.create(0));
            if (bad(quads, sprite.contents()) != 0) throw new IllegalStateException("An internal edge crosses the transparent barrel: " + name);
            if (model.isGui3d() || model.usesBlockLight()) throw new IllegalStateException("The generated item's GUI lighting changed: " + name);
            var original = new ItemModelGenerator().processFrames(0, "layer0", sprite.contents());
            reproduced += bad(UnbakedGeometryHelper.bakeElements(original, material -> sprite, BlockModelRotation.X0_Y0), sprite.contents());
        }
        if (reproduced == 0) throw new IllegalStateException("The original barrel strip was not reproduced by the control models");
        Nekogenix.LOGGER.info("Nekogenix syringe surface smoke passed: {} models have no internal side strips; original generator reproduced {} invalid edge samples", names().size(), reproduced);
    }

    private static int bad(List<BakedQuad> quads, SpriteContents sprite) {
        int bad = 0, w = sprite.width(), h = sprite.height();
        for (var quad : quads) {
            Direction side = quad.getDirection();
            if (side.getAxis() == Direction.Axis.Z) continue;
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            int[] vertices = quad.getVertices(); int stride = vertices.length / 4;
            for (int i = 0; i < 4; i++) {
                float x = Float.intBitsToFloat(vertices[i * stride]) * w;
                float y = h - Float.intBitsToFloat(vertices[i * stride + 1]) * h;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
            boolean vertical = side.getAxis() == Direction.Axis.X;
            int boundary = Math.round(vertical ? (minX + maxX) / 2 : (minY + maxY) / 2);
            int start = Math.round(vertical ? minY : minX);
            int end = Math.round(vertical ? maxY : maxX);
            for (int i = start; i < end; i++) {
                int x = vertical ? boundary - (side == Direction.EAST ? 1 : 0) : i;
                int y = vertical ? i : boundary - (side == Direction.DOWN ? 1 : 0);
                int nx = x + (side == Direction.WEST ? -1 : side == Direction.EAST ? 1 : 0);
                int ny = y + (side == Direction.UP ? -1 : side == Direction.DOWN ? 1 : 0);
                if (x < 0 || x >= w || y < 0 || y >= h || sprite.isTransparent(0, x, y)
                        || nx >= 0 && nx < w && ny >= 0 && ny < h && !sprite.isTransparent(0, nx, ny)) {
                    bad++;
                }
            }
        }
        return bad;
    }
}
