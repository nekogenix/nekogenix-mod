package io.github.nekogenix;

import io.github.nekogenix.client.skin.CatSkinScreen;
import io.github.nekogenix.client.skin.CatSkins;
import io.github.nekogenix.client.skin.NewCatSkinScreen;
import io.github.nekogenix.skin.CatSkin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
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
        private static Path imported, rejected, template;
        private static CatSkin testSkin;
        private static CatNameplatePreview nameplates;
        private static int menuTicks = -1, previousSkins;

        @SubscribeEvent public static void status(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) throws java.io.IOException {
            if (!ENABLED) return;
            var mc = Minecraft.getInstance(); mc.getToasts().clear();
            if (menuTicks >= 0) {
                if (++menuTicks == 20) capture = "cat-skins-main-menu.png";
                if (menuTicks == 30) {
                    check(mc.screen instanceof NewCatSkinScreen && mc.level == null, "The form also works outside a world");
                    Nekogenix.LOGGER.info("Nekogenix skins smoke passed: custom texture draft naming UV export cancellation persistence preview deletion and unlicensed session rejection");
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
                    for (net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<?> row : options.children()) for (var entry : row.children())
                        if (entry instanceof Button button && button.getMessage().getString().equals(text("title").getString())) { button.onPress(); found = true; break; }
                    if (found) break;
                }
                check(found && mc.screen instanceof CatSkinScreen, "Cat Skin opens from native Skin Customization");
                var done = find(mc.screen, net.minecraft.network.chat.CommonComponents.GUI_DONE);
                check(done.getWidth() == 200 && done.getHeight() == 20 && done.getX() == (mc.screen.width - 200) / 2, "Done has the centered vanilla size");
                check(CatSkin.VARIANTS.contains(CatSkins.selected().variant()) && !name(mc.screen).visible, "A vanilla variant is selected and its name is read only");
            }
            if (age == 55) {
                var selected = find(mc.screen, Component.translatable("skins.nekogenix.variant." + CatSkins.selected().variant()));
                mc.screen.mouseClicked(selected.getX() + 5, selected.getY() + 25, 0);
                capture = "cat-skins-selected-card.png";
            }
            if (age == 65) {
                previousSkins = CatSkins.entries().size(); button(mc.screen, "import").onPress();
                check(mc.screen instanceof NewCatSkinScreen, "New Skin opens the draft form");
                imported = Files.createTempFile("nekogenix-custom-skin-", ".png");
                rejected = Files.createTempFile("nekogenix-rejected-skin-", ".png");
                template = Files.createTempFile("nekogenix-template-", ".png");
                Files.write(rejected, CatSkinTests.png(250, 32, -1));
                var image = new java.awt.image.BufferedImage(64, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) {
                    int color = (x / 2 + y / 3) % 5 == 0 ? 0xff61dcc6 : 0xff343044;
                    if (y == 6 && (x == 6 || x == 9)) color = 0xffdfec74;
                    image.setRGB(x, y, color);
                }
                javax.imageio.ImageIO.write(image, "PNG", imported.toFile()); mc.screen.onFilesDrop(List.of(rejected));
            }
            if (age == 85) {
                check(CatSkins.entries().size() == previousSkins && !button(mc.screen, "save").active, "Invalid dimensions cannot be saved");
                capture = "cat-skins-rejected-form.png";
                var placeholder = io.github.nekogenix.client.skin.CatSkinTemplate.placeholder();
                var pixels = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(placeholder.png()));
                for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) {
                    int c = pixels.getRGB(x, y);
                    check((c >> 16 & 255) == (c >> 8 & 255) && (c >> 8 & 255) == (c & 255), "The empty preview has no colored body parts");
                }
                check((pixels.getRGB(5, 6) & 255) > (pixels.getRGB(7, 6) & 255), "Body outlines are brighter than their interior");
                Files.write(mc.gameDirectory.toPath().resolve("screenshots/cat-skin-placeholder.png"), placeholder.png());
            }
            if (age == 90) mc.screen.onFilesDrop(List.of(imported));
            if (age == 110) {
                check(CatSkins.entries().size() == previousSkins && button(mc.screen, "save").active, "A preview does not add anything to the library");
                name(mc.screen).setValue("Midnight Mint"); ((NewCatSkinScreen)mc.screen).saveTemplate(template);
                capture = "cat-skins-custom-form.png";
            }
            if (age == 115) { mc.options.guiScale().set(4); mc.resizeDisplay(); capture = "cat-skins-small-form.png"; }
            if (age == 125) {
                check(name(mc.screen).getValue().equals("Midnight Mint"), "Resizing retains the draft name"); mc.screen.onClose();
                check(mc.screen instanceof CatSkinScreen && CatSkins.entries().size() == previousSkins, "Cancel discards a draft without saving it");
            }
            if (age == 130) { button(mc.screen, "import").onPress(); mc.screen.onFilesDrop(List.of(imported)); }
            if (age == 150) { name(mc.screen).setValue("Midnight Mint"); button(mc.screen, "save").onPress(); }
            if (age == 165) {
                check(mc.screen instanceof CatSkinScreen && CatSkins.entries().size() == previousSkins + 1, "Save adds exactly one named custom skin");
                testSkin = CatSkins.entries().stream().filter(e -> e.name().equals("Midnight Mint")).findFirst().orElseThrow().skin();
                var restored = new io.github.nekogenix.client.skin.SkinLibrary(mc.gameDirectory.toPath().resolve("nekogenix/cat_skins/library"));
                check(restored.entries().stream().anyMatch(e -> e.name().equals("Midnight Mint") && e.skin().hash().equals(testSkin.hash())), "The custom texture and its name are persisted");
                capture = "cat-skins-custom-library.png";
                check(!CatSkins.licensed(), "The development client has no verified account"); CatSkins.apply(testSkin);
                check(!CatSkins.selected().hash().equals(testSkin.hash()), "Applying cannot skip the account check");
            }
            if (age == 170) {
                var edit = name(mc.screen); mc.screen.mouseClicked(edit.getX() + 1, edit.getY() + 1, 0);
                check(edit.isFocused(), "The styled name field can be edited");
                edit.setValue("Moonlit Mint"); mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0);
            }
            if (age == 180) {
                check(CatSkins.entries().stream().anyMatch(e -> e.name().equals("Moonlit Mint") && e.skin().hash().equals(testSkin.hash())), "Enter saves the new custom name");
                var edit = name(mc.screen); mc.screen.mouseClicked(edit.getX() + 1, edit.getY() + 1, 0); edit.setValue("Discard this");
                mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0);
                check(mc.screen instanceof CatSkinScreen && edit.getValue().equals("Moonlit Mint"), "Escape cancels a name edit without leaving the library");
                focusCustom(mc.screen); capture = "cat-skins-card-actions.png";
            }
            if (age == 185) {
                mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_U, 0, 0);
                check(!CatSkins.selected().hash().equals(testSkin.hash()), "Card actions preserve account verification");
                mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE, 0, 0);
                check(mc.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen, "Deleting a custom skin requires confirmation");
                find(mc.screen, net.minecraft.network.chat.CommonComponents.GUI_NO).onPress();
                check(CatSkins.entries().size() == previousSkins + 1, "Cancel keeps the custom skin");
            }
            if (age == 195) {
                focusCustom(mc.screen); mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE, 0, 0);
                find(mc.screen, net.minecraft.network.chat.CommonComponents.GUI_YES).onPress();
            }
            if (age == 215) {
                check(CatSkins.entries().size() == previousSkins, "Custom skins can be removed");
                check(!name(mc.screen).visible, "After deletion the vanilla name is read only again");
                new CatSkin("custom", Files.readAllBytes(template)).validated();
                Files.copy(template, mc.gameDirectory.toPath().resolve("screenshots/cat-skin-template.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.delete(imported); Files.delete(rejected); Files.delete(template);
                mc.options.guiScale().set(2); mc.resizeDisplay();
                nameplates = new CatNameplatePreview(); mc.setScreen(nameplates);
            }
            if (nameplates != null) nameplates.tickActors(age);
            if (age == 235) capture = "cat-player-nameplates.png";
            if (age == 240) {
                nameplates.verify();
                mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen()); mc.options.guiScale().set(2); mc.resizeDisplay();
                mc.setScreen(new NewCatSkinScreen(new CatSkinScreen(new net.minecraft.client.gui.screens.TitleScreen()))); menuTicks = 0;
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
        private static Button button(Screen screen, String key) {
            return find(screen, text(key));
        }
        private static Button find(Screen screen, Component label) { return screen.children().stream().filter(e -> e instanceof Button button && button.getMessage().getString().equals(label.getString())).map(e -> (Button)e).findFirst().orElseThrow(); }
        private static void focusCustom(Screen screen) { var card = find(screen, Component.literal("Moonlit Mint")); screen.mouseClicked(card.getX() + 5, card.getY() + 25, 0); }
        private static EditBox name(Screen screen) { return screen.children().stream().filter(e -> e instanceof EditBox).map(e -> (EditBox)e).findFirst().orElseThrow(); }
        private static Component text(String key) { return Component.translatable("skins.nekogenix." + key); }
        private static void check(boolean valid, String message) { if (!valid) throw new IllegalStateException("Cat skin smoke failed: " + message); }
    }
}
