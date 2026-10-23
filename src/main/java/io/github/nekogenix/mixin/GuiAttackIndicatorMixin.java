package io.github.nekogenix.mixin;

import io.github.nekogenix.Content;
import io.github.nekogenix.gameplay.CatForm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiAttackIndicatorMixin {
    private static final ResourceLocation VANILLA_ATTACK_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_background");
    private static final ResourceLocation VANILLA_ATTACK_PROGRESS = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
    private static final ResourceLocation VANILLA_FULL_ATTACK = ResourceLocation.withDefaultNamespace("hud/crosshair_attack_indicator_full");
    private static final ResourceLocation CAT_ATTACK_BACKGROUND = Content.id("hud/cat_attack_indicator_background");
    private static final ResourceLocation CAT_ATTACK_PROGRESS = Content.id("hud/cat_attack_indicator_progress");
    private static final ResourceLocation CAT_FULL_ATTACK = Content.id("hud/cat_attack_indicator_full");

    @Redirect(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private void nekogenix$catAttackIndicator(GuiGraphics graphics, ResourceLocation sprite,
            int x, int y, int width, int height) {
        if (catActive()) {
            if (sprite.equals(VANILLA_ATTACK_BACKGROUND)) {
                graphics.blitSprite(CAT_ATTACK_BACKGROUND, x, y, 16, 16);
                return;
            }
            if (sprite.equals(VANILLA_FULL_ATTACK)) {
                graphics.blitSprite(CAT_FULL_ATTACK, x, y, 16, 16);
                return;
            }
        }
        graphics.blitSprite(sprite, x, y, width, height);
    }

    @Redirect(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"
            )
    )
    private void nekogenix$catAttackProgress(GuiGraphics graphics, ResourceLocation sprite,
            int spriteWidth, int spriteHeight, int u, int v,
            int x, int y, int width, int height) {
        if (catActive() && sprite.equals(VANILLA_ATTACK_PROGRESS)) {
            graphics.blitSprite(CAT_ATTACK_PROGRESS, 16, 16, 0, 0, x, y, width, 16);
            return;
        }
        graphics.blitSprite(sprite, spriteWidth, spriteHeight, u, v, x, y, width, height);
    }

    private static boolean catActive() {
        var player = Minecraft.getInstance().player;
        return player != null && CatForm.active(player);
    }
}
