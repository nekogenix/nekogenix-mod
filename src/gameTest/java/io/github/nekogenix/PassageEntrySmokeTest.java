package io.github.nekogenix;

import io.github.nekogenix.gameplay.CameraPosture;
import io.github.nekogenix.gameplay.CatForm;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class PassageEntrySmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.passageEntrySmokeTest");
    private static final int[] samples = new int[3];
    private static int sprinting;

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20) {
            var level = player.serverLevel(); level.setDayTime(6000);
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
            for (var pos : BlockPos.betweenClosed(-2, -61, -2, 2, -57, 30))
                level.setBlockAndUpdate(pos, (pos.getY() == -61 || (pos.getY() == -60 || pos.getY() == -58) && pos.getZ() >= 3 && pos.getZ() <= 5
                        ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        if (age == 20 || age == 245 || age == 490) {
            if (age != 20) CatTransformationTests.set(player, age == 245 ? NekoState.Phase.NEKO : NekoState.Phase.CAT, 0, 9);
            CatForm.sync(player); CameraPosture.refresh(player);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setInvulnerable(true);
            player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(20);
            player.teleportTo(player.serverLevel(), .5, -60, 2.69, 0, 0); player.setDeltaMovement(0, 0, 0);
        }
        if (age < 20) return;
        int phase = age < 245 ? 0 : age < 490 ? 1 : 2;
        if (player.getZ() > 3.4 && player.getZ() < 5.6) {
            check(player.getY() >= -59.001, "The player enters the raised opening instead of a hole at foot level");
            check(player.getBbHeight() < 1.001 && player.getEyeHeight() < .9, "The server keeps the complete body below the roof");
            check(player.level().noCollision(player, player.getBoundingBox().deflate(.0001)), "The player never moves inside the ceiling");
            if (phase < 2) check(player.hasPose(Pose.SWIMMING) && !CameraPosture.freeProne(player), "Humans and neko use native crawling restrictions");
            else {
                check(!CameraPosture.upright(player) && !player.hasPose(Pose.SWIMMING), "A full cat uses the quadruped body");
                if (player.isSprinting()) sprinting++;
            }
            samples[phase]++;
        }
        if (age == 240 || age == 480 || age == 685) {
            check(samples[phase] > 3 && player.getZ() > 6.5, "The jump input takes every form all the way through the tunnel: " + phase + " samples=" + samples[phase] + " z=" + player.getZ());
            check(player.getBbHeight() > 1.7 && player.getForcedPose() == null && !CameraPosture.passage(player), "Leaving the tunnel restores the upright body");
            if (phase == 2) {
                check(sprinting > 3, "Full cats can sprint freely inside the tunnel");
                Nekogenix.LOGGER.info("Nekogenix passage entry server smoke passed: human neko cat raised opening natural jump native crawling free cat sprint and automatic exit");
            }
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static double previousEye = Double.NaN;
        private static int cameraSamples;

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance(); var player = mc.player;
            if (player == null || mc.screen != null) return;
            int age = player.tickCount;
            boolean entry = age >= 58 && age <= 80 || age >= 278 && age <= 300 || age >= 528 && age <= 550;
            if (!entry) { previousEye = Double.NaN; return; }
            float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
            double eye = mc.gameRenderer.getMainCamera().getPosition().y - net.minecraft.util.Mth.lerp(partial, player.yo, player.getY());
            if (Double.isFinite(previousEye)) check(Math.abs(eye - previousEye) < .55, "First person passage entry never snaps the camera: " + previousEye + " -> " + eye);
            previousEye = eye; cameraSamples++;
        }

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance(); var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            if (age == 30) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
            player.setYRot(0); player.setXRot(0);
            boolean moving = age >= 50 && age < 240 || age >= 270 && age < 480 || age >= 520 && age < 685;
            mc.options.keyUp.setDown(moving && player.getZ() < 9 && !(age >= 245 && age < 490));
            mc.options.keySprint.setDown(moving && player.getZ() < 9);
            mc.options.keyJump.setDown(age == 60 || age == 280 || age == 530);
            if (age == 55 || age == 275 || age == 525) check(player.getBbHeight() > 1.7, "All three entries start upright at the Human camera height");
            if (age == 67 || age == 287 || age == 537) check(player.getBbHeight() < .71 && player.getY() > -59.05,
                    "The ordinary jump raises the body into the front opening before tucking: age=" + age + " height=" + player.getBbHeight() + " y=" + player.getY());
            if (age == 700) {
                check(cameraSamples > 30, "Camera smoothing was measured through every form's actual entry");
                Nekogenix.LOGGER.info("Nekogenix passage entry client smoke passed: raised openings natural jump all three forms and movement input without a forward key");
                mc.stop();
            }
        }

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
        public static void movement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
            if (!ENABLED) return;
            int age = event.getEntity().tickCount;
            if (age >= 270 && age < 480 && event.getEntity().getZ() < 9) {
                check(!net.minecraft.client.Minecraft.getInstance().options.keyUp.isDown(), "The alternate movement source does not press the forward key");
                event.getInput().forwardImpulse = event.getEntity().isVisuallyCrawling() ? .3F : 1;
            }
        }
    }

    private static void check(boolean valid, String message) { if (!valid) throw new IllegalStateException("Passage entry smoke failed: " + message); }
}
