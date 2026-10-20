package io.github.nekogenix;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatCrouchCameraSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catCrouchCameraSmokeTest");
    private static final int[] SETTINGS = {0, 100, 100, 50, 0};
    private static final double[] STANDING = {.43, 1.62, .76, .76, .43};
    private static final double[] CROUCHING = {.35, 1.27, .41, .545, .35};

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        var level = player.serverLevel();
        if (age == 20) {
            CatTransformationTests.cat(player);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(20);
            level.setDayTime(6000);
            for (BlockPos pos : BlockPos.betweenClosed(4, -59, -2, 10, -59, 2)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            for (BlockPos pos : BlockPos.betweenClosed(-3, -60, 4, 13, -57, 4)) level.setBlockAndUpdate(pos,
                    ((pos.getX() + pos.getY()) % 2 == 0 ? Blocks.SMOOTH_STONE : Blocks.DEEPSLATE).defaultBlockState());
            player.teleportTo(level, .5, -60, .5, 0, 0);
        }
        if (age == 240) player.teleportTo(level, 6.5, -60, .5, 0, 0);
        for (int i = 0; i < SETTINGS.length; i++) if (age == i * 100 + 95) {
            check(player.getPose() == Pose.CROUCHING, "Server crouching matches the view for scenario " + i);
            double height = io.github.nekogenix.gameplay.CameraProjectiles.interactionEye(player).y - player.getY();
            check(Math.abs(height - CROUCHING[i]) < .025, "Interaction origin follows the crouched camera for scenario " + i);
        }
        if (age == 540) {
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            io.github.nekogenix.gameplay.CatForm.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            player.teleportTo(level, .5, -60, .5, 0, 0);
        }
        if (age == 640) Nekogenix.LOGGER.info("Nekogenix cat crouch camera server smoke passed: synchronized cat poses under a low roof and human restoration");
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static final boolean[] SMOOTH = new boolean[SETTINGS.length];
        private static String capture;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            mc.getToasts().clear();
            for (int i = 0; i < SETTINGS.length; i++) {
                int time = age - (i * 100 + 20);
                if (time == 10) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(SETTINGS[i]);
                if (time == 40 || time == 95) check(Math.abs(height() - STANDING[i]) < .015, "Standing camera settles for scenario " + i + " at " + age + ": " + height());
                if (time == 45) capture = "cat-crouch-camera-" + i + "-standing.png";
                if (time == 50) mc.options.keyShift.setDown(true);
                if (time == 75) {
                    check(mc.player.isCrouching() && Math.abs(height() - CROUCHING[i]) < .015 && SMOOTH[i],
                            "Visible crouching moves the rendered camera smoothly for scenario " + i + ": " + height());
                    capture = "cat-crouch-camera-" + i + "-crouching.png";
                }
                if (time == 80) mc.options.keyShift.setDown(false);
            }
            if (age == 550) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
            if (age == 580) mc.options.keyShift.setDown(true);
            if (age == 610) check(!mc.player.getData(Content.CAT_FORM) && Math.abs(height() - 1.27) < .015, "Human crouching keeps its normal height");
            if (age == 620) mc.options.keyShift.setDown(false);
            if (age == 650) {
                check(Math.abs(height() - 1.62) < .015, "Human standing height restores normally");
                Nekogenix.LOGGER.info("Nekogenix cat crouch camera client smoke passed: visible smooth crouching at Cat Human and custom heights under a low ceiling and normal human camera");
                mc.stop();
            }
        }

        private static double height() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc.gameRenderer.getMainCamera().getPosition().y - mc.player.getY();
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            for (int i = 0; i < SETTINGS.length; i++) {
                int time = mc.player.tickCount - (i * 100 + 20);
                if (time > 50 && time < 75 && height() > CROUCHING[i] + .005 && height() < STANDING[i] - .005) SMOOTH[i] = true;
            }
            if (capture == null) return;
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                var directory = mc.gameDirectory.toPath().resolve("screenshots");
                java.nio.file.Files.createDirectories(directory);
                image.writeToFile(directory.resolve(capture));
            }
            capture = null;
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Cat crouch camera smoke failed: " + message);
    }
}
