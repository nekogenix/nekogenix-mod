package io.github.nekogenix.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class EntityTargeting {
    public static HitResult pick(Player player, Vec3 origin, Vec3 direction) {
        double blockRange = player.blockInteractionRange();
        double entityRange = player.entityInteractionRange();
        var end = origin.add(direction.scale(Math.max(blockRange, entityRange)));
        var block = player.level().clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double distance = origin.distanceToSqr(block.getLocation());
        var entity = ProjectileUtil.getEntityHitResult(player, origin, end, new AABB(origin, end).inflate(1),
                target -> !target.isSpectator() && target.isPickable(), distance);
        HitResult result = block;
        double allowedRange = blockRange;
        if (entity != null && origin.distanceToSqr(entity.getLocation()) < distance) {
            result = entity;
            allowedRange = entityRange;
        }
        if (origin.distanceToSqr(result.getLocation()) >= allowedRange * allowedRange) {
            result = BlockHitResult.miss(result.getLocation(), Direction.getNearest(direction.x, direction.y, direction.z),
                    BlockPos.containing(result.getLocation()));
        }
        return result;
    }

    public static HitResult throughPlants(Player player, Vec3 origin, Vec3 direction, HitResult original) {
        if (!(original instanceof BlockHitResult block) || original.getType() != HitResult.Type.BLOCK
                || !player.level().getBlockState(block.getBlockPos()).getCollisionShape(player.level(), block.getBlockPos()).isEmpty()) {
            return original;
        }
        var end = origin.add(direction.scale(player.entityInteractionRange()));
        var obstruction = player.level().clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double distance = origin.distanceToSqr(obstruction.getLocation());
        var entity = ProjectileUtil.getEntityHitResult(player, origin, end, new AABB(origin, end).inflate(1),
                target -> !target.isSpectator() && target.isPickable(), distance);
        return entity != null && origin.distanceToSqr(entity.getLocation()) < distance ? entity : original;
    }

    private EntityTargeting() {}
}
