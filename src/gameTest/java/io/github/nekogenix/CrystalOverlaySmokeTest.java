package io.github.nekogenix;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CrystalOverlaySmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.crystalOverlaySmokeTest");

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount == 20) {
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
            player.serverLevel().setDayTime(6000);
            player.addEffect(new MobEffectInstance(Content.CRYSTALLIZATION, 600, 0, false, false, true));
        }
        if (player.tickCount == 300) player.removeEffect(Content.CRYSTALLIZATION);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;
        private static boolean reloaded;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            mc.getToasts().clear();
            if (age == 100) capture = "crystal-overlay-standard.png";
            if (age == 120) org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 700);
            if (age == 140) capture = "crystal-overlay-wide.png";
            if (age == 150) { mc.options.guiScale().set(4); mc.resizeDisplay(); }
            if (age == 170) capture = "crystal-overlay-large-gui.png";
            if (age == 180) org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 700, 900);
            if (age == 200) capture = "crystal-overlay-portrait.png";
            if (age == 210) mc.reloadResourcePacks().thenRun(() -> reloaded = true);
            if (age == 260) {
                var sprite = mc.getGuiSprites().getSprite(Content.id("crystallization_outline"));
                if (!reloaded || sprite.contents().width() < 256
                        || !(mc.getGuiSprites().getSpriteScaling(sprite) instanceof net.minecraft.client.resources.metadata.gui.GuiSpriteScaling.NineSlice)) {
                    throw new IllegalStateException("Crystal overlay sprite or nine slice metadata failed to load");
                }
            }
            if (age == 290) capture = "crystal-overlay-reloaded.png";
            if (age == 380) {
                if (io.github.nekogenix.client.CrystallizationOverlay.strength(1) != 0) throw new IllegalStateException("Crystal overlay failed to fade away");
                capture = "crystal-overlay-cleared.png";
            }
            if (age == 390) {
                Nekogenix.LOGGER.info("Nekogenix crystal overlay smoke passed: standard wide portrait large GUI resource reload and complete fade");
                mc.stop();
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                var directory = mc.gameDirectory.toPath().resolve("screenshots");
                java.nio.file.Files.createDirectories(directory);
                image.writeToFile(directory.resolve(capture));
            }
            capture = null;
        }
    }
}
