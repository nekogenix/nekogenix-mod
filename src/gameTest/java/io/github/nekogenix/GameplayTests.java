package io.github.nekogenix;

import com.mojang.authlib.GameProfile;
import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.gameplay.FelineSounds;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.gameplay.SalivaExposure;
import io.github.nekogenix.state.NekoState;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class GameplayTests {
    @GameTest(template = "empty")
    public static void transformationAndPersistence(GameTestHelper helper) {
        NekoState state = new NekoState();
        helper.assertTrue(state.begin(), "A human can start transformation");
        helper.assertFalse(state.begin(), "Repeated injection cannot reset progress");
        for (int i = 0; i < 1199; i++) state.tick(helper.getLevel().random);
        helper.assertTrue(state.phase() == NekoState.Phase.SHOCK, "Shock lasts the full minute");
        state.tick(helper.getLevel().random);
        helper.assertTrue(state.phase() == NekoState.Phase.TRANSFORMING, "Transformation starts after shock");
        for (int day = 1; day <= 3; day++) {
            for (int i = 0; i < 24000; i++) state.tick(helper.getLevel().random);
            int count = (state.has(1) ? 1 : 0) + (state.has(2) ? 1 : 0) + (state.has(4) ? 1 : 0);
            helper.assertTrue(count == day, "Exactly one unique ability per day");
            NekoState restored = new NekoState();
            restored.deserializeNBT(helper.getLevel().registryAccess(), state.serializeNBT(helper.getLevel().registryAccess()));
            state = restored;
        }
        helper.assertTrue(state.isNeko(), "All three days complete transformation");
        for (int i = 0; i < 8; i++) {
            helper.assertFalse(state.loseLife(), "First eight deaths retain neko state");
            helper.assertTrue(state.isNeko(), "Abilities survive the first eight deaths");
        }
        helper.assertTrue(state.loseLife(), "Ninth death returns human");
        helper.assertFalse(state.has(1) || state.has(2) || state.has(4), "No ability survives the ninth death");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 250)
    public static void injectionCancellationAndCompletion(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getFoodData().setFoodLevel(17);
        ItemStack injection = Content.INJECTION.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, injection);
        helper.runAfterDelay(65, () -> InjectionHandler.input(player, true));
        helper.runAfterDelay(71, () -> helper.assertTrue(player.getHealth() == 20F,
                "The approach before needle contact does not hurt"));
        helper.runAfterDelay(76, () -> helper.assertTrue(player.getHealth() == 20F,
                "The slow initial insertion does not hurt before the jab"));
        helper.runAfterDelay(79, () -> helper.assertTrue(player.getHealth() == 19F
                && player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN,
                "Needle contact hurts before any transformation begins"));
        helper.runAfterDelay(85, () -> {
            InjectionHandler.input(player, false);
            helper.assertTrue(player.getMainHandItem() == injection, "Interrupted injection is not consumed");
            helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN, "Cancellation does not transform");
            helper.assertTrue(player.getData(Content.INJECTION_RETURN).started() >= 0,
                    "Releasing starts a synchronized return instead of clearing the animation");
        });
        helper.runAfterDelay(90, () -> {
            InjectionHandler.input(player, false);
            helper.assertTrue(player.getData(Content.INJECTION_STARTED) >= 0,
                    "Repeated release does not interrupt the return animation");
        });
        helper.runAfterDelay(99, () -> {
            helper.assertTrue(player.getData(Content.INJECTION_STARTED) == -1
                    && player.getData(Content.INJECTION_RETURN).started() == -1,
                    "The cancelled animation finishes and clears its transient state");
            helper.assertTrue(player.getMainHandItem() == injection && player.getHealth() == 19F
                    && player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN,
                    "Returning retains the contact damage without consuming the syringe or applying transformation");
        });
        for (int tick : new int[] {100, 120, 140, 160, 180}) {
            helper.runAfterDelay(tick, () -> InjectionHandler.input(player, true));
        }
        helper.runAfterDelay(185, () -> {
            helper.assertTrue(player.getMainHandItem().is(Content.INJECTION), "Reverse animation precedes used syringe");
        });
        helper.runAfterDelay(197, () -> {
            helper.assertTrue(player.getMainHandItem().is(Content.USED_SYRINGE), "Completed injection becomes used syringe");
            helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.SHOCK, "Injection starts shock");
            helper.assertTrue(player.getHealth() == 17F, "Two needle entries and the first sickness poison tick each deal one damage");
            remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void foodAndMilkKeepSpecialBehavior(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        NekoState state = player.getData(Content.NEKO);
        state.begin();
        for (int i = 0; i < 73200; i++) state.tick(helper.getLevel().random);
        state.stabilize();
        eat(player, Items.APPLE.getDefaultInstance());
        helper.runAfterDelay(36, () -> {
            helper.assertTrue(player.getFoodData().getFoodLevel() == 2, "Neko apple restores two food points");
            helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 1.2F) < .001F, "Neko apple restores half vanilla saturation");
            eat(player, Items.BEEF.getDefaultInstance());
        });
        helper.runAfterDelay(72, () -> {
            helper.assertTrue(player.getFoodData().getFoodLevel() == 8, "Raw beef restores cooked nutrition");
            helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 8F) < .001F, "Raw beef uses cooked saturation capped by hunger");
            eat(player, Items.POISONOUS_POTATO.getDefaultInstance());
        });
        helper.runAfterDelay(108, () -> {
            helper.assertTrue(player.hasEffect(MobEffects.POISON) && player.getEffect(MobEffects.POISON).getAmplifier() == 1,
                    "Poisonous potato guarantees poison two");
            eat(player, Items.GOLDEN_APPLE.getDefaultInstance());
        });
        helper.runAfterDelay(144, () -> {
            helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION) && player.hasEffect(MobEffects.REGENERATION), "Golden apple retains both buffs");
            eat(player, Items.MUSHROOM_STEW.getDefaultInstance());
        });
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(player.getMainHandItem().is(Items.BOWL), "Stew returns its bowl");
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 400));
            eat(player, Items.MILK_BUCKET.getDefaultInstance());
        });
        helper.runAfterDelay(216, () -> {
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Milk returns its bucket");
            helper.assertFalse(player.hasEffect(MobEffects.POISON), "Milk clears vanilla poison");
            helper.assertTrue(player.hasEffect(MobEffects.CONFUSION), "Milk discomfort follows clearing");
            helper.assertTrue(state.isNeko(), "Milk does not cure neko state");
            remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void heatedCauldronCleansOnlyOne(GameTestHelper helper) {
        BlockPos full = new BlockPos(1, 2, 1);
        BlockPos partial = new BlockPos(4, 2, 1);
        BlockPos cooled = new BlockPos(1, 2, 4);
        for (BlockPos pos : new BlockPos[] {full, partial, cooled}) {
            helper.setBlock(pos.below(), Blocks.MAGMA_BLOCK);
            helper.setBlock(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, pos.equals(partial) ? 2 : 3));
            BlockPos absolute = helper.absolutePos(pos);
            for (int i = 0; i < 2; i++) {
                ItemEntity dirty = new ItemEntity(helper.getLevel(), absolute.getX() + .5, absolute.getY() + .5, absolute.getZ() + .5, Content.USED_SYRINGE.toStack());
                dirty.setNoGravity(true);
                dirty.setDeltaMovement(0, 0, 0);
                helper.getLevel().addFreshEntity(dirty);
            }
        }
        helper.runAfterDelay(50, () -> {
            helper.assertBlockProperty(full, LayeredCauldronBlock.LEVEL, 3);
            helper.assertItemEntityCountIs(Content.SYRINGE.get(), full, 1.2, 0);
        });
        helper.runAfterDelay(90, () -> {
            helper.assertBlockProperty(full, LayeredCauldronBlock.LEVEL, 2);
            helper.assertBlockProperty(cooled, LayeredCauldronBlock.LEVEL, 2);
            helper.setBlock(cooled.below(), Blocks.STONE);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertBlockProperty(full, LayeredCauldronBlock.LEVEL, 1);
            helper.assertBlockProperty(partial, LayeredCauldronBlock.LEVEL, 1);
            helper.assertItemEntityCountIs(Content.SYRINGE.get(), full, 1.2, 0);
        });
        helper.runAfterDelay(199, () -> helper.assertBlockPresent(Blocks.WATER_CAULDRON, full));
        helper.runAfterDelay(225, () -> {
            helper.assertBlockPresent(Blocks.CAULDRON, full);
            helper.assertBlockPresent(Blocks.CAULDRON, partial);
            helper.assertItemEntityCountIs(Content.SYRINGE.get(), full, 1.2, 1);
            helper.assertItemEntityCountIs(Content.USED_SYRINGE.get(), full, 1.2, 1);
            helper.assertItemEntityCountIs(Content.SYRINGE.get(), partial, 1.2, 0);
            helper.assertItemEntityCountIs(Content.USED_SYRINGE.get(), partial, 1.2, 2);
            helper.assertBlockProperty(cooled, LayeredCauldronBlock.LEVEL, 2);
            helper.assertItemEntityCountIs(Content.SYRINGE.get(), cooled, 1.2, 0);
            helper.assertItemEntityCountIs(Content.USED_SYRINGE.get(), cooled, 1.2, 2);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void delayedExposureAndBrewing(GameTestHelper helper) {
        var brewing = helper.getLevel().potionBrewing();
        helper.assertTrue(brewing.mix(Content.SALIVA_SWAB.toStack(), Items.GLASS_BOTTLE.getDefaultInstance()).is(Content.SALIVA_BOTTLE), "Glass bottle brews with swab");
        helper.assertTrue(brewing.mix(Items.GUNPOWDER.getDefaultInstance(), Content.SALIVA_BOTTLE.toStack()).is(Content.SPLASH_SALIVA), "Saliva bottle brews with gunpowder");
        Cat cat = helper.spawn(EntityType.CAT, new BlockPos(2, 2, 2));
        SalivaExposure.schedule(cat);
        long now = helper.getLevel().getServer().overworld().getGameTime();
        helper.assertTrue(cat.getData(Content.EXPOSURE) == now + 24000, "Exposure waits a full game day");
        helper.assertFalse(cat.hasEffect(MobEffects.POISON), "Exposure is not immediate");
        cat.setData(Content.EXPOSURE, now + 5);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(cat.hasEffect(MobEffects.POISON) && cat.getEffect(MobEffects.POISON).getAmplifier() == 2, "Exposure applies poison three");
            helper.assertTrue(cat.hasEffect(MobEffects.CONFUSION), "Exposure also applies nausea");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void catOwnershipAndExistingCreeperTarget(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var creeper = helper.spawn(EntityType.CREEPER, new BlockPos(5, 2, 2));
        creeper.setTarget(player);
        helper.assertTrue(creeper.getTarget() == player, "A human is a valid creeper target");
        NekoState state = player.getData(Content.NEKO);
        state.begin();
        for (int i = 0; i < 73200; i++) state.tick(helper.getLevel().random);
        helper.runAfterDelay(5, () -> helper.assertTrue(creeper.getTarget() == null,
                "A creeper releases its existing target when that player becomes neko"));
        helper.runAfterDelay(65, () -> {
            Cat cat = helper.spawn(EntityType.CAT, new BlockPos(2, 2, 2));
            cat.tame(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.COTTON_SWAB.toStack(2));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cat));
            helper.assertTrue(player.getInventory().countItem(Content.SALIVA_SWAB.get()) == 1,
                    "An owned tame cat yields one sample");
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Sampling consumes one cotton swab");
            cat.setOwnerUUID(UUID.randomUUID());
            float before = player.getHealth();
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cat));
            helper.assertTrue(player.getHealth() < before, "A foreign cat bites immediately");
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cat));
            helper.assertTrue(player.getInventory().countItem(Content.SALIVA_SWAB.get()) == 1
                    && player.getMainHandItem().getCount() == 1, "A foreign cat provides no sample and consumes no swab");
            remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void stagesAndFelineVoices(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var state = player.getData(Content.NEKO);
        state.begin();
        FelineSounds.sync(player);
        var hurt = NeoForge.EVENT_BUS.post(new PlayLevelSoundEvent.AtEntity(player,
                Holder.direct(SoundEvents.PLAYER_HURT), SoundSource.PLAYERS, 1, 1));
        helper.assertTrue(hurt.isCanceled(), "Predicted hurt sounds wait for actual damage during shock");
        for (int stage = 0; stage < 3; stage++) {
            var saved = state.serializeNBT(helper.getLevel().registryAccess());
            saved.putInt("phase", NekoState.Phase.TRANSFORMING.ordinal());
            saved.putInt("elapsed", stage * NekoState.DAY + 120);
            state.deserializeNBT(helper.getLevel().registryAccess(), saved);
            TransformationHandler.refreshEffects(player);
            var effect = player.getEffect(Content.TRANSFORMING);
            helper.assertTrue(effect != null && effect.getAmplifier() == stage
                    && effect.getDuration() == NekoState.DAY - 120, "Saved progress restores the correct stage and timer");
            for (int i = 120; i < NekoState.DAY; i++) state.tick(helper.getLevel().random);
            int count = (state.has(1) ? 1 : 0) + (state.has(2) ? 1 : 0) + (state.has(4) ? 1 : 0);
            helper.assertTrue(count == stage + 1, "Each completed stage grants one distinct ability");
        }
        TransformationHandler.refreshEffects(player);
        helper.assertTrue(state.isNeko() && !player.hasEffect(Content.TRANSFORMING) && player.hasEffect(Content.CAT_TRANSFORMING), "Three neko stages give way to a separate Cat Transformation");
        var death = NeoForge.EVENT_BUS.post(new PlayLevelSoundEvent.AtPosition(player.level(), player.position(),
                Holder.direct(SoundEvents.PLAYER_DEATH), SoundSource.PLAYERS, 1, 1));
        helper.assertTrue(death.getSound().value() == SoundEvents.CAT_DEATH, "Server voice events identify their player");
        for (int i = 0; i < 9; i++) state.loseLife();
        FelineSounds.sync(player);
        hurt = NeoForge.EVENT_BUS.post(new PlayLevelSoundEvent.AtEntity(player,
                Holder.direct(SoundEvents.PLAYER_HURT), SoundSource.PLAYERS, 1, 1));
        helper.assertTrue(hurt.getSound().value() == SoundEvents.PLAYER_HURT, "Human voices return with human state");
        remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void darknessAtDistanceAndAtNight(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2)).atY(150);
        player.teleportTo(origin.getX(), origin.getY(), origin.getZ());
        level.setDayTime(6000);
        level.setBlockAndUpdate(origin.above(2), Blocks.STONE.defaultBlockState());
        BlockPos cell = origin.offset(12, 0, 0);
        for (BlockPos pos : BlockPos.betweenClosed(cell.offset(-1, -1, -1), cell.offset(1, 1, 1))) {
            level.setBlockAndUpdate(pos, (pos.equals(cell) ? Blocks.AIR : Blocks.STONE).defaultBlockState());
        }
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(level.getMaxLocalRawBrightness(cell) == 0, "The distant chamber has zero light");
            helper.assertTrue(TransformationHandler.inDarkness(player), "Darkness twelve blocks away activates vision");
            level.setBlockAndUpdate(cell, Blocks.STONE.defaultBlockState());
        });
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(TransformationHandler.inDarkness(player), "Partial daylight alone does not activate vision");
            level.setBlockAndUpdate(origin, Blocks.TORCH.defaultBlockState());
            level.setDayTime(13000);
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, origin) > 0,
                    "The nearby torch emits light");
            helper.assertTrue(TransformationHandler.inDarkness(player), "Unlit night within sixteen blocks survives a nearby torch");
            remove(player);
            helper.succeed();
        });
    }

    private static void eat(ServerPlayer player, ItemStack stack) {
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.startUsingItem(InteractionHand.MAIN_HAND);
    }

    static ServerPlayer player(GameTestHelper helper) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "neko-test"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        player.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        player.setNoGravity(true);
        player.getAbilities().invulnerable = false;
        helper.onEachTick(() -> {
            if (player.server.getPlayerList().getPlayer(player.getUUID()) == player) player.doTick();
        });
        return player;
    }

    static void remove(ServerPlayer player) { player.server.getPlayerList().remove(player); }
}
