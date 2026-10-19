package io.github.nekogenix.mixin;

import io.github.nekogenix.client.CameraHeight;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Entity entity;
    @Shadow private boolean detached;
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow private float partialTickTime;

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"), index = 1)
    private double nekogenix$height(double y) {
        if (detached || !(entity instanceof LocalPlayer player) || player != Minecraft.getInstance().player) return y;
        return CameraHeight.position(player, partialTickTime, y, Mth.lerp(partialTickTime, eyeHeightOld, eyeHeight));
    }
}
