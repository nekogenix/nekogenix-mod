package io.github.nekogenix.mixin;

import io.github.nekogenix.gameplay.CameraProjectiles;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class InteractionReachMixin {
    @Redirect(method = {"canInteractWithBlock", "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 nekogenix$interactionEye(Player player) {
        return CameraProjectiles.interactionEye(player);
    }
}
