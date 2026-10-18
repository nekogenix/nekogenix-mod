package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class PlayerFeeding {
    @SubscribeEvent
    public static void feed(PlayerInteractEvent.EntityInteract event) {
        Player feeder = event.getEntity();
        if (!(event.getTarget() instanceof Player target) || target == feeder || !target.isAlive()
                || target.isSpectator() || !feeder.isAlive() || feeder.isSpectator()
                || !event.getItemStack().is(ItemTags.CAT_FOOD)) return;
        boolean feline = event.getLevel().isClientSide ? target.getData(Content.FELINE_VOICE)
                : target.getData(Content.NEKO).phase() != io.github.nekogenix.state.NekoState.Phase.HUMAN;
        if (!feline) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(target instanceof ServerPlayer player) || !(player.getFoodData().needsFood() || NekoBreeding.canFeed(player))
                || player.isUsingItem() || !feeder.canInteractWithEntity(player, 0)) return;
        var offered = event.getItemStack();
        var food = offered.getFoodProperties(player);
        if (food == null) return;
        int beforeFood = player.getFoodData().getFoodLevel();
        float beforeSaturation = player.getFoodData().getSaturationLevel();
        var meal = offered.copyWithCount(1);
        var result = player.eat(player.level(), meal, food);
        if (!player.hasInfiniteMaterials() && !result.isEmpty() && !player.addItem(result)) player.drop(result, false);
        NekoDiet.adjustMeal(player, offered, beforeFood, beforeSaturation);
        NekoBreeding.feed(player, feeder.getUUID());
        player.serverLevel().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, offered.copyWithCount(1)),
                player.getX(), player.getEyeY() - 0.2, player.getZ(), 6, 0.15, 0.1, 0.15, 0.02);
        player.level().playSound(null, player, SoundEvents.CAT_EAT, SoundSource.PLAYERS, 0.8F, 1F);
        offered.consume(1, feeder);
    }

    private PlayerFeeding() {}
}
