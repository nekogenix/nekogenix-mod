package io.github.nekogenix;

import io.github.nekogenix.gameplay.CameraPosture;
import io.github.nekogenix.gameplay.CameraProjectiles;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class PassageEntryTests {
    @GameTest(template = "empty")
    public static void humansNekoAndUprightCatsEnterAndLeaveLowPassages(GameTestHelper helper) {
        var roof = helper.absolutePos(new BlockPos(2, 5, 3));
        helper.getLevel().setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(roof.below(2), Blocks.STONE.defaultBlockState());
        for (var phase : new NekoState.Phase[] {NekoState.Phase.HUMAN, NekoState.Phase.NEKO, NekoState.Phase.CAT}) {
            var player = GameplayTests.player(helper);
            if (phase != NekoState.Phase.HUMAN) CatTransformationTests.set(player, phase, 0, 9);
            player.setPos(roof.getX() + .5, roof.getY() - 2, roof.getZ() - .31);
            player.setOnGround(true);
            CameraProjectiles.receive(player, 1.62F, 100);
            helper.assertTrue(player.getBbHeight() > 1.7, "The entry starts from the upright body: " + phase);
            helper.assertTrue(CameraPosture.preparePassageJump(player, new Vec3(0, 0, 1)), "A raised one block opening permits a jump attempt: " + phase);
            helper.assertFalse(CameraPosture.enterPassage(player), "The body cannot duck into a raised hole before the jump reaches it");
            helper.assertTrue(player.getBbHeight() > 1.7 && player.getForcedPose() == null, "Preparing a jump leaves the standing pose unchanged");
            player.setPos(player.position().add(0, 1.005, 0)); player.setOnGround(false);
            var velocity = new Vec3(0, .165, .1); player.setDeltaMovement(velocity);
            helper.assertTrue(CameraPosture.enterPassage(player), "The airborne body tucks when it reaches the opening: " + phase);
            helper.assertTrue(player.getDeltaMovement().equals(velocity), "Entering the passage preserves the ordinary jump momentum");
            helper.assertTrue(player.getBbHeight() < 1.001 && player.getEyeHeight() < .82,
                    "The lowered hitbox covers the camera and fits the passage: " + phase);
            helper.assertTrue(phase == NekoState.Phase.CAT ? player.hasPose(Pose.STANDING) && !CameraPosture.upright(player)
                    : player.hasPose(Pose.SWIMMING), "Cats use four paws and humanoids use native crawling: " + phase);
            CameraProjectiles.receive(player, 1.62F, 100);
            helper.assertTrue(player.getBbHeight() < 1.001, "A raised camera report cannot undo entry before the body clears the roof");
            player.setPos(roof.getX() + .5, roof.getY() - 1, roof.getZ() + .5);
            player.tickCount += 11;
            CameraPosture.prone(player, 100);
            helper.assertTrue(CameraPosture.passage(player) && !CameraPosture.freeProne(player), "The passage keeps the appropriate movement restrictions");
            helper.assertTrue(player.level().noCollision(player, player.getBoundingBox().deflate(.0001)), "The body never intersects the roof");
            player.setPos(roof.getX() + .5, roof.getY() - 1, roof.getZ() + 1.5);
            CameraPosture.prone(player, 100);
            CameraProjectiles.receive(player, 1.62F, 100);
            helper.assertTrue(!CameraPosture.passage(player) && player.getForcedPose() == null && player.getBbHeight() > 1.7,
                    "Open space restores the selected upright body: " + phase);
            GameplayTests.remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void entryRejectsWallsInvalidDirectionsAndForeignPoses(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var pos = helper.absolutePos(new BlockPos(2, 3, 3));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - .31);
        player.setOnGround(true);
        var forward = new Vec3(0, 0, 1);
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "Open ground cannot trigger crawling");
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "A passage at foot level keeps the previous movement rules");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "A solid wall cannot trigger entry");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "An opening smaller than the crawling hitbox is rejected");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState());
        for (var direction : new Vec3[] {Vec3.ZERO, new Vec3(Double.NaN, 0, 1), new Vec3(0, 0, Double.POSITIVE_INFINITY),
                new Vec3(0, 1, 1), new Vec3(0, 0, 20), new Vec3(0, 0, -1)})
            helper.assertFalse(CameraPosture.preparePassageJump(player, direction), "Malformed or unrelated movement cannot request entry");
        player.setForcedPose(Pose.SITTING);
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "Entry preserves another mod's forced pose");
        player.setForcedPose(null); player.getAbilities().flying = true;
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "Creative flight does not turn a jump into crawling");
        player.getAbilities().flying = false; player.setOnGround(false);
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "An airborne request cannot replace an ordinary jump");
        player.setOnGround(true); player.setPos(player.position().add(0, 0, -.4));
        helper.assertFalse(CameraPosture.preparePassageJump(player, forward), "The player must actually be up against the entrance");
        helper.assertFalse(CameraPosture.passage(player), "Rejected requests leave the pose unchanged");
        GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anAbortedJumpNeverLeavesThePlayerCrawling(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var pos = helper.absolutePos(new BlockPos(2, 3, 3));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.above(2), Blocks.STONE.defaultBlockState());
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - .31); player.setOnGround(true);
        var forward = new Vec3(0, 0, 1);
        helper.assertTrue(CameraPosture.preparePassageJump(player, forward), "A jump attempt starts at the raised opening");
        CameraPosture.cancelPassageJump(player);
        player.setPos(player.position().add(0, 1, 0)); player.setOnGround(false);
        helper.assertFalse(CameraPosture.enterPassage(player), "Stopping forward movement cancels the entry attempt");
        player.setPos(player.position().add(0, -1, 0)); player.setOnGround(true);
        helper.assertTrue(CameraPosture.preparePassageJump(player, forward), "A cancelled attempt does not block the next jump");
        player.tickCount += 13; player.setPos(player.position().add(0, 1, 0)); player.setOnGround(false);
        helper.assertFalse(CameraPosture.enterPassage(player) || CameraPosture.passageJumpPending(player), "An expired attempt cannot tuck the body during a later jump");
        helper.assertTrue(player.getForcedPose() == null && player.getBbHeight() > 1.7, "Failed attempts preserve the upright pose");
        GameplayTests.remove(player); helper.succeed();
    }
}
