package io.github.nekogenix;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.nekogenix.gameplay.InjectionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class InjectionSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.injectionSmokeTest");
    private static long returnStarted = -1;
    private static int returns;
    private static float previousStage;
    private static boolean sawIntermediate;
    private static int returnSounds;
    private static float lastReturnPitch;
    private static long lastSoundReturn = -1;

    @SubscribeEvent
    public static void sound(net.neoforged.neoforge.event.PlayLevelSoundEvent event) {
        if (!ENABLED || !event.getLevel().isClientSide || event.getSound() == null || !event.getSound().value().getLocation()
                .equals(net.minecraft.sounds.SoundEvents.HONEY_BLOCK_SLIDE.getLocation())) return;
        var player = Minecraft.getInstance().player;
        if (player == null || player.getData(Content.INJECTION_RETURN).started() < 0) return;
        long started = player.getData(Content.INJECTION_RETURN).started();
        if (started != lastSoundReturn) { lastSoundReturn = started; lastReturnPitch = 0; }
        float pitch = event.getNewPitch();
        check(lastReturnPitch == 0 || pitch > lastReturnPitch, "Return sound pitch rises as the syringe retracts");
        lastReturnPitch = pitch;
        returnSounds++;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int age = mc.player.tickCount;
        mc.options.keyUse.setDown(age >= 40 && age < 55 || age >= 90 && age < 135
                || age >= 170 && age < 245 || age >= 280 && age < 385);
        var returning = mc.player.getData(Content.INJECTION_RETURN);
        if (returning.started() >= 0) {
            var stack = mc.player.getMainHandItem();
            check(stack.is(Content.INJECTION) && !mc.player.hasEffect(Content.SHOCK), "Cancelled injection stays unused");
            float stage = ItemProperties.getProperty(stack, Content.id("injection_stage"))
                    .call(stack, mc.level, mc.player, 0);
            if (returnStarted != returning.started()) {
                returns++;
                returnStarted = returning.started();
                previousStage = stage;
                check(returning.progress() > 0 && returning.progress() < InjectionHandler.FORWARD_TICKS,
                        "Return begins at a partial injection");
                check(returns == 1 ? returning.progress() < 20 : stage > 0,
                        "Middle and late cancellations do not jump straight to a full syringe");
                Nekogenix.LOGGER.info("Nekogenix injection return {} started at progress {} with texture {}",
                        returns, returning.progress(), stage);
            }
            check(stage >= 0 && stage <= previousStage && stage <= 3, "Filled textures move backwards without empty syringe frames");
            previousStage = stage;
            if (stage > 0 && mc.level.getGameTime() > returning.started() + 2) sawIntermediate = true;
            var extension = IClientItemExtensions.of(stack);
            var pose = new PoseStack();
            boolean active = extension.applyForgeHandTransform(pose, mc.player, HumanoidArm.RIGHT, stack, 0, 0, 0);
            if (returning.remaining(mc.level.getGameTime(), 0) > 0) {
                check(active, "Hand animation remains active during the return");
                var nextPose = new PoseStack();
                extension.applyForgeHandTransform(nextPose, mc.player, HumanoidArm.RIGHT, stack, .5F, 0, 0);
                check(Math.abs(pose.last().pose().m31() - nextPose.last().pose().m31()) < .05F,
                        "Hand position changes smoothly between frames");
            }
        }
        if (age == 75 || age == 155 || age == 265) {
            check(mc.player.getMainHandItem().is(Content.INJECTION) && !mc.player.hasEffect(Content.SHOCK)
                    && mc.player.getData(Content.INJECTION_STARTED) == -1 && returning.started() == -1,
                    "Interrupted injection returns fully and clears animation state");
            check(IClientItemExtensions.of(mc.player.getMainHandItem()).getArmPose(mc.player,
                    InteractionHand.MAIN_HAND, mc.player.getMainHandItem()) == null, "Normal arm pose returns after cancellation");
        }
        if (age == 249) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "injection-return-prealpha5.png",
                mc.getMainRenderTarget(), message -> {});
        if (age == 400) {
            check(returns == 3 && sawIntermediate && returnSounds >= 6,
                    "Early middle and late cancellations animate backwards with return sounds");
            check(mc.player.getMainHandItem().is(Content.USED_SYRINGE) && mc.player.hasEffect(Content.SHOCK),
                    "Holding through completion still gives a used syringe and starts sickness");
            Nekogenix.LOGGER.info("Nekogenix injection smoke passed: three partial returns, descending filled textures, smooth hand motion and successful full injection");
            mc.stop();
        }
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new IllegalStateException("Nekogenix injection smoke failed: " + message);
    }
}
