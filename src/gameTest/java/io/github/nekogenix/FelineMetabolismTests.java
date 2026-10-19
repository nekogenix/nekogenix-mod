package io.github.nekogenix;

import io.github.nekogenix.state.NekoState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class FelineMetabolismTests {
    @GameTest(template = "empty")
    public static void allPhasesAndExhaustionSources(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var food = player.getFoodData();
        for (var phase : NekoState.Phase.values()) {
            phase(player, phase);
            boolean reduced = phase == NekoState.Phase.NEKO || phase == NekoState.Phase.CAT_TRANSFORMING || phase == NekoState.Phase.CAT;
            float factor = reduced ? .5F : 1;
            food.setExhaustion(0); player.causeFoodExhaustion(2);
            equal(helper, food.getExhaustionLevel(), 2 * factor, "Player exhaustion respects the completed third stage");
            food.setExhaustion(0); food.addExhaustion(6);
            equal(helper, food.getExhaustionLevel(), 6 * factor, "Direct FoodData costs also receive the reduction exactly once");
            food.setExhaustion(0); player.setSprinting(false); player.jumpFromGround();
            equal(helper, food.getExhaustionLevel(), .05F * factor, "Ordinary jumping uses half the exhaustion");
            food.setExhaustion(0); player.setSprinting(true); player.jumpFromGround();
            equal(helper, food.getExhaustionLevel(), .2F * factor, "Sprint jumping uses half the exhaustion");
            player.setSprinting(false); food.setExhaustion(0);
            MobEffects.HUNGER.value().applyEffectTick(player, 2);
            equal(helper, food.getExhaustionLevel(), .015F * factor, "Hunger effect exhaustion is also reduced");
        }
        food.setExhaustion(3); food.addExhaustion(-1);
        equal(helper, food.getExhaustionLevel(), 2, "Exhaustion reductions supplied by other mods are unchanged");
        var standalone = new FoodData(); standalone.addExhaustion(6);
        equal(helper, standalone.getExhaustionLevel(), 6, "Unowned FoodData keeps vanilla behavior");
        phase(player, NekoState.Phase.HUMAN); food.setExhaustion(0); food.addExhaustion(2);
        equal(helper, food.getExhaustionLevel(), 2, "Returning to human immediately restores vanilla costs");
        GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void regenerationConsumptionAndSavedState(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        for (var phase : new NekoState.Phase[] {NekoState.Phase.HUMAN, NekoState.Phase.CAT}) {
            phase(player, phase);
            float factor = phase == NekoState.Phase.CAT ? .5F : 1;
            reset(player, 20, 6); player.setHealth(18);
            for (int i = 0; i < 10; i++) player.getFoodData().tick(player);
            equal(helper, player.getHealth(), 19, "Fast regeneration keeps its healing rate");
            equal(helper, player.getFoodData().getExhaustionLevel(), 6 * factor, "Fast regeneration costs half the exhaustion");
            reset(player, 18, 0); player.setHealth(18);
            for (int i = 0; i < 80; i++) player.getFoodData().tick(player);
            equal(helper, player.getHealth(), 19, "Slow regeneration keeps its healing rate");
            equal(helper, player.getFoodData().getExhaustionLevel(), 6 * factor, "Slow regeneration costs half the exhaustion");
            reset(player, 10, 0); player.setHealth(20);
            for (int i = 0; i < 12; i++) { player.causeFoodExhaustion(1); player.getFoodData().tick(player); }
            helper.assertTrue(player.getFoodData().getFoodLevel() == (phase == NekoState.Phase.CAT ? 9 : 8), "Actual food consumption follows the reduced costs without refunds");
        }
        var saved = new CompoundTag(); player.getFoodData().addAdditionalSaveData(saved);
        player.getFoodData().readAdditionalSaveData(saved);
        player.getFoodData().setExhaustion(0); player.causeFoodExhaustion(2);
        equal(helper, player.getFoodData().getExhaustionLevel(), 1, "Loading saved food values retains the owner binding");
        var clone = GameplayTests.player(helper);
        clone.restoreFrom(player, true);
        phase(player, NekoState.Phase.HUMAN); phase(clone, NekoState.Phase.CAT);
        player.getFoodData().setExhaustion(0); clone.causeFoodExhaustion(2);
        equal(helper, clone.getFoodData().getExhaustionLevel(), 1, "Transferred food data follows the new player's phase before their first tick");
        GameplayTests.remove(clone);
        phase(player, NekoState.Phase.CAT);
        for (int i = 0; i < 9; i++) player.getData(Content.NEKO).loseLife();
        player = player.server.getPlayerList().respawn(player, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        player.getFoodData().setExhaustion(0); player.causeFoodExhaustion(2);
        equal(helper, player.getFoodData().getExhaustionLevel(), 2, "Respawning as human does not retain the cat discount");
        GameplayTests.remove(player); helper.succeed();
    }

    private static void reset(ServerPlayer player, int food, float saturation) {
        var tag = new CompoundTag(); tag.putInt("foodLevel", food); tag.putFloat("foodSaturationLevel", saturation);
        player.getFoodData().readAdditionalSaveData(tag);
    }
    private static void phase(ServerPlayer player, NekoState.Phase phase) {
        var tag = new CompoundTag(); tag.putInt("phase", phase.ordinal()); tag.putInt("lives", 9);
        tag.putInt("abilities", phase.ordinal() >= NekoState.Phase.NEKO.ordinal() ? 7 : 0);
        player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), tag);
    }
    private static void equal(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < .0001F, message + ": expected " + expected + ", got " + actual);
    }
}
