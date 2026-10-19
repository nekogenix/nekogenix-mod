package io.github.nekogenix.skin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

public final class CatSkin {
    public static final int MAX_BYTES = 16_384;
    private static final boolean[][] SURFACE = surface();
    public static final List<String> VARIANTS = List.of("tabby", "black", "red", "siamese", "british_shorthair",
            "calico", "persian", "ragdoll", "white", "jellie", "all_black");
    public static final CatSkin DEFAULT = new CatSkin("default", new byte[0]);

    public static final class InvalidImageException extends IOException {
        public final int width, height;
        private InvalidImageException(String reason) { this(reason, -1, -1); }
        private InvalidImageException(String reason, int width, int height) {
            super(reason); this.width = width; this.height = height;
        }
    }

    private final String variant;
    private final byte[] png;
    private final String hash;

    public CatSkin(String variant, byte[] png) {
        if (!(VARIANTS.contains(variant) || variant.equals("default") || variant.equals("custom"))
                || png.length > MAX_BYTES || variant.equals("custom") == (png.length == 0)) {
            throw new IllegalArgumentException("Invalid cat skin");
        }
        this.variant = variant;
        this.png = png.clone();
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public String variant() { return variant; }
    public byte[] png() { return png.clone(); }
    public String hash() { return hash; }

    public CatSkin validated() throws IOException {
        return variant.equals("custom") ? new CatSkin(variant, normalize(png)) : this;
    }

    public static byte[] normalize(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new InvalidImageException("skin_size");
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new InvalidImageException("skin_format");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!reader.getFormatName().equalsIgnoreCase("png")) throw new InvalidImageException("skin_format");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width != 64 || height != 32) throw new InvalidImageException("skin_dimensions", width, height);
                var source = reader.read(0);
                var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, width, height, source.getRGB(0, 0, width, height, null, 0, width), 0, width);
                boolean visible = false;
                for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) if (SURFACE[y][x]) {
                    int pixel = image.getRGB(x, y);
                    visible |= (pixel >>> 24) != 0;
                    image.setRGB(x, y, pixel | 0xff000000);
                }
                if (!visible) throw new InvalidImageException("skin_empty");
                var output = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", output);
                if (output.size() > MAX_BYTES) throw new InvalidImageException("skin_size");
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidImageException invalid) throw invalid;
            var invalid = new InvalidImageException("skin_format"); invalid.initCause(e); throw invalid;
        }
    }

    private static boolean[][] surface() {
        var pixels = new boolean[32][64];
        int[][] cubes = {{0, 0, 5, 4, 5}, {20, 0, 4, 16, 6}, {0, 10, 1, 1, 2}, {6, 10, 1, 1, 2},
                {0, 15, 1, 8, 1}, {4, 15, 1, 8, 1}, {8, 13, 2, 6, 2}, {40, 0, 2, 10, 2}, {0, 24, 3, 2, 2}};
        for (var cube : cubes) {
            int u = cube[0], v = cube[1], w = cube[2], h = cube[3], d = cube[4];
            for (int y = v; y < v + d; y++) for (int x = u + d; x < u + d + 2 * w; x++) pixels[y][x] = true;
            for (int y = v + d; y < v + d + h; y++) for (int x = u; x < u + 2 * (w + d); x++) pixels[y][x] = true;
        }
        return pixels;
    }
}
