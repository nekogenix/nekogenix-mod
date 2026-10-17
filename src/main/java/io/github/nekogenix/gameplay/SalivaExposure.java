package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.state.NekoState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class SalivaExposure {
    public static void schedule(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return;
        long due = level.getServer().overworld().getGameTime() + NekoState.DAY;
        long existing = entity.getData(Content.EXPOSURE);
        if (existing < 0 || due < existing) entity.setData(Content.EXPOSURE, due);
    }

    private static void tick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.hasData(Content.EXPOSURE)) return;
        long due = entity.getData(Content.EXPOSURE);
        if (due >= 0 && entity.isAlive() && level.getServer().overworld().getGameTime() >= due) {
            entity.removeData(Content.EXPOSURE);
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200, 0));
            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) tick(living);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) { tick(event.getEntity()); }

    @SubscribeEvent
    public static void brewing(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(Ingredient.of(Items.GLASS_BOTTLE), Ingredient.of(Content.SALIVA_SWAB.get()), Content.SALIVA_BOTTLE.toStack());
        event.getBuilder().addRecipe(Ingredient.of(Content.SALIVA_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), Content.SPLASH_SALIVA.toStack());
    }

    private SalivaExposure() {}
}
