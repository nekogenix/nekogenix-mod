package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.gameplay.CatForm;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CameraHeight {
    private static LocalPlayer owner;
    private static double height;
    private static long lastFrame;
    private static long predictionUntil;
    private static double predictedHeight;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean cat = CatForm.active(player);
        if (!cat && player.tickCount < 20) return;
        if (ClientConfig.CAMERA_CAT_FORM.get() != cat) {
            ClientConfig.CAMERA_CAT_FORM.set(cat);
            ClientConfig.CAMERA_HEIGHT.set(cat ? 0 : 100);
            ClientConfig.SPEC.save();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        owner = null;
        lastFrame = 0;
        predictionUntil = 0;
    }

    public static double position(LocalPlayer player, float partial, double vanillaY, float vanillaEye) {
        long now = Util.getNanos();
        if (owner != player) predictionUntil = 0;
        double feet = Mth.lerp(partial, player.yo, player.getY());
        if (player.isSleeping() || player.isSpectator() || !player.isAlive()) {
            owner = null;
            return vanillaY;
        }
        double scale = player.getScale();
        double human = Player.STANDING_DIMENSIONS.eyeHeight();
        double cat = CatForm.STANDING_EYE_HEIGHT;
        if (player.isCrouching()) {
            human = 1.27;
            cat = CatForm.CROUCHING_EYE_HEIGHT;
        }
        double requested = Mth.lerp(ClientConfig.CAMERA_HEIGHT.get() / 100.0, cat, human) * scale;
        if (!CatForm.active(player) && player.getPose() != Pose.STANDING && player.getPose() != Pose.CROUCHING) {
            requested = Math.min(requested, player.getEyeHeight());
        }
        double x = Mth.lerp(partial, player.xo, player.getX());
        double z = Mth.lerp(partial, player.zo, player.getZ());
        var base = new Vec3(x, feet, z);
        double clearance = clearance(player, base, requested);
        double target = Math.min(requested, clearance);
        var velocity = player.getDeltaMovement().multiply(1, 0, 1).scale(6);
        double length = velocity.length();
        if (length > 3) velocity = velocity.scale(3 / length);
        if (length > .01) {
            var obstacle = player.level().clip(new ClipContext(base.add(0, .2, 0), base.add(velocity).add(0, .2, 0),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (obstacle.getType() != HitResult.Type.MISS) {
                double free = Math.max(0, obstacle.getLocation().subtract(base.add(0, .2, 0)).length() - .2);
                velocity = velocity.normalize().scale(free);
            }
            for (int step = 1; step <= 3; step++) {
                double ahead = clearance(player, base.add(velocity.scale(step / 3.0)), requested);
                target = Math.min(target, ahead);
            }
        }
        if (target < clearance - .001) {
            predictedHeight = target;
            predictionUntil = now + 200_000_000;
        }
        if (now < predictionUntil) target = Math.min(target, predictedHeight);
        double seconds = lastFrame == 0 ? 0 : Math.min(.1, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        if (owner != player) {
            owner = player;
            height = Math.min(vanillaEye, clearance);
        }
        if (!Minecraft.getInstance().isPaused()) {
            height += (target - height) * (1 - Math.exp(-seconds / (target < height ? .075 : .18)));
        }
        height = Math.min(height, clearance);
        return feet + height;
    }

    private static double clearance(LocalPlayer player, Vec3 feet, double requested) {
        double safe = requested;
        for (int corner = 0; corner < 4; corner++) {
            double x = (corner % 2 == 0 ? -.16 : .16);
            double z = (corner / 2 == 0 ? -.16 : .16);
            var start = feet.add(x, .08, z);
            var end = feet.add(x, requested + .13, z);
            var hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) safe = Math.min(safe, Math.max(.08, hit.getLocation().y - feet.y - .13));
        }
        return safe;
    }

    private CameraHeight() {}
}
