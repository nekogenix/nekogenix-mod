package io.github.nekogenix;

import io.github.nekogenix.client.AbilityControls;
import io.github.nekogenix.client.FelineVision;
import io.github.nekogenix.config.ClientConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class AbilityControlsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.abilityControlsSmokeTest");

    @SubscribeEvent
    public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount == 20) {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setInvulnerable(true);
            player.serverLevel().setDayTime(6000);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
        }
        if (player.tickCount == 80) CatTransformationTests.cat(player);
        if (player.tickCount == 225) {
            check(player.getData(Content.NEKO).has(io.github.nekogenix.state.NekoState.VISION)
                    && !player.hasEffect(MobEffects.NIGHT_VISION), "Manual off remains off on the server despite the earned ability");
            Nekogenix.LOGGER.info("Nekogenix ability controls server smoke passed: authoritative manual vision state preserved");
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static boolean fadeIn, fadeOut;
        private static String capture;

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) throws java.io.IOException {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            if (age == 30) {
                var keys = java.util.Arrays.stream(mc.options.keyMappings).filter(key -> key.getCategory().equals("key.categories.nekogenix")).toList();
                check(net.minecraft.network.chat.Component.translatable(AbilityControls.FELINE_VISION.getName()).getString().equals("Toggle Feline Vision"), "The key binding clearly describes toggling vision");
                check(keys.size() == 2 && keys.contains(AbilityControls.DOUBLE_JUMP) && keys.contains(AbilityControls.FELINE_VISION), "Both real key mappings appear under Nekogenix");
                check(keys.stream().allMatch(net.minecraft.client.KeyMapping::isUnbound), "New hotkeys default to unassigned");
                var screen = new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(null, mc.options);
                mc.setScreen(screen);
                bind(screen, AbilityControls.DOUBLE_JUMP, org.lwjgl.glfw.GLFW.GLFW_KEY_G);
                bind(screen, AbilityControls.FELINE_VISION, org.lwjgl.glfw.GLFW.GLFW_KEY_H);
                var list = screen.children().stream().filter(net.minecraft.client.gui.screens.options.controls.KeyBindsList.class::isInstance)
                        .map(net.minecraft.client.gui.screens.options.controls.KeyBindsList.class::cast).findFirst().orElseThrow();
                list.setScrollAmount(Double.MAX_VALUE);
                capture = "nekogenix-keybinds.png";
            }
            if (age == 40) {
                mc.screen.onClose();
                String file = java.nio.file.Files.readString(mc.gameDirectory.toPath().resolve("options.txt"));
                check(file.contains("key_key.nekogenix.double_jump:key.keyboard.g") && file.contains("key_key.nekogenix.feline_vision:key.keyboard.h"),
                        "Native Key Binds assigns and saves both hotkeys");
            }
            if (age == 45 || age == 110) {
                ClientConfig.AUTOMATIC_FELINE_VISION.set(false);
                ClientConfig.SPEC.save();
            }
            if (age == 50 || age == 54) click(AbilityControls.DOUBLE_JUMP);
            if (age == 50 || age == 95 || age == 115 || age == 160) click(AbilityControls.FELINE_VISION);
            if (age == 52) check(!ClientConfig.NEKO_DOUBLE_JUMP.get() && !player.hasEffect(MobEffects.NIGHT_VISION), "Assigned hotkeys toggle double jump but cannot give a human vision");
            if (age == 56) check(ClientConfig.NEKO_DOUBLE_JUMP.get(), "The next hotkey press enables double jump again");
            if (age == 90) ClientConfig.AUTOMATIC_FELINE_VISION.set(true);
            if (age == 100) check(!player.hasEffect(MobEffects.NIGHT_VISION), "The vision hotkey does not override automatic daylight mode");
            float strength = FelineVision.strength(1);
            if (age >= 120 && age <= 145 && strength > .05F && strength < .95F) fadeIn = true;
            if (age >= 165 && age <= 190 && strength > .05F && strength < .95F) fadeOut = true;
            if (age == 150) check(player.hasEffect(MobEffects.NIGHT_VISION) && strength > .999F && mc.level.isDay(), "Manual vision reaches full strength in daylight");
            if (age == 200) check(strength == 0 && !player.hasEffect(MobEffects.NIGHT_VISION), "Manual off completes its fade without a residual effect");
            if (age == 210) {
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(player));
                click(AbilityControls.DOUBLE_JUMP);
                click(AbilityControls.FELINE_VISION);
            }
            if (age == 215) {
                check(ClientConfig.NEKO_DOUBLE_JUMP.get() && !player.hasEffect(MobEffects.NIGHT_VISION), "Hotkeys do not change abilities inside a GUI");
                mc.screen.onClose();
            }
            if (age == 220) check(ClientConfig.NEKO_DOUBLE_JUMP.get() && !player.hasEffect(MobEffects.NIGHT_VISION), "Closing a GUI cannot replay ignored hotkeys");
            if (age == 230) {
                check(fadeIn && fadeOut, "Manual controls preserve smooth vision transitions");
                Nekogenix.LOGGER.info("Nekogenix ability controls client smoke passed: native key assignment saved bindings jump toggles ability validation manual daylight vision smooth fades and GUI isolation");
                mc.stop();
            }
        }

        private static void bind(net.minecraft.client.gui.screens.options.controls.KeyBindsScreen screen, net.minecraft.client.KeyMapping mapping, int key) {
            screen.selectedKey = mapping;
            screen.keyPressed(key, 0, 0);
            screen.keyReleased(key, 0, 0);
        }

        private static void click(net.minecraft.client.KeyMapping mapping) { net.minecraft.client.KeyMapping.click(mapping.getKey()); }

        @SubscribeEvent
        public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var directory = mc.gameDirectory.toPath().resolve("screenshots");
            java.nio.file.Files.createDirectories(directory);
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(directory.resolve(capture)); }
            capture = null;
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Ability controls smoke failed: " + message);
    }
}
