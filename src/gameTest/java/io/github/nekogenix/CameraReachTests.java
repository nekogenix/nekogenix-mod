package io.github.nekogenix;

import io.github.nekogenix.gameplay.CameraProjectiles;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CameraReachTests {
    @GameTest(template = "empty")
    public static void cameraReachKeepsNativeRangeAndExpiry(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var block = helper.absolutePos(new BlockPos(5, 3, 3));
        double range = player.blockInteractionRange();
        player.setPos(block.getX() - range + .01, block.getY(), block.getZ() + .5);
        helper.assertFalse(player.canInteractWithBlock(block, 0), "Without a camera sample vanilla eye distance is retained");
        CameraProjectiles.receive(player, .43F);
        helper.assertTrue(player.canInteractWithBlock(block, 0), "A low camera retains the final part of normal block reach");
        player.setPos(player.getX() - .02, player.getY(), player.getZ());
        helper.assertFalse(player.canInteractWithBlock(block, 0), "Camera reach does not extend past the native boundary");
        player.setPos(player.getX() + .02, player.getY(), player.getZ());
        double entityRange = player.entityInteractionRange();
        var entity = new AABB(player.getX() + entityRange - .01, player.getY(), player.getZ() - .2,
                player.getX() + entityRange + .4, player.getY() + .5, player.getZ() + .2);
        helper.assertTrue(player.canInteractWithEntity(entity, 0), "Entity reach uses the same low viewpoint");
        helper.assertFalse(player.canInteractWithEntity(entity.move(.02, 0, 0), 0), "Entity reach remains bounded");
        player.tickCount += 41;
        io.github.nekogenix.gameplay.CameraPosture.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        helper.assertFalse(player.canInteractWithBlock(block, 0), "An expired sample restores vanilla eye validation");
        GameplayTests.remove(player); helper.succeed();
    }
}
