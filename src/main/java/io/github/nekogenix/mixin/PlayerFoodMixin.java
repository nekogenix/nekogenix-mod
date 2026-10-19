package io.github.nekogenix.mixin;

import io.github.nekogenix.gameplay.FelineFoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerFoodMixin {
    @Shadow protected FoodData foodData;

    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"))
    private void nekogenix$bindExhaustionOwner(float amount, CallbackInfo callback) {
        ((FelineFoodData) foodData).nekogenix$setOwner((Player)(Object)this);
    }

    @Inject(method = "getFoodData", at = @At("RETURN"))
    private void nekogenix$bindFoodOwner(CallbackInfoReturnable<FoodData> callback) {
        ((FelineFoodData) callback.getReturnValue()).nekogenix$setOwner((Player)(Object)this);
    }
}
