package io.github.nekogenix;

import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class TransformationTests {
    @GameTest(template = "empty")
    public static void deathCancelsEveryEarlyStage(GameTestHelper helper) {
        ServerPlayer player = GameplayTests.player(helper);
        for (int phase = 0; phase < 4; phase++) {
            setState(player, phase);
            helper.assertTrue(player.getData(Content.NEKO).serializeNBT(player.registryAccess()).getInt("lives") == 0,
                    "Old in-progress saves do not receive feline lives");
            player.setSprinting(true);
            TransformationHandler.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN, "Death cancels each early phase");
            helper.assertFalse(player.hasEffect(Content.SHOCK) || player.hasEffect(Content.TRANSFORMING)
                    || player.hasEffect(Content.AGILITY) || player.hasEffect(Content.VISION) || player.hasEffect(Content.PAWS),
                    "Death immediately removes transformation markers");
            player = player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
            helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN
                    && !player.getData(Content.NEKO).has(7), "Respawn copies human state without unlocked abilities");
            helper.assertFalse(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .hasModifier(Content.id("feline_sprint")), "Sprint boost does not survive an interrupted transformation");
        }
        setState(player, 4);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        player = player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
        helper.assertTrue(player.getData(Content.NEKO).isNeko()
                && player.getData(Content.NEKO).serializeNBT(player.registryAccess()).getInt("lives") == 8,
                "Fully transformed players retain their abilities and consume one life");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 280)
    public static void milkAndChickenRespectTransformationPhase(GameTestHelper helper) {
        ServerPlayer player = GameplayTests.player(helper);
        for (int phase = 0; phase < 4; phase++) {
            final int selected = phase;
            helper.runAfterDelay(1 + phase * 36, () -> {
                player.removeAllEffects();
                setState(player, selected);
                eat(player, Items.MILK_BUCKET.getDefaultInstance());
            });
            helper.runAfterDelay(35 + phase * 36, () -> {
                var poison = player.getEffect(MobEffects.POISON);
                helper.assertTrue(poison != null && poison.getAmplifier() == 0 && poison.getDuration() >= 596,
                        "Milk applies thirty seconds of poison after clearing at every early phase");
                helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Milk returns a bucket");
                helper.assertTrue(player.getData(Content.NEKO).isTransforming(), "Milk does not cure transformation");
            });
        }
        helper.runAfterDelay(145, () -> {
            player.removeAllEffects();
            setState(player, 4);
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1200));
            eat(player, chicken());
        });
        helper.runAfterDelay(179, () -> {
            helper.assertTrue(player.getFoodData().getFoodLevel() == 6, "Neko chicken retains cooked nutrition");
            helper.assertTrue(player.getEffect(MobEffects.HUNGER).getDuration() > 600,
                    "Eating chicken does not clear pre-existing Hunger");
            player.removeEffect(MobEffects.HUNGER);
            eat(player, chicken());
        });
        helper.runAfterDelay(213, () -> {
            helper.assertFalse(player.hasEffect(MobEffects.HUNGER), "Even guaranteed raw chicken Hunger cannot affect a full neko");
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new CompoundTag());
            eat(player, chicken());
        });
        helper.runAfterDelay(247, () -> {
            helper.assertTrue(player.hasEffect(MobEffects.HUNGER), "Human chicken effects remain unchanged");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void playersFeedOnlyHungryFelinePlayers(GameTestHelper helper) {
        ServerPlayer feeder = GameplayTests.player(helper);
        ServerPlayer target = GameplayTests.player(helper);
        target.teleportTo(feeder.getX() + 1, feeder.getY(), feeder.getZ());
        for (int phase = 0; phase <= 4; phase++) {
            setState(target, phase);
            target.getFoodData().setFoodLevel(0);
            target.getFoodData().setSaturation(0);
            feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 2));
            interact(feeder, target);
            int expected = phase == 4 ? 5 : 2;
            helper.assertTrue(target.getFoodData().getFoodLevel() == expected, "Feeding restores phase-appropriate cod nutrition");
            helper.assertTrue(feeder.getMainHandItem().getCount() == 1, "Feeding consumes one fish");
            if (phase < 4) helper.assertFalse(target.hasData(Content.BREEDING) && target.getData(Content.BREEDING).inLove(),
                    "Feeding does not start breeding before full transformation");
        }
        target.getFoodData().setFoodLevel(20);
        interact(feeder, target);
        helper.assertTrue(feeder.getMainHandItem().getCount() == 1, "Full players do not waste offered fish");
        target.getFoodData().setFoodLevel(0);
        feeder.setItemInHand(InteractionHand.MAIN_HAND, Items.COOKED_COD.getDefaultInstance());
        interact(feeder, target);
        helper.assertTrue(target.getFoodData().getFoodLevel() == 0, "Only vanilla Cat food is accepted");
        feeder.setGameMode(GameType.CREATIVE);
        feeder.setItemInHand(InteractionHand.OFF_HAND, Items.SALMON.getDefaultInstance());
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(feeder, InteractionHand.OFF_HAND, target));
        helper.assertTrue(target.getFoodData().getFoodLevel() == 6 && feeder.getOffhandItem().getCount() == 1,
                "Offhand feeding preserves creative items and uses full neko salmon nutrition");
        target.getData(Content.NEKO).deserializeNBT(target.registryAccess(), new CompoundTag());
        target.getFoodData().setFoodLevel(0);
        feeder.setItemInHand(InteractionHand.MAIN_HAND, Items.COD.getDefaultInstance());
        interact(feeder, target);
        helper.assertTrue(target.getFoodData().getFoodLevel() == 0, "Humans cannot be fed as cats");
        GameplayTests.remove(feeder);
        GameplayTests.remove(target);
        helper.succeed();
    }

    static void setState(ServerPlayer player, int phase) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("phase", phase == 0 ? 1 : phase == 4 ? 3 : 2);
        tag.putInt("elapsed", phase <= 1 ? 0 : phase == 4 ? 72000 : (phase - 1) * 24000);
        tag.putInt("abilities", phase <= 1 ? 0 : phase == 2 ? 1 : phase == 3 ? 3 : 7);
        tag.putInt("lives", 9);
        player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), tag);
        TransformationHandler.refreshEffects(player);
    }

    private static ItemStack chicken() {
        var stack = Items.CHICKEN.getDefaultInstance();
        stack.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F)
                .effect(new MobEffectInstance(MobEffects.HUNGER, 600), 1F).build());
        return stack;
    }

    private static void eat(ServerPlayer player, ItemStack stack) {
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.startUsingItem(InteractionHand.MAIN_HAND);
    }

    private static void interact(ServerPlayer feeder, ServerPlayer target) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(feeder, InteractionHand.MAIN_HAND, target));
    }
}
