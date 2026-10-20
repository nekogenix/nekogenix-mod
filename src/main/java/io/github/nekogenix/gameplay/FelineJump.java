package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.state.NekoState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineJump {
    public static final class Session {
        private boolean ready, used;
    }

    private static boolean eligible(Player player) {
        boolean agility = player.level().isClientSide ? player.hasEffect(Content.AGILITY)
                : player.getData(Content.NEKO).has(NekoState.AGILITY);
        return agility && player.isAlive() && !player.isSpectator() && !player.getAbilities().flying
                && (player.getAbilities().mayfly || player.getFoodData().getFoodLevel() > 6)
                && !player.isPassenger() && !player.isFallFlying() && !player.onClimbable()
                && !player.isInWaterOrBubble() && !player.isInLava();
    }

    @SubscribeEvent
    public static void groundJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && player.onGround() && eligible(player)) {
            player.getData(Content.FELINE_JUMP).ready = true;
            player.getData(Content.FELINE_JUMP).used = false;
        }
    }

    public static boolean jump(Player player) {
        Session session = player.getData(Content.FELINE_JUMP);
        if (!session.ready || player.onGround() || !eligible(player)) return false;
        session.ready = false;
        session.used = true;
        var velocity = player.getDeltaMovement();
        double power = player.getAttributeValue(Attributes.JUMP_STRENGTH) + Math.max(.2F, player.getJumpBoostPower());
        player.setDeltaMovement(velocity.x, power, velocity.z);
        player.fallDistance = 0;
        player.hasImpulse = true;
        if (player instanceof ServerPlayer) {
            player.level().playSound(null, player, SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, .3F, 1.25F);
        }
        return true;
    }

    public static boolean canStartFlight(Player player) {
        return !player.onGround() && eligible(player) && player.getData(Content.FELINE_JUMP).used;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.hasData(Content.FELINE_JUMP) && (player.onGround() || !eligible(player))) {
            player.getData(Content.FELINE_JUMP).ready = false;
            player.getData(Content.FELINE_JUMP).used = false;
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().getData(Content.FELINE_JUMP).ready = false;
        event.getEntity().getData(Content.FELINE_JUMP).used = false;
    }

    private FelineJump() {}
}
