package io.github.nekogenix;

import io.github.nekogenix.state.BreedingState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class BreedingTests {
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void wildCatRetaliationAndFelineTrust(GameTestHelper helper) {
        ServerPlayer player = GameplayTests.player(helper);
        BlockPos origin = floor(helper, 200, 20);
        player.teleportTo(origin.getX(), origin.getY(), origin.getZ());
        Cat cat = EntityType.CAT.create(helper.getLevel());
        cat.moveTo(player.getX() + 1, player.getY(), player.getZ());
        cat.setNoAi(true);
        helper.getLevel().addFreshEntity(cat);
        player.setItemInHand(InteractionHand.MAIN_HAND, Content.COTTON_SWAB.toStack(2));
        helper.runAfterDelay(65, () -> {
            cat.setNoAi(false);
            float health = player.getHealth();
            interact(player, cat);
            helper.assertTrue(player.getHealth() < health, "A wild cat bites the player attempting to sample it");
            float afterBite = player.getHealth();
            interact(player, cat);
            helper.assertTrue(player.getHealth() == afterBite && player.getMainHandItem().getCount() == 2
                    && player.getInventory().countItem(Content.SALIVA_SWAB.get()) == 0,
                    "One attempt does not cause repeated bites consume swabs or yield samples");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(cat.distanceToSqr(player) > 9, "The provoked wild cat actually flees");
            cat.discard();
            TransformationTests.setState(player, 4);
            Cat calm = EntityType.CAT.create(helper.getLevel());
            calm.moveTo(player.getX() + 2, player.getY(), player.getZ());
            helper.getLevel().addFreshEntity(calm);
            helper.runAfterDelay(5, () -> {
                for (var wrapped : calm.goalSelector.getAvailableGoals()) {
                    if (wrapped.getGoal() instanceof AvoidEntityGoal<?>) {
                        helper.assertFalse(wrapped.isRunning() || wrapped.getGoal().canUse(),
                                "An unprovoked cat does not avoid a full cat player");
                    }
                }
                player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
            });
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(calm.distanceToSqr(player) > 9, "Ordinary human avoidance remains available");
                calm.discard();
                GameplayTests.remove(player);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void catAndCatPlayerProduceOneKitten(GameTestHelper helper) {
        ServerPlayer feeder = GameplayTests.player(helper);
        ServerPlayer target = GameplayTests.player(helper);
        BlockPos origin = floor(helper, 260, 8);
        target.teleportTo(origin.getX(), origin.getY(), origin.getZ());
        feeder.teleportTo(origin.getX() + 1, origin.getY(), origin.getZ());
        CatTransformationTests.cat(target);
        target.getFoodData().setFoodLevel(20);
        Cat cat = EntityType.CAT.create(helper.getLevel());
        cat.tame(feeder);
        cat.setOrderedToSit(false);
        cat.moveTo(origin.getX() + 4, origin.getY(), origin.getZ());
        helper.getLevel().addFreshEntity(cat);
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 3));
        interact(feeder, target);
        cat.mobInteract(feeder, InteractionHand.MAIN_HAND);
        helper.assertTrue(target.getData(Content.BREEDING).inLove() && cat.isInLove(),
                "Feeding a full cat player and a healthy adult cat puts both in love");
        helper.runAfterDelay(140, () -> {
            var kittens = helper.getLevel().getEntitiesOfClass(Cat.class, target.getBoundingBox().inflate(10), Cat::isBaby);
            helper.assertTrue(kittens.size() == 1, "Cat approaches a cat player and produces exactly one kitten");
            helper.assertTrue(kittens.getFirst().isOwnedBy(feeder), "Kitten inherits the cat parent owner");
            helper.assertTrue(cat.getAge() > 0 && !cat.isInLove() && !target.getData(Content.BREEDING).canFeed(),
                    "Both parents leave love mode and receive breeding cooldowns");
            int count = feeder.getMainHandItem().getCount();
            interact(feeder, target);
            helper.assertTrue(feeder.getMainHandItem().getCount() == count, "A full player on cooldown does not waste fish");
            cat.discard();
            kittens.forEach(Cat::discard);
            GameplayTests.remove(feeder);
            GameplayTests.remove(target);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void twoCatPlayersProduceOneKitten(GameTestHelper helper) {
        ServerPlayer feeder = GameplayTests.player(helper);
        ServerPlayer first = GameplayTests.player(helper);
        ServerPlayer second = GameplayTests.player(helper);
        BlockPos origin = floor(helper, 310, 5);
        feeder.teleportTo(origin.getX(), origin.getY(), origin.getZ());
        first.teleportTo(origin.getX() + 1, origin.getY(), origin.getZ());
        second.teleportTo(origin.getX(), origin.getY(), origin.getZ() + 1);
        CatTransformationTests.cat(first);
        CatTransformationTests.cat(second);
        first.getFoodData().setFoodLevel(20);
        second.getFoodData().setFoodLevel(20);
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON, 4));
        interact(feeder, first);
        interact(feeder, second);
        helper.assertTrue(feeder.getMainHandItem().getCount() == 2, "Each full cat player accepts one fish for breeding");
        helper.runAfterDelay(120, () -> {
            var kittens = helper.getLevel().getEntitiesOfClass(Cat.class, first.getBoundingBox().inflate(8), Cat::isBaby);
            helper.assertTrue(kittens.size() == 1, "Two fed cat players produce exactly one kitten");
            helper.assertTrue(kittens.getFirst().isOwnedBy(first) || kittens.getFirst().isOwnedBy(second),
                    "The kitten belongs to one of its cat parents");
            var saved = first.getData(Content.BREEDING).serializeNBT(first.registryAccess());
            BreedingState restored = new BreedingState();
            restored.deserializeNBT(first.registryAccess(), saved);
            helper.assertFalse(restored.inLove() || restored.canFeed(), "Saving and loading preserves the breeding cooldown");
            helper.assertFalse(first.getData(Content.BREEDING).inLove() || second.getData(Content.BREEDING).inLove(),
                    "Both players leave love mode without spawning duplicate kittens");
            kittens.forEach(Cat::discard);
            GameplayTests.remove(feeder);
            GameplayTests.remove(first);
            GameplayTests.remove(second);
            helper.succeed();
        });
    }

    private static BlockPos floor(GameTestHelper helper, int y, int radius) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2)).atY(y);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, -1, radius))) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
        }
        return origin;
    }

    private static void interact(ServerPlayer player, net.minecraft.world.entity.Entity target) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target));
    }
}
