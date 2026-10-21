package io.github.nekogenix.gameplay;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class NekoSicknessEffect extends MobEffect {
    public NekoSicknessEffect() { super(MobEffectCategory.HARMFUL, 0xB769A2); }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return MobEffects.POISON.value().shouldApplyEffectTickThisTick(duration, amplifier);
    }

    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return MobEffects.POISON.value().applyEffectTick(entity, amplifier);
    }
}
