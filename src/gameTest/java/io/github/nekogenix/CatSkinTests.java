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
            var skin = library.add("My cat", png(64, 32, 0xffaabbcc));
            library.add("Duplicate", skin.skin().png());
            helper.assertTrue(library.entries().size() == 1, "Importing the same PNG does not duplicate it");
            library.select(skin.skin()); library = new SkinLibrary(directory);
            helper.assertTrue(library.selected().hash().equals(skin.skin().hash()), "The selected custom skin is restored on another launch");
            library.remove(skin.skin());
            helper.assertTrue(library.entries().isEmpty() && library.selected().variant().equals("default"), "Deleting the active custom skin restores Automatic");
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
