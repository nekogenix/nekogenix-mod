package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class FelineWetness {
    public static final int DRYING_TICKS = 200;
    public static boolean exposed(Player player) {
        var level = player.level();
        if (!level.isRaining() || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return false;
        var head = BlockPos.containing(player.getX(), player.getBoundingBox().maxY - .001, player.getZ());
        return level.hasChunkAt(head) && level.isRainingAt(head);
    }

    public static boolean update(ServerPlayer player) {
        var state = player.getData(Content.NEKO);
        if (!player.isAlive() || player.isSpectator() || !state.isNeko()) {
            if (player.hasEffect(Content.HEAVY_EARS)) player.removeEffect(Content.HEAVY_EARS);
            if (player.hasEffect(Content.HEAVY_FUR)) player.removeEffect(Content.HEAVY_FUR);
            return false;
        }
        Holder<MobEffect> effect = state.isCat() ? Content.HEAVY_FUR : Content.HEAVY_EARS;
        Holder<MobEffect> other = state.isCat() ? Content.HEAVY_EARS : Content.HEAVY_FUR;
        var current = player.getEffect(effect);
        var previous = player.getEffect(other);
        if (player.isInWaterOrBubble() || exposed(player)) set(player, effect, -1);
        else if (current != null && current.isInfiniteDuration()) set(player, effect, DRYING_TICKS);
        else if (current == null && previous != null) {
            set(player, effect, previous.isInfiniteDuration() ? DRYING_TICKS : previous.getDuration());
        }
        if (previous != null) player.removeEffect(other);
        return active(player);
    }

    public static boolean active(LivingEntity entity) {
        return entity.hasEffect(Content.HEAVY_EARS) || entity.hasEffect(Content.HEAVY_FUR);
    }

    public static boolean isWetEffect(MobEffectInstance instance) {
        return instance.is(Content.HEAVY_EARS) || instance.is(Content.HEAVY_FUR);
    }

    private static void set(ServerPlayer player, Holder<MobEffect> effect, int duration) {
        var current = player.getEffect(effect);
        if (current != null && current.getDuration() == duration) return;
        var replacement = new MobEffectInstance(effect, duration, 0, false, false, true);
        if (current == null) player.addEffect(replacement);
        else player.forceAddEffect(replacement, null);
    }

    private FelineWetness() {}
}
