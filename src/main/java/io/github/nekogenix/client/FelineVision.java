package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class FelineVision {
    private static LocalPlayer observer;
    private static float previous, strength;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player != observer) {
            observer = mc.player;
            previous = strength = 0;
        }
        if (observer == null || mc.isPaused()) return;
        previous = strength;
        var effect = observer.getEffect(MobEffects.NIGHT_VISION);
        boolean enabled = observer.hasEffect(Content.VISION) && owned(effect) && !effect.endsWithin(20);
        strength = Mth.clamp(strength + (enabled ? .05F : -.05F), 0, 1);
    }

    private static boolean owned(MobEffectInstance effect) {
        return effect != null && effect.isAmbient() && !effect.isVisible() && !effect.showIcon();
    }

    public static boolean applies(LivingEntity entity) {
        return entity == observer && owned(entity.getEffect(MobEffects.NIGHT_VISION))
                && (entity.hasEffect(Content.VISION) || previous > 0 || strength > 0);
    }

    public static float strength(float partial) {
        float value = Mth.lerp(Mth.clamp(partial, 0, 1), previous, strength);
        return value * value * (3 - 2 * value);
    }

    public static boolean changing() { return previous != strength; }

    private FelineVision() {}
}
