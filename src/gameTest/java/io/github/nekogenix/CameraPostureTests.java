package io.github.nekogenix;

import io.github.nekogenix.gameplay.CameraPosture;
import io.github.nekogenix.gameplay.CameraProjectiles;
import io.github.nekogenix.gameplay.CatForm;
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
public final class CameraPostureTests {
    @GameTest(template = "empty")
    public static void raisedCatCamerasExposeARealHitbox(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        CatTransformationTests.cat(player);
        for (float height : new float[] {.43F, .6F, .85F, .906F, .9179F, 1.1F, 1.62F}) {
            CameraProjectiles.receive(player, height, 100);
            helper.assertTrue(Math.abs(player.getEyeHeight() - height) < .001, "The cat's physical eyes follow the actual camera");
            helper.assertTrue(player.getBbHeight() >= height + .17F, "The head cannot sit outside the hittable body");
            var start = player.position().add(0, height, -3);
            helper.assertTrue(player.getBoundingBox().clip(start, start.add(0, 0, 6)).isPresent(),
                    "An enemy shot at the raised head can hit the player at " + height);
            helper.assertTrue(Math.abs(CameraProjectiles.interactionEye(player).y - player.getEyeY()) < .001,
                    "Interaction and projectile origins do not float above the cat");
            helper.assertTrue(CameraPosture.upright(player) == (height > .9061), "The cat changes body pose above the forty percent camera height");
            helper.assertTrue(Math.abs(CameraPosture.modelEye(player) - (height > .9061 ? 1.62F : .43F)) < .001,
                    "Changing the slider never produces a stretched intermediate cat model");
        }
        helper.assertTrue(Math.abs(player.getBbHeight() - 1.8) < .001, "The Human camera has a full height cat hitbox");
        CameraProjectiles.receive(player, .43F, 0);
        helper.assertTrue(Math.abs(player.getBbHeight() - .7) < .001, "Lowering the camera restores the ordinary cat size");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catPostureUsesFortyPercentAndKeepsCeilingProtection(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        CatTransformationTests.cat(player);
        for (boolean crouching : new boolean[] {false, true}) for (int setting : new int[] {0, 39, 40, 41, 42, 100}) {
            player.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
            float height = (float)io.github.nekogenix.gameplay.CameraGeometry.requested(player, setting);
            CameraProjectiles.receive(player, height, setting);
            helper.assertTrue(CameraPosture.upright(player) == (setting > 40), "The forty percent boundary also holds while crouching at " + setting);
        }
        player.setPose(Pose.STANDING);
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        CameraProjectiles.receive(player, .9179F, 41);
        helper.assertFalse(CameraPosture.upright(player), "A standing preference still lowers the body in a real low passage");
        helper.assertTrue(player.getBbHeight() <= 1.001, "The low passage cannot contain a tall hitbox");
        GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lowHumanCameraUsesTheWholeSliderAndCatSwimmingKeepsItsBuild(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        float previous = -1;
        for (int percent : new int[] {0, 10, 20, 30, 40, 47, 48, 49, 50, 75, 100}) {
            float requested = (float)io.github.nekogenix.gameplay.CameraGeometry.selected(percent);
            CameraProjectiles.receive(player, requested, percent);
            if (percent >= 48) player.setPose(Pose.STANDING);
            helper.assertTrue(Math.abs(io.github.nekogenix.gameplay.CameraGeometry.requested(player, percent) - requested) < .001,
                    "The human camera has a distinct height at " + percent + " percent");
            float actual = (float)(CameraProjectiles.interactionEye(player).y - player.getY());
            helper.assertTrue(actual > previous && Math.abs(actual - requested) < .001,
                    "Every part of the human slider changes the actual viewpoint at " + percent + ": " + actual + " vs " + requested);
            previous = actual;
        }
        CatTransformationTests.cat(player);
        CameraProjectiles.receive(player, 1.62F, 100);
        player.setSwimming(true); player.setPose(Pose.SWIMMING);
        CameraProjectiles.receive(player, .43F, 100);
        helper.assertTrue(CameraPosture.upright(player) && CameraPosture.modelEye(player) > 1.6,
                "Swimming lowers the actual viewpoint without replacing the upright cat's body with a quadruped");
        helper.assertTrue(player.getBbHeight() <= .71, "Swimming still uses a compact collision box");
        CameraProjectiles.receive(player, .43F, 0);
        helper.assertFalse(CameraPosture.upright(player), "A low cat preference also stays quadruped while swimming");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catCeilingsAndExpiredViewsCannotLeaveAHighAttackOrigin(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        CatTransformationTests.cat(player);
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        CameraProjectiles.receive(player, 1.62F, 100);
        helper.assertTrue(player.getEyeHeight() <= .821 && player.getBbHeight() <= 1.001,
                "A one block roof constrains the entire cat body and its real eyes");
        helper.assertTrue(player.level().noCollision(player, player.getBoundingBox().deflate(.0001)), "The resized body fits below the roof");
        helper.assertTrue(CameraProjectiles.interactionEye(player).y <= player.getY() + .821, "The server cannot attack from above the ceiling");
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        CameraProjectiles.receive(player, 1.62F, 100);
        player.tickCount += 41;
        CameraPosture.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
        helper.assertTrue(Math.abs(player.getEyeHeight() - CatForm.STANDING_EYE_HEIGHT) < .001,
                "Stale camera data returns the cat to its physical default");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void humanProneMovementOnlyUsesCrawlRestrictionsInLowSpaces(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        CameraProjectiles.receive(player, 1.3F, 0);
        helper.assertTrue(player.getBbHeight() >= 1.47 && player.getEyeHeight() > 1.29,
                "The humanoid body still covers the camera during a gradual prone transition");
        CameraProjectiles.receive(player, .4F, 0);
        helper.assertTrue(player.hasPose(Pose.SWIMMING) && player.getForcedPose() == Pose.SWIMMING
                && Math.abs(player.getBbHeight() - .6) < .001, "Low camera uses the actual vanilla prone pose and hitbox");
        helper.assertTrue(CameraPosture.freeProne(player), "An open space permits ordinary walking input while prone");
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        helper.assertFalse(CameraPosture.freeProne(player), "A one block passage keeps native crawling restrictions");
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(CameraPosture.freeProne(player), "Leaving the passage restores ordinary movement without changing the low camera");
        CameraPosture.prone(player, 100);
        helper.assertTrue(player.getForcedPose() == null, "Raising the setting releases the owned prone pose");
        player.setForcedPose(Pose.SITTING);
        CameraPosture.prone(player, 0);
        helper.assertTrue(player.getForcedPose() == Pose.SITTING, "Another mod's forced pose is preserved");
        player.setForcedPose(null);
        GameplayTests.remove(player);
        helper.succeed();
    }
}
