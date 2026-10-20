package io.github.nekogenix.gameplay;

import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class SalivaCloudEffect extends InstantenousMobEffect {
    public SalivaCloudEffect() { super(MobEffectCategory.HARMFUL, 0x879AC7); }

    @Override public void applyInstantenousEffect(Entity source, Entity owner, LivingEntity target, int amplifier, double intensity) {
        SalivaExposure.schedule(target);
    }

    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        SalivaExposure.schedule(entity);
        return false;
    }
}
