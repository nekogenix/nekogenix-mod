package io.github.nekogenix.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.joml.Vector3f;

public final class SyringeGeometry implements IUnbakedGeometry<SyringeGeometry> {
    public static final IGeometryLoader<SyringeGeometry> LOADER = (json, context) -> new SyringeGeometry();

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
            ModelState state, ItemOverrides overrides) {
        var hint = context.getRenderTypeHint();
        var builder = IModelBuilder.of(context.useAmbientOcclusion(), context.useBlockLight(), false, context.getTransforms(),
                overrides, sprites.apply(context.getMaterial("particle")), hint == null ? RenderTypeGroup.EMPTY : context.getRenderType(hint));
        addQuads(context, builder, sprites, state);
        return builder.build();
    }

    private void addQuads(IGeometryBakingContext context, IModelBuilder<?> builder,
            Function<Material, TextureAtlasSprite> sprites, ModelState state) {
        var sprite = sprites.apply(context.getMaterial("layer0"));
        var contents = sprite.contents();
        int width = contents.width(), height = contents.height();
        boolean[][] opaque = new boolean[height][width];
        contents.getUniqueFrames().forEach(frame -> {
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                opaque[y][x] |= !contents.isTransparent(frame, x, y);
        });
        var elements = new ArrayList<BlockElement>();
        elements.add(new ItemModelGenerator().processFrames(0, "layer0", contents).getFirst());
        float sx = 16F / width, sy = 16F / height;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (!opaque[y][x]) continue;
            if (y == 0 || !opaque[y - 1][x]) edge(elements, x, y, sx, sy, Direction.UP);
            if (y == height - 1 || !opaque[y + 1][x]) edge(elements, x, y, sx, sy, Direction.DOWN);
            if (x == 0 || !opaque[y][x - 1]) edge(elements, x, y, sx, sy, Direction.EAST);
            if (x == width - 1 || !opaque[y][x + 1]) edge(elements, x, y, sx, sy, Direction.WEST);
        }
        ClientHooks.fixItemModelSeams(elements, sprite);
        var transform = context.getRootTransform();
        if (!transform.isIdentity()) state = UnbakedGeometryHelper.composeRootTransformIntoModelState(state, transform);
        UnbakedGeometryHelper.bakeElements(elements, material -> sprite, state).forEach(builder::addUnculledFace);
    }

    private static void edge(List<BlockElement> elements, int x, int y, float sx, float sy, Direction side) {
        float left = x * sx, right = (x + 1) * sx, top = 16 - y * sy, bottom = 16 - (y + 1) * sy;
        var from = new Vector3f(left, top, 7.5F);
        var to = new Vector3f(right, bottom, 8.5F);
        switch (side) {
            case UP -> to.y = top;
            case DOWN -> from.y = bottom;
            case EAST -> to.x = left;
            case WEST -> from.x = right;
            default -> throw new IllegalArgumentException("Expected a sprite edge");
        }
        elements.add(new BlockElement(from, to, Map.of(side, new BlockElementFace(null, 0, "layer0",
                new BlockFaceUV(new float[] {left, y * sy, right, (y + 1) * sy}, 0))), null, true));
    }
}
