package io.github.nekogenix.gameplay;

import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CameraGeometry {
    public static double selected(int percent) {
        return Mth.lerp(Mth.clamp(percent, 0, 100) / 100.0, CatForm.STANDING_EYE_HEIGHT, Player.STANDING_DIMENSIONS.eyeHeight());
    }

    public static double requested(Player player, int percent) {
        double height = selected(percent) * player.getScale() - crouchOffset(player, percent);
        if (CameraPosture.passage(player) || CameraPosture.passageJumpPending(player)) height = Math.min(height, (CatForm.active(player) ? CatForm.STANDING_EYE_HEIGHT : .4F) * player.getScale());
        if (CatForm.active(player) && CameraPosture.seated(player)) return .595 * player.getScale();
        if (CatForm.active(player) && !CameraPosture.ordinaryPose(player)) height = Math.min(height, CatForm.STANDING_EYE_HEIGHT * player.getScale());
        if (!CatForm.active(player) && !CameraPosture.cameraProne(player) && player.getPose() != Pose.STANDING && player.getPose() != Pose.CROUCHING) {
            height = Math.min(height, player.getEyeHeight());
        }
        return height;
    }

    public static double crouchOffset(Player player, int percent) {
        return player.isCrouching() ? Mth.lerp(Mth.clamp(percent, 0, 100) / 100.0,
                CatForm.STANDING_EYE_HEIGHT - CatForm.CROUCHING_EYE_HEIGHT,
                Player.STANDING_DIMENSIONS.eyeHeight() - 1.27) * player.getScale() : 0;
    }

    public static double ceiling(Player player, Vec3 feet, double height) {
        return ceiling(player, feet, height, .08);
    }

    public static double ceiling(Player player, Vec3 feet, double height, double margin) {
        double ceiling = Double.POSITIVE_INFINITY;
        var column = new AABB(feet.x - .12, feet.y + .08, feet.z - .12,
                feet.x + .12, feet.y + height + margin, feet.z + .12);
        for (var shape : player.level().getBlockCollisions(player, column)) {
            for (var box : shape.toAabbs()) {
                if (box.minY <= Math.max(feet.y, player.getY()) + Math.min(.2, player.getBbHeight() * .25) || !box.intersects(column)) continue;
                ceiling = Math.min(ceiling, Math.max(.08, box.minY - feet.y - margin));
            }
        }
        return ceiling;
    }

    public static Vec3 reachableMovement(Player player, Vec3 feet, Vec3 movement) {
        var body = CatForm.active(player) ? CatForm.dimensions(Pose.STANDING).scale(player.getScale()).makeBoundingBox(feet)
                : CameraPosture.cameraProne(player) ? net.minecraft.world.entity.EntityDimensions.scalable(.6F, .6F).scale(player.getScale()).makeBoundingBox(feet)
                : player.getBoundingBox().move(feet.subtract(player.position()));
        return Entity.collideBoundingBox(player, movement, body, player.level(), List.of());
    }

    public static double approach(double height, double target, double seconds) {
        return height + (target - height) * (1 - Math.exp(-Math.max(0, seconds) / (target < height ? .075 : .18)));
    }

    private CameraGeometry() {}
}
