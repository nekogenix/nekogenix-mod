package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatInteractions {
    private static final String FLEE_UNTIL = "nekogenix_flee_until";
    private static final String FLEE_FROM = "nekogenix_flee_from";

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Cat cat) || !event.getItemStack().is(Content.COTTON_SWAB)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide || !cat.isAlive() || !cat.isTame()) return;
        Player player = event.getEntity();
        if (cat.isOwnedBy(player)) {
            if (cat.getPersistentData().getLong(FLEE_UNTIL) > cat.level().getGameTime()) return;
            event.getItemStack().consume(1, player);
            var sample = Content.SALIVA_SWAB.toStack();
            if (!player.addItem(sample)) player.drop(sample, false);
            cat.level().playSound(null, cat.blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.0F, 1.1F);
        } else if (cat.getPersistentData().getLong(FLEE_UNTIL) <= cat.level().getGameTime()) {
            cat.getPersistentData().putLong(FLEE_UNTIL, cat.level().getGameTime() + 120);
            cat.getPersistentData().putUUID(FLEE_FROM, player.getUUID());
            cat.doHurtTarget(player);
            cat.setTarget(null);
            cat.setInSittingPose(false);
            cat.level().playSound(null, cat.blockPosition(), SoundEvents.CAT_HISS, SoundSource.NEUTRAL, 1F, 1.1F);
        }
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof Creeper creeper) {
            creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>(creeper, Player.class, 6F, 1.0, 1.2, TransformationHandler::isNeko));
        }
        if (event.getEntity() instanceof Cat cat) {
            cat.goalSelector.addGoal(0, new AvoidEntityGoal<Player>(cat, Player.class, 16F, 1.8, 1.8,
                    entity -> fleeing(cat, entity)) {
                @Override
                public boolean canUse() {
                    return cat.getPersistentData().getLong(FLEE_UNTIL) > cat.level().getGameTime() && super.canUse();
                }

                @Override
                public boolean canContinueToUse() {
                    return cat.getPersistentData().getLong(FLEE_UNTIL) > cat.level().getGameTime() && super.canContinueToUse();
                }

                @Override
                public void start() { cat.setInSittingPose(false); super.start(); }

                @Override
                public void stop() {
                    super.stop();
                    cat.setInSittingPose(cat.isOrderedToSit());
                }
            });
        }
    }

    private static boolean fleeing(Cat cat, LivingEntity entity) {
        var tag = cat.getPersistentData();
        return tag.getLong(FLEE_UNTIL) > cat.level().getGameTime() && tag.hasUUID(FLEE_FROM)
                && tag.getUUID(FLEE_FROM).equals(entity.getUUID());
    }

    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Creeper && event.getNewAboutToBeSetTarget() != null
                && TransformationHandler.isNeko(event.getNewAboutToBeSetTarget())) event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Creeper creeper
                && creeper.getTarget() != null && TransformationHandler.isNeko(creeper.getTarget())) {
            creeper.setTarget(null);
        }
    }

    private CatInteractions() {}
}
