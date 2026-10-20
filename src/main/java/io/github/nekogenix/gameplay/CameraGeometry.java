package io.github.nekogenix.gameplay;

import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CameraGeometry {
    public static double requested(Player player, int percent) {
        double height = Mth.lerp(Mth.clamp(percent, 0, 100) / 100.0,
                CatForm.STANDING_EYE_HEIGHT, Player.STANDING_DIMENSIONS.eyeHeight()) * player.getScale() - crouchOffset(player, percent);
        if (!CatForm.active(player) && player.getPose() != Pose.STANDING && player.getPose() != Pose.CROUCHING) {
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
        var body = player.getBoundingBox().move(feet.subtract(player.position()));
        return Entity.collideBoundingBox(player, movement, body, player.level(), List.of());
    }

    public static double approach(double height, double target, double seconds) {
        return height + (target - height) * (1 - Math.exp(-Math.max(0, seconds) / (target < height ? .075 : .18)));
    }

    private CameraGeometry() {}
}
