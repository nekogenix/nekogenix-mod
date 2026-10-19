package io.github.nekogenix.mixin;

import io.github.nekogenix.Content;
import io.github.nekogenix.gameplay.FelineFoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements FelineFoodData {
    @Unique private Player nekogenix$owner;

    @Override public void nekogenix$setOwner(Player player) { nekogenix$owner = player; }

    @Inject(method = "tick", at = @At("HEAD"))
    private void nekogenix$bindTickOwner(Player player, CallbackInfo callback) {
        // Also bind replacement FoodData instances supplied by other mods.
        nekogenix$owner = player;
    }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float nekogenix$reduceExhaustion(float amount) {
        return amount > 0 && nekogenix$owner != null && !nekogenix$owner.level().isClientSide
                && nekogenix$owner.getData(Content.NEKO).isNeko() ? amount * .5F : amount;
    }
}
