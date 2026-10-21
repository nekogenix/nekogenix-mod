package io.github.nekogenix;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nekogenix.gameplay.InjectionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class InjectionAnimationChecks {
    static void verify() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        long now = mc.level.getGameTime();
        for (var hand : InteractionHand.values()) {
            ItemStack original = player.getItemInHand(hand);
            var started = hand == InteractionHand.MAIN_HAND ? Content.INJECTION_STARTED : Content.OFFHAND_INJECTION_STARTED;
            var returning = hand == InteractionHand.MAIN_HAND ? Content.INJECTION_RETURN : Content.OFFHAND_INJECTION_RETURN;
            long originalStart = player.getData(started);
            var originalReturn = player.getData(returning);
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            try {
                for (var item : new net.minecraft.world.item.Item[] {Content.INJECTION.get(), Content.STABILIZING_INJECTION.get()}) {
                    player.setItemInHand(hand, item.getDefaultInstance());
                    for (float equip : new float[] {0, .6F}) for (float swing : new float[] {0, .36F, .9F}) {
                        Matrix4f idle = idle(arm, equip, swing);
                        player.setData(started, now);
                        player.setData(returning, InjectionHandler.ReturnAnimation.NONE);
                        check(transform(hand, 0, equip, swing).equals(idle, .00001F),
                                "Injection begins at the actual held pose including equip and swing");
                        player.setData(started, now - InjectionHandler.TOTAL_TICKS + 1);
                        check(transform(hand, .999F, equip, swing).equals(idle, .00001F),
                                "Completed injection reaches the actual held pose before native rendering resumes");
                        for (int progress : new int[] {0, 3, 10, 11, 35, 79}) {
                            player.setData(started, now - progress);
                            player.setData(returning, InjectionHandler.ReturnAnimation.NONE);
                            Matrix4f forward = transform(hand, 0, equip, swing);
                            player.setData(returning, new InjectionHandler.ReturnAnimation(now, progress));
                            check(transform(hand, 0, equip, swing).equals(forward, .00001F),
                                    "Cancellation starts at the current syringe pose without a position or rotation jump");
                            Matrix4f previous = forward;
                            for (int step = 1; step < InjectionHandler.RETURN_TICKS * 8; step++) {
                                float elapsed = step / 8F;
                                int whole = (int)elapsed;
                                player.setData(returning, new InjectionHandler.ReturnAnimation(now - whole, progress));
                                Matrix4f current = transform(hand, elapsed - whole, equip, swing);
                                for (var corner : new Vector3f[] {new Vector3f(-.4F, -.4F, 0), new Vector3f(.4F, .4F, 0)}) {
                                    check(previous.transformPosition(new Vector3f(corner)).distance(current.transformPosition(corner)) < .05F,
                                            "Returning syringe corners move continuously between rendered frames");
                                }
                                previous = current;
                            }
                            player.setData(returning, new InjectionHandler.ReturnAnimation(now - InjectionHandler.RETURN_TICKS + 1, progress));
                            check(transform(hand, .999F, equip, swing).equals(idle, .00001F),
                                    "Cancelled injection reaches the actual held pose before native rendering resumes");
                            player.setData(returning, new InjectionHandler.ReturnAnimation(now - InjectionHandler.RETURN_TICKS, progress));
                            var pose = new PoseStack();
                            check(!IClientItemExtensions.of(player.getItemInHand(hand)).applyForgeHandTransform(pose, player, arm,
                                    player.getItemInHand(hand), 0, equip, swing) && pose.last().pose().equals(new Matrix4f(), .00001F),
                                    "An ended return leaves normal hand rendering untouched");
                        }
                        player.setData(returning, InjectionHandler.ReturnAnimation.NONE);
                    }
                }
            } finally {
                player.setItemInHand(hand, original);
                player.setData(started, originalStart);
                player.setData(returning, originalReturn);
            }
        }
        Nekogenix.LOGGER.info("Nekogenix injection transition checks passed: both injection types both hands equip swing early late cancellation and completion endpoints");
    }

    private static Matrix4f transform(InteractionHand hand, float partial, float equip, float swing) {
        var player = Minecraft.getInstance().player;
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        var pose = new PoseStack();
        check(IClientItemExtensions.of(player.getItemInHand(hand)).applyForgeHandTransform(pose, player, arm,
                player.getItemInHand(hand), partial, equip, swing), "Active injection supplies a hand pose");
        return new Matrix4f(pose.last().pose());
    }

    private static Matrix4f idle(HumanoidArm arm, float equip, float swing) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float arc = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        return new Matrix4f().translate(side * -.4F * arc, .2F * Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI), -.2F * Mth.sin(swing * Mth.PI))
                .translate(side * .56F, -.52F - equip * .6F, -.72F)
                .rotate(Axis.YP.rotationDegrees(side * (45 - Mth.sin(swing * swing * Mth.PI) * 20)))
                .rotate(Axis.ZP.rotationDegrees(side * arc * -20)).rotate(Axis.XP.rotationDegrees(arc * -80))
                .rotate(Axis.YP.rotationDegrees(side * -45));
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new IllegalStateException("Injection animation check failed: " + message);
    }

    private InjectionAnimationChecks() {}
}
