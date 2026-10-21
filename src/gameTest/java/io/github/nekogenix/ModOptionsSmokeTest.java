package io.github.nekogenix;

import io.github.nekogenix.client.ModOptionsScreen;
import io.github.nekogenix.client.skin.CatSkinScreen;
import io.github.nekogenix.config.ClientConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class ModOptionsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.modOptionsSmokeTest");
    private static int ticks;
    private static boolean started;
    private static String capture;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) throws java.io.IOException {
        if (!ENABLED) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (!(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) || mc.getOverlay() != null) return;
            started = true;
        }
        mc.getToasts().clear();
        ticks++;
        if (ticks == 10) open(new net.minecraft.client.gui.screens.TitleScreen());
        if (ticks == 14) {
            check(mc.level == null && mc.screen instanceof ModOptionsScreen, "Config opens from Mods without a world");
            check(ClientConfig.NEKO_DOUBLE_JUMP.get(), "Double jump defaults to enabled");
            check(ClientConfig.AUTOMATIC_FELINE_VISION.get(), "Feline Vision defaults to automatic");
            visionMode().onPress();
            doubleJump().onPress();
            check(!ClientConfig.NEKO_DOUBLE_JUMP.get(), "Config disables double jump immediately");
            slide("options.nekogenix.neko_voice", 0);
            slide("options.nekogenix.camera_height", 0);
            capture = "mod-config-title.png";
        }
        if (ticks == 20) {
            check(ClientConfig.CAMERA_HEIGHT.get() == 0 && ClientConfig.NEKO_VOICE_VOLUME.get() == 0, "Both config sliders update their original values");
            button("skins.nekogenix.title").onPress();
        }
        if (ticks == 24) {
            check(mc.screen instanceof CatSkinScreen && mc.level == null, "Cat skin editor works from the main menu");
            mc.screen.onClose();
        }
        if (ticks == 30) {
            check(mc.screen instanceof ModOptionsScreen, "Cat Skin returns to Config");
            mc.screen.onClose();
            check(mc.screen instanceof ModListScreen, "Done returns to Mods");
            String file = java.nio.file.Files.readString(mc.gameDirectory.toPath().resolve("config/nekogenix-client.toml"));
            check(file.contains("cameraHeight = 0") && file.contains("nekoVoiceVolume = 0.0"), "Changes persist in the existing client config");
            check(file.contains("nekoDoubleJump = false"), "The double jump preference persists alongside the original settings");
            check(file.contains("automaticFelineVision = false"), "Manual vision mode persists in the same config");
        }
        if (ticks == 40) {
            String address = "127.0.0.1:" + Integer.getInteger("nekogenix.smokePort");
            net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(), mc,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
                    new net.minecraft.client.multiplayer.ServerData("Smoke", address, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);
        }
        if (mc.player == null) return;
        int age = mc.player.tickCount;
        if (age == 40) open(new net.minecraft.client.gui.screens.PauseScreen(true));
        if (age == 45) {
            check(mc.screen instanceof ModOptionsScreen, "Config opens from Mods in game");
            check(Boolean.FALSE.equals(doubleJump().getValue()), "Config retains the disabled preference in game");
            check(Boolean.FALSE.equals(visionMode().getValue()), "Config retains manual vision mode in game");
            doubleJump().onPress();
            slide("options.nekogenix.neko_voice", .5);
            slide("options.nekogenix.camera_height", 1);
            button("skins.nekogenix.title").onPress();
        }
        if (age == 50) {
            check(mc.screen instanceof CatSkinScreen, "In game skin editor is reachable");
            mc.screen.onClose();
            check(ClientConfig.CAMERA_HEIGHT.get() == 100 && Math.abs(ClientConfig.NEKO_VOICE_VOLUME.get() - .5) < .001,
                    "Navigating to skins flushes pending slider changes");
            mc.screen.onClose();
        }
        if (age == 60) mc.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(null, mc.options));
        if (age == 65) check(widgets().stream().anyMatch(w -> w.getMessage().getString().equals("Camera Height: Human")), "Vanilla Options reflects the config camera setting");
        if (age == 70) mc.setScreen(new net.minecraft.client.gui.screens.options.SoundOptionsScreen(null, mc.options));
        if (age == 75) check(widgets().stream().anyMatch(w -> w.getMessage().getString().equals("Neko Voice: 50%")), "Vanilla sounds reflect the config voice setting");
        if (age == 76) mc.setScreen(new net.minecraft.client.gui.screens.options.controls.ControlsScreen(null, mc.options));
        if (age == 77) {
            check(Boolean.FALSE.equals(visionMode().getValue()) && visionMode().getMessage().getString().equals("Feline Vision: Manual"),
                    "Controls reflects the manual vision mode selected in Config");
            visionMode().onPress();
            check(Boolean.TRUE.equals(doubleJump().getValue()) && doubleJump().getMessage().getString().equals("Neko Double Jump: ON"),
                    "Controls reflects the preference changed in Config using the vanilla ON label");
            doubleJump().onPress();
            check(!ClientConfig.NEKO_DOUBLE_JUMP.get(), "Controls changes the same config value");
            capture = "neko-double-jump-controls.png";
        }
        if (age == 78) {
            open(new net.minecraft.client.gui.screens.PauseScreen(true));
            check(Boolean.TRUE.equals(visionMode().getValue()), "Config reflects automatic vision selected in Controls");
            check(Boolean.FALSE.equals(doubleJump().getValue()), "Config reflects the preference changed in Controls");
            doubleJump().onPress();
        }
        if (age == 80) {
            open(new net.minecraft.client.gui.screens.PauseScreen(true));
            mc.options.guiScale().set(4);
            mc.resizeDisplay();
        }
        if (age == 90) {
            var items = widgets();
            check(items.stream().filter(w -> w instanceof AbstractSliderButton).count() == 2, "Exactly two settings sliders");
            for (var item : items) check(item.getX() >= 0 && item.getY() >= 0 && item.getRight() <= mc.screen.width && item.getBottom() <= mc.screen.height, "All controls fit the maximum GUI scale");
            var done = button("gui.done");
            check(done.getWidth() == 200 && done.getX() == (mc.screen.width - 200) / 2, "Done uses vanilla width and centering");
            capture = "mod-config-ingame-scale4.png";
        }
        if (age == 100) {
            mc.screen.onClose();
            check(mc.screen instanceof ModListScreen, "Done returns to Mods in game");
            Nekogenix.LOGGER.info("Nekogenix mod options smoke passed: real Mods Config button main menu and in game shared slider values saved config skin navigation and maximum GUI scale");
            mc.stop();
        }
    }

    private static void open(Screen parent) {
        var mc = Minecraft.getInstance();
        mc.setScreen(new ModListScreen(parent));
        var list = mc.screen.children().stream().filter(ModListWidget.class::isInstance).map(ModListWidget.class::cast).findFirst().orElseThrow();
        list.children().stream().filter(entry -> entry.getInfo().getModId().equals(Nekogenix.ID)).findFirst().orElseThrow().setFocused(true);
        var config = button("fml.menu.mods.config");
        check(config.active, "Nekogenix Config button is enabled");
        config.onPress();
    }

    private static List<AbstractWidget> widgets() {
        var result = new java.util.ArrayList<AbstractWidget>();
        for (var listener : Minecraft.getInstance().screen.children()) {
            if (listener instanceof OptionsList list) {
                for (int i = 0; i < list.children().size(); i++) {
                    net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<?> entry = list.children().get(i);
                    for (var child : entry.children()) if (child instanceof AbstractWidget widget) result.add(widget);
                }
            } else if (listener instanceof AbstractWidget widget) result.add(widget);
        }
        return result;
    }

    private static Button button(String key) {
        String name = Component.translatable(key).getString();
        return widgets().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(name)).findFirst().orElseThrow();
    }

    private static net.minecraft.client.gui.components.CycleButton<?> doubleJump() {
        return cycle("options.nekogenix.double_jump");
    }

    private static net.minecraft.client.gui.components.CycleButton<?> visionMode() {
        return cycle("options.nekogenix.feline_vision");
    }

    private static net.minecraft.client.gui.components.CycleButton<?> cycle(String key) {
        String name = Component.translatable(key).getString();
        return widgets().stream().filter(net.minecraft.client.gui.components.CycleButton.class::isInstance)
                .map(widget -> (net.minecraft.client.gui.components.CycleButton<?>)widget)
                .filter(widget -> widget.getMessage().getString().startsWith(name + ":")).findFirst().orElseThrow();
    }

    private static void slide(String key, double value) {
        String name = Component.translatable(key).getString();
        var slider = widgets().stream().filter(AbstractSliderButton.class::isInstance).map(AbstractSliderButton.class::cast)
                .filter(widget -> widget.getMessage().getString().startsWith(name + ":")).findFirst().orElseThrow();
        double x = slider.getX() + 4 + (slider.getWidth() - 8) * value;
        slider.mouseClicked(x, slider.getY() + 10, 0);
        slider.mouseReleased(x, slider.getY() + 10, 0);
    }

    @SubscribeEvent
    public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
        if (!ENABLED || capture == null) return;
        var mc = Minecraft.getInstance();
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        java.nio.file.Files.createDirectories(directory);
        try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(directory.resolve(capture)); }
        capture = null;
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Mod options smoke failed: " + message);
    }
}
