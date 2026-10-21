package io.github.nekogenix.mixin;

import io.github.nekogenix.gameplay.CatForm;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class CatDimensionsMixin {
    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void nekogenix$catDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> callback) {
        Player player = (Player)(Object)this;
        if (player.isAddedToLevel() && CatForm.active(player)) callback.setReturnValue(io.github.nekogenix.gameplay.CameraPosture.catDimensions(player, pose));
    }
}
