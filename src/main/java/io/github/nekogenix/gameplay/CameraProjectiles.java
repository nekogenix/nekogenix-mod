package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CameraProjectiles {
    public static final class View {
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, View> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.FLOAT, view -> view.bodyEye,
                        net.minecraft.network.codec.ByteBufCodecs.BOOL, view -> view.upright,
                        net.minecraft.network.codec.ByteBufCodecs.BOOL, view -> view.prone, View::new);
        float height, bodyEye = -1;
        boolean upright, prone;
        int tick = Integer.MIN_VALUE, preference = 100;
        public View() {}
        private View(float eye, boolean upright, boolean prone) { bodyEye = eye; this.upright = upright; this.prone = prone; }
    }

    public static void receive(Player player, float height) {
        int preference = Math.round((height / player.getScale() - CatForm.STANDING_EYE_HEIGHT)
                / (Player.STANDING_DIMENSIONS.eyeHeight() - CatForm.STANDING_EYE_HEIGHT) * 100);
        receive(player, height, Mth.clamp(preference, 0, 100));
    }

    public static void receive(Player player, float height, int preference) {
        if (!(player instanceof ServerPlayer) || !Float.isFinite(height) || !player.isAlive() || player.isSpectator()) return;
        var view = player.getData(Content.CAMERA_VIEW);
        view.height = Mth.clamp(height, .08F, (float)(1.62 * player.getScale()));
        view.preference = Mth.clamp(preference, 0, 100);
        view.tick = player.tickCount;
        CameraPosture.accept(player, view.height, view.preference);
    }

    public static Vec3 interactionEye(Player player) {
        if (!(player instanceof ServerPlayer) || !player.isAlive() || player.isSpectator() || player.isSleeping()
                || !player.hasData(Content.CAMERA_VIEW)) return player.getEyePosition();
        var view = player.getData(Content.CAMERA_VIEW);
        long age = (long)player.tickCount - view.tick;
        if (age < 0 || age > 40) return player.getEyePosition();
        double height = Math.min(view.bodyEye, CameraGeometry.ceiling(player, player.position(), view.bodyEye));
        return player.position().add(0, height, 0);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk() || !(event.getEntity() instanceof Projectile projectile)
                || projectile.tickCount != 0 || !(projectile.getOwner() instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator() || player.isSleeping() || !player.hasData(Content.CAMERA_VIEW)) return;
        var view = player.getData(Content.CAMERA_VIEW);
        long age = (long)player.tickCount - view.tick;
        if (age < 0 || age > 40 || projectile.position().distanceToSqr(player.getEyePosition()) > 2.25) return;
        double desired = Math.min(view.bodyEye, CameraGeometry.ceiling(player, player.position(), view.bodyEye));
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
