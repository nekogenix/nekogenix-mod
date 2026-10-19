package io.github.nekogenix;

import io.github.nekogenix.client.skin.SkinLibrary;
import io.github.nekogenix.skin.CatSkin;
import io.github.nekogenix.skin.SkinPackets;
import io.github.nekogenix.skin.SkinServer;
import io.github.nekogenix.skin.SkinStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CatSkinTests {
    @GameTest(template = "empty")
    public static void pngValidationAndPacketBounds(GameTestHelper helper) throws Exception {
        byte[] original = png(64, 32, 0xffff8844);
        var skin = new CatSkin("custom", original).validated();
        original[0] = 0;
        helper.assertTrue(skin.validated().hash().equals(skin.hash()), "Validated PNG is immutable and has a stable content hash");
        for (byte[] invalid : new byte[][] {png(128, 64, -1), png(64, 32, 0), new byte[CatSkin.MAX_BYTES + 1], new byte[] {1, 2, 3}}) {
            boolean rejected = false;
            try { CatSkin.normalize(invalid); } catch (java.io.IOException e) { rejected = true; }
            helper.assertTrue(rejected, "Wrong dimensions, invisible, oversized and malformed images are rejected");
        }
        try {
            CatSkin.normalize(png(250, 32, -1));
            throw new IllegalStateException("A texture sheet cannot be imported as a single cat skin");
        } catch (CatSkin.InvalidImageException e) {
            helper.assertTrue(e.width == 250 && e.height == 32, "A rejected skin reports its actual dimensions");
        }
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            SkinPackets.Apply.CODEC.encode(buffer, new SkinPackets.Apply(skin));
            helper.assertTrue(SkinPackets.Apply.CODEC.decode(buffer).skin().hash().equals(skin.hash()), "PNG packets round trip without changing their contents");
            buffer.clear(); buffer.writeUtf("custom"); buffer.writeVarInt(CatSkin.MAX_BYTES + 1);
            boolean rejected = false;
            try { SkinPackets.Apply.CODEC.decode(buffer); } catch (RuntimeException e) { rejected = true; }
            helper.assertTrue(rejected, "Oversized network payload is rejected before allocating its PNG");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transparentBodyPixelsBecomeOpaque(GameTestHelper helper) throws Exception {
        var image = new java.awt.image.BufferedImage(64, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) image.setRGB(x, y, 0x00123456);
        image.setRGB(6, 6, 0x01778899);
        var encoded = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, "PNG", encoded);
        byte[] normalized = CatSkin.normalize(encoded.toByteArray());
        var result = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(normalized));
        helper.assertTrue(result.getRGB(6, 6) == 0xff778899, "A single barely visible pixel becomes fully opaque without recoloring");
        int[][] points = {{7, 7}, {27, 10}, {2, 12}, {8, 12}, {1, 18}, {5, 18}, {10, 18}, {42, 5}, {3, 26}};
        for (var point : points) helper.assertTrue(result.getRGB(point[0], point[1]) == 0xff123456,
                "Every body part is present even where its PNG was completely transparent");
        helper.assertTrue(result.getRGB(63, 31) == 0x00123456, "Unused texture space stays unchanged");
        helper.assertTrue(java.util.Arrays.equals(normalized, CatSkin.normalize(normalized)), "Opacity normalization is stable across client server and cache validation");
        image.setRGB(6, 6, 0); image.setRGB(63, 31, 0xffffffff);
        encoded.reset(); javax.imageio.ImageIO.write(image, "PNG", encoded);
        try {
            CatSkin.normalize(encoded.toByteArray());
            throw new IllegalStateException("A pixel outside the cat surface bypassed empty skin validation");
        } catch (CatSkin.InvalidImageException expected) {}

        var palette = new java.awt.image.IndexColorModel(8, 2, new byte[] {17, 68}, new byte[] {34, 85}, new byte[] {51, 102}, new byte[] {1, 0});
        var indexed = new java.awt.image.BufferedImage(64, 32, java.awt.image.BufferedImage.TYPE_BYTE_INDEXED, palette);
        encoded.reset(); javax.imageio.ImageIO.write(indexed, "PNG", encoded);
        var opaque = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(CatSkin.normalize(encoded.toByteArray())));
        helper.assertTrue(opaque.getRGB(7, 7) == 0xff112233, "Indexed PNG transparency cannot bypass opacity normalization");
        Path directory = Files.createTempDirectory("nekogenix-skin-opacity-");
        try {
            var old = new CatSkin("custom", encoded.toByteArray());
            Files.write(directory.resolve(old.hash() + ".png"), old.png());
            Files.writeString(directory.resolve("library.json"), "{\"skins\":[{\"name\":\"Old cat\",\"hash\":\"" + old.hash()
                    + "\"}],\"selected\":\"" + old.hash() + "\"}");
            var migrated = new SkinLibrary(directory);
            helper.assertTrue(!migrated.selected().hash().equals(old.hash()) && migrated.selected().hash().equals(old.validated().hash()),
                    "The existing selected texture migrates to its opaque version without losing the selection");
            migrated = new SkinLibrary(directory);
            helper.assertTrue(migrated.entries().getFirst().name().equals("Old cat") && migrated.selected().hash().equals(old.validated().hash()),
                    "Migrated names selection and texture files survive another launch");
        } finally { remove(directory); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ownerActivityCleanupAndDeduplication(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("nekogenix-skin-store-");
        try {
            long now = SkinStore.RETENTION * 3;
            UUID absent = UUID.randomUUID(), present = UUID.randomUUID();
            var skin = new CatSkin("custom", png(64, 32, 0xff88cc55));
            var store = new SkinStore(directory);
            store.put(absent, skin, now - SkinStore.RETENTION);
            store.put(present, skin, now - SkinStore.RETENTION);
            try (var files = Files.list(directory)) { helper.assertTrue(files.filter(p -> p.toString().endsWith(".png")).count() == 1, "Identical player skins share one image"); }
            store.get(absent);
            store.seen(present, now);
            store.clean(now);
            helper.assertTrue(store.get(absent).variant().equals("custom"), "A full year is retained until the boundary has passed");
            store.clean(now + 1);
            helper.assertTrue(store.get(absent).variant().equals("default") && store.get(present).variant().equals("custom"),
                    "Viewing does not renew absence, while the owner's activity retains the shared image");
            store = new SkinStore(directory);
            helper.assertTrue(store.get(present).hash().equals(skin.validated().hash()), "The selected skin survives server restart");
            store.put(present, new CatSkin("jellie", new byte[0]), now + 2);
            try (var files = Files.list(directory)) { helper.assertTrue(files.noneMatch(p -> p.toString().endsWith(".png")), "Unreferenced images are removed after replacement"); }
            helper.succeed();
        } finally { remove(directory); }
    }

    @GameTest(template = "empty")
    public static void localLibraryImportSelectionAndDeletion(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("nekogenix-skin-library-");
        try {
            var library = new SkinLibrary(directory);
            String initial = library.selected().variant();
            helper.assertTrue(CatSkin.VARIANTS.contains(initial), "The first selection is a vanilla cat variant");
            helper.assertTrue(new SkinLibrary(directory).selected().variant().equals(initial), "The initial random selection is persisted immediately");
            var skin = library.add("My cat", png(64, 32, 0xffaabbcc));
            library.add("Duplicate", skin.skin().png());
            helper.assertTrue(library.entries().size() == 1, "Importing the same PNG does not duplicate it");
            helper.assertTrue(library.entries().getFirst().name().equals("Duplicate"), "Saving an existing texture uses the chosen name");
            library.select(skin.skin()); library = new SkinLibrary(directory);
            helper.assertTrue(library.selected().hash().equals(skin.skin().hash()), "The selected custom skin is restored on another launch");
            helper.assertTrue(library.entries().getFirst().name().equals("Duplicate"), "Custom names survive another launch");
            library.rename(skin.skin(), "Renamed cat");
            library = new SkinLibrary(directory);
            helper.assertTrue(library.entries().getFirst().name().equals("Renamed cat") && library.selected().hash().equals(skin.skin().hash()),
                    "Renaming persists without changing the texture or selected skin");
            try {
                library.rename(new CatSkin("jellie", new byte[0]), "Not allowed");
                throw new IllegalStateException("Vanilla skin renamed");
            } catch (java.io.IOException expected) {
                helper.assertTrue(library.entries().getFirst().name().equals("Renamed cat"), "Vanilla names cannot change the custom library");
            }
            try {
                library.add("Invalid sheet", png(250, 32, -1));
                throw new IllegalStateException("Invalid import accepted");
            } catch (CatSkin.InvalidImageException expected) {
                helper.assertTrue(library.entries().size() == 1 && library.selected().hash().equals(skin.skin().hash()),
                        "Rejecting an invalid import preserves the existing library and selected skin");
            }
            library.remove(skin.skin());
            helper.assertTrue(library.entries().isEmpty() && CatSkin.VARIANTS.contains(library.selected().variant()), "Deleting the active custom skin restores a saved vanilla variant");
            Path legacy = directory.resolve("legacy"); Files.createDirectories(legacy);
            Files.writeString(legacy.resolve("library.json"), "{\"skins\":[],\"selected\":\"default\"}");
            var migrated = new SkinLibrary(legacy);
            helper.assertTrue(CatSkin.VARIANTS.contains(migrated.selected().variant())
                    && new SkinLibrary(legacy).selected().variant().equals(migrated.selected().variant()), "Legacy Automatic selections migrate once and remain stable");
            helper.succeed();
        } finally { remove(directory); }
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void skinUploadsCannotSkipAuthentication(GameTestHelper helper) throws Exception {
        var player = GameplayTests.player(helper);
        SkinServer.apply(player, new CatSkin("jellie", new byte[0]));
        SkinServer.proof(player);
        SkinServer.request(player, player.getUUID(), true);
        helper.runAfterDelay(10, () -> {
            try {
                Path directory = helper.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/nekogenix/cat_skins");
                var store = new SkinStore(directory);
                helper.assertTrue(store.get(player.getUUID()).variant().equals("default"), "An unauthenticated upload never reaches persistent skin storage");
                helper.succeed();
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            finally { GameplayTests.remove(player); }
        });
    }

    static byte[] png(int width, int height, int color) throws java.io.IOException {
        var image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setRGB(x, y, color);
        var output = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, "PNG", output); return output.toByteArray();
    }
    private static void remove(Path directory) throws java.io.IOException {
        if (!directory.toAbsolutePath().getParent().equals(Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath())) throw new IllegalStateException("Unexpected test path");
        try (var paths = Files.walk(directory)) {
            for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
