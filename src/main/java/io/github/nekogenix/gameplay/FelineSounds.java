package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineSounds {
    public static void sync(Player player) {
        boolean active = player.getData(Content.NEKO).phase() != NekoState.Phase.HUMAN;
        if (player.getData(Content.FELINE_VOICE) != active) player.setData(Content.FELINE_VOICE, active);
    }

    @SubscribeEvent
    public static void voice(PlayLevelSoundEvent event) {
        if (event.getSource() != SoundSource.PLAYERS || event.getSound() == null) return;
        SoundEvent replacement = replacement(event.getSound().value());
        if (replacement == null) return;
        Player speaker = null;
        if (event instanceof PlayLevelSoundEvent.AtEntity atEntity && atEntity.getEntity() instanceof Player player) {
            speaker = player;
        } else if (event instanceof PlayLevelSoundEvent.AtPosition atPosition && !event.getLevel().isClientSide) {
            // Player.playSound uses a position event on the server. Only match its exact coordinates.
            var position = atPosition.getPosition();
            var players = event.getLevel().getEntitiesOfClass(Player.class,
                    new AABB(position, position).inflate(0.01), player -> player.position().equals(position));
            if (players.size() == 1) speaker = players.getFirst();
        }
        if (speaker != null && speaker.getData(Content.FELINE_VOICE)) {
            event.setSound(Holder.direct(replacement));
        }
    }

    private static SoundEvent replacement(SoundEvent sound) {
        if (sound == SoundEvents.PLAYER_HURT || sound == SoundEvents.PLAYER_HURT_DROWN
                || sound == SoundEvents.PLAYER_HURT_FREEZE || sound == SoundEvents.PLAYER_HURT_ON_FIRE
                || sound == SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH
                || sound == SoundEvents.PLAYER_SMALL_FALL || sound == SoundEvents.PLAYER_BIG_FALL) return SoundEvents.CAT_HURT;
        if (sound == SoundEvents.PLAYER_DEATH) return SoundEvents.CAT_DEATH;
        if (sound == SoundEvents.GENERIC_EAT || sound == SoundEvents.GENERIC_DRINK) return SoundEvents.CAT_EAT;
        if (sound == SoundEvents.PLAYER_BURP || sound == SoundEvents.PLAYER_BREATH) return SoundEvents.CAT_PURREOW;
        return null;
    }

    @SubscribeEvent
    public static void ambient(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()
                || !player.getData(Content.FELINE_VOICE) || player.tickCount % 120 != 0
                || player.getRandom().nextBoolean()) return;
        SoundEvent sound;
        if (player.getFoodData().getFoodLevel() < 10) sound = SoundEvents.CAT_BEG_FOR_FOOD;
        else if (player.isSleeping() || player.isCrouching()) sound = SoundEvents.CAT_PURR;
        else sound = switch (player.getRandom().nextInt(4)) {
            case 0 -> SoundEvents.CAT_STRAY_AMBIENT;
            case 1 -> SoundEvents.CAT_PURREOW;
            case 2 -> SoundEvents.CAT_PURR;
            default -> SoundEvents.CAT_AMBIENT;
        };
        play(player, sound, 0.5F);
    }

    @SubscribeEvent
    public static void hiss(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player.getData(Content.FELINE_VOICE)
                && player.getAttackStrengthScale(0) >= 0.9F) play(player, SoundEvents.CAT_HISS, 0.6F);
    }

    private static void play(Player player, SoundEvent sound, float volume) {
        player.level().playSound(null, player, sound, SoundSource.PLAYERS,
                volume, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    private FelineSounds() {}
}
