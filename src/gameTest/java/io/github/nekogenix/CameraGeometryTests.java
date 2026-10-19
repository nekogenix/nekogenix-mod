package io.github.nekogenix;

import io.github.nekogenix.gameplay.CameraGeometry;
import io.github.nekogenix.gameplay.CameraProjectiles;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CameraGeometryTests {
    @GameTest(template = "empty")
    public static void floorStepsWallsAndRealCeilings(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var pos = helper.absolutePos(new BlockPos(3, 3, 3));
        for (int x = -1; x <= 2; x++) for (int z = -1; z <= 1; z++) {
            for (int y = -1; y <= 3; y++) helper.getLevel().setBlockAndUpdate(pos.offset(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        helper.assertTrue(Double.isInfinite(CameraGeometry.ceiling(player, player.position(), 1.62)), "Ordinary floor is not a ceiling");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.TALL_GRASS.defaultBlockState());
        helper.assertTrue(Double.isInfinite(CameraGeometry.ceiling(player, player.position(), 1.62)), "Foliage does not lower the camera");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE_SLAB.defaultBlockState());
        player.setPos(player.getX(), pos.getY() + .5, player.getZ());
        helper.assertTrue(Double.isInfinite(CameraGeometry.ceiling(player, player.position().add(0, -.4, 0), 1.62)), "A step below the real feet is ignored during interpolated movement");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        player.setPos(player.getX(), pos.getY(), player.getZ());
        helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.east().above(), Blocks.STONE.defaultBlockState());
        var reachable = CameraGeometry.reachableMovement(player, player.position(), new Vec3(2, 0, 0));
        helper.assertTrue(reachable.x < .25 && reachable.x >= 0, "Forecast stops at an impassable wall");
        helper.assertTrue(Double.isInfinite(CameraGeometry.ceiling(player, player.position().add(reachable), 1.62)), "A wall is not mistaken for a reachable tunnel");
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(Math.abs(CameraGeometry.ceiling(player, player.position(), 1.62) - .92) < .0001, "A real one block ceiling limits camera height");
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.assertTrue(Math.abs(CameraGeometry.ceiling(player, player.position(), 1.62) - .42) < .0001, "A real half block ceiling is protected");
        GameplayTests.remove(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crouchingTransitionsRemainGradual(GameTestHelper helper) {
        double standing = 1.025;
        double crouching = .81;
        double first = CameraGeometry.approach(standing, crouching, 1.0 / 60);
        helper.assertTrue(first < standing && first > crouching + .1, "A custom height does not snap to the crouched target");
        double height = first;
        for (int i = 0; i < 60; i++) {
            double next = CameraGeometry.approach(height, crouching, 1.0 / 60);
            helper.assertTrue(next <= height && next >= crouching, "Crouching approaches without overshoot");
            height = next;
        }
        helper.assertTrue(Math.abs(height - crouching) < .001, "The crouched target is eventually reached");
        double rising = CameraGeometry.approach(crouching, standing, 1.0 / 60);
        helper.assertTrue(rising > crouching && rising < standing - .1, "Standing up is also gradual");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileOriginsValidationAndLifetime(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var pos = helper.absolutePos(new BlockPos(3, 3, 3));
        for (int y = 0; y <= 3; y++) helper.getLevel().setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        CatTransformationTests.cat(player); player.refreshDimensions();
        CameraProjectiles.receive(player, 1.62F);
        var arrow = new Arrow(helper.getLevel(), player, Items.ARROW.getDefaultInstance(), null);
        var velocity = new Vec3(.2, .4, 2); arrow.setDeltaMovement(velocity);
        helper.getLevel().addFreshEntity(arrow);
        helper.assertTrue(Math.abs(arrow.getY() - player.getY() - 1.52) < .001 && arrow.getDeltaMovement().equals(velocity), "A cat's arrow starts at the selected human camera with its ballistics unchanged");
        arrow.discard();
        CameraProjectiles.receive(player, .8F); CameraProjectiles.receive(player, Float.NaN);
        var snowball = new Snowball(helper.getLevel(), player); helper.getLevel().addFreshEntity(snowball);
        helper.assertTrue(Math.abs(snowball.getY() - player.getY() - .7) < .001, "A non finite packet cannot replace the last valid height"); snowball.discard();
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        CameraProjectiles.receive(player, 1000);
        var trident = new ThrownTrident(helper.getLevel(), player, Items.TRIDENT.getDefaultInstance()); helper.getLevel().addFreshEntity(trident);
        helper.assertTrue(Math.abs(trident.getY() - player.getY() - .82) < .001, "The server caps exaggerated input at a real ceiling"); trident.discard();
        helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        int age = player.tickCount; player.tickCount += 41;
        arrow = new Arrow(helper.getLevel(), player, Items.ARROW.getDefaultInstance(), null); double original = arrow.getY(); helper.getLevel().addFreshEntity(arrow);
        helper.assertTrue(arrow.getY() == original, "An expired camera sample cannot move a projectile"); arrow.discard(); player.tickCount = age;
        CameraProjectiles.receive(player, 1.62F);
        arrow = new Arrow(helper.getLevel(), player, Items.ARROW.getDefaultInstance(), null); original = arrow.getY();
        CameraProjectiles.join(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(arrow, helper.getLevel(), true));
        helper.assertTrue(arrow.getY() == original, "Loading an existing projectile does not relocate it");
        arrow.setPos(arrow.position().add(5, 0, 0)); original = arrow.getY(); helper.getLevel().addFreshEntity(arrow);
        helper.assertTrue(arrow.getY() == original, "Remote projectiles are not treated as a new shot from the player"); arrow.discard();
        GameplayTests.remove(player); helper.succeed();
    }
}
