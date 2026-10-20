package io.github.nekogenix;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineScentSmokeTest {
    private static final boolean TIGHT = Boolean.getBoolean("nekogenix.scentTightSpaceSmokeTest");
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.scentSmokeTest") || TIGHT;
    private static boolean scene;

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player.tickCount != 20) return;
        CatTransformationTests.cat(player);
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(true);
        var level = player.serverLevel();
        level.setDayTime(6000);
        if (!scene) {
            scene = true;
            for (BlockPos pos : BlockPos.betweenClosed(118, -61, 114, 140, -61, 140)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            for (BlockPos pos : BlockPos.betweenClosed(118, -60, 128, 140, -54, 128)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            if (TIGHT) for (BlockPos pos : BlockPos.betweenClosed(126, -59, 132, 131, -59, 138)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            EntityType<?>[] types = {EntityType.HUSK, EntityType.CAT, EntityType.WOLF, EntityType.COW};
            double[] xs = {123.5, 126, 129, 132};
            for (int i = 0; i < types.length; i++) {
                var mob = (Mob)types[i].create(level); mob.setNoAi(true); mob.setPersistenceRequired();
                mob.setPos(xs[i], -60, 125); level.addFreshEntity(mob);
            }
            for (double[] pos : new double[][] {{128.5, -44.9, 124}, {128.5, -60, 105}}) {
                var cow = EntityType.COW.create(level); cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(pos[0], pos[1], pos[2]); level.addFreshEntity(cow);
            }
        }
        boolean control = player.getGameProfile().getName().equals("ScentObserver");
        player.teleportTo(level, control ? 135 : 128.5, -60, control ? 125 : 136, control ? 0 : 180, 0);
        Nekogenix.LOGGER.info("Nekogenix scent scene ready for {}", player.getGameProfile().getName());
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;
        private static boolean reloaded;
        private static int sniffs;

        @SubscribeEvent public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            if (!TIGHT || event.getSound() == null || !event.getSound().getLocation().equals(net.minecraft.sounds.SoundEvents.FOX_SNIFF.getLocation())) return;
            int age = net.minecraft.client.Minecraft.getInstance().player.tickCount;
            check(Math.abs(age - (sniffs == 0 ? 140 : 360)) <= 3, "Scent still requires five seconds of sneaking in a low passage");
            sniffs++;
        }
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            mc.getToasts().clear();
            int age = mc.player.tickCount;
            boolean control = mc.getUser().getName().equals("ScentObserver");
            if (control) {
                if (age == 500) capture = "scent-other-client.png";
                if (age == 650) { Nekogenix.LOGGER.info("Nekogenix scent observer smoke passed: no crouching and no shared glowing effect"); mc.stop(); }
            } else {
                if (TIGHT && age == 35) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
                if (age == 40 || age == 215 || age == 260) mc.options.keyShift.setDown(true);
                if (TIGHT && age == 80) {
                    check(mc.player.isShiftKeyDown() && mc.player.isCrouching(), "A cat can crouch even under a low ceiling");
                    check(mc.gameRenderer.getMainCamera().getPosition().y - mc.player.getY() < .8, "The selected Human camera is lowered by the roof");
                }
                if (TIGHT && age == 125) check(sniffs == 0, "The ability cannot activate before its delay");
                if (TIGHT && age == 190) check(sniffs == 1, "Scent activates while the cat crouches under a low ceiling");
                if (TIGHT && age == 228) check(sniffs == 1, "Resuming during fade does not repeat the activation delay or sound");
                if (TIGHT && age == 365) check(sniffs == 2, "After fading out the next activation requires a fresh delay");
                if (age == 125) capture = "scent-before-delay.png";
                if (age == 145) capture = "scent-fading-in.png";
                if (age == 190) capture = "scent-full.png";
                if (age == 205 || age == 240 || age == 380) mc.options.keyShift.setDown(false);
                if (age == 213) capture = "scent-fading-out.png";
                if (age == 228) capture = "scent-resumed.png";
                if (age == 265) capture = "scent-new-delay.png";
                if (age == 365) {
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1000, 700);
                    capture = "scent-resized.png";
                }
                if (age == 370) mc.reloadResourcePacks().thenRun(() -> reloaded = true);
                if (age >= 410 && reloaded) {
                    check(mc.player.getData(Content.CAT_FORM), "The local observer remains a full cat");
                    Nekogenix.LOGGER.info("Nekogenix scent client smoke passed: timed crouching fades recovery resize and resource reload"); mc.stop();
                    if (TIGHT) Nekogenix.LOGGER.info("Nekogenix tight scent smoke passed: native crouching pose lowered Human camera five second activation fade and recovery");
                }
            }
            if (age > 30) for (var entity : mc.level.entitiesForRendering()) {
                check(!entity.isCurrentlyGlowing(), "Scent never gives another entity the glowing flag");
                check(entity.getTeam() == null, "Scent never adds scoreboard teams");
            }
        }
        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                java.nio.file.Files.createDirectories(mc.gameDirectory.toPath().resolve("screenshots"));
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(capture));
            }
            capture = null;
        }
        private static void check(boolean valid, String message) { if (!valid) throw new IllegalStateException("Feline scent smoke failed: " + message); }
    }
}
