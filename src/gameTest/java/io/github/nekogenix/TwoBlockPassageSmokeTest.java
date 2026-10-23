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
public final class TwoBlockPassageSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.twoBlockPassageSmokeTest");
    private static final int[] samples = new int[4];

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || !player.getGameProfile().getName().equals("NekoHands")) return;
        int age = player.tickCount;
        if (age < 20) return;
        int scenario = Math.min(3, (age - 20) / 190), start = 20 + scenario * 190, elapsed = age - start;
        boolean raised = scenario % 2 == 0;
        if (elapsed == 0) {
            var level = player.serverLevel(); level.setDayTime(6000);
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
            for (var pos : BlockPos.betweenClosed(-2, -61, -2, 2, -57, 12)) {
                boolean stone = pos.getY() == -61
                        || pos.getY() == -58 && pos.getZ() >= 0 && pos.getZ() <= 2
                        || pos.getZ() >= 3 && pos.getZ() <= 5 && (pos.getY() == (raised ? -58 : -59) || raised && pos.getY() == -60);
                level.setBlockAndUpdate(pos, (stone ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
            CatTransformationTests.set(player, scenario < 2 ? NekoState.Phase.HUMAN : NekoState.Phase.NEKO, 0, 9);
            CatForm.sync(player); CameraPosture.refresh(player);
            player.setInvulnerable(true); player.getFoodData().setFoodLevel(20);
            player.teleportTo(player.serverLevel(), .5, -60, 2.69, 0, 0); player.setDeltaMovement(0, 0, 0);
        }
        if (!raised && elapsed >= 63 && elapsed <= 80) {
            check(CameraPosture.passage(player) && player.getBbHeight() < 1.001,
                    "The server accepts crouch entry before movement: elapsed=" + elapsed + " pose=" + player.getPose());
        }
        if (elapsed > 40 && player.getZ() > 3.35 && player.getZ() < 5.6) {
            check(player.getBbHeight() < 1.001 && player.hasPose(Pose.SWIMMING), "The server accepts the crawling body: " + scenario);
            check(Math.abs(player.getY() - (raised ? -59 : -60)) < .02, "The requested entrance floor is preserved: " + scenario);
            check(player.level().noCollision(player, player.getBoundingBox().deflate(.0001)), "The body does not clip the roof");
            samples[scenario]++;
        }
        if (elapsed == 180) {
            check(samples[scenario] > 3 && player.getZ() > 6.5, "The complete passage is crossed: scenario=" + scenario + " z=" + player.getZ() + " samples=" + samples[scenario]);
            check(scenario < 2 ? player.getBbHeight() > 1.7 && player.getForcedPose() == null : CameraPosture.freeProne(player),
                    "Exit restores the selected camera posture");
            Nekogenix.LOGGER.info("Nekogenix two block passage scenario {} passed", scenario);
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static double lastEye = Double.NaN;
        private static int cameraSamples;
        private static double lastEntryZ = Double.NaN;

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
        public static void input(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
            if (!ENABLED) return;
            var player = event.getEntity();
            int scenario = Math.min(3, Math.max(0, (player.tickCount - 20) / 190));
            int elapsed = player.tickCount - (20 + scenario * 190);
            if (scenario % 2 == 1 && elapsed >= 61 && elapsed <= 63 && player.isShiftKeyDown()) {
                check(CameraPosture.passage(player) && player.getBbHeight() < 1.001,
                        "Crouching shrinks the hitbox immediately without waiting for the camera: elapsed=" + elapsed
                                + " passage=" + CameraPosture.passage(player) + " height=" + player.getBbHeight()
                                + " z=" + player.getZ() + " pose=" + player.getPose());
            }
        }

        @SubscribeEvent public static void movement(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
            if (!ENABLED || event.getEntity() != net.minecraft.client.Minecraft.getInstance().player) return;
            var player = event.getEntity();
            int scenario = Math.min(3, Math.max(0, (player.tickCount - 20) / 190));
            int elapsed = player.tickCount - (20 + scenario * 190);
            if (scenario % 2 != 1 || elapsed < 61 || elapsed > 85) { lastEntryZ = Double.NaN; return; }
            if (Double.isFinite(lastEntryZ)) check(player.getZ() > lastEntryZ + .001,
                    "Holding forward while entering must not stall or move backwards: elapsed=" + elapsed
                            + " z=" + lastEntryZ + " -> " + player.getZ());
            lastEntryZ = player.getZ();
        }

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance(); var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            int scenario = Math.min(3, Math.max(0, (age - 20) / 190)), elapsed = age - (20 + scenario * 190);
            if (elapsed == 10) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(scenario < 2 ? 100 : 40);
            player.setYRot(0); player.setXRot(0);
            mc.options.keyUp.setDown(elapsed >= 25 && elapsed < 180 && player.getZ() < 9);
            mc.options.keyShift.setDown(scenario % 2 == 1 && elapsed >= 60 && elapsed < 165);
            mc.options.keyJump.setDown(scenario % 2 == 0 && elapsed == 35);
            if (age == 790) {
                check(cameraSamples > 20, "Camera transitions were observed in every entrance");
                Nekogenix.LOGGER.info("Nekogenix two block passage client smoke passed: human and neko upper and lower entrances with two block approach ceilings");
                mc.stop();
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance(); var player = mc.player;
            if (player == null || mc.screen != null) return;
            int age = player.tickCount, scenario = Math.min(3, Math.max(0, (age - 20) / 190)), elapsed = age - (20 + scenario * 190);
            int entry = scenario % 2 == 0 ? 35 : 60;
            if (elapsed < entry || elapsed > entry + 20) { lastEye = Double.NaN; return; }
            double eye = mc.gameRenderer.getMainCamera().getPosition().y;
            if (Double.isFinite(lastEye)) check(Math.abs(eye - lastEye) < .65, "The entry camera does not jump to the floor: " + lastEye + " -> " + eye);
            lastEye = eye; cameraSamples++;
        }
    }

    private static void check(boolean value, String reason) { if (!value) throw new IllegalStateException("Two block passage smoke failed: " + reason); }
}
