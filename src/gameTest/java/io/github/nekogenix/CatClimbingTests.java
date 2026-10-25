package io.github.nekogenix;

import io.github.nekogenix.gameplay.CameraGeometry;
import io.github.nekogenix.gameplay.CameraPosture;
import io.github.nekogenix.gameplay.CameraProjectiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CatClimbingTests {
    @GameTest(template = "empty")
    public static void climbingKeepsTheSelectedCatCameraAndBody(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        CatTransformationTests.cat(player);
        var base = helper.absolutePos(new BlockPos(2, 3, 2));
        var level = helper.getLevel();
        for (var state : new BlockState[] {
                Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH),
                Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true),
                Blocks.SCAFFOLDING.defaultBlockState(),
                Blocks.TWISTING_VINES.defaultBlockState(),
                Blocks.WEEPING_VINES.defaultBlockState()}) {
            level.setBlock(base.north(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(base.above().north(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(base, state, 2);
            level.setBlock(base.above(), state, 2);
            for (int setting : new int[] {0, 40, 41, 100}) {
                for (boolean crouching : new boolean[] {false, true}) {
                    player.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
                    player.setOnGround(false);
                    float expected = (float)(CameraGeometry.selected(setting) - CameraGeometry.crouchOffset(player, setting));
                    for (double y : new double[] {.1, .6, 1.1, 0}) {
                        boolean climbing = y > 0;
                        player.setPos(base.getX() + (climbing ? .5 : 2.5), base.getY() + y, base.getZ() + .5);
                        helper.assertTrue(player.onClimbable() == climbing, "The scenario must enter and leave a real climbable: " + state);
                        float requested = (float)CameraGeometry.requested(player, setting);
                        helper.assertTrue(Math.abs(requested - expected) < .001,
                                "Climbable contact cannot lower the requested camera: " + state + " setting=" + setting);
                        CameraProjectiles.receive(player, requested, setting);
                        helper.assertTrue(Math.abs(player.getEyeHeight() - expected) < .001,
                                "Physical eyes must stay at the chosen camera height while climbing and leaving");
                        helper.assertTrue(CameraPosture.upright(player) == (setting > 40),
                                "Entering ascending and leaving cannot flip the selected cat model");
                        helper.assertTrue(player.getBbHeight() >= (setting > 40 ? crouching ? 1.44 : 1.79 : .69),
                                "The physical body must retain the selected standing or quadruped size");
                    }
                }
            }
        }
        player.setPose(Pose.STANDING);
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
        level.setBlock(base.above(), Blocks.STONE.defaultBlockState(), 2);
        CameraProjectiles.receive(player, 1.62F, 100);
        helper.assertTrue(player.onClimbable() && !CameraPosture.upright(player) && player.getBbHeight() < 1.001,
                "A real low ceiling must still lower the cat even beside climbable blocks");
        GameplayTests.remove(player);
        helper.succeed();
    }
}
