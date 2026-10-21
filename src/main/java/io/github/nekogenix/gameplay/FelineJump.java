package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.integration.sable.FelineOrientation;
import io.github.nekogenix.state.NekoState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.StatAwardEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineJump {
    private static final int MAX_AIR_TICKS = 20;

    public static final class Session {
        private boolean ready, used;
        private int pending = -1, grounded = -1, airborne = -1;
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
            arm(player);
        }
    }

    @SubscribeEvent
    public static void jumpStat(StatAwardEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer) || event.getStat() != Stats.CUSTOM.get(Stats.JUMP)
                || event.getValue() <= 0 || !eligible(player)) return;
        Session session = player.getData(Content.FELINE_JUMP);
        if (player.onGround() || !session.used && session.grounded >= 0 && player.tickCount - session.grounded <= 4) arm(player);
    }

    public static void groundInput(Player player) {
        if (player.level().isClientSide && player.onGround() && eligible(player)) {
            Session session = player.getData(Content.FELINE_JUMP);
            if (session.pending < 0) session.pending = player.tickCount;
        }
    }

    private static void arm(Player player) {
        Session session = player.getData(Content.FELINE_JUMP);
        session.ready = true;
        session.used = false;
        session.pending = -1;
        if (session.airborne < 0 || player.onGround() && player.verticalCollisionBelow) session.airborne = player.tickCount;
    }

    public static boolean jump(Player player) {
        Session session = player.getData(Content.FELINE_JUMP);
        if (expired(player, session)) session.ready = false;
        if (!session.ready || player.onGround() || !eligible(player)) return false;
        session.ready = false;
        session.used = true;
        var velocity = player.getDeltaMovement();
        double power = player.getAttributeValue(Attributes.JUMP_STRENGTH) + Math.max(.2F, player.getJumpBoostPower());
        var up = FelineOrientation.up(player);
        player.setDeltaMovement(velocity.add(up.scale(power - velocity.dot(up))));
        player.fallDistance = 0;
        player.hasImpulse = true;
        return true;
    }

    public static boolean canStartFlight(Player player) {
        return !player.onGround() && eligible(player) && player.getData(Content.FELINE_JUMP).used;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean eligible = eligible(player);
        if (!player.hasData(Content.FELINE_JUMP) && !eligible) return;
        Session session = player.getData(Content.FELINE_JUMP);
        if (player.onGround() && player.verticalCollisionBelow) session.airborne = -1;
        else if (session.airborne < 0) session.airborne = player.tickCount;
        if (!eligible) {
            reset(session);
        } else if (session.pending >= 0 && player.tickCount - session.pending <= 4
                && player.getDeltaMovement().dot(FelineOrientation.up(player)) > 0) {
            if (!player.onGround()) arm(player);
        } else if (player.onGround()) {
            reset(session);
        } else {
            session.pending = -1;
        }
        if (eligible && player.onGround()) session.grounded = player.tickCount;
        if (expired(player, session)) session.ready = false;
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Session session = event.getEntity().getData(Content.FELINE_JUMP);
        reset(session);
        session.airborne = -1;
    }

    private static boolean expired(Player player, Session session) {
        return session.airborne >= 0 && player.tickCount - session.airborne > MAX_AIR_TICKS;
    }

    private static void reset(Session session) {
        session.ready = session.used = false;
        session.pending = session.grounded = -1;
    }

    private FelineJump() {}
}
