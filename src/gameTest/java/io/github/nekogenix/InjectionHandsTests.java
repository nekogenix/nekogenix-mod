package io.github.nekogenix;

import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.state.NekoState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class InjectionHandsTests {
    @GameTest(template = "empty")
    public static void acceleratedStagesPersistAndReset(GameTestHelper helper) {
        NekoState state = new NekoState();
        helper.assertFalse(state.accelerate(), "Humans cannot acquire acceleration without an injection");
        state.begin();
        helper.assertTrue(state.accelerate() && !state.accelerate(), "A second dose accelerates exactly once");
        for (int i = 0; i < NekoState.SHOCK_DURATION; i++) state.tick(helper.getLevel().random);
        helper.assertTrue(state.phase() == NekoState.Phase.TRANSFORMING && state.stageRemaining() == 6000,
                "Sickness keeps its normal duration and each accelerated stage lasts five minutes");
        for (int stage = 1; stage <= 3; stage++) {
            for (int i = 0; i < 5999; i++) state.tick(helper.getLevel().random);
            helper.assertTrue(Integer.bitCount(state.serializeNBT(helper.getLevel().registryAccess()).getInt("abilities")) == stage - 1,
                    "Abilities are not granted before the full stage ends");
            state.tick(helper.getLevel().random);
            NekoState loaded = new NekoState();
            loaded.deserializeNBT(helper.getLevel().registryAccess(), state.serializeNBT(helper.getLevel().registryAccess()));
            state = loaded;
            helper.assertTrue(state.stageDuration() == 6000 && Integer.bitCount(
                    state.serializeNBT(helper.getLevel().registryAccess()).getInt("abilities")) == stage,
                    "Saved accelerated stages retain their duration and distinct abilities");
        }
        helper.assertTrue(state.phase() == NekoState.Phase.CAT_TRANSFORMING && state.lives() == 9,
                "Three five-minute stages grant the nine lives");
        for (int i = 0; i < 5999; i++) state.tick(helper.getLevel().random);
        helper.assertFalse(state.isCat(), "Cat Transformation also waits the full five minutes");
        state.tick(helper.getLevel().random);
        helper.assertTrue(state.isCat(), "The fourth five-minute period produces a cat");
        for (int i = 0; i < 9; i++) state.loseLife();
        helper.assertTrue(state.phase() == NekoState.Phase.HUMAN && state.stageDuration() == 24000,
                "Acceleration does not carry over into a later transformation");
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("phase", NekoState.Phase.TRANSFORMING.ordinal());
        legacy.putInt("elapsed", 25000);
        NekoState restored = new NekoState();
        restored.deserializeNBT(helper.getLevel().registryAccess(), legacy);
        helper.assertTrue(restored.stageDuration() == 24000 && restored.stage() == 1 && restored.elapsed() == 25000,
                "Existing saves without the acceleration flag retain their original progress");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 190)
    public static void twoCompletedDosesDealTwoHitsAndAccelerate(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.getFoodData().setFoodLevel(17);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, Items.DIAMOND_HELMET.getDefaultInstance());
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE.getDefaultInstance());
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, Items.DIAMOND_LEGGINGS.getDefaultInstance());
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, Items.DIAMOND_BOOTS.getDefaultInstance());
        player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        player.setItemInHand(InteractionHand.OFF_HAND, Content.INJECTION.toStack());
        for (int tick : new int[] {65, 85, 105, 125, 145}) {
            helper.runAfterDelay(tick, () -> InjectionHandler.input(player, true, true));
        }
        helper.runAfterDelay(76, () -> helper.assertTrue(player.getHealth() == 20,
                "Neither needle deals damage during the slow approach"));
        helper.runAfterDelay(79, () -> helper.assertTrue(player.getHealth() == 18
                && player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN,
                "Both needles deal their own contact damage even through armor without starting transformation early"));
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(player.getMainHandItem().is(Content.USED_SYRINGE)
                    && player.getOffhandItem().is(Content.USED_SYRINGE), "Both complete doses produce used syringes");
            helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.SHOCK
                    && player.getData(Content.NEKO).stageDuration() == 6000, "Two doses accelerate one transformation");
            helper.assertTrue(player.getHealth() == 18 && InjectionHandler.started(player, InteractionHand.MAIN_HAND) == -1
                    && InjectionHandler.started(player, InteractionHand.OFF_HAND) == -1, "Completion adds no damage and clears both animations");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 190)
    public static void interruptedPartnerDoesNotGrantAcceleration(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.getFoodData().setFoodLevel(17);
        var off = Content.INJECTION.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        helper.runAfterDelay(65, () -> InjectionHandler.input(player, true, true));
        helper.runAfterDelay(85, () -> InjectionHandler.input(player, true, true));
        helper.runAfterDelay(100, () -> {
            InjectionHandler.input(player, true, false);
            helper.assertTrue(InjectionHandler.returning(player, InteractionHand.OFF_HAND).started() >= 0
                    && InjectionHandler.returning(player, InteractionHand.MAIN_HAND).started() < 0,
                    "Releasing one hand reverses only that syringe");
        });
        for (int tick : new int[] {105, 125, 145}) helper.runAfterDelay(tick, () -> InjectionHandler.input(player, true, false));
        helper.runAfterDelay(117, () -> helper.assertTrue(InjectionHandler.started(player, InteractionHand.OFF_HAND) == -1
                && InjectionHandler.active(player, InteractionHand.MAIN_HAND), "One hand completes its return while the other keeps injecting"));
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(player.getMainHandItem().is(Content.USED_SYRINGE) && player.getOffhandItem() == off,
                    "Only the completed dose is consumed");
            helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.SHOCK
                    && player.getData(Content.NEKO).stageDuration() == 24000, "An interrupted partner cannot accelerate the remaining dose");
            InjectionHandler.input(player, false, true);
            helper.assertTrue(InjectionHandler.started(player, InteractionHand.OFF_HAND) == -1,
                    "A later injection cannot stack onto an existing transformation");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 190)
    public static void offhandAndStaggeredDosesCompleteIndependently(GameTestHelper helper) {
        var single = GameplayTests.player(helper);
        var paired = GameplayTests.player(helper);
        for (var player : new net.minecraft.server.level.ServerPlayer[] {single, paired}) player.getFoodData().setFoodLevel(17);
        single.setItemInHand(InteractionHand.MAIN_HAND, Items.SHIELD.getDefaultInstance());
        single.setItemInHand(InteractionHand.OFF_HAND, Content.INJECTION.toStack());
        paired.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        paired.setItemInHand(InteractionHand.OFF_HAND, Content.INJECTION.toStack());
        for (int tick : new int[] {65, 85, 105, 125, 145, 165}) {
            helper.runAfterDelay(tick, () -> {
                InjectionHandler.input(single, false, true);
                InjectionHandler.input(paired, true, tick >= 70);
            });
        }
        helper.runAfterDelay(70, () -> InjectionHandler.input(paired, true, true));
        helper.runAfterDelay(90, () -> helper.assertTrue(paired.getHealth() == 18,
                "Slightly staggered needles both hurt despite vanilla damage immunity frames"));
        helper.runAfterDelay(175, () -> {
            helper.assertTrue(single.getMainHandItem().is(Items.SHIELD) && single.getOffhandItem().is(Content.USED_SYRINGE)
                    && single.getData(Content.NEKO).stageDuration() == 24000, "An offhand injection works without consuming the main hand");
            helper.assertTrue(paired.getMainHandItem().is(Content.USED_SYRINGE) && paired.getOffhandItem().is(Content.USED_SYRINGE)
                    && paired.getData(Content.NEKO).stageDuration() == 6000, "Staggered completion preserves the paired dose until both finish");
            GameplayTests.remove(single);
            GameplayTests.remove(paired);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 190)
    public static void stabilizationUsesEitherHandButNeverBoth(GameTestHelper helper) {
        var left = GameplayTests.player(helper);
        var both = GameplayTests.player(helper);
        for (var player : new net.minecraft.server.level.ServerPlayer[] {left, both}) {
            player.getFoodData().setFoodLevel(17);
            var state = new CompoundTag();
            state.putInt("phase", NekoState.Phase.CAT_TRANSFORMING.ordinal());
            state.putInt("abilities", 7);
            state.putInt("lives", 9);
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), state);
            player.setItemInHand(InteractionHand.OFF_HAND, Content.STABILIZING_INJECTION.toStack());
        }
        both.setItemInHand(InteractionHand.MAIN_HAND, Content.STABILIZING_INJECTION.toStack());
        for (int tick : new int[] {65, 85, 105, 125, 145}) helper.runAfterDelay(tick, () -> {
            InjectionHandler.input(left, false, true);
            InjectionHandler.input(both, true, true);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(left.getOffhandItem().is(Content.USED_SYRINGE) && left.getMainHandItem().isEmpty()
                    && left.hasEffect(Content.CRYSTALLIZATION), "Left-handed stabilization applies crystallization and returns its used syringe");
            helper.assertTrue(both.getMainHandItem().is(Content.USED_SYRINGE) && both.getOffhandItem().is(Content.STABILIZING_INJECTION)
                    && both.getHealth() == 19, "Two held stabilizers still inject only one dose");
            for (var player : new net.minecraft.server.level.ServerPlayer[] {left, both}) {
                helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.NEKO
                        && player.getData(Content.NEKO).stageDuration() == 24000, "Stabilization never accelerates transformation");
                GameplayTests.remove(player);
            }
            helper.succeed();
        });
    }
}
