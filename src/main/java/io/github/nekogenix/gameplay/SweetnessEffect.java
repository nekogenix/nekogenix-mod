package io.github.nekogenix.gameplay;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class SweetnessEffect extends MobEffect {
    public static final int DURATION = 24_000;

    public SweetnessEffect() { super(MobEffectCategory.BENEFICIAL, 0xE4B85C); }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 == 0; }

    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) player.getFoodData().eat(1, .5F);
        return true;
    }
}
