package io.github.nekogenix.skin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

public final class CatSkin {
    public static final int MAX_BYTES = 16_384;
    public static final List<String> VARIANTS = List.of("tabby", "black", "red", "siamese", "british_shorthair",
            "calico", "persian", "ragdoll", "white", "jellie", "all_black");
    public static final CatSkin DEFAULT = new CatSkin("default", new byte[0]);

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
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IOException("skin_size");
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("skin_format");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!reader.getFormatName().equalsIgnoreCase("png") || reader.getWidth(0) != 64 || reader.getHeight(0) != 32)
                    throw new IOException("skin_dimensions");
                var image = reader.read(0);
                boolean visible = false;
                for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) visible |= (image.getRGB(x, y) >>> 24) != 0;
                if (!visible) throw new IOException("skin_empty");
                var output = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", output);
                if (output.size() > MAX_BYTES) throw new IOException("skin_size");
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (RuntimeException e) { throw new IOException("skin_format", e); }
    }
}
