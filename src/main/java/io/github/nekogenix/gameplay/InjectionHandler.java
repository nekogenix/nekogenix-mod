package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class InjectionHandler {
    public static final int APPROACH_TICKS = 10;
    public static final int NEEDLE_ENTRY_TICKS = 12;
    public static final int FORWARD_TICKS = 80;
    public static final int RETURN_TICKS = 12;
    public static final int TOTAL_TICKS = FORWARD_TICKS + RETURN_TICKS;
    private static final ResourceKey<DamageType> NEEDLE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Content.id("injection"));

    public record ReturnAnimation(long started, int progress) {
        public static final ReturnAnimation NONE = new ReturnAnimation(-1, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, ReturnAnimation> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, ReturnAnimation::started,
                ByteBufCodecs.VAR_INT, ReturnAnimation::progress, ReturnAnimation::new);

        public float remaining(long now, float partial) {
            return started < 0 ? 0 : net.minecraft.util.Mth.clamp(1 - (now - started + partial) / RETURN_TICKS, 0, 1);
        }
    }

    public static final class Session {
        private final Dose main = new Dose();
        private final Dose off = new Dose();
        private boolean paired;
        private NekoState transformation;

        private Dose dose(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? main : off; }
        private Dose other(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? off : main; }
    }

    private static final class Dose {
        private ItemStack stack = ItemStack.EMPTY;
        private int slot;
        private int elapsed;
        private long lastInput;
        private ResourceKey<net.minecraft.world.level.Level> dimension;
        private boolean held;
        private boolean active;
        private boolean returning;
        private boolean waitingForRelease;
        private boolean completed;
    }

    public static void input(ServerPlayer player, boolean held) {
        input(player, held, false);
    }

    public static void input(ServerPlayer player, boolean held, boolean offHeld) {
        updateInput(player, InteractionHand.MAIN_HAND, held);
        updateInput(player, InteractionHand.OFF_HAND, offHeld);
    }

    private static void updateInput(ServerPlayer player, InteractionHand hand, boolean held) {
        Session session = player.getData(Content.INJECTION_SESSION);
        Dose dose = session.dose(hand);
        Dose other = session.other(hand);
        dose.held = held;
        dose.lastInput = player.level().getGameTime();
        if (!held) {
            dose.waitingForRelease = false;
            if (dose.active && !dose.returning && dose.elapsed < FORWARD_TICKS) startReturn(player, hand, session);
            return;
        }
        if (dose.active || dose.waitingForRelease || !validPlayer(player)
                || !canInject(player, player.getItemInHand(hand))
                || other.active && (!canDual(player) || other.returning)) return;
        dose.stack = player.getItemInHand(hand);
        dose.slot = player.getInventory().selected;
        dose.dimension = player.level().dimension();
        dose.elapsed = 0;
        dose.active = true;
        dose.returning = dose.completed = false;
        session.paired = other.active && canDual(player);
        setStarted(player, hand, player.level().getGameTime());
        sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.45F, 1.4F);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = player.getData(Content.INJECTION_SESSION);
        tickHand(player, InteractionHand.MAIN_HAND, session);
        tickHand(player, InteractionHand.OFF_HAND, session);
        if (!session.main.active && !session.off.active) {
            session.paired = session.main.completed = session.off.completed = false;
            session.transformation = null;
        }
    }

    private static void tickHand(ServerPlayer player, InteractionHand hand, Session session) {
        Dose dose = session.dose(hand);
        if (!dose.active) return;
        if (!validPlayer(player) || player.getInventory().selected != dose.slot
                || player.level().dimension() != dose.dimension
                || player.getItemInHand(hand) != dose.stack || !isInjection(dose.stack)) {
            cancel(player, hand, session);
            return;
        }
        if (!dose.returning && dose.elapsed < FORWARD_TICKS && (!dose.held
                || player.level().getGameTime() - dose.lastInput > 30)) startReturn(player, hand, session);
        if (dose.returning) {
            var animation = returning(player, hand);
            float remaining = animation.remaining(player.level().getGameTime(), 0);
            if (remaining <= 0) {
                cancel(player, hand, session);
                dose.waitingForRelease = dose.held;
            } else if ((player.level().getGameTime() - animation.started()) % 4 == 0) {
                sound(player, SoundEvents.HONEY_BLOCK_SLIDE, 0.35F * remaining,
                        1.4F - animation.progress() * remaining / 160F);
            }
            return;
        }
        dose.elapsed = (int)Math.clamp(player.level().getGameTime() - started(player, hand), 0, TOTAL_TICKS);
        if (dose.elapsed == NEEDLE_ENTRY_TICKS) {
            sound(player, SoundEvents.HONEY_BLOCK_HIT, .25F, 1.7F);
            player.hurt(new net.minecraft.world.damagesource.DamageSource(
                    player.registryAccess().holderOrThrow(NEEDLE_DAMAGE)), 1.0F);
            if (!player.isAlive()) {
                cancel(player, hand, session);
                return;
            }
        }
        if (dose.elapsed > NEEDLE_ENTRY_TICKS && dose.elapsed < FORWARD_TICKS && dose.elapsed % 20 == 0) {
            sound(player, SoundEvents.HONEY_BLOCK_SLIDE, 0.45F, 1.4F - dose.elapsed / 160F);
        }
        if (dose.elapsed == FORWARD_TICKS) sound(player, SoundEvents.BOTTLE_EMPTY, 0.7F, 1.25F);
        if (dose.elapsed < TOTAL_TICKS) return;
        boolean stabilizing = dose.stack.is(Content.STABILIZING_INJECTION);
        NekoState state = player.getData(Content.NEKO);
        boolean completed = stabilizing ? state.stabilize() : state.begin();
        if (completed && !stabilizing) session.transformation = state;
        if (!completed && !stabilizing && session.paired && session.other(hand).completed
                && state == session.transformation) completed = state.accelerate();
        if (completed) {
            if (stabilizing) player.addEffect(new net.minecraft.world.effect.MobEffectInstance(Content.CRYSTALLIZATION,
                    CrystallizationEffect.DURATION, 0, false, false, true));
            FelineSounds.sync(player);
            player.setItemInHand(hand, Content.USED_SYRINGE.toStack());
            TransformationHandler.refreshEffects(player);
            player.serverLevel().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1,
                    player.getZ(), 15, 0.3, 0.5, 0.3, 0.03);
            if (stabilizing || !session.other(hand).completed) {
                sound(player, stabilizing ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.PLAYER_BREATH, 0.9F, stabilizing ? 1.2F : 0.65F);
            }
        }
        clear(player, hand, dose);
        dose.completed = completed;
        dose.waitingForRelease = true;
    }

    public static boolean isInjection(ItemStack stack) {
        return stack.is(Content.INJECTION) || stack.is(Content.STABILIZING_INJECTION);
    }

    public static boolean isSyringe(ItemStack stack) {
        return stack.is(Content.SYRINGE) || stack.is(Content.USED_SYRINGE) || isInjection(stack);
    }

    public static boolean canDual(Player player) {
        return player.getMainHandItem().is(Content.INJECTION) && player.getOffhandItem().is(Content.INJECTION);
    }

    public static long started(LivingEntity player, InteractionHand hand) {
        return player.getData(hand == InteractionHand.MAIN_HAND ? Content.INJECTION_STARTED : Content.OFFHAND_INJECTION_STARTED);
    }

    public static ReturnAnimation returning(LivingEntity player, InteractionHand hand) {
        return player.getData(hand == InteractionHand.MAIN_HAND ? Content.INJECTION_RETURN : Content.OFFHAND_INJECTION_RETURN);
    }

    public static boolean active(LivingEntity player, InteractionHand hand) {
        long started = started(player, hand);
        return started >= 0 && player.level().getGameTime() - started < TOTAL_TICKS;
    }

    private static void setStarted(ServerPlayer player, InteractionHand hand, long time) {
        player.setData(hand == InteractionHand.MAIN_HAND ? Content.INJECTION_STARTED : Content.OFFHAND_INJECTION_STARTED, time);
    }

    private static void setReturn(ServerPlayer player, InteractionHand hand, ReturnAnimation animation) {
        player.setData(hand == InteractionHand.MAIN_HAND ? Content.INJECTION_RETURN : Content.OFFHAND_INJECTION_RETURN, animation);
    }

    private static boolean canInject(ServerPlayer player, ItemStack stack) {
        var phase = player.getData(Content.NEKO).phase();
        return stack.is(Content.INJECTION) && phase == NekoState.Phase.HUMAN
                || stack.is(Content.STABILIZING_INJECTION) && phase == NekoState.Phase.CAT_TRANSFORMING;
    }

    private static boolean validPlayer(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isUsingItem();
    }

    private static void startReturn(ServerPlayer player, InteractionHand hand, Session session) {
        Dose dose = session.dose(hand);
        dose.returning = true;
        session.paired = false;
        long now = player.level().getGameTime();
        int progress = (int) Math.clamp(now - started(player, hand), 0, FORWARD_TICKS - 1);
        setReturn(player, hand, new ReturnAnimation(now, progress));
    }

    private static void cancel(ServerPlayer player, InteractionHand hand, Session session) {
        session.paired = false;
        clear(player, hand, session.dose(hand));
    }

    private static void clear(ServerPlayer player, InteractionHand hand, Dose dose) {
        dose.active = dose.returning = false;
        dose.stack = ItemStack.EMPTY;
        setStarted(player, hand, -1L);
        setReturn(player, hand, ReturnAnimation.NONE);
    }

    static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private InjectionHandler() {}
}
