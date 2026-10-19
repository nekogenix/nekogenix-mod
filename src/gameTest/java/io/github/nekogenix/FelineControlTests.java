package io.github.nekogenix;

import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class FelineControlTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void completedFormsStartFallDamageAtTenBlocks(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.getFoodData().setFoodLevel(17);
        helper.runAfterDelay(65, () -> {
            for (var phase : new NekoState.Phase[] {NekoState.Phase.NEKO, NekoState.Phase.CAT_TRANSFORMING, NekoState.Phase.CAT}) {
                CatTransformationTests.set(player, phase, 0, 9);
                float[] distances = {4, 9, 9.999F, 10, 11, 20};
                float[] damage = {0, 0, 0, .25F, .5F, 2.75F};
                for (int i = 0; i < distances.length; i++) {
                    player.invulnerableTime = 0;
                    player.setHealth(20);
                    player.causeFallDamage(distances[i], 1, player.damageSources().fall());
                    helper.assertTrue(player.getHealth() == 20 - damage[i], "Fall damage starts with the first soft paw step at ten blocks for " + phase + " distance " + distances[i]);
                }
            }
            CatTransformationTests.set(player, NekoState.Phase.TRANSFORMING, NekoState.DAY, 0);
            player.invulnerableTime = 0; player.setHealth(20);
            player.causeFallDamage(4, 1, player.damageSources().fall());
            helper.assertTrue(player.getHealth() == 19.75F, "Unfinished transformation keeps the earlier soft paw reduction and ordinary threshold");
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
            player.invulnerableTime = 0; player.setHealth(20);
            player.causeFallDamage(4, 1, player.damageSources().fall());
            helper.assertTrue(player.getHealth() == 19, "Human fall damage stays unchanged");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void allSyringesBlockOffhandUseAndPlacement(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.getFoodData().setFoodLevel(10);
        for (var item : new net.minecraft.world.item.Item[] {Content.SYRINGE.get(), Content.USED_SYRINGE.get(), Content.INJECTION.get(), Content.STABILIZING_INJECTION.get()}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, item.getDefaultInstance());
            player.setItemInHand(InteractionHand.OFF_HAND, Items.APPLE.getDefaultInstance());
            player.gameMode.useItem(player, player.serverLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND);
            helper.assertFalse(player.isUsingItem(), "A syringe blocks food in the second hand");
            var pos = helper.absolutePos(new BlockPos(2, 1, 2));
            player.serverLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            player.setItemInHand(InteractionHand.OFF_HAND, Items.STONE.getDefaultInstance());
            player.gameMode.useItemOn(player, player.serverLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
            helper.assertTrue(player.serverLevel().isEmptyBlock(pos.above()) && player.getOffhandItem().getCount() == 1,
                    "A syringe also blocks offhand placement without consuming the held item");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, Items.STICK.getDefaultInstance());
        player.setItemInHand(InteractionHand.OFF_HAND, Items.APPLE.getDefaultInstance());
        player.gameMode.useItem(player, player.serverLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND);
        helper.assertTrue(player.isUsingItem(), "Ordinary main hand items leave the offhand available");
        player.stopUsingItem();
        GameplayTests.remove(player);
        helper.succeed();
    }
}
