package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.gameplay.CameraGeometry;
import io.github.nekogenix.network.CameraViewInput;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CameraHeight {
    private static LocalPlayer owner;
    private static double height;
    private static long lastFrame;
    private static long predictionUntil;
    private static double predictedHeight;
    private static float sentHeight = Float.NaN;
    private static int sentTick;
    private static boolean insideTick;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        insideTick = true;
    }

    @SubscribeEvent
    public static void tickEnd(ClientTickEvent.Post event) {
        insideTick = false;
    }

    public static void syncForTick() {
        if (insideTick) sync();
    }

    @SubscribeEvent
    public static void input(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        sync();
    }

    private static void sync() {
        var player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) return;
        boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
        float current = firstPerson && owner == player ? (float)height : player.getEyeHeight();
        if (!Float.isFinite(sentHeight) || Math.abs(current - sentHeight) > .002F || player.tickCount - sentTick >= 20) {
            PacketDistributor.sendToServer(new CameraViewInput(current));
            sentHeight = current;
            sentTick = player.tickCount;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        owner = null;
        lastFrame = 0;
        predictionUntil = 0;
        sentHeight = Float.NaN;
        sentTick = 0;
        insideTick = false;
    }

    public static double position(LocalPlayer player, float partial, double vanillaY, float vanillaEye) {
        long now = Util.getNanos();
        if (owner != player) predictionUntil = 0;
        double feet = Mth.lerp(partial, player.yo, player.getY());
        if (player.isSleeping() || player.isSpectator() || !player.isAlive()) {
            owner = null;
            return vanillaY;
        }
        double requested = CameraGeometry.requested(player, ClientConfig.cameraHeight());
        double x = Mth.lerp(partial, player.xo, player.getX());
        double z = Mth.lerp(partial, player.zo, player.getZ());
        var base = new Vec3(x, feet, z);
        double clearance = CameraGeometry.ceiling(player, base, Math.max(requested, owner == player ? height : vanillaEye));
        double target = Math.min(requested, clearance);
        var velocity = player.getDeltaMovement().multiply(1, 0, 1).scale(8);
        double length = velocity.length();
        if (length > 4) velocity = velocity.scale(4 / length);
        if (requested > player.getEyeHeight() + .05 && length > .01) {
            velocity = CameraGeometry.reachableMovement(player, base, velocity);
            int steps = Math.max(1, (int)Math.ceil(velocity.length() / .25));
            var body = player.getBoundingBox().move(base.subtract(player.position()));
            for (int step = 1; step <= steps; step++) {
                var offset = velocity.scale(step / (double)steps);
                if (!player.level().noCollision(player, body.move(offset))) break;
                double ahead = CameraGeometry.ceiling(player, base.add(offset), requested);
                target = Math.min(target, ahead);
            }
        }
        if (target < Math.min(requested, clearance) - .001) {
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
            height = CameraGeometry.approach(height, target, seconds);
        }
        height = Math.min(height, clearance);
        return feet + height;
    }

    private CameraHeight() {}
}
