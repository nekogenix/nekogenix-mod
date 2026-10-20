package io.github.nekogenix.mixin;

import io.github.nekogenix.client.FelineVision;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class FelineVisionMixin {
    @Inject(method = "getNightVisionScale", at = @At("RETURN"), cancellable = true)
    private static void nekogenix$felineVision(LivingEntity entity, float partial, CallbackInfoReturnable<Float> callback) {
        if (FelineVision.applies(entity)) callback.setReturnValue(FelineVision.strength(partial));
    }
}
