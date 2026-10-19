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
    public static void felineHurtSoundsRequireHealthLoss(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        helper.runAfterDelay(65, () -> {
            CatTransformationTests.cat(player);
            io.github.nekogenix.gameplay.FelineSounds.sync(player);
            player.getFoodData().setFoodLevel(17);
            int[] sounds = {0};
            java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity> listener = event -> {
                if (event.getEntity() == player && event.getSound().value() == net.minecraft.sounds.SoundEvents.CAT_HURT) sounds[0]++;
            };
            var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
            bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false,
                    net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity.class, listener);
            try {
                player.invulnerableTime = 0;
                player.causeFallDamage(4, 1, player.damageSources().fall());
                player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_SMALL_FALL, 1, 1);
                player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_BIG_FALL, 1, 1);
                helper.assertTrue(sounds[0] == 0, "Harmless and predicted landings do not produce hurt meows");
                player.causeFallDamage(10, 1, player.damageSources().fall());
                helper.assertTrue(player.getHealth() == 19.75F && sounds[0] == 1, "Real fall damage produces one hurt sound");
                player.causeFallDamage(10, 1, player.damageSources().fall());
                helper.assertTrue(sounds[0] == 1, "Invulnerability frames do not produce another hurt sound");
                player.invulnerableTime = 0;
                player.setInvulnerable(true);
                player.causeFallDamage(20, 1, player.damageSources().fall());
                helper.assertTrue(sounds[0] == 1, "An immune player does not produce a hurt meow");
                player.setInvulnerable(false);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 200));
                player.causeFallDamage(10, 1, player.damageSources().fall());
                helper.assertTrue(player.getHealth() == 19.75F && sounds[0] == 1, "Fully absorbed damage does not produce a hurt meow");
                player.setAbsorptionAmount(0);
                player.invulnerableTime = 0;
                player.hurt(player.damageSources().generic(), 1);
                helper.assertTrue(sounds[0] == 2, "Other real damage retains its feline hurt sound");
                helper.succeed();
            } finally {
                bus.unregister(listener);
                GameplayTests.remove(player);
            }
        });
    }

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
