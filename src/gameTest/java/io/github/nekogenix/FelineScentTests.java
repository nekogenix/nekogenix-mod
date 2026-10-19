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
        for (int i = 0; i < 99; i++) helper.assertFalse(focus.tick(true, true) || focus.visible(), "Nothing appears before five continuous seconds");
        focus.tick(true, false);
        for (int i = 0; i < 99; i++) focus.tick(true, true);
        helper.assertFalse(focus.visible(), "Interrupting a pending hold restarts the delay");
        helper.assertTrue(focus.tick(true, true), "The hundredth held tick activates scent exactly once");
        for (int i = 0; i < 19; i++) helper.assertFalse(focus.tick(true, true), "Fading in never repeats the activation sound");
        helper.assertTrue(focus.opacity(1) == 1, "The outline reaches full brightness");
        for (int i = 0; i < 10; i++) focus.tick(true, false);
        helper.assertTrue(Math.abs(focus.opacity(1) - .5F) < .001, "Release fades out rather than clearing immediately");
        float halfway = focus.opacity(1);
        helper.assertFalse(focus.tick(true, true), "Releasing briefly does not replay the activation");
        helper.assertTrue(focus.opacity(0) == halfway && focus.opacity(1) > halfway, "Reentry continues smoothly from current strength without five seconds");
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
    public static void colorsAndCylindricalRange(GameTestHelper helper) {
        var observer = GameplayTests.player(helper);
        var other = GameplayTests.player(helper);
        helper.assertTrue(FelineScent.color(other) == FelineScent.PLAYER, "Humans are white");
        CatTransformationTests.cat(other);
        helper.assertTrue(FelineScent.color(other) == FelineScent.PLAYER, "Player cats remain white");
        for (var type : new EntityType<?>[] {EntityType.ZOMBIE, EntityType.CREEPER, EntityType.GUARDIAN})
            helper.assertTrue(FelineScent.color((net.minecraft.world.entity.LivingEntity)type.create(helper.getLevel())) == FelineScent.HOSTILE, "Hostile creatures are red");
        for (var type : new EntityType<?>[] {EntityType.CAT, EntityType.WOLF, EntityType.BEE, EntityType.IRON_GOLEM, EntityType.LLAMA})
            helper.assertTrue(FelineScent.color((net.minecraft.world.entity.LivingEntity)type.create(helper.getLevel())) == FelineScent.NEUTRAL, "Cats and defensive creatures are yellow");
        for (var type : new EntityType<?>[] {EntityType.COW, EntityType.SHEEP, EntityType.VILLAGER, EntityType.RABBIT, EntityType.FROG, EntityType.AXOLOTL})
            helper.assertTrue(FelineScent.color((net.minecraft.world.entity.LivingEntity)type.create(helper.getLevel())) == FelineScent.PASSIVE, "Harmless creatures are green");
        var rabbit = EntityType.RABBIT.create(helper.getLevel()); rabbit.setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.EVIL);
        helper.assertTrue(FelineScent.color(rabbit) == FelineScent.HOSTILE, "Only the killer rabbit is dangerous to players");
        observer.setPos(0, 20, 0);
        other.setPos(20, 25, 0);
        helper.assertTrue(FelineScent.inRange(observer, other), "Horizontal twenty and vertical five are inclusive and independent");
        other.setPos(20.01, 20, 0);
        helper.assertFalse(FelineScent.inRange(observer, other), "More than twenty horizontally is excluded");
        other.setPos(0, 25.01, 0);
        helper.assertFalse(FelineScent.inRange(observer, other), "More than five vertically is excluded");
        other.setPos(15, 20, 15);
        helper.assertFalse(FelineScent.inRange(observer, other), "Diagonal corners outside the horizontal circle are excluded");
        helper.assertFalse(FelineScent.inRange(observer, observer), "The sensing player never outlines themselves");
        GameplayTests.remove(observer); GameplayTests.remove(other);
        helper.succeed();
    }
}
