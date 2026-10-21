package io.github.nekogenix;

import io.github.nekogenix.gameplay.FelineVisionControl;
import io.github.nekogenix.gameplay.TransformationHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class FelineVisionControlTests {
    @GameTest(template = "empty", timeoutTicks = 50)
    public static void manualVisionRequiresAbilityAndFadesInDaylight(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        long time = player.serverLevel().getDayTime();
        player.teleportTo(player.getX(), 150, player.getZ());
        time(player, 6000);
        FelineVisionControl.receive(player, false, true);
        TransformationHandler.tick(new PlayerTickEvent.Post(player));
        helper.assertFalse(player.hasEffect(MobEffects.NIGHT_VISION), "Manual requests cannot grant a human the ability");
        TransformationTests.setState(player, 4);
        FelineVisionControl.receive(player, false, true);
        TransformationHandler.tick(new PlayerTickEvent.Post(player));
        var vision = player.getEffect(MobEffects.NIGHT_VISION);
        helper.assertTrue(vision != null && vision.getDuration() > 200 && vision.isAmbient() && !vision.isVisible() && !vision.showIcon(),
                "Earned manual vision works in daylight and keeps the hidden fading effect");
        FelineVisionControl.receive(player, false, false);
        TransformationHandler.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 20, "Disabling starts the existing one second fade");
        time(player, time);
        helper.runAfterDelay(25, () -> {
            helper.assertFalse(player.hasEffect(MobEffects.NIGHT_VISION), "Manual vision finishes fading without being refreshed");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void manualControlsPreservePotionVisionAndSeparatePlayers(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var other = GameplayTests.player(helper);
        long time = player.serverLevel().getDayTime();
        TransformationTests.setState(player, 4);
        TransformationTests.setState(other, 4);
        player.teleportTo(player.getX(), 150, player.getZ());
        other.teleportTo(other.getX(), 150, other.getZ());
        time(player, 6000);
        FelineVisionControl.receive(player, false, true);
        helper.assertTrue(FelineVisionControl.active(player) && !FelineVisionControl.active(other), "Manual activation belongs only to the requesting player");
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600));
        TransformationHandler.tick(new PlayerTickEvent.Post(player));
        FelineVisionControl.receive(player, false, false);
        TransformationHandler.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION) && player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 600
                        && !player.getData(Content.NEKO).visionOwned,
                "Manual on and off cannot replace or remove potion night vision");
        time(player, time);
        GameplayTests.remove(player);
        GameplayTests.remove(other);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void returningToAutomaticUsesLightingAgain(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        long time = player.serverLevel().getDayTime();
        TransformationTests.setState(player, 4);
        player.teleportTo(player.getX(), 150, player.getZ());
        time(player, 6000);
        helper.assertFalse(FelineVisionControl.active(player), "Fresh sessions use automatic daylight detection");
        FelineVisionControl.receive(player, false, true);
        helper.assertTrue(FelineVisionControl.active(player), "Manual on overrides daylight");
        FelineVisionControl.receive(player, true, true);
        helper.assertFalse(FelineVisionControl.active(player), "Automatic ignores the manual enabled flag and follows daylight");
        time(player, 18000);
        helper.assertTrue(FelineVisionControl.active(player), "Automatic still activates near unlit night air");
        time(player, time);
        GameplayTests.remove(player);
        helper.succeed();
    }

    private static void time(net.minecraft.server.level.ServerPlayer player, long time) {
        player.serverLevel().setDayTime(time);
        player.serverLevel().updateSkyBrightness();
    }
}
