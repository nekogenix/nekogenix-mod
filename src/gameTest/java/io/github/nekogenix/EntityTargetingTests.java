package io.github.nekogenix;

import io.github.nekogenix.gameplay.EntityTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class EntityTargetingTests {
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    @GameTest(template = "empty")
    public static void flowersAndPlantFamiliesDoNotHideEntities(GameTestHelper helper) {
        var player = player(helper);
        var base = player.blockPosition();
        var stand = stand(helper, base, 2.5);
        for (Block plant : new Block[] {Blocks.SHORT_GRASS, Blocks.FERN, Blocks.DEAD_BUSH, Blocks.DANDELION,
                Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.OXEYE_DAISY,
                Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.WITHER_ROSE, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
                Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS, Blocks.NETHER_WART, Blocks.SWEET_BERRY_BUSH,
                Blocks.OAK_SAPLING, Blocks.MANGROVE_PROPAGULE, Blocks.SUGAR_CANE, Blocks.BAMBOO_SAPLING,
                Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS, Blocks.NETHER_SPROUTS, Blocks.HANGING_ROOTS,
                Blocks.KELP, Blocks.SEAGRASS, Blocks.TUBE_CORAL, Blocks.SMALL_DRIPLEAF}) {
            var state = plant.defaultBlockState();
            var pos = base.south();
            helper.getLevel().setBlock(pos, state, 2);
            var center = state.getShape(helper.getLevel(), pos).bounds().getCenter();
            var origin = new Vec3(pos.getX() + center.x, player.getY() + center.y, player.getZ());
            stand.setPos(origin.x, stand.getY(), stand.getZ());
            var original = EntityTargeting.pick(player, origin, FORWARD);
            helper.assertTrue(original.getType() == HitResult.Type.BLOCK, plant + " is selected before applying entity priority");
            var result = EntityTargeting.throughPlants(player, origin, FORWARD, original);
            helper.assertTrue(result instanceof EntityHitResult hit && hit.getEntity() == stand, plant + " does not hide the entity");
        }
        stand.discard(); GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bothHalvesOfTallPlantsAndAllPlayerForms(GameTestHelper helper) {
        var player = player(helper);
        var base = player.blockPosition();
        var stand = stand(helper, base, 2.5);
        for (Block plant : new Block[] {Blocks.PEONY, Blocks.ROSE_BUSH, Blocks.LILAC, Blocks.SUNFLOWER, Blocks.TALL_GRASS, Blocks.LARGE_FERN}) {
            for (var half : DoubleBlockHalf.values()) {
                var pos = half == DoubleBlockHalf.LOWER ? base.south() : base.south().above();
                helper.getLevel().setBlock(pos, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, half), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            for (var phase : new io.github.nekogenix.state.NekoState.Phase[] {
                    io.github.nekogenix.state.NekoState.Phase.HUMAN, io.github.nekogenix.state.NekoState.Phase.NEKO,
                    io.github.nekogenix.state.NekoState.Phase.CAT}) {
                CatTransformationTests.set(player, phase, 0, 0);
                for (double height : new double[] {.4, 1.4}) {
                    var origin = player.position().add(0, height, 0);
                    var original = EntityTargeting.pick(player, origin, FORWARD);
                    helper.assertTrue(original.getType() == HitResult.Type.BLOCK, plant + " outline covers height " + height);
                    var result = EntityTargeting.throughPlants(player, origin, FORWARD, original);
                    helper.assertTrue(result instanceof EntityHitResult hit && hit.getEntity() == stand,
                            phase + " can select through " + plant + " at height " + height);
                }
            }
        }
        stand.discard(); GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void plantsRemainSelectableWithoutAnEligibleTarget(GameTestHelper helper) {
        var player = player(helper);
        var base = player.blockPosition();
        helper.getLevel().setBlock(base.south(), Blocks.PEONY.defaultBlockState(), 2);
        var origin = player.position().add(0, .4, 0);
        var original = EntityTargeting.pick(player, origin, FORWARD);
        helper.assertTrue(original.getType() == HitResult.Type.BLOCK, "A plant without a target is selectable");
        helper.assertTrue(EntityTargeting.throughPlants(player, origin, FORWARD, original) == original, "No target preserves the original block hit");
        var stand = stand(helper, base, 5);
        helper.assertTrue(EntityTargeting.throughPlants(player, origin, FORWARD, original) == original, "A distant entity does not make the plant unbreakable");
        stand.setPos(player.getX() + 1.5, player.getY(), player.getZ() + 2);
        helper.assertTrue(EntityTargeting.throughPlants(player, origin, FORWARD, original) == original, "A nearby entity outside the crosshair does not steal selection");
        stand.discard(); GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realObstaclesStillBlockTheEntity(GameTestHelper helper) {
        var player = player(helper);
        var base = player.blockPosition();
        var stand = stand(helper, base, 2.7);
        helper.getLevel().setBlock(base.south(), Blocks.PEONY.defaultBlockState(), 2);
        var origin = player.position().add(0, .4, 0);
        var original = EntityTargeting.pick(player, origin, FORWARD);
        for (Block obstacle : new Block[] {Blocks.STONE, Blocks.GLASS, Blocks.OAK_LEAVES, Blocks.OAK_FENCE, Blocks.CACTUS}) {
            helper.getLevel().setBlock(base.south(2), obstacle.defaultBlockState(), 2);
            helper.assertTrue(EntityTargeting.throughPlants(player, origin, FORWARD, original) == original,
                    obstacle + " still blocks the entity behind the plant");
        }
        helper.getLevel().setBlock(base.south(2), Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(EntityTargeting.throughPlants(player, origin, FORWARD, original) instanceof EntityHitResult,
                "Removing the obstacle exposes the entity again");
        stand.discard(); GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nearestTargetAndNativeReachArePreserved(GameTestHelper helper) {
        var player = player(helper);
        var base = player.blockPosition();
        var far = stand(helper, base, 2.7);
        var near = stand(helper, base, 1.9);
        helper.getLevel().setBlock(base.south(), Blocks.PEONY.defaultBlockState(), 2);
        var origin = player.position().add(0, .4, 0);
        var original = EntityTargeting.pick(player, origin, FORWARD);
        var result = EntityTargeting.throughPlants(player, origin, FORWARD, original);
        helper.assertTrue(result instanceof EntityHitResult hit && hit.getEntity() == near, "The nearest intersected entity wins");
        near.discard(); far.setPos(player.getX(), player.getY(), player.getZ() + 4);
        helper.assertTrue(EntityTargeting.throughPlants(player, origin, FORWARD, original) == original, "The entity range is not extended to the block range");
        far.setPos(player.getX(), player.getY(), player.getZ() + 2.7);
        helper.getLevel().setBlock(base.south(), Blocks.AIR.defaultBlockState(), 2);
        var unobstructed = EntityTargeting.pick(player, origin, FORWARD);
        helper.assertTrue(unobstructed instanceof EntityHitResult, "Native entity selection still works");
        helper.assertTrue(EntityTargeting.throughPlants(player, origin, FORWARD, unobstructed) == unobstructed, "Existing entity targets are preserved");
        far.discard(); GameplayTests.remove(player); helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var pos = helper.absolutePos(new BlockPos(3, 3, 3));
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = 0; z <= 5; z++) {
            helper.getLevel().setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return player;
    }

    private static ArmorStand stand(GameTestHelper helper, BlockPos base, double distance) {
        var stand = new ArmorStand(helper.getLevel(), base.getX() + .5, base.getY(), base.getZ() + .5 + distance);
        stand.setNoGravity(true);
        helper.getLevel().addFreshEntity(stand);
        return stand;
    }
}
