package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CameraProjectiles {
    public static final class View {
        private float height;
        private int tick = Integer.MIN_VALUE;
    }

    public static void receive(Player player, float height) {
        if (!(player instanceof ServerPlayer) || !Float.isFinite(height) || !player.isAlive() || player.isSpectator()) return;
        var view = player.getData(Content.CAMERA_VIEW);
        view.height = Mth.clamp(height, .08F, (float)(1.62 * player.getScale()));
        view.tick = player.tickCount;
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk() || !(event.getEntity() instanceof Projectile projectile)
                || projectile.tickCount != 0 || !(projectile.getOwner() instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator() || player.isSleeping() || !player.hasData(Content.CAMERA_VIEW)) return;
        var view = player.getData(Content.CAMERA_VIEW);
        long age = (long)player.tickCount - view.tick;
        if (age < 0 || age > 40 || projectile.position().distanceToSqr(player.getEyePosition()) > 2.25) return;
        double desired = Math.min(view.height, CameraGeometry.ceiling(player, player.position(), view.height));
        double offset = desired - player.getEyeHeight();
        if (Math.abs(offset) < .002) return;
        var origin = projectile.position();
        var shifted = origin.add(0, offset, 0);
        if (!player.level().noCollision(projectile, new net.minecraft.world.phys.AABB(shifted, shifted).inflate(.03))) return;
        if (player.level().clip(new ClipContext(origin, shifted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) return;
        projectile.setPos(shifted);
    }

    private CameraProjectiles() {}
}
