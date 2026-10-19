package io.github.nekogenix.mixin;

import io.github.nekogenix.client.CameraTargeting;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class CameraTargetingMixin {
    @Inject(method = "pick(F)V", at = @At("RETURN"))
    private void nekogenix$target(float partial, CallbackInfo callback) {
        CameraTargeting.update(partial);
        io.github.nekogenix.client.CameraHeight.syncForTick();
    }
}
