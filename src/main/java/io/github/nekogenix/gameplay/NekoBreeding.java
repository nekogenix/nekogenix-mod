package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.state.BreedingState;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class NekoBreeding {
    public static boolean canFeed(ServerPlayer player) {
        return player.getData(Content.NEKO).isCat() && player.getData(Content.BREEDING).canFeed();
    }

    public static void feed(ServerPlayer player, UUID feeder) {
        if (!canFeed(player)) return;
        player.getData(Content.BREEDING).feed(feeder);
        hearts(player);
        player.level().playSound(null, player, SoundEvents.CAT_PURR, SoundSource.PLAYERS, 0.6F, 1F);
    }

    private static boolean ready(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.getData(Content.NEKO).isCat()
                && player.getData(Content.BREEDING).inLove();
    }

    private static boolean available(ServerPlayer player, UUID partner) {
        var state = player.getData(Content.BREEDING);
        return ready(player) && (state.partner == null || state.partner.equals(partner));
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void hurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasData(Content.BREEDING)) {
            player.getData(Content.BREEDING).clearLove();
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.hasData(Content.BREEDING)) return;
        var state = player.getData(Content.BREEDING);
        state.tick();
        if (!ready(player)) { state.clearLove(); return; }
        if (player.tickCount % 10 == 0) hearts(player);
        if (state.partner != null) {
            var entity = player.serverLevel().getEntity(state.partner);
            if (entity instanceof Cat cat && cat.isAlive() && cat.isInLove() && cat.distanceToSqr(player) <= 64) return;
            if (!(entity instanceof ServerPlayer partner) || !available(partner, player.getUUID())
                    || player.distanceToSqr(partner) > 64) {
                state.partner = null;
                state.courtship = 0;
                return;
            }
            if (player.getUUID().compareTo(partner.getUUID()) > 0) return;
            state.courtship++;
            if (state.courtship >= 60 && player.distanceToSqr(partner) < 9) breed(player, partner, null);
        } else if (player.tickCount % 10 == 0) {
            var partner = player.serverLevel().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(8),
                    other -> other != player && available(other, player.getUUID()) && player.distanceToSqr(other) <= 64)
                    .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
            if (partner != null) {
                state.partner = partner.getUUID();
                partner.getData(Content.BREEDING).partner = player.getUUID();
            }
        }
    }

    private static void breed(ServerPlayer player, ServerPlayer partner, Cat cat) {
        if (!ready(player) || partner != null && !ready(partner)
                || cat != null && (!catReady(cat) || cat.level() != player.level())) return;
        var level = player.serverLevel();
        Cat kitten = cat == null ? EntityType.CAT.create(level) : cat.getBreedOffspring(level, cat);
        if (kitten == null) return;
        if (cat == null) {
            BuiltInRegistries.CAT_VARIANT.getRandom(level.random).ifPresent(kitten::setVariant);
            kitten.setOwnerUUID(player.getUUID());
            kitten.setTame(true, true);
        }
        kitten.setBaby(true);
        kitten.moveTo(cat == null ? player.position() : cat.position());
        UUID cause = player.getData(Content.BREEDING).cause();
        if (cause == null && partner != null) cause = partner.getData(Content.BREEDING).cause();
        var feeder = cause == null ? null : level.getPlayerByUUID(cause);
        player.getData(Content.BREEDING).finish();
        if (partner != null) partner.getData(Content.BREEDING).finish();
        if (cat != null) { cat.setAge(BreedingState.COOLDOWN_TICKS); cat.resetLove(); }
        if (!level.addFreshEntity(kitten)) return;
        if (feeder != null) feeder.awardStat(Stats.ANIMALS_BRED);
        hearts(player);
        if (partner != null) hearts(partner);
        level.playSound(null, kitten, SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 0.8F, 1.3F);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(level, kitten.getX(), kitten.getY(), kitten.getZ(), level.random.nextInt(7) + 1));
        }
    }

    private static boolean catReady(Cat cat) {
        return cat.isAlive() && cat.isTame() && cat.getAge() == 0 && cat.isInLove()
                && !cat.isOrderedToSit() && !cat.isPanicking();
    }

    private static void hearts(ServerPlayer player) {
        player.serverLevel().sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1,
                player.getZ(), 3, 0.35, 0.4, 0.35, 0.02);
    }

    public static final class CatPartnerGoal extends Goal {
        private final Cat cat;
        private ServerPlayer partner;
        private int courtship;

        public CatPartnerGoal(Cat cat) {
            this.cat = cat;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!catReady(cat)) return false;
            partner = cat.level().getEntitiesOfClass(ServerPlayer.class, cat.getBoundingBox().inflate(8),
                    player -> available(player, cat.getUUID()) && cat.distanceToSqr(player) <= 64)
                    .stream().min(Comparator.comparingDouble(cat::distanceToSqr)).orElse(null);
            return partner != null;
        }

        @Override
        public boolean canContinueToUse() {
            return catReady(cat) && partner != null && partner.level() == cat.level()
                    && available(partner, cat.getUUID()) && cat.distanceToSqr(partner) <= 64;
        }

        @Override
        public void start() {
            courtship = 0;
            partner.getData(Content.BREEDING).partner = cat.getUUID();
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            cat.getLookControl().setLookAt(partner, 10, cat.getMaxHeadXRot());
            cat.getNavigation().moveTo(partner, Cat.WALK_SPEED_MOD);
            if (++courtship >= 60 && cat.distanceToSqr(partner) < 9) breed(partner, null, cat);
        }

        @Override
        public void stop() {
            if (partner != null && cat.getUUID().equals(partner.getData(Content.BREEDING).partner)) {
                partner.getData(Content.BREEDING).partner = null;
            }
            partner = null;
            courtship = 0;
            cat.getNavigation().stop();
        }
    }

    private NekoBreeding() {}
}
