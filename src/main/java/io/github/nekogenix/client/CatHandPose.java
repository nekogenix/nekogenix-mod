package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

final class CatHandPose {
    static void apply(RenderHandEvent event, LocalPlayer player, HumanoidArm arm) {
        PoseStack pose = event.getPoseStack();
        ItemStack stack = event.getItemStack();
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float partial = event.getPartialTick();
        float equip = event.getEquipProgress();
        float swing = event.getSwingProgress();
        boolean using = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == event.getHand();
        if (stack.getItem() instanceof CrossbowItem) {
            if (using) {
                equip(pose, side, equip);
                pose.translate(side * -.4785682F, -.094387F, .05731531F);
                pose.mulPose(Axis.XP.rotationDegrees(-11.935F));
                pose.mulPose(Axis.YP.rotationDegrees(side * 65.3F));
                pose.mulPose(Axis.ZP.rotationDegrees(side * -9.785F));
                float elapsed = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partial + 1);
                float charge = Math.min(1, elapsed / CrossbowItem.getChargeDuration(stack, player));
                shake(pose, elapsed, charge);
                pose.translate(0, 0, charge * .04F);
                pose.scale(1, 1, 1 + charge * .2F);
                pose.mulPose(Axis.YN.rotationDegrees(side * 45));
            } else {
                attack(pose, side, equip, swing);
                if (CrossbowItem.isCharged(stack) && swing < .001F && event.getHand() == InteractionHand.MAIN_HAND) {
                    pose.translate(side * -.641864F, 0, 0);
                    pose.mulPose(Axis.YP.rotationDegrees(side * 10));
                }
            }
            return;
        }
        if (IClientItemExtensions.of(stack).applyForgeHandTransform(pose, player, arm, stack, partial, equip, swing)) return;
        if (using) {
            if (stack.getUseAnimation() == net.minecraft.world.item.UseAnim.EAT || stack.getUseAnimation() == net.minecraft.world.item.UseAnim.DRINK) {
                float remaining = player.getUseItemRemainingTicks() - partial + 1;
                float fraction = remaining / stack.getUseDuration(player);
                if (fraction < .8F) pose.translate(0, Mth.abs(Mth.cos(remaining / 4 * Mth.PI) * .1F), 0);
                float amount = 1 - (float)Math.pow(fraction, 27);
                pose.translate(amount * .6F * side, amount * -.5F, 0);
                pose.mulPose(Axis.YP.rotationDegrees(side * amount * 90));
                pose.mulPose(Axis.XP.rotationDegrees(amount * 10));
                pose.mulPose(Axis.ZP.rotationDegrees(side * amount * 30));
            }
            switch (stack.getUseAnimation()) {
                case NONE, EAT, DRINK, BLOCK, BOW, SPEAR, BRUSH -> equip(pose, side, equip);
                default -> {}
            }
            switch (stack.getUseAnimation()) {
                case BOW -> {
                    pose.translate(side * -.2785682F, .18344387F, .15731531F);
                    pose.mulPose(Axis.XP.rotationDegrees(-13.935F));
                    pose.mulPose(Axis.YP.rotationDegrees(side * 35.3F));
                    pose.mulPose(Axis.ZP.rotationDegrees(side * -9.785F));
                    float elapsed = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partial + 1);
                    float charge = elapsed / 20;
                    charge = Math.min(1, (charge * charge + charge * 2) / 3);
                    shake(pose, elapsed, charge);
                    pose.translate(0, 0, charge * .04F);
                    pose.scale(1, 1, 1 + charge * .2F);
                    pose.mulPose(Axis.YN.rotationDegrees(side * 45));
                }
                case SPEAR -> {
                    pose.translate(side * -.5F, .7F, .1F);
                    pose.mulPose(Axis.XP.rotationDegrees(-55));
                    pose.mulPose(Axis.YP.rotationDegrees(side * 35.3F));
                    pose.mulPose(Axis.ZP.rotationDegrees(side * -9.785F));
                    float elapsed = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partial + 1);
                    float charge = Math.min(1, elapsed / 10);
                    shake(pose, elapsed, charge);
                    pose.translate(0, 0, charge * .2F);
                    pose.scale(1, 1, 1 + charge * .2F);
                    pose.mulPose(Axis.YN.rotationDegrees(side * 45));
                }
                case BRUSH -> {
                    float progress = 1 - (player.getUseItemRemainingTicks() % 10 - partial + 1) / 10;
                    float angle = -15 + 75 * Mth.cos(progress * 2 * Mth.PI);
                    if (arm == HumanoidArm.LEFT) {
                        pose.translate(.1, .83, .35);
                        pose.mulPose(Axis.XP.rotationDegrees(-80));
                        pose.mulPose(Axis.YP.rotationDegrees(-90));
                        pose.mulPose(Axis.XP.rotationDegrees(angle));
                        pose.translate(-.3, .22, .35);
                    } else {
                        pose.translate(-.25, .22, .35);
                        pose.mulPose(Axis.XP.rotationDegrees(-80));
                        pose.mulPose(Axis.YP.rotationDegrees(90));
                        pose.mulPose(Axis.XP.rotationDegrees(angle));
                    }
                }
                default -> {}
            }
        } else if (player.isAutoSpinAttack()) {
            equip(pose, side, equip);
            pose.translate(side * -.4F, .8F, .3F);
            pose.mulPose(Axis.YP.rotationDegrees(side * 65));
            pose.mulPose(Axis.ZP.rotationDegrees(side * -85));
        } else attack(pose, side, equip, swing);
    }

    private static void equip(PoseStack pose, float side, float progress) {
        pose.translate(side * .56F, -.52F - progress * .6F, -.72F);
    }

    private static void attack(PoseStack pose, float side, float equip, float progress) {
        float swing = Mth.sin(Mth.sqrt(progress) * Mth.PI);
        pose.translate(side * -.4F * swing, .2F * Mth.sin(Mth.sqrt(progress) * Mth.TWO_PI), -.2F * Mth.sin(progress * Mth.PI));
        equip(pose, side, equip);
        pose.mulPose(Axis.YP.rotationDegrees(side * (45 - Mth.sin(progress * progress * Mth.PI) * 20)));
        pose.mulPose(Axis.ZP.rotationDegrees(side * swing * -20));
        pose.mulPose(Axis.XP.rotationDegrees(swing * -80));
        pose.mulPose(Axis.YP.rotationDegrees(side * -45));
    }

    private static void shake(PoseStack pose, float elapsed, float charge) {
        if (charge > .1F) pose.translate(0, Mth.sin((elapsed - .1F) * 1.3F) * (charge - .1F) * .004F, 0);
    }

    private CatHandPose() {}
}
