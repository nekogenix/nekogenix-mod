package io.github.nekogenix;

import io.github.nekogenix.gameplay.FelineJump;
import io.github.nekogenix.gameplay.FelineSounds;
import io.github.nekogenix.gameplay.NekoDiet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineRefinementsTests {
    private static final Map<ServerPlayer, Integer> MEAL_FOOD = new WeakHashMap<>();
    private static final Map<ServerPlayer, List<Integer>> HISSES = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mealSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null || event.getSound().value() != SoundEvents.PLAYER_BURP) return;
        for (var player : MEAL_FOOD.keySet()) {
            if (player.level() == event.getLevel() && player.position().equals(event.getPosition())) {
                MEAL_FOOD.put(player, player.getFoodData().getFoodLevel());
            }
        }
    }

    @SubscribeEvent
    public static void hissSound(PlayLevelSoundEvent.AtEntity event) {
        if (event.getEntity() instanceof ServerPlayer player && HISSES.containsKey(player)
                && event.getSound() != null && event.getSound().value() == SoundEvents.CAT_HISS) {
            HISSES.get(player).add(player.tickCount);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void nativeFoodUsesNekoValuesAndRestoresItems(GameTestHelper helper) {
        ServerPlayer player = GameplayTests.player(helper);
        TransformationTests.setState(player, 4);
        var beef = Items.BEEF.getDefaultInstance().copyWithCount(2);
        var original = beef.get(DataComponents.FOOD);
        MEAL_FOOD.put(player, -1);
        eat(player, beef);
        helper.runAfterDelay(34, () -> {
            helper.assertTrue(MEAL_FOOD.remove(player) == 8, "Native eating already has neko nutrition before consume callbacks");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 8, "Nutrition is applied once");
            helper.assertTrue(beef.getCount() == 1 && beef.get(DataComponents.FOOD).equals(original),
                    "Uneaten food retains its original components");
            eat(player, Items.SPIDER_EYE.getDefaultInstance());
        });
        helper.runAfterDelay(68, () -> {
            helper.assertFalse(player.hasEffect(MobEffects.POISON), "Spider eye does not poison a neko");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 1, "Spider eye uses neko nutrition");
            var interrupted = Items.BEEF.getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, interrupted);
            NekoDiet.beforeEating(new LivingEntityUseItemEvent.Tick(player, interrupted, 1));
            helper.assertTrue(interrupted.getFoodProperties(player).nutrition() == 8, "Consumption receives the neko food component");
            NekoDiet.afterTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(interrupted.get(DataComponents.FOOD).equals(original), "Cancelled final ticks cannot leave altered food behind");
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new CompoundTag());
            eat(player, Items.SPIDER_EYE.getDefaultInstance());
        });
        helper.runAfterDelay(102, () -> {
            helper.assertTrue(player.hasEffect(MobEffects.POISON), "Spider eye still poisons humans");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 2, "Human nutrition remains vanilla");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void airJumpRequiresGroundJumpAndOnlyWorksOnce(GameTestHelper helper) {
        ServerPlayer player = GameplayTests.player(helper);
        TransformationTests.setState(player, 4);
        player.setOnGround(false);
        helper.assertFalse(FelineJump.jump(player), "Walking off a ledge does not create a second jump");
        player.setOnGround(true);
        player.jumpFromGround();
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - .42) < .001, "First jump keeps vanilla strength");
        player.setOnGround(false);
        player.setDeltaMovement(.73, -.2, -.41);
        player.hurtMarked = false;
        helper.assertTrue(FelineJump.jump(player), "A grounded jump unlocks one air jump");
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - .62) < .001, "Second jump has Jump Boost II strength");
        helper.assertTrue(player.getDeltaMovement().x == .73 && player.getDeltaMovement().z == -.41, "Double jumping preserves both horizontal velocity components");
        helper.assertFalse(player.hurtMarked, "A predicted jump does not overwrite the client's momentum with a server motion packet");
        helper.assertTrue(FelineJump.canStartFlight(player), "Flight becomes available after the second jump");
        helper.assertFalse(FelineJump.jump(player), "Repeated requests cannot create a third jump");
        player.setOnGround(true);
        player.jumpFromGround();
        player.setOnGround(false);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, Items.ELYTRA.getDefaultInstance());
        helper.assertTrue(FelineJump.jump(player), "Wearing elytra does not prevent the second jump");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        player.setOnGround(true);
        FelineJump.tick(new PlayerTickEvent.Post(player));
        player.jumpFromGround();
        player.setOnGround(false);
        helper.assertTrue(FelineJump.jump(player), "Landing permits another pair of jumps");
        player.setOnGround(true);
        player.jumpFromGround();
        player.setOnGround(false);
        player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new CompoundTag());
        helper.assertFalse(FelineJump.jump(player), "Losing agility rejects pending air jumps");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeJumpAndFlightStateAreSeparate(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        TransformationTests.setState(player, 4);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.getAbilities().flying = false;
        player.setOnGround(true);
        FelineJump.tick(new PlayerTickEvent.Post(player));
        helper.assertFalse(FelineJump.canStartFlight(player), "Landing clears the previous flight handoff");
        player.jumpFromGround();
        player.setOnGround(false);
        helper.assertFalse(FelineJump.canStartFlight(player), "A ground jump alone does not hand off to flight");
        helper.assertTrue(FelineJump.jump(player), "Creative players still get a second jump");
        helper.assertTrue(FelineJump.canStartFlight(player), "Creative flight can follow a completed double jump");
        player.getAbilities().flying = true;
        helper.assertFalse(FelineJump.jump(player), "Jumping while already flying remains flight input");
        FelineJump.tick(new PlayerTickEvent.Post(player));
        player.getAbilities().flying = false;
        helper.assertFalse(FelineJump.canStartFlight(player), "Leaving flight cannot reuse the earlier jump");
        player.setOnGround(true); player.jumpFromGround(); player.setOnGround(false);
        player.startFallFlying();
        helper.assertFalse(FelineJump.jump(player), "An active elytra glide cannot gain an extra jump");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void attackHissesAreRandomAndRespectCooldown(GameTestHelper helper) {
        ServerPlayer player = GameplayTests.player(helper);
        TransformationTests.setState(player, 4);
        FelineSounds.sync(player);
        var target = helper.spawn(EntityType.COW, new BlockPos(4, 2, 4));
        target.setNoAi(true);
        List<Integer> sounds = new ArrayList<>();
        HISSES.put(player, sounds);
        player.getRandom().setSeed(47);
        for (int i = 0; i < 100; i++) {
            player.tickCount = i * 40;
            target.invulnerableTime = 0;
            target.setHealth(target.getMaxHealth());
            target.hurt(player.damageSources().playerAttack(player), 1);
            int count = sounds.size();
            for (int repeat = 1; repeat < 40 && count > 0 && sounds.getLast() == i * 40; repeat++) {
                player.tickCount = i * 40 + repeat;
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().playerAttack(player), .01F);
                helper.assertTrue(sounds.size() == count, "No additional hiss during the two second cooldown");
            }
        }
        helper.assertTrue(sounds.size() >= 15 && sounds.size() <= 45, "Hissing is occasional rather than guaranteed");
        for (int i = 1; i < sounds.size(); i++) helper.assertTrue(sounds.get(i) - sounds.get(i - 1) >= 40, "Hisses stay at least forty ticks apart");
        HISSES.remove(player);
        GameplayTests.remove(player);
        helper.succeed();
    }

    private static void eat(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.startUsingItem(InteractionHand.MAIN_HAND);
    }
}
