package io.github.nekogenix.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CameraTargeting {
    public static void update(float partial) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || mc.getCameraEntity() != player || !mc.options.getCameraType().isFirstPerson()
                || player.isSpectator() || !player.isAlive() || player.isSleeping()) return;
        var eye = player.getEyePosition(partial);
        var origin = new Vec3(Mth.lerp(partial, player.xo, player.getX()),
                CameraHeight.position(player, partial, eye.y, player.getEyeHeight()), Mth.lerp(partial, player.zo, player.getZ()));
        if (Math.abs(origin.y - eye.y) < .02) return;
        double blockRange = player.blockInteractionRange();
        double entityRange = player.entityInteractionRange();
        double range = Math.max(blockRange, entityRange);
        var direction = player.getViewVector(partial);
        var end = origin.add(direction.scale(range));
        var block = mc.level.clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double distance = origin.distanceToSqr(block.getLocation());
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, origin, end,
                new AABB(origin, end).inflate(1), target -> !target.isSpectator() && target.isPickable(), distance);
        HitResult result = block;
        if (entity != null && origin.distanceToSqr(entity.getLocation()) < distance
                && eye.distanceToSqr(entity.getLocation()) < entityRange * entityRange
                && mc.level.clip(new ClipContext(eye, entity.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) {
            result = entity;
        } else if (block.getType() != HitResult.Type.MISS) {
            var obstruction = mc.level.clip(new ClipContext(eye, block.getLocation(), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (eye.distanceToSqr(block.getLocation()) >= blockRange * blockRange
                    || obstruction.getType() != HitResult.Type.MISS && !obstruction.getBlockPos().equals(block.getBlockPos())) {
                result = BlockHitResult.miss(block.getLocation(), Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(block.getLocation()));
            }
        }
        mc.hitResult = result;
        mc.crosshairPickEntity = result instanceof EntityHitResult hit ? hit.getEntity() : null;
    }

    private CameraTargeting() {}
}
