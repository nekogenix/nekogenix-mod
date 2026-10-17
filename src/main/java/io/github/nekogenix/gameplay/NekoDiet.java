package io.github.nekogenix.gameplay;

import io.github.nekogenix.Nekogenix;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class NekoDiet {
    private record Rule(int nutrition, float saturationScale, Item cookedEquivalent) {}
    private record Meal(Item item, int food, float saturation, long tick) {}
    private record CakeMeal(int slices, int food, float saturation, long tick) {}
    private static final Map<Item, Rule> RULES = rules();
    private static final Map<ServerPlayer, Meal> MEALS = new WeakHashMap<>();
    private static final Map<ServerPlayer, CakeMeal> CAKES = new WeakHashMap<>();

    private static Map<Item, Rule> rules() {
        Map<Item, Rule> rules = new HashMap<>();
        put(rules, 2, .5F, Items.APPLE, Items.BREAD, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
                Items.BAKED_POTATO, Items.PUMPKIN_PIE, Items.CHORUS_FRUIT, Items.BEETROOT_SOUP, Items.HONEY_BOTTLE);
        put(rules, 3, .5F, Items.MUSHROOM_STEW, Items.GOLDEN_CARROT, Items.SUSPICIOUS_STEW);
        put(rules, 1, .5F, Items.TROPICAL_FISH, Items.COOKIE, Items.MELON_SLICE, Items.DRIED_KELP,
                Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.CARROT, Items.POTATO, Items.POISONOUS_POTATO,
                Items.BEETROOT, Items.SWEET_BERRIES, Items.GLOW_BERRIES);
        put(rules, 1, 1F, Items.PUFFERFISH);
        put(rules, 7, .75F, Items.RABBIT_STEW);
        raw(rules, Items.PORKCHOP, Items.COOKED_PORKCHOP, 8);
        raw(rules, Items.BEEF, Items.COOKED_BEEF, 8);
        raw(rules, Items.CHICKEN, Items.COOKED_CHICKEN, 6);
        raw(rules, Items.MUTTON, Items.COOKED_MUTTON, 6);
        raw(rules, Items.RABBIT, Items.COOKED_RABBIT, 5);
        raw(rules, Items.COD, Items.COOKED_COD, 5);
        raw(rules, Items.SALMON, Items.COOKED_SALMON, 6);
        return Map.copyOf(rules);
    }

    private static void put(Map<Item, Rule> map, int nutrition, float saturation, Item... items) {
        for (Item item : items) map.put(item, new Rule(nutrition, saturation, null));
    }

    private static void raw(Map<Item, Rule> map, Item raw, Item cooked, int nutrition) {
        map.put(raw, new Rule(nutrition, 1, cooked));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforeEating(LivingEntityUseItemEvent.Tick event) {
        if (event.getDuration() > 1 || !(event.getEntity() instanceof ServerPlayer player)
                || !TransformationHandler.isNeko(player) || !RULES.containsKey(event.getItem().getItem())) return;
        MEALS.put(player, new Meal(event.getItem().getItem(), player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(), player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void finish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (stack.is(Items.MILK_BUCKET)) {
            if (TransformationHandler.isNeko(player)) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            TransformationHandler.refreshEffects(player);
            return;
        }
        if (!TransformationHandler.isNeko(player)) return;
        Meal meal = MEALS.remove(player);
        Rule rule = RULES.get(stack.getItem());
        FoodProperties food = stack.getFoodProperties(player);
        if (rule != null && food != null && meal != null && meal.item() == stack.getItem()
                && meal.tick() == player.level().getGameTime()) {
            float saturation = food.saturation() * rule.saturationScale();
            if (rule.cookedEquivalent() != null) {
                FoodProperties cooked = rule.cookedEquivalent().getDefaultInstance().getFoodProperties(player);
                if (cooked != null) saturation = cooked.saturation();
            }
            adjust(player, meal.food(), meal.saturation(), food.nutrition(), food.saturation(), rule.nutrition(), saturation);
        }
        if (stack.is(Items.COOKIE)) poison(player, 300, 0);
        else if (stack.is(Items.ROTTEN_FLESH)) poison(player, 160, 0);
        else if (stack.is(Items.POISONOUS_POTATO)) poison(player, 200, 1);
    }

    private static void poison(ServerPlayer player, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
    }

    @SubscribeEvent
    public static void potatoPoison(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player && TransformationHandler.isNeko(player)
                && player.isUsingItem() && player.getUseItem().is(Items.POISONOUS_POTATO)
                && player.getUseItemRemainingTicks() <= 1 && event.getEffectInstance().is(MobEffects.POISON)
                && event.getEffectInstance().getAmplifier() == 0 && event.getEffectInstance().getDuration() == 100
                && event.getEffectSource() == null) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void cakeInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !TransformationHandler.isNeko(player)) return;
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof CakeBlock) && !(block instanceof CandleCakeBlock)) return;
        // Observe vanilla's successful slice consumption without replacing cake, candle or item interactions.
        CAKES.putIfAbsent(player, new CakeMeal(player.getStats().getValue(Stats.CUSTOM, Stats.EAT_CAKE_SLICE),
                player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(), player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void afterTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CakeMeal cake = CAKES.remove(player);
        if (cake != null && TransformationHandler.isNeko(player)) {
            int count = player.getStats().getValue(Stats.CUSTOM, Stats.EAT_CAKE_SLICE) - cake.slices();
            if (count > 0 && player.level().getGameTime() - cake.tick() <= 1) {
                adjust(player, cake.food(), cake.saturation(), 2 * count, .4F * count, count, 0);
            }
        }
        Meal meal = MEALS.get(player);
        if (meal != null && player.level().getGameTime() > meal.tick()) MEALS.remove(player);
    }

    private static void adjust(ServerPlayer player, int beforeFood, float beforeSat,
            int vanillaFood, float vanillaSat, int nekoFood, float nekoSat) {
        var data = player.getFoodData();
        int expectedFood = Math.min(20, beforeFood + vanillaFood);
        int desiredFood = Math.min(20, beforeFood + nekoFood);
        float expectedSat = Math.min(expectedFood, beforeSat + vanillaSat);
        float desiredSat = Math.min(desiredFood, beforeSat + nekoSat);
        data.setFoodLevel(Mth.clamp(data.getFoodLevel() + desiredFood - expectedFood, 0, 20));
        data.setSaturation(Mth.clamp(data.getSaturationLevel() + desiredSat - expectedSat, 0, data.getFoodLevel()));
    }

    private NekoDiet() {}
}
