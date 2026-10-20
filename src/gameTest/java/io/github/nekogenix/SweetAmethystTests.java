package io.github.nekogenix;

import io.github.nekogenix.gameplay.CrystallizationEffect;
import io.github.nekogenix.gameplay.SweetnessEffect;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class SweetAmethystTests {
    @GameTest(template = "empty")
    public static void oneDrinkNourishesGraduallyForTwentyMinutes(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0);
        var bottle = Content.NEKO_STABILIZER.toStack();
        var result = bottle.finishUsingItem(helper.getLevel(), player);
        var effect = player.getEffect(Content.SWEETNESS);
        helper.assertTrue(result.is(Items.GLASS_BOTTLE) && effect != null && effect.getDuration() == SweetnessEffect.DURATION,
                "One drink returns glass and grants twenty minutes of Sweetness");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 0 && !player.hasEffect(Content.CRYSTALLIZATION), "Drinking does not instantly fill food or crystallize the player");
        for (int i = 0; i < 200; i++) effect.tick(player, () -> {});
        helper.assertTrue(player.getFoodData().getFoodLevel() == 10 && player.getFoodData().getSaturationLevel() == 10,
                "Half the refill has completed after ten seconds");
        for (int i = 0; i < 200; i++) effect.tick(player, () -> {});
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20 && player.getFoodData().getSaturationLevel() == 20,
                "Food and saturation reach the vanilla maximum after twenty seconds");
        player.getFoodData().setFoodLevel(18); player.getFoodData().setSaturation(17);
        for (int i = 0; i < 60; i++) effect.tick(player, () -> {});
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20 && player.getFoodData().getSaturationLevel() == 20,
                "Later food expenditure is gradually replenished without another drink");
        var restored = MobEffectInstance.load((CompoundTag)effect.save());
        helper.assertTrue(restored != null && restored.is(Content.SWEETNESS) && restored.getDuration() == effect.getDuration(), "Saved effects retain the remaining nourishment time");
        while (effect.getDuration() > 0) effect.tick(player, () -> {});
        player.getFoodData().setFoodLevel(5); player.getFoodData().setSaturation(0);
        helper.assertFalse(effect.tick(player, () -> {}), "Nourishment expires after its original duration");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 5, "Expired Sweetness cannot restore food");
        GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1250)
    public static void crystallizationRampsDamagesAndExpiresWithoutResidualSlowdown(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.getFoodData().setFoodLevel(7); player.getFoodData().setSaturation(0);
        double original = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.addEffect(new MobEffectInstance(Content.CRYSTALLIZATION, CrystallizationEffect.DURATION, 0, false, false, true));
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == original, "The slowdown does not snap on at application");
        helper.runAfterDelay(30, () -> {
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(speed > original * .2 && speed < original * .9, "Movement gradually slows during crystal growth");
            helper.assertTrue(player.getHealth() == 20, "Early crystal growth does not damage the player");
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - original * .05) < .0001,
                    "Full crystallization leaves five percent movement speed");
            helper.assertTrue(player.getHealth() < 20 && player.getTicksFrozen() == 0, "Crystal damage is real and does not alter powder snow freezing state");
            helper.assertFalse(player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "The penalty belongs to Crystallization without an extra Slowness effect");
        });
        helper.runAfterDelay(1170, () -> helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > original * .2,
                "Movement recovers gradually before the effect ends"));
        helper.runAfterDelay(1210, () -> {
            helper.assertFalse(player.hasEffect(Content.CRYSTALLIZATION), "Crystallization lasts exactly one minute");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - original) < .0001 && player.isAlive(),
                    "Expiry removes the modifier and the full course is survivable from full health");
            GameplayTests.remove(player); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void milkRemovesBothEffectsAndTheirModifiers(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        double original = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.addEffect(new MobEffectInstance(Content.CRYSTALLIZATION, CrystallizationEffect.DURATION));
        player.addEffect(new MobEffectInstance(Content.SWEETNESS, SweetnessEffect.DURATION));
        helper.runAfterDelay(75, () -> {
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) < original * .1, "The speed penalty is active before milk");
            Items.MILK_BUCKET.getDefaultInstance().finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(!player.hasEffect(Content.CRYSTALLIZATION) && !player.hasEffect(Content.SWEETNESS), "Milk cures both temporary effects");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - original) < .0001, "Curing cannot leave a stale speed modifier");
            player.addEffect(new MobEffectInstance(Content.CRYSTALLIZATION, CrystallizationEffect.DURATION));
            player.removeEffect(Content.CRYSTALLIZATION);
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == original, "Repeated application and removal does not accumulate modifiers");
            GameplayTests.remove(player); helper.succeed();
        });
    }
}
