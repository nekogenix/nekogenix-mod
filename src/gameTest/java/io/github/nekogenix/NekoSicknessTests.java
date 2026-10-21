package io.github.nekogenix;

import io.github.nekogenix.gameplay.TransformationHandler;
import java.util.ArrayList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class NekoSicknessTests {
    @GameTest(template = "empty", timeoutTicks = 220)
    public static void sicknessIncludesVanillaPoisonWithoutAnotherEffect(GameTestHelper helper) {
        var players = new ArrayList<ServerPlayer>();
        for (boolean paired : new boolean[] {false, true}) {
            var sick = GameplayTests.player(helper);
            var control = GameplayTests.player(helper);
            players.add(sick); players.add(control);
            sick.getFoodData().setFoodLevel(10); control.getFoodData().setFoodLevel(10);
            helper.runAfterDelay(65, () -> {
                var state = sick.getData(Content.NEKO);
                state.begin();
                if (paired) state.accelerate();
                TransformationHandler.refreshEffects(sick);
                control.addEffect(new MobEffectInstance(MobEffects.POISON, state.sicknessDuration()));
                helper.assertTrue(sick.getEffect(Content.SHOCK).getDuration() == (paired ? 3600 : 1200),
                        "Both dose durations include poisoning for the full sickness");
                for (int time : new int[] {40, 90, 120}) helper.runAfterDelay(time, () -> {
                    helper.assertTrue(sick.getHealth() == control.getHealth() && sick.getHealth() < 20,
                            "Actual sickness damage matches vanilla poison throughout the illness");
                    helper.assertTrue(sick.hasEffect(Content.SHOCK) && !sick.hasEffect(MobEffects.POISON),
                            "Poisoning belongs to Neko Sickness and never creates another status entry");
                });
            });
        }
        helper.runAfterDelay(195, () -> { players.forEach(GameplayTests::remove); helper.succeed(); });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void poisoningStopsExactlyWithSickness(GameTestHelper helper) {
        var players = new ArrayList<ServerPlayer>();
        for (boolean paired : new boolean[] {false, true}) {
            var player = GameplayTests.player(helper);
            players.add(player);
            player.getFoodData().setFoodLevel(10);
            helper.runAfterDelay(65, () -> {
                var state = player.getData(Content.NEKO);
                state.begin();
                if (paired) state.accelerate();
                for (int i = 0; i < state.sicknessDuration() - 50; i++) state.tick(helper.getLevel().random);
                TransformationHandler.refreshEffects(player);
                helper.runAfterDelay(60, () -> {
                    helper.assertFalse(player.hasEffect(Content.SHOCK) || player.hasEffect(MobEffects.POISON),
                            "No hidden poison survives the end of either sickness duration");
                    float health = player.getHealth();
                    helper.assertTrue(health < 20, "The final seconds still poison the player");
                    helper.runAfterDelay(25, () -> {
                        helper.assertTrue(player.getHealth() == health, "Sickness cannot keep damaging the player after it ends");
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                    });
                });
                helper.runAfterDelay(115, () -> helper.assertTrue(player.hasEffect(MobEffects.POISON),
                        "Independent vanilla poison keeps its own lifetime after sickness"));
            });
        }
        helper.runAfterDelay(190, () -> { players.forEach(GameplayTests::remove); helper.succeed(); });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void milkCannotSeparatePoisoningFromSickness(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        helper.runAfterDelay(65, () -> {
            player.getData(Content.NEKO).begin();
            TransformationHandler.refreshEffects(player);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
            var milk = Items.MILK_BUCKET.getDefaultInstance();
            milk.finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(player.hasEffect(Content.SHOCK) && !player.hasEffect(MobEffects.POISON),
                    "Milk cures independent poison while the sickness retains its built in symptoms");
            player.setHealth(2);
            player.invulnerableTime = 0;
            Content.SHOCK.value().applyEffectTick(player, 0);
            helper.assertTrue(player.getHealth() == 1, "Sickness deals ordinary poison damage");
            player.invulnerableTime = 0;
            Content.SHOCK.value().applyEffectTick(player, 0);
            helper.assertTrue(player.getHealth() == 1 && player.isAlive(), "Poisoning cannot kill the player by itself");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }
}
