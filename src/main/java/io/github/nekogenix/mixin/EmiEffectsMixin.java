package io.github.nekogenix.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Collection;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EffectRenderingInventoryScreen.class, priority = 1100)
public abstract class EmiEffectsMixin {
    @Dynamic("EMI")
    @ModifyExpressionValue(method = "emi$drawCenteredEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"), remap = false)
    private Collection<MobEffectInstance> nekogenix$visibleEffects(Collection<MobEffectInstance> effects) {
        return effects.stream().filter(effect -> IClientMobEffectExtensions.of(effect).isVisibleInInventory(effect)).toList();
    }
}
