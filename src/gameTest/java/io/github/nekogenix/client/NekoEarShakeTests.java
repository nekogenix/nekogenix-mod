package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class NekoEarShakeTests {
    @GameTest(template = "empty")
    public static void leavingWaterShakesImmediatelyDuringDrying(GameTestHelper helper) {
        var state = new NekoEarShake.Shake();
        state.tick(true, true, false, true);
        state.tick(false, false, false, true);
        helper.assertTrue(state.progress(0) == 0, "The head leaving water starts shaking while the drying effect remains");
        for (int i = 1; i < 30; i++) {
            state.tick(false, false, false, true);
            helper.assertTrue(state.progress(0) == i / 30F, "Drying must not delay or restart the water exit animation");
        }
        for (int i = 30; i < 200; i++) state.tick(false, false, false, true);
        helper.assertTrue(state.progress(0) < 0, "Water exit triggers one finite shake during drying");
        state.tick(false, false, false, false);
        helper.assertTrue(state.progress(0) < 0, "Water drying must not repeat the shake already played when the head exited");
        for (int i = 0; i < 32; i++) state.tick(false, false, false, false);
        helper.assertTrue(state.progress(0) < 0, "The water drying effect ends without a second shake");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overlappingTriggersPreserveTheRunningAnimation(GameTestHelper helper) {
        var state = new NekoEarShake.Shake();
        state.tick(true, true, true, true);
        state.tick(false, false, true, true);
        for (int i = 1; i < 30; i++) {
            if (i % 2 == 1) state.effectReappliedAfterRemoval();
            state.tick(false, false, true, i % 3 != 0);
            helper.assertTrue(state.progress(.5F) == (i + .5F) / 30F,
                    "Effect removal and reapplication cannot restart or interrupt an ongoing water shake");
        }
        state.tick(false, false, true, true);
        helper.assertTrue(state.progress(0) < 0, "Overlapping events cannot extend the animation");
        state.tick(false, false, false, false);
        helper.assertTrue(state.progress(0) == 0, "An independent event after completion starts a new shake");
        state.tick(true, true, true, true);
        helper.assertTrue(state.progress(0) < 0, "Submerging the head stops shaking underwater");
        state.tick(false, false, true, true);
        helper.assertTrue(state.progress(0) == 0, "Leaving water again starts a fresh shake even under rain");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void endingRainUsesTheExistingWaterShakeExactlyOnce(GameTestHelper helper) {
        var rain = new NekoEarShake.Shake();
        rain.tick(false, false, false, false);
        helper.assertTrue(rain.progress(0) < 0, "A dry initial state must not shake");
        for (int i = 0; i < 40; i++) rain.tick(false, false, true, true);
        helper.assertTrue(rain.progress(0) < 0, "An active rain effect must not repeatedly shake the ears");
        for (int i = 0; i < 200; i++) rain.tick(false, false, false, true);
        helper.assertTrue(rain.progress(0) < 0, "Rain drying must wait until the effect ends");
        rain.tick(false, false, false, false);
        var water = new NekoEarShake.Shake();
        water.tick(true, true, false, false);
        water.tick(false, false, false, false);
        for (int i = 0; i < 32; i++) {
            helper.assertTrue(rain.progress(.5F) == water.progress(.5F), "Rain and water must play exactly the same existing animation");
            rain.tick(false, false, false, false);
            water.tick(false, false, false, false);
        }
        helper.assertTrue(rain.progress(0) < 0, "Removing the effect triggers one finite animation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void immediateRainReapplicationAndWaterDoNotLoseTheShake(GameTestHelper helper) {
        var state = new NekoEarShake.Shake();
        state.effectReappliedAfterRemoval();
        state.tick(false, false, true, true);
        helper.assertTrue(state.progress(0) < 0, "The first effect packet must not pretend a previous effect ended");
        state.effectReappliedAfterRemoval();
        state.tick(false, false, true, true);
        helper.assertTrue(state.progress(0) == 0, "Removal and reapplication in one tick still trigger a shake");
        state.tick(false, false, true, true);
        helper.assertTrue(state.progress(0) > 0, "An unchanged active effect must not restart the animation");
        for (int i = 0; i < 32; i++) state.tick(false, false, true, true);
        helper.assertTrue(state.progress(0) < 0, "The animation ends even if the rain effect is already back");
        state.tick(true, true, false, true);
        state.tick(true, true, false, false);
        helper.assertTrue(state.progress(0) < 0, "Ears must not shake while the head is submerged");
        state.tick(false, false, false, false);
        helper.assertTrue(state.progress(0) == 0, "A submerged head defers the shake until it exits water");
        state.tick(false, false, true, true);
        state.tick(false, false, false, false);
        helper.assertTrue(state.progress(0) == 2 / 30F, "An overlapping removal must not restart the water exit animation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waterContactWithoutASubmergedHeadDoesNotShake(GameTestHelper helper) {
        var state = new NekoEarShake.Shake();
        state.tick(false, true, false, true);
        for (int i = 0; i < 200; i++) state.tick(false, false, false, true);
        state.tick(false, false, false, false);
        helper.assertTrue(state.progress(0) < 0, "Wading without wetting the head cannot cause an ear shake when drying ends");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lastWaterOrRainContactDeterminesTheDryingShake(GameTestHelper helper) {
        var waterThenRain = new NekoEarShake.Shake();
        waterThenRain.tick(true, true, false, true);
        waterThenRain.tick(false, false, true, true);
        for (int i = 0; i < 200; i++) waterThenRain.tick(false, false, false, true);
        waterThenRain.tick(false, false, false, false);
        helper.assertTrue(waterThenRain.progress(0) == 0, "Rain after leaving water still triggers its shake when drying ends");
        var rainThenWater = new NekoEarShake.Shake();
        rainThenWater.tick(false, false, true, true);
        rainThenWater.tick(true, true, false, true);
        rainThenWater.tick(false, false, false, true);
        helper.assertTrue(rainThenWater.progress(0) == 0, "Water exit shakes immediately even after prior rain");
        for (int i = 0; i < 200; i++) rainThenWater.tick(false, false, false, true);
        rainThenWater.tick(false, false, false, false);
        helper.assertTrue(rainThenWater.progress(0) < 0, "Water after rain cannot leave a stale rain trigger that duplicates its exit shake");
        helper.succeed();
    }
}
