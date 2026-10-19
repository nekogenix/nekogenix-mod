package io.github.nekogenix;

import io.github.nekogenix.client.skin.CatSkinScreen;
import io.github.nekogenix.client.skin.CatSkins;
import io.github.nekogenix.skin.CatSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatSkinSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catSkinSmokeTest");
    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (ENABLED && event.getEntity() instanceof ServerPlayer player && player.tickCount == 20) CatTransformationTests.cat(player);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;
        private static java.nio.file.Path imported;
        private static CatSkin testSkin;
        private static int menuTicks = -1;

        @SubscribeEvent public static void status(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = Minecraft.getInstance();
            mc.getToasts().clear();
            if (menuTicks >= 0) {
                if (++menuTicks == 20) capture = "cat-skins-main-menu.png";
                if (menuTicks == 30) {
                    check(mc.screen instanceof CatSkinScreen && mc.level == null, "The library also works outside a world");
                    Nekogenix.LOGGER.info("Nekogenix skins smoke passed: native menu vanilla library custom PNG preview small window deletion main menu and unlicensed session rejection");
                    mc.stop();
                }
                return;
            }
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            if (age == 40) mc.setScreen(new SkinCustomizationScreen(null, mc.options));
            if (age == 50) {
                boolean found = false;
                for (var child : mc.screen.children()) if (child instanceof OptionsList options) {
                    for (net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<?> row : options.children()) for (var entry : row.children()) if (entry instanceof Button button
                            && button.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("skins.nekogenix.title").getString())) {
                        button.onPress(); found = true; break;
                    }
                    if (found) break;
                }
                check(found && mc.screen instanceof CatSkinScreen, "Cat Skin opens from native Skin Customization");
            }
            if (age == 80) capture = "cat-skins-library.png";
            if (age == 90) {
                try {
                    imported = java.nio.file.Files.createTempFile("nekogenix-skin-preview-", ".png");
                    var image = new java.awt.image.BufferedImage(64, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) image.setRGB(x, y, ((x + y) / 4 % 2 == 0 ? 0xffa968b0 : 0xffe6b8c3));
                    javax.imageio.ImageIO.write(image, "PNG", imported.toFile());
                    ((CatSkinScreen)mc.screen).onFilesDrop(java.util.List.of(imported));
                } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            }
            if (age == 120) {
                testSkin = CatSkins.entries().stream().filter(e -> e.name().startsWith("nekogenix-skin-preview-")).map(io.github.nekogenix.client.skin.SkinLibrary.Entry::skin).findFirst().orElseThrow();
                check(testSkin.variant().equals("custom"), "A PNG imports into the library without affecting the official player skin");
                capture = "cat-skins-custom.png";
                ((CatSkinScreen)mc.screen).mouseDragged(50, 100, 0, 40, 0);
            }
            if (age == 130) {
                mc.options.guiScale().set(4); mc.resizeDisplay(); capture = "cat-skins-small.png";
            }
            if (age == 160) {
                check(!CatSkins.licensed(), "A development client without a valid Minecraft session cannot enable cat skins");
                CatSkins.apply(testSkin);
                check(!CatSkins.selected().hash().equals(testSkin.hash()), "Applying cannot skip the account check");
                CatSkins.remove(testSkin);
            }
            if (age == 180) {
                check(CatSkins.entries().stream().noneMatch(e -> e.skin().hash().equals(testSkin.hash())), "Custom skins can be removed from the library");
                try { java.nio.file.Files.deleteIfExists(imported); } catch (java.io.IOException e) { throw new IllegalStateException(e); }
                mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
                mc.options.guiScale().set(2); mc.resizeDisplay();
                mc.setScreen(new CatSkinScreen(new net.minecraft.client.gui.screens.TitleScreen()));
                menuTicks = 0;
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED || capture == null) return;
            var mc = Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(capture));
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            capture = null;
        }
        private static void check(boolean valid, String message) { if (!valid) throw new IllegalStateException("Cat skin smoke failed: " + message); }
    }
}
