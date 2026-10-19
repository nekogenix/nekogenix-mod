package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.InjectionSounds;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.EffectCures;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class TransformationHandler {
    public static boolean isNeko(net.minecraft.world.entity.LivingEntity entity) {
        return entity instanceof Player && entity.getData(Content.NEKO).isNeko();
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        NekoState state = player.getData(Content.NEKO);
        FelineSounds.sync(player);
        if (state.tick(player.getRandom())) {
            refreshEffects(player);
            InjectionHandler.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.9F);
        }
        if (state.phase() == NekoState.Phase.SHOCK && state.elapsed() == 200) {
            int first = player.getRandom().nextInt(InjectionSounds.OCEAN.size());
            player.setData(Content.FIRST_OCEAN_SOUND, first);
            player.playNotifySound(InjectionSounds.OCEAN.get(first).get(), SoundSource.AMBIENT, 1F, 1F);
        }
        if (state.phase() == NekoState.Phase.SHOCK && state.elapsed() == 260) {
            int first = player.getData(Content.FIRST_OCEAN_SOUND);
            if (first >= 0 && first < InjectionSounds.OCEAN.size()) {
                int second = player.getRandom().nextInt(InjectionSounds.OCEAN.size() - 1);
                if (second >= first) second++;
                player.playNotifySound(InjectionSounds.OCEAN.get(second).get(), SoundSource.AMBIENT, 1F, 1F);
            }
        }
        boolean agility = state.has(NekoState.AGILITY);
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), "feline_sprint", agility && player.isSprinting());
        modifier(player.getAttribute(Attributes.SNEAKING_SPEED), "feline_sneak", agility);
        if (player.tickCount % 20 == 0) refreshEffects(player);
        if (player.tickCount % 10 == 0) vision(player, state);
    }

    public static void refreshEffects(ServerPlayer player) {
        NekoState state = player.getData(Content.NEKO);
        CatForm.sync(player);
        if (player.getData(Content.FELINE_DIET) != state.isNeko()) player.setData(Content.FELINE_DIET, state.isNeko());
        marker(player, Content.SHOCK, state.phase() == NekoState.Phase.SHOCK,
                NekoState.SHOCK_DURATION - state.elapsed());
        if (state.phase() == NekoState.Phase.SHOCK && !player.hasEffect(MobEffects.CONFUSION)) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                    NekoState.SHOCK_DURATION - state.elapsed(), 0, false, false, false));
        }
        marker(player, Content.TRANSFORMING, state.phase() == NekoState.Phase.TRANSFORMING,
                state.stageRemaining(), state.stage());
        marker(player, Content.CAT_TRANSFORMING, state.phase() == NekoState.Phase.CAT_TRANSFORMING, state.stageRemaining());
        marker(player, Content.AGILITY, state.has(NekoState.AGILITY), -1);
        marker(player, Content.VISION, state.has(NekoState.VISION), -1);
        marker(player, Content.PAWS, state.has(NekoState.PAWS), -1);
    }

    private static void marker(ServerPlayer player, Holder<MobEffect> effect, boolean enabled, int duration) {
        marker(player, effect, enabled, duration, 0);
    }

    private static void marker(ServerPlayer player, Holder<MobEffect> effect, boolean enabled, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (enabled && (current == null || current.getAmplifier() != amplifier
                || (effect.is(Content.TRANSFORMING) || effect.is(Content.CAT_TRANSFORMING)) && Math.abs(current.getDuration() - duration) > 20)) {
            if (current != null) player.removeEffect(effect);
            player.addEffect(new MobEffectInstance(effect, Math.max(-1, duration), amplifier, false, false, true));
        }
        if (!enabled && player.hasEffect(effect)) player.removeEffect(effect);
    }

    private static void modifier(AttributeInstance attribute, String name, boolean enabled) {
        if (attribute == null) return;
        var id = Content.id(name);
        if (enabled && !attribute.hasModifier(id)) {
            attribute.addTransientModifier(new AttributeModifier(id, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!enabled && attribute.hasModifier(id)) attribute.removeModifier(id);
    }

    private static void vision(ServerPlayer player, NekoState state) {
        boolean dark = state.has(NekoState.VISION) && inDarkness(player);
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (state.visionOwned && current != null && (!current.isAmbient() || current.isVisible() || current.showIcon())) {
            state.visionOwned = false;
        }
        if (!dark) {
            if (state.visionOwned) player.removeEffect(MobEffects.NIGHT_VISION);
            state.visionOwned = false;
        } else if (current == null || state.visionOwned && current.getDuration() < 260) {
            state.applyingVision = true;
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, false));
            state.applyingVision = false;
            state.visionOwned = true;
        }
    }

    public static boolean inDarkness(Player player) {
        var level = player.level();
        BlockPos origin = player.blockPosition();
        if (level.isDay() && level.canSeeSky(origin) && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, origin) > 0) return false;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-16, -16, -16), origin.offset(16, 16, 16))) {
            if (pos.distSqr(origin) > 256 || !level.hasChunkAt(pos)) continue;
            boolean dark = level.isNight()
                    ? level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) == 0
                    : level.getMaxLocalRawBrightness(pos) == 0;
            if (dark && !level.getBlockState(pos).isSolidRender(level, pos)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void effectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().is(MobEffects.NIGHT_VISION)) {
            NekoState state = player.getData(Content.NEKO);
            if (!state.applyingVision) state.visionOwned = false;
        }
    }

    @SubscribeEvent
    public static void milk(MobEffectEvent.Remove event) {
        if (event.getCure() != EffectCures.MILK || !(event.getEntity() instanceof ServerPlayer player)
                || player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN) return;
        var effect = event.getEffect();
        if (effect.is(Content.SHOCK) || effect.is(Content.TRANSFORMING) || effect.is(Content.CAT_TRANSFORMING) || effect.is(Content.AGILITY)
                || effect.is(Content.VISION) || effect.is(Content.PAWS)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fallDistance(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.getData(Content.NEKO).isNeko()) return;
        if (event.getDistance() < 10) event.setCanceled(true);
        else event.setDistance(event.getDistance() - 6);
    }

    @SubscribeEvent
    public static void fall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypes.FALL)
                && player.getData(Content.NEKO).has(NekoState.PAWS)) event.setAmount(event.getAmount() * 0.25F);
    }

    @SubscribeEvent
    public static void phantom(PlayerSpawnPhantomsEvent event) {
        if (isNeko(event.getEntity())) event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NekoState state = player.getData(Content.NEKO);
            boolean wasFeline = state.phase() != NekoState.Phase.HUMAN;
            boolean sickness = state.phase() == NekoState.Phase.SHOCK;
            if (state.isTransforming() && !state.isNeko()) state.cancelTransformation();
            else if (!player.serverLevel().getLevelData().isHardcore()) state.loseLife();
            if (wasFeline && state.phase() == NekoState.Phase.HUMAN) {
                var nausea = player.getEffect(MobEffects.CONFUSION);
                if (sickness && nausea != null && !nausea.isVisible() && !nausea.showIcon()) player.removeEffect(MobEffects.CONFUSION);
                refreshEffects(player);
                vision(player, state);
                modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), "feline_sprint", false);
                modifier(player.getAttribute(Attributes.SNEAKING_SPEED), "feline_sneak", false);
            }
        }
    }

    private TransformationHandler() {}
}
