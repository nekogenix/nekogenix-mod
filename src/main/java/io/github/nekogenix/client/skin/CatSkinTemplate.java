package io.github.nekogenix.client.skin;

import io.github.nekogenix.skin.CatSkin;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class CatSkinTemplate {
    public static CatSkin create() throws IOException {
        var image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        // UV coordinates and cuboid dimensions from Minecraft 1.21.1's cat model.
        cube(image, 0, 0, 5, 4, 5, 0xe9b45d);
        cube(image, 20, 0, 4, 16, 6, 0x55b6a0);
        cube(image, 0, 10, 1, 1, 2, 0xe188a3);
        cube(image, 6, 10, 1, 1, 2, 0xe188a3);
        cube(image, 0, 15, 1, 8, 1, 0xa68bd4);
        cube(image, 4, 15, 1, 8, 1, 0xa68bd4);
        cube(image, 8, 13, 2, 6, 2, 0x97be69);
        cube(image, 40, 0, 2, 10, 2, 0x72a7d8);
        cube(image, 0, 24, 3, 2, 2, 0xd78368);
        var output = new ByteArrayOutputStream(); ImageIO.write(image, "PNG", output);
        return new CatSkin("custom", output.toByteArray()).validated();
    }

    private static void cube(BufferedImage image, int u, int v, int x, int y, int z, int color) {
        face(image, u + z, v, x, z, color, 10);
        face(image, u + z + x, v, x, z, color, -12);
        face(image, u, v + z, z, y, color, -6);
        face(image, u + z, v + z, x, y, color, 0);
        face(image, u + z + x, v + z, z, y, color, 6);
        face(image, u + z + x + z, v + z, x, y, color, -18);
    }
    private static void face(BufferedImage image, int u, int v, int width, int height, int color, int shade) {
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int delta = shade - (x == 0 || y == 0 ? 25 : 0);
            int r = Math.clamp((color >> 16 & 255) + delta, 0, 255);
            int g = Math.clamp((color >> 8 & 255) + delta, 0, 255);
            int b = Math.clamp((color & 255) + delta, 0, 255);
            image.setRGB(u + x, v + y, 0xff000000 | r << 16 | g << 8 | b);
        }
    }
    private CatSkinTemplate() {}
}
