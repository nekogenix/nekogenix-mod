package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.InjectionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class InjectionHandVisibility {
    private static final float TRANSITION_TICKS = 6;
    private static final float LOWER_DISTANCE = 2;
    private static final float[] previous = new float[2];
    private static final float[] hidden = new float[2];
    private static LocalPlayer owner;
    private static PoseStack.Pose shiftedPose;
    private static float shiftX, shiftY, shiftZ;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player != owner) {
            reset();
            owner = player;
        }
        if (player == null) return;
        for (var hand : InteractionHand.values()) {
            int index = hand.ordinal();
            previous[index] = hidden[index];
            var other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            long start = InjectionHandler.started(player, other);
            boolean lower = player.isAlive() && !InjectionHandler.canDual(player)
                    && !InjectionHandler.active(player, hand) && start >= 0
                    && player.level().getGameTime() - start < InjectionHandler.FORWARD_TICKS
                    && InjectionHandler.returning(player, other).started() < 0;
            hidden[index] = Mth.clamp(hidden[index] + (lower ? 1 : -1) / TRANSITION_TICKS, 0, 1);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void hand(RenderHandEvent event) {
        var pose = event.getPoseStack().last();
        var matrix = pose.pose();
        if (pose == shiftedPose) {
            matrix.m30(matrix.m30() - shiftX);
            matrix.m31(matrix.m31() - shiftY);
            matrix.m32(matrix.m32() - shiftZ);
        }
        shiftedPose = null;
        var player = Minecraft.getInstance().player;
        if (event.isCanceled() || player == null || player != owner || InjectionHandler.canDual(player)
                || InjectionHandler.active(player, event.getHand())) return;
        int index = event.getHand().ordinal();
        float amount = Mth.lerp(event.getPartialTick(), previous[index], hidden[index]);
        if (amount <= 0) return;
        if (amount >= 1) {
            event.setCanceled(true);
            return;
        }
        amount = amount * amount * (3 - 2 * amount);
        float distance = -LOWER_DISTANCE * amount;
        shiftX = matrix.m10() * distance;
        shiftY = matrix.m11() * distance;
        shiftZ = matrix.m12() * distance;
        event.getPoseStack().translate(0, distance, 0);
        shiftedPose = pose;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }

    private static void reset() {
        owner = null;
        shiftedPose = null;
        java.util.Arrays.fill(previous, 0);
        java.util.Arrays.fill(hidden, 0);
    }

    private InjectionHandVisibility() {}
}
