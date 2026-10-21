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
    private static final Map<ServerPlayer, Integer> JUMP_SOUNDS = new WeakHashMap<>();

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
        if (event.getEntity() instanceof ServerPlayer player && JUMP_SOUNDS.containsKey(player)) {
            JUMP_SOUNDS.merge(player, 1, Integer::sum);
        }
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
    public static void airJumpDoesNotPlayAnySound(GameTestHelper helper) {
        for (boolean cat : new boolean[] {false, true}) {
            var player = GameplayTests.player(helper);
            if (cat) CatTransformationTests.cat(player);
            else TransformationTests.setState(player, 4);
            player.setOnGround(true);
            player.jumpFromGround();
            player.setOnGround(false);
            JUMP_SOUNDS.put(player, 0);
            helper.assertTrue(FelineJump.jump(player), "Both feline forms can perform the silent air jump");
            helper.assertTrue(JUMP_SOUNDS.remove(player) == 0, "An accepted air jump does not emit a sound");
            GameplayTests.remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void airJumpExpiresAfterTwentyAirborneTicks(GameTestHelper helper) {
        for (boolean cat : new boolean[] {false, true}) {
            for (int elapsed : new int[] {19, 20, 21, 30, 100}) {
                var player = GameplayTests.player(helper);
                if (cat) CatTransformationTests.cat(player);
                else TransformationTests.setState(player, 4);
                player.setOnGround(true);
                player.verticalCollisionBelow = true;
                player.jumpFromGround();
                player.setOnGround(false);
                player.verticalCollisionBelow = false;
                player.tickCount += elapsed;
                var velocity = new net.minecraft.world.phys.Vec3(.4, -.8, -.3);
                player.setDeltaMovement(velocity);
                boolean accepted = FelineJump.jump(player);
                helper.assertTrue(accepted == (elapsed <= 20), "Both feline forms have exactly twenty ticks to air jump at " + elapsed);
                if (!accepted) helper.assertTrue(player.getDeltaMovement().equals(velocity), "Expired requests cannot alter velocity");
                player.setOnGround(true);
                player.verticalCollisionBelow = true;
                player.jumpFromGround();
                player.setOnGround(false);
                player.verticalCollisionBelow = false;
                player.tickCount++;
                helper.assertTrue(FelineJump.jump(player), "Landing and jumping again starts a fresh window even before another player tick");
                GameplayTests.remove(player);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void jumpConfirmationCannotExtendTheAirborneDeadline(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        CatTransformationTests.cat(player);
        player.setOnGround(true);
        player.verticalCollisionBelow = true;
        FelineJump.tick(new PlayerTickEvent.Post(player));
        player.jumpFromGround();
        player.setOnGround(false);
        player.verticalCollisionBelow = false;
        player.tickCount += 3;
        player.awardStat(net.minecraft.stats.Stats.JUMP);
        player.tickCount += 18;
        helper.assertFalse(FelineJump.jump(player), "Delayed movement compatibility confirmation does not restart the timer");
        player.setOnGround(true);
        player.verticalCollisionBelow = true;
        FelineJump.tick(new PlayerTickEvent.Post(player));
        player.jumpFromGround();
        player.setOnGround(false);
        player.verticalCollisionBelow = false;
        player.tickCount++;
        helper.assertTrue(FelineJump.jump(player), "A fresh jump is available after landing");
        player.tickCount += 100;
        FelineJump.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(FelineJump.canStartFlight(player), "The deadline does not remove the flight handoff after a completed air jump");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void jumpStatConfirmsCustomGroundJumps(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        TransformationTests.setState(player, 4);
        helper.assertFalse(player.hasData(Content.FELINE_JUMP), "A newly transformed player has no previous jump session");
        player.setOnGround(true);
        FelineJump.tick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.hasData(Content.FELINE_JUMP), "Ground contact is tracked before the first custom jump");
        FelineJump.dimension(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                player, net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
        player.setOnGround(false);
        player.awardStat(net.minecraft.stats.Stats.JUMP);
        helper.assertFalse(FelineJump.jump(player), "An airborne statistic cannot arm another jump");
        player.setOnGround(true);
        player.awardStat(net.minecraft.stats.Stats.WALK_ONE_CM);
        player.setOnGround(false);
        helper.assertFalse(FelineJump.jump(player), "Movement statistics cannot arm a jump");
        player.setOnGround(true);
        FelineJump.tick(new PlayerTickEvent.Post(player));
        player.setOnGround(false);
        player.awardStat(net.minecraft.stats.Stats.JUMP);
        helper.assertTrue(FelineJump.jump(player), "The server confirms a custom takeoff even when the movement mod clears onGround before awarding the jump");
        helper.assertFalse(FelineJump.jump(player), "Custom ground jumps still permit only one air jump");
        player.awardStat(net.minecraft.stats.Stats.JUMP);
        helper.assertFalse(FelineJump.jump(player), "A repeated airborne jump statistic cannot rearm a used jump");
        FelineJump.dimension(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                player, net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
        helper.assertFalse(FelineJump.canStartFlight(player), "Changing dimensions clears the flight handoff");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void airJumpRequiresEnoughFoodUnlessCreative(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        TransformationTests.setState(player, 4);
        for (int food : new int[] {0, 6, 7}) {
            player.getFoodData().setFoodLevel(food);
            player.setOnGround(true);
            player.jumpFromGround();
            player.setOnGround(false);
            helper.assertTrue(FelineJump.jump(player) == (food > 6), "Air jump uses the sprint hunger threshold at " + food);
        }
        player.getFoodData().setFoodLevel(20);
        player.setOnGround(true); player.jumpFromGround(); player.setOnGround(false);
        player.getFoodData().setFoodLevel(6);
        helper.assertFalse(FelineJump.jump(player), "Becoming hungry after takeoff blocks the second jump");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.getFoodData().setFoodLevel(0);
        player.setOnGround(true); player.jumpFromGround(); player.setOnGround(false);
        helper.assertTrue(FelineJump.jump(player), "Creative air jumps do not require food");
        helper.assertTrue(FelineJump.canStartFlight(player), "Creative flight remains available after a hungry air jump");
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
