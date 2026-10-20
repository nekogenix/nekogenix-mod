package io.github.nekogenix;

import io.github.nekogenix.gameplay.CatForm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineAbilitySmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.felineAbilitySmokeTest");
    private static int jumps;

    @SubscribeEvent public static void sound(net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity event) {
        if (ENABLED && event.getEntity() instanceof ServerPlayer && event.getSound() != null
                && event.getSound().value() == net.minecraft.sounds.SoundEvents.CAT_PURREOW) jumps++;
    }

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        var level = player.serverLevel();
        if (age == 20) {
            CatTransformationTests.cat(player);
            player.setGameMode(GameType.SURVIVAL);
            player.setInvulnerable(true);
            player.getFoodData().setFoodLevel(20);
            level.setDayTime(6000);
            for (BlockPos pos : BlockPos.betweenClosed(-2, -59, -2, 2, -59, 2)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            player.teleportTo(level, .5, -60, .5, 0, 0);
        }
        if (age == 80) check(player.getPose() == Pose.CROUCHING && player.getEyeHeight() == CatForm.CROUCHING_EYE_HEIGHT,
                "The server accepts the feline crouching pose below a one block ceiling");
        if (age == 125) check(player.getPose() == Pose.STANDING, "Releasing sneak restores standing inside the passage");
        if (age == 140) player.teleportTo(level, 32.5, -60, .5, 0, 0);
        if (age == 180 || age == 310 || age == 330) level.setDayTime(18000);
        if (age == 250 || age == 320 || age == 420) level.setDayTime(6000);
        if (age == 380) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
        if (age == 430 || age == 500 || age == 560) {
            player.setGameMode(age == 560 ? GameType.CREATIVE : GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.getFoodData().setFoodLevel(age == 430 ? 6 : age == 500 ? 7 : 0);
            player.getFoodData().setSaturation(0);
            player.teleportTo(level, 32.5, -60, .5, 0, 0);
            player.setDeltaMovement(0, 0, 0);
        }
        if (age == 480) check(jumps == 0, "A hungry client cannot request an authorized air jump");
        if (age == 550) check(jumps == 1, "Seven food points allow one air jump");
        if (age == 620) {
            check(jumps == 2, "Creative mode allows an air jump with no food");
            check(!player.getData(Content.NEKO).visionOwned && player.getEffect(MobEffects.NIGHT_VISION).getDuration() > 300,
                    "Ordinary potion vision is preserved in daylight");
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            CatForm.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        }
        if (age == 640) {
            check(player.getDimensions(Pose.STANDING).height() > 1.7F, "Human pose dimensions remain vanilla");
            Nekogenix.LOGGER.info("Nekogenix feline ability server smoke passed: low ceiling crouching hunger boundary creative exception and external night vision");
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static float prior;
        private static boolean fadeIn, fadeOut, recovery;
        private static String capture;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            if (age == 30) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
            mc.options.keyShift.setDown(age >= 40 && age < 110);
            mc.options.keyJump.setDown(age == 460 || age == 468 || age == 530 || age == 538 || age == 590 || age == 598);
            if (age == 80) check(player.isCrouching() && player.getPose() == Pose.CROUCHING
                    && mc.gameRenderer.getMainCamera().getPosition().y - player.getY() < .8,
                    "The actual client crouches with its Human camera safely below the ceiling");
            if (age == 125) check(!player.isCrouching(), "The client releases crouch under the ceiling");
            if (age >= 170 && age <= 375) {
                float value = io.github.nekogenix.client.FelineVision.strength(1);
                check(Math.abs(value - prior) < .09F, "Vision brightness changes smoothly each tick");
                if (age >= 190 && age <= 205 && value > .05F && value < .95F) fadeIn = true;
                if (age >= 255 && age <= 270 && value > .05F && value < .95F) fadeOut = true;
                if (age >= 335 && value > prior && value < .95F) recovery = true;
                if (player.hasEffect(MobEffects.NIGHT_VISION)) check(Math.abs(net.minecraft.client.renderer.GameRenderer.getNightVisionScale(player, 1) - value) < .001,
                        "The real lighting renderer receives the gradual feline vision strength");
                if (age == 230) check(value > .999F, "Vision reaches full brightness");
                if (age == 290) check(value == 0 && !player.hasEffect(MobEffects.NIGHT_VISION), "Daylight finishes fading without a residual effect");
                prior = value;
            }
            if (age == 185 || age == 195 || age == 230 || age == 260 || age == 290) capture = "feline-vision-" + age + ".png";
            if (age == 405 || age == 450) check(net.minecraft.client.renderer.GameRenderer.getNightVisionScale(player, 1) == 1,
                    "Ordinary night vision keeps its vanilla strength");
            if (age == 470) check(player.getDeltaMovement().y <= 0, "A second jump press with six food cannot reverse descent");
            if (age == 540 || age == 600) check(player.getDeltaMovement().y > 0, "A fed or creative player can double jump");
            if (age == 650) {
                check(fadeIn && fadeOut && recovery, "Vision fades in out and reverses a partial fade");
                check(!player.getData(Content.CAT_FORM) && player.getDimensions(Pose.CROUCHING).height() > 1.4F, "Returning to human restores human crouching dimensions");
                Nekogenix.LOGGER.info("Nekogenix feline ability client smoke passed: crouching under a low ceiling smooth rendered vision fade recovery hungry jump rejection and creative jump");
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

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Feline ability smoke failed: " + message);
    }
}
