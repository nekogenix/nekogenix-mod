package io.github.nekogenix;

import io.github.nekogenix.gameplay.TransformationHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CatPostureTests {
    @GameTest(template = "empty")
    public static void crouchingCannotRetainFelineSprint(GameTestHelper helper) {
        for (boolean cat : new boolean[] {false, true}) {
            var player = GameplayTests.player(helper);
            if (cat) CatTransformationTests.cat(player);
            else TransformationTests.setState(player, 4);
            var event = new PlayerTickEvent.Post(player);
            player.setSprinting(true);
            TransformationHandler.tick(event);
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(speed.hasModifier(Content.id("feline_sprint")), "Standing sprint receives feline acceleration");
            player.setOnGround(false);
            player.setDeltaMovement(.4, -.2, .3);
            var momentum = player.getDeltaMovement();
            player.setShiftKeyDown(true);
            TransformationHandler.tick(event);
            helper.assertFalse(player.isSprinting() || speed.hasModifier(Content.id("feline_sprint")),
                    "Sneaking in midair stops both vanilla sprint and feline acceleration");
            helper.assertTrue(player.getDeltaMovement().equals(momentum), "Changing posture does not teleport or erase airborne momentum");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SNEAKING_SPEED) - .6) < .001,
                    "Ordinary feline sneaking keeps its intended speed");
            player.setOnGround(true);
            player.setSprinting(true);
            TransformationHandler.tick(event);
            helper.assertFalse(player.isSprinting(), "Holding the sprint key cannot reenable sprint while crouching");
            player.setShiftKeyDown(false);
            player.setSprinting(true);
            TransformationHandler.tick(event);
            helper.assertTrue(speed.hasModifier(Content.id("feline_sprint")), "Releasing sneak allows sprinting again");
            player.getAbilities().flying = true;
            player.setShiftKeyDown(true);
            TransformationHandler.tick(event);
            helper.assertTrue(player.isSprinting(), "Descending in Creative flight does not cancel flight sprint");
            GameplayTests.remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catPassengersUseTheVehicleSeatHeight(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        CatTransformationTests.cat(player);
        player.refreshDimensions();
        for (var type : new EntityType<?>[] {EntityType.BOAT, EntityType.MINECART}) {
            var vehicle = type.create(helper.getLevel());
            vehicle.setPos(helper.absoluteVec(new Vec3(2, 2, 2)));
            helper.getLevel().addFreshEntity(vehicle);
            helper.assertTrue(player.startRiding(vehicle, true), "Cat players can board " + type);
            vehicle.positionRider(player);
            helper.assertTrue(player.getVehicleAttachmentPoint(vehicle).equals(Vec3.ZERO),
                    "Cat passengers do not inherit the human hip offset");
            helper.assertTrue(player.position().distanceTo(vehicle.getPassengerRidingPosition(player)) < .00001,
                    "The cat's feet rest at the vehicle's actual seat attachment");
            player.stopRiding();
            vehicle.discard();
        }
        GameplayTests.remove(player);
        helper.succeed();
    }
}
