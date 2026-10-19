package io.github.nekogenix;

import io.github.nekogenix.gameplay.CatForm;
import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.gameplay.NekoBreeding;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.pneumatic.PneumaticStandBlockEntity;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CatTransformationTests {
    @GameTest(template = "empty")
    public static void catTransformationAndPermanentStabilization(GameTestHelper helper) {
        var state = new NekoState();
        state.begin();
        for (int i = 0; i < NekoState.SHOCK_DURATION + 3 * NekoState.DAY; i++) state.tick(helper.getLevel().random);
        helper.assertTrue(state.phase() == NekoState.Phase.CAT_TRANSFORMING && state.has(7)
                && state.lives() == 9 && state.stageRemaining() == NekoState.DAY, "All three abilities and nine lives precede the final twenty minutes");
        for (int i = 0; i < NekoState.DAY - 1; i++) state.tick(helper.getLevel().random);
        helper.assertFalse(state.isCat(), "Full cat form cannot arrive early");
        var finalTick = state.serializeNBT(helper.getLevel().registryAccess());
        helper.assertTrue(state.stabilize() && !state.stabilize() && !state.begin(), "Stabilization is permanent and cannot be restarted");
        var restored = new NekoState();
        restored.deserializeNBT(helper.getLevel().registryAccess(), state.serializeNBT(helper.getLevel().registryAccess()));
        for (int i = 0; i < 2 * NekoState.DAY; i++) restored.tick(helper.getLevel().random);
        helper.assertTrue(restored.phase() == NekoState.Phase.NEKO && restored.lives() == 9 && !restored.isCat(), "Saved stabilization retains neko form indefinitely");
        restored.deserializeNBT(helper.getLevel().registryAccess(), finalTick);
        restored.tick(helper.getLevel().random);
        helper.assertTrue(restored.isCat() && restored.has(7) && restored.lives() == 9 && !restored.stabilize(), "The unstabilized Cat Transformation completes into a cat");
        var old = new CompoundTag();
        old.putInt("phase", 3); old.putInt("elapsed", 72000); old.putInt("abilities", 7); old.putInt("lives", 6);
        restored.deserializeNBT(helper.getLevel().registryAccess(), old);
        for (int i = 0; i < NekoState.DAY; i++) restored.tick(helper.getLevel().random);
        helper.assertTrue(restored.phase() == NekoState.Phase.NEKO && restored.lives() == 6, "Existing neko saves stay stable and retain remaining lives");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catTransformationDeathsAndCrouching(GameTestHelper helper) {
        ServerPlayer player = GameplayTests.player(helper);
        set(player, NekoState.Phase.CAT_TRANSFORMING, 500, 9);
        helper.assertFalse(NekoBreeding.canFeed(player), "An unstabilized neko cannot enter love mode");
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        player = player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
        var state = player.getData(Content.NEKO);
        helper.assertTrue(state.phase() == NekoState.Phase.CAT_TRANSFORMING && state.elapsed() == 500 && state.lives() == 8,
                "Death during Cat Transformation spends one life without resetting the stage");
        cat(player);
        CatForm.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(Math.abs(player.getBbHeight() - .7F) < .001 && Math.abs(player.getEyeHeight() - CatForm.STANDING_EYE_HEIGHT) < .001
                && player.getData(Content.CAT_FORM), "Cat form changes the collision box eye height and synchronized appearance");
        player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
        helper.assertTrue(Math.abs(player.getEyeHeight() - CatForm.CROUCHING_EYE_HEIGHT) < .001,
                "Crouching lowers the feline viewpoint to its former height");
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        helper.assertTrue(player.getEyeHeight() > CatForm.CROUCHING_EYE_HEIGHT,
                "Standing restores the raised feline viewpoint");
        helper.assertTrue(NekoBreeding.canFeed(player), "Only full cat players can be fed for love mode");
        for (int i = 0; i < 9; i++) state.loseLife();
        TransformationHandler.refreshEffects(player);
        CatForm.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.getBbHeight() > 1.7F && !player.getData(Content.CAT_FORM) && !NekoBreeding.canFeed(player),
                "Losing the ninth life restores human size appearance and breeding eligibility");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void crystallizingInjectionOnlyStopsCatTransformation(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.getFoodData().setFoodLevel(17);
        player.setItemInHand(InteractionHand.MAIN_HAND, Content.STABILIZING_INJECTION.toStack());
        InjectionHandler.input(player, true);
        helper.assertTrue(player.getData(Content.INJECTION_STARTED) == -1, "Humans cannot consume stabilizing injection");
        set(player, NekoState.Phase.CAT_TRANSFORMING, 1000, 7);
        for (int tick : new int[] {66, 86, 106, 126, 146}) helper.runAfterDelay(tick, () -> InjectionHandler.input(player, true));
        helper.runAfterDelay(77, () -> helper.assertTrue(player.getHealth() == 20F,
                "The initial insertion preserves health until the jab"));
        helper.runAfterDelay(80, () -> helper.assertTrue(player.getHealth() == 19F,
                "Crystallizing injection damage also happens at needle contact"));
        helper.runAfterDelay(165, () -> {
            helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.NEKO && player.getData(Content.NEKO).lives() == 7
                    && player.getMainHandItem().is(Content.USED_SYRINGE) && player.getHealth() == 19F, "Stabilization retains abilities and remaining lives and returns a used syringe");
            InjectionHandler.input(player, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
            InjectionHandler.input(player, true);
            helper.assertTrue(player.getData(Content.INJECTION_STARTED) == -1, "Neko injection cannot resume a stabilized Cat Transformation");
            cat(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.STABILIZING_INJECTION.toStack());
            InjectionHandler.input(player, true);
            helper.assertTrue(player.getData(Content.INJECTION_STARTED) == -1, "Stabilizer cannot reverse a completed cat transformation");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 450)
    public static void brewingAndOneBottleFillBothSyringes(GameTestHelper helper) {
        var level = helper.getLevel();
        var brewingPos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(brewingPos, Blocks.BREWING_STAND.defaultBlockState());
        var brewing = (BrewingStandBlockEntity) level.getBlockEntity(brewingPos);
        helper.assertTrue(brewing.canPlaceItem(0, Items.HONEY_BOTTLE.getDefaultInstance()), "The brewing bottle slot accepts honey for custom recipes");
        brewing.setItem(0, Items.HONEY_BOTTLE.getDefaultInstance());
        brewing.setItem(3, Items.AMETHYST_SHARD.getDefaultInstance());
        brewing.setItem(4, Items.BLAZE_POWDER.getDefaultInstance());
        helper.runAfterDelay(425, () -> {
            helper.assertTrue(brewing.getItem(0).is(Content.NEKO_STABILIZER), "Honey and amethyst brew directly into Potion of Sweet Amethyst");
            var pos = brewingPos.east(3);
            level.setBlockAndUpdate(pos, Content.PNEUMATIC_STAND.get().defaultBlockState());
            var stand = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
            stand.setItem(0, Content.SYRINGE.toStack()); stand.setItem(2, Content.SYRINGE.toStack());
            stand.setItem(3, brewing.removeItem(0, 1)); stand.setItem(4, new ItemStack(Items.WIND_CHARGE, 2));
            for (int i = 0; i < 190; i++) PneumaticStandBlockEntity.tick(level, pos, stand.getBlockState(), stand);
            var saved = stand.saveWithFullMetadata(level.registryAccess());
            var restored = (PneumaticStandBlockEntity) BlockEntity.loadStatic(pos, stand.getBlockState(), saved, level.registryAccess());
            level.removeBlockEntity(pos); level.setBlockEntity(restored);
            for (int i = 190; i < 405; i++) PneumaticStandBlockEntity.tick(level, pos, restored.getBlockState(), restored);
            helper.assertTrue(restored.getItem(0).is(Content.STABILIZING_INJECTION) && restored.getItem(2).is(Content.STABILIZING_INJECTION)
                    && restored.getItem(3).is(Items.GLASS_BOTTLE) && restored.getItem(4).getCount() == 1,
                    "One bottle and charge fill both syringes across reload and return a glass bottle");
            helper.succeed();
        });
    }

    static void cat(ServerPlayer player) { set(player, NekoState.Phase.CAT, 0, 9); }

    static void set(ServerPlayer player, NekoState.Phase phase, int elapsed, int lives) {
        var saved = new CompoundTag();
        saved.putInt("phase", phase.ordinal()); saved.putInt("elapsed", elapsed);
        saved.putInt("abilities", 7); saved.putInt("lives", lives);
        player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), saved);
        TransformationHandler.refreshEffects(player);
    }
}
