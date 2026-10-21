package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nekogenix.gameplay.InjectionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

final class InjectionAnimation {
    static boolean apply(PoseStack pose, LocalPlayer player, InteractionHand hand, HumanoidArm arm,
            float partial, float equip, float swing) {
        long start = InjectionHandler.started(player, hand);
        var returning = InjectionHandler.returning(player, hand);
        float age = player.level().getGameTime() - start + partial;
        float retreat;
        if (returning.started() >= 0) {
            retreat = returning.remaining(player.level().getGameTime(), partial);
            if (retreat <= 0) return false;
            age = returning.progress();
        } else {
            if (start < 0 || age < 0 || age >= InjectionHandler.TOTAL_TICKS) return false;
            retreat = 1 - (age - InjectionHandler.FORWARD_TICKS) / InjectionHandler.RETURN_TICKS;
        }
        retreat = smooth(retreat);
        float approach = smooth(age / InjectionHandler.APPROACH_TICKS);
        float amount = approach * retreat;
        float jab = Mth.clamp((age - InjectionHandler.APPROACH_TICKS)
                / (InjectionHandler.NEEDLE_ENTRY_TICKS - InjectionHandler.APPROACH_TICKS), 0, 1);
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swingArc = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        float idleX = side * (.56F - .4F * swingArc);
        float idleY = -.52F - equip * .6F + .2F * Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI);
        float idleZ = -.72F - .2F * Mth.sin(swing * Mth.PI);
        float shake = amount * (1 - jab);
        pose.translate(Mth.lerp(amount, idleX, side * .11F),
                Mth.lerp(amount, idleY, -.52F + .45072432F) - .09F * jab * retreat
                        + Mth.sin(age * .48F) * .001F * shake,
                Mth.lerp(amount, idleZ, -.789F));
        var idleRotation = Axis.YP.rotationDegrees(side * (45 - Mth.sin(swing * swing * Mth.PI) * 20))
                .mul(Axis.ZP.rotationDegrees(side * swingArc * -20))
                .mul(Axis.XP.rotationDegrees(swingArc * -80))
                .mul(Axis.YP.rotationDegrees(side * -45));
        var injectionRotation = Axis.YP.rotationDegrees(side * 90)
                .mul(Axis.ZP.rotationDegrees(side * 145)).mul(Axis.XP.rotationDegrees(25));
        pose.mulPose(idleRotation.slerp(injectionRotation, amount));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.cos(age * .48F) * .1F * shake));
        return true;
    }

    private static float smooth(float value) {
        value = Mth.clamp(value, 0, 1);
        return value * value * (3 - 2 * value);
    }

    private InjectionAnimation() {}
}
