package io.github.nekogenix.client;

import io.github.nekogenix.gameplay.CatForm;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CameraTargeting {
    public static void update(float partial) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || mc.getCameraEntity() != player || !mc.options.getCameraType().isFirstPerson()
                || player.isSpectator() || !player.isAlive() || player.isSleeping()) return;
        var eye = player.getEyePosition(partial);
        var origin = new Vec3(Mth.lerp(partial, player.xo, player.getX()),
                CameraHeight.position(player, partial, eye.y, player.getEyeHeight()), Mth.lerp(partial, player.zo, player.getZ()));
        boolean cat = CatForm.active(player);
        if (!cat && Math.abs(origin.y - eye.y) < .02) return;
        double blockRange = player.blockInteractionRange();
        double entityRange = player.entityInteractionRange();
        double range = Math.max(blockRange, entityRange);
        var direction = player.getViewVector(partial);
        var end = origin.add(direction.scale(range));
        var block = mc.level.clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        var obstruction = block;
        if (cat) {
            obstruction = mc.level.clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player) {
                @Override
                public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                    if (state.is(BlockTags.REPLACEABLE) && state.getCollisionShape(level, pos).isEmpty()) return Shapes.empty();
                    return super.getBlockShape(state, level, pos);
                }
            });
        }
        double distance = origin.distanceToSqr(obstruction.getLocation());
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, origin, end,
                new AABB(origin, end).inflate(1), target -> !target.isSpectator() && target.isPickable(), distance);
        HitResult result = block;
        double allowedRange = blockRange;
        if (entity != null && origin.distanceToSqr(entity.getLocation()) < distance) {
            result = entity;
            allowedRange = entityRange;
        }
        if (origin.distanceToSqr(result.getLocation()) >= allowedRange * allowedRange) {
            result = BlockHitResult.miss(result.getLocation(), Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(result.getLocation()));
        }
        mc.hitResult = result;
        mc.crosshairPickEntity = result instanceof EntityHitResult hit ? hit.getEntity() : null;
    }

    private CameraTargeting() {}
}
