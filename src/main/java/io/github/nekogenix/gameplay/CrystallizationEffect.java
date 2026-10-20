package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

public final class CrystallizationEffect extends MobEffect {
    public static final int DURATION = 1_200;
    public static final int BLEND_TICKS = 60;
    private static final net.minecraft.resources.ResourceLocation SLOWNESS = Content.id("crystallization");
    private static final BlockParticleOption PARTICLE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState());

    public CrystallizationEffect() {
        super(MobEffectCategory.HARMFUL, 0xA586CE);
        setBlendDuration(BLEND_TICKS);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SLOWNESS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> -.95);
        withSoundOnAdded(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    @Override public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        var speed = attributes.getInstance(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SLOWNESS, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return true;
        var effect = entity.getEffect(Content.CRYSTALLIZATION);
        if (effect == null) return true;
        float strength = effect.getBlendFactor(entity, 1);
        if (entity.tickCount % 5 == 0) {
            var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            double amount = -.95 * strength;
            var current = speed == null ? null : speed.getModifier(SLOWNESS);
            if (speed != null && (current == null || Math.abs(current.amount() - amount) > .001)) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(SLOWNESS, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            if (strength > .05F) level.sendParticles(PARTICLE, entity.getX(), entity.getY() + entity.getBbHeight() * .65,
                    entity.getZ(), 2, entity.getBbWidth() * .5, entity.getBbHeight() * .3, entity.getBbWidth() * .5, .025);
        }
        if (strength >= .9F && entity.tickCount % 80 == 0) {
            entity.hurt(entity.damageSources().magic(), 1);
            level.playSound(null, entity, SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.PLAYERS, .5F, .7F);
        }
        if (effect.getDuration() == 1) level.playSound(null, entity, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, .5F, 1.2F);
        return true;
    }
}
