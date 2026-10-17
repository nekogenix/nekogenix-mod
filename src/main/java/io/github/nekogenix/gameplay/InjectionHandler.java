package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class InjectionHandler {
    public static final int FORWARD_TICKS = 80;
    public static final int RETURN_TICKS = 12;
    public static final int TOTAL_TICKS = FORWARD_TICKS + RETURN_TICKS;

    public static final class Session {
        private ItemStack stack = ItemStack.EMPTY;
        private int slot;
        private int elapsed;
        private long lastInput;
        private boolean held;
        private boolean active;
        private boolean waitingForRelease;
    }

    public static void input(ServerPlayer player, boolean held) {
        Session session = player.getData(Content.INJECTION_SESSION);
        session.held = held;
        session.lastInput = player.level().getGameTime();
        if (!held) {
            session.waitingForRelease = false;
            if (session.active && session.elapsed < FORWARD_TICKS) cancel(player, session);
            return;
        }
        if (session.active || session.waitingForRelease || !validPlayer(player)
                || player.getData(Content.NEKO).phase() != NekoState.Phase.HUMAN
                || !player.getMainHandItem().is(Content.INJECTION)) return;
        session.stack = player.getMainHandItem();
        session.slot = player.getInventory().selected;
        session.elapsed = 0;
        session.active = true;
        player.setData(Content.INJECTION_STARTED, player.level().getGameTime());
        sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.45F, 1.4F);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = player.getData(Content.INJECTION_SESSION);
        if (!session.active) return;
        if (!validPlayer(player) || player.getInventory().selected != session.slot
                || player.getMainHandItem() != session.stack || !session.stack.is(Content.INJECTION)
                || (session.elapsed < FORWARD_TICKS && (!session.held
                    || player.level().getGameTime() - session.lastInput > 30))) {
            cancel(player, session);
            return;
        }
        session.elapsed++;
        if (session.elapsed < FORWARD_TICKS && session.elapsed % 20 == 0) {
            sound(player, SoundEvents.HONEY_BLOCK_SLIDE, 0.45F, 1.4F - session.elapsed / 160F);
        }
        if (session.elapsed == FORWARD_TICKS) sound(player, SoundEvents.BOTTLE_EMPTY, 0.7F, 1.25F);
        if (session.elapsed < TOTAL_TICKS) return;
        if (player.getData(Content.NEKO).begin()) {
            FelineSounds.sync(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.USED_SYRINGE.toStack());
            player.hurt(player.damageSources().generic(), 1.0F);
            TransformationHandler.refreshEffects(player);
            player.serverLevel().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1,
                    player.getZ(), 15, 0.3, 0.5, 0.3, 0.03);
            sound(player, SoundEvents.CAT_PURREOW, 0.9F, 1.0F);
        }
        cancel(player, session);
        session.waitingForRelease = true;
    }

    private static boolean validPlayer(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isUsingItem();
    }

    private static void cancel(ServerPlayer player, Session session) {
        session.active = false;
        session.stack = ItemStack.EMPTY;
        player.setData(Content.INJECTION_STARTED, -1L);
    }

    static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private InjectionHandler() {}
}
