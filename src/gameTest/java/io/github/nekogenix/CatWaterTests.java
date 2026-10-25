package io.github.nekogenix;

import io.github.nekogenix.gameplay.CameraGeometry;
import io.github.nekogenix.gameplay.CameraPosture;
import io.github.nekogenix.gameplay.CameraProjectiles;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CatWaterTests {
    @GameTest(template = "empty")
    public static void standingCatsKeepTheirEyesAndBodyWhileTreadingWater(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        CatTransformationTests.cat(player);
        var water = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        player.setPos(water.getX() + .5, water.getY() + .1, water.getZ() + .5);
        player.setOnGround(false);
        player.baseTick();
        helper.assertTrue(player.isInWater(), "The regression runs in actual water");
        CameraProjectiles.receive(player, 1.62F, 100);
        helper.assertTrue(player.getEyeHeight() > 1.6 && player.getBbHeight() > 1.79,
                "Treading water must not turn an upright cat into a small body with low eyes");
        helper.assertTrue(CameraGeometry.requested(player, 100) > 1.6,
                "The client must request the same upright water viewpoint as the server");
        CameraProjectiles.receive(player, .43F, 0);
        helper.assertTrue(player.getEyeHeight() < .44 && player.getBbHeight() < .71,
                "A quadruped keeps its small water body");
        player.setSwimming(true);
        player.setPose(Pose.SWIMMING);
        CameraProjectiles.receive(player, 1.62F, 100);
        helper.assertTrue(CameraPosture.upright(player) && player.getBbHeight() < .71 && player.getEyeHeight() < .44,
                "Actual swimming still uses compact collisions and preserves the selected model build");
        player.setSwimming(false);
        player.setPose(Pose.STANDING);
        CameraProjectiles.receive(player, 1.62F, 100);
        helper.assertTrue(player.getEyeHeight() > 1.6 && player.getBbHeight() > 1.79,
                "Stopping the swimming stroke restores the upright surface body");
        GameplayTests.remove(player);
        helper.succeed();
    }
}
