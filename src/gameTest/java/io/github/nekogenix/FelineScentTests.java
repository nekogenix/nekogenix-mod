package io.github.nekogenix;

import io.github.nekogenix.gameplay.FelineScent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class FelineScentTests {
    @GameTest(template = "empty")
    public static void focusDelayFadeAndRecovery(GameTestHelper helper) {
        var focus = new FelineScent.Focus();
        for (int i = 0; i < 150; i++) focus.tick(false, true);
        helper.assertFalse(focus.visible(), "Neko and human players cannot use feline scent");
        for (int i = 0; i < 59; i++) helper.assertFalse(focus.tick(true, true) || focus.visible(), "Nothing appears before three continuous seconds");
        focus.tick(true, false);
        for (int i = 0; i < 59; i++) focus.tick(true, true);
        helper.assertFalse(focus.visible(), "Interrupting a pending hold restarts the delay");
        helper.assertTrue(focus.tick(true, true), "The sixtieth crouching tick activates scent exactly once");
        for (int i = 0; i < 19; i++) helper.assertFalse(focus.tick(true, true), "Fading in never repeats the activation sound");
        helper.assertTrue(focus.opacity(1) == 1, "The outline reaches full brightness");
        for (int i = 0; i < 10; i++) focus.tick(true, false);
        helper.assertTrue(Math.abs(focus.opacity(1) - .5F) < .001, "Release fades out rather than clearing immediately");
        float halfway = focus.opacity(1);
        helper.assertFalse(focus.tick(true, true), "Releasing briefly does not replay the activation");
        helper.assertTrue(focus.opacity(0) == halfway && focus.opacity(1) > halfway, "Reentry continues smoothly from current strength without another delay");
        for (int i = 0; i < 20; i++) focus.tick(true, false);
        helper.assertTrue(focus.opacity(1) == 0, "The fade reaches exactly zero");
        focus.tick(true, true);
        helper.assertFalse(focus.visible(), "After fading completely a new hold must wait again");
        for (int i = 0; i < 120; i++) focus.tick(true, true);
        focus.tick(false, true);
        helper.assertFalse(focus.visible(), "Death or loss of cat form clears all focus immediately");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scentUsesTheCrouchingPoseInsteadOfTheSneakKey(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        CatTransformationTests.cat(player);
        player.setShiftKeyDown(true);
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        helper.assertFalse(FelineScent.crouching(player), "Holding sneak without crouching cannot activate scent");
        player.setShiftKeyDown(false);
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        helper.assertTrue(FelineScent.crouching(player), "An actual crouch works without a held sneak key");
        player.getAbilities().flying = true;
        helper.assertFalse(FelineScent.crouching(player), "Creative descent cannot count as crouching");
        player.getAbilities().flying = false;
        player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
        helper.assertFalse(FelineScent.crouching(player), "Crawling is distinct from crouching");
        GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void colorsAndBoxRange(GameTestHelper helper) {
        var observer = GameplayTests.player(helper);
        var other = GameplayTests.player(helper);
        helper.assertTrue(FelineScent.color(other) == FelineScent.PLAYER, "Humans are white");
        CatTransformationTests.cat(other);
        helper.assertTrue(FelineScent.color(other) == FelineScent.PLAYER, "Player cats remain white");
        for (var type : new EntityType<?>[] {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.GUARDIAN})
            helper.assertTrue(FelineScent.color((net.minecraft.world.entity.LivingEntity)type.create(helper.getLevel())) == FelineScent.HOSTILE, "Hostile creatures are red");
        for (var type : new EntityType<?>[] {EntityType.CAT, EntityType.WOLF, EntityType.BEE, EntityType.IRON_GOLEM, EntityType.LLAMA})
            helper.assertTrue(FelineScent.color((net.minecraft.world.entity.LivingEntity)type.create(helper.getLevel())) == FelineScent.NEUTRAL, "Cats and defensive creatures are yellow");
        for (var type : new EntityType<?>[] {EntityType.COW, EntityType.SHEEP, EntityType.VILLAGER, EntityType.RABBIT,
                EntityType.FROG, EntityType.AXOLOTL, EntityType.CREEPER, EntityType.PHANTOM})
            helper.assertTrue(FelineScent.color((net.minecraft.world.entity.LivingEntity)type.create(helper.getLevel())) == FelineScent.PASSIVE, "Harmless creatures are green");
        var rabbit = EntityType.RABBIT.create(helper.getLevel()); rabbit.setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.EVIL);
        helper.assertTrue(FelineScent.color(rabbit) == FelineScent.HOSTILE, "Only the killer rabbit is dangerous to players");
        observer.setPos(0, 20, 0);
        for (double x : new double[] {-30, 30}) for (double z : new double[] {-30, 30}) for (double y : new double[] {5, 35}) {
            other.setPos(x, y, z);
            helper.assertTrue(FelineScent.inRange(observer, other), "All eight box corners include thirty horizontally and fifteen vertically");
        }
        for (double[] outside : new double[][] {{30.01, 20, 0}, {-30.01, 20, 0}, {0, 20, 30.01}, {0, 20, -30.01},
                {0, 35.01, 0}, {0, 4.99, 0}}) {
            other.setPos(outside[0], outside[1], outside[2]);
            helper.assertFalse(FelineScent.inRange(observer, other), "Crossing any box face excludes the target");
        }
        other.setPos(25, 20, 25);
        helper.assertTrue(FelineScent.inRange(observer, other), "The square includes diagonal targets beyond a circular radius of thirty");
        helper.assertFalse(FelineScent.inRange(observer, observer), "The sensing player never outlines themselves");
        GameplayTests.remove(observer); GameplayTests.remove(other);
        helper.succeed();
    }
}
