package io.github.nekogenix.client.skin;

import io.github.nekogenix.skin.CatSkin;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class CatSkinTemplate {
    public static CatSkin create() throws IOException { return create(false); }
    public static CatSkin placeholder() throws IOException { return create(true); }

    private static CatSkin create(boolean neutral) throws IOException {
        var image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        // UV coordinates and cuboid dimensions from Minecraft 1.21.1's cat model.
        cube(image, 0, 0, 5, 4, 5, 0x70a8cf, neutral);
        cube(image, 20, 0, 4, 16, 6, 0x8879bf, neutral);
        cube(image, 0, 10, 1, 1, 2, 0xda8f92, neutral);
        cube(image, 6, 10, 1, 1, 2, 0xda8f92, neutral);
        cube(image, 0, 15, 1, 8, 1, 0xc7a665, neutral);
        cube(image, 4, 15, 1, 8, 1, 0xc7a665, neutral);
        cube(image, 8, 13, 2, 6, 2, 0x65aaa2, neutral);
        cube(image, 40, 0, 2, 10, 2, 0xbd8968, neutral);
        cube(image, 0, 24, 3, 2, 2, 0xb2c19a, neutral);
        var output = new ByteArrayOutputStream(); ImageIO.write(image, "PNG", output);
        return new CatSkin("custom", output.toByteArray()).validated();
    }

    private static void cube(BufferedImage image, int u, int v, int x, int y, int z, int color, boolean neutral) {
        face(image, u + z, v, x, z, color, 10, neutral);
        face(image, u + z + x, v, x, z, color, -12, neutral);
        face(image, u, v + z, z, y, color, -6, neutral);
        face(image, u + z, v + z, x, y, color, 0, neutral);
        face(image, u + z + x, v + z, z, y, color, 6, neutral);
        face(image, u + z + x + z, v + z, x, y, color, -18, neutral);
    }
    private static void face(BufferedImage image, int u, int v, int width, int height, int color, int shade, boolean neutral) {
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            boolean edge = x == 0 || y == 0 || x == width - 1 || y == height - 1;
            int delta = shade - (edge ? 22 : 0);
            if (neutral) { color = edge ? 0x606060 : 0x343434; delta = shade / 4; }
            int r = Math.clamp((color >> 16 & 255) + delta, 0, 255);
            int g = Math.clamp((color >> 8 & 255) + delta, 0, 255);
            int b = Math.clamp((color & 255) + delta, 0, 255);
            image.setRGB(u + x, v + y, 0xff000000 | r << 16 | g << 8 | b);
        }
    }
    private CatSkinTemplate() {}
}
