package io.github.nekogenix;

import io.github.nekogenix.gameplay.FelineWetness;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class FelineWetnessTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void localRainRemovesOnlyFelineSpeedAndShowsTheCorrectEffect(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var level = helper.getLevel();
        float oldRain = level.getRainLevel(1);
        try {
            player.teleportTo(level, 8.5, 70, 8.5, 0, 0);
            player.setSprinting(true);
            double vanillaSprint = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double vanillaSneak = player.getAttributeValue(Attributes.SNEAKING_SPEED);
            level.setRainLevel(1);
            biome(level, Biomes.PLAINS);
            for (var phase : new NekoState.Phase[] {NekoState.Phase.NEKO, NekoState.Phase.CAT_TRANSFORMING, NekoState.Phase.CAT}) {
                player.getData(Content.NEKO).setPhase(phase);
                TransformationHandler.refreshEffects(player);
                tick(player);
                helper.assertTrue(FelineWetness.exposed(player), "Open plains really receive rain");
                helper.assertTrue(player.hasEffect(phase == NekoState.Phase.CAT ? Content.HEAVY_FUR : Content.HEAVY_EARS),
                        "The ordinary effect matches the neko or cat form");
                helper.assertTrue(!(player.hasEffect(Content.HEAVY_EARS) && player.hasEffect(Content.HEAVY_FUR)),
                        "Changing forms cannot leave two rain effects");
                var instance = player.getEffect(phase == NekoState.Phase.CAT ? Content.HEAVY_FUR : Content.HEAVY_EARS);
                helper.assertTrue(instance.isInfiniteDuration() && instance.showIcon() && !instance.isVisible(),
                        "Rain is a visible condition effect without particles or a fake countdown");
                equal(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED), vanillaSprint, "Wet sprint speed matches a vanilla player");
                equal(helper, player.getAttributeValue(Attributes.SNEAKING_SPEED), vanillaSneak, "Wet sneak speed matches a vanilla player");
                var roof = new BlockPos(8, 74, 8);
                for (var block : new net.minecraft.world.level.block.Block[] {Blocks.STONE, Blocks.GLASS, Blocks.OAK_LEAVES, Blocks.OAK_SLAB}) {
                    level.setBlockAndUpdate(roof, block.defaultBlockState());
                    tick(player);
                    helper.assertTrue(FelineWetness.active(player) && !wetEffect(player).isInfiniteDuration() && wetEffect(player).getDuration() == FelineWetness.DRYING_TICKS, "Shelter starts ten seconds of drying: " + block);
                    dryTicks(player, FelineWetness.DRYING_TICKS);
                    tick(player);
                    helper.assertFalse(FelineWetness.active(player), "Drying completes below every roof");
                    equal(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED), vanillaSprint * 2, "Drying restores the sprint bonus");
                    equal(helper, player.getAttributeValue(Attributes.SNEAKING_SPEED), vanillaSneak * 2, "Drying restores the sneak bonus");
                    level.removeBlock(roof, false);
                    tick(player);
                    helper.assertTrue(FelineWetness.active(player), "Leaving shelter reapplies the rain condition");
                }
                var foreign = Content.id("test_external_speed");
                player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(foreign, .2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                tick(player);
                equal(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED), vanillaSprint * 1.2, "Rain preserves other mods' speed modifiers");
                player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(foreign);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100));
                tick(player);
                equal(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED), vanillaSprint * 1.2, "Normal Speed potions still work in rain");
                player.removeEffect(MobEffects.MOVEMENT_SPEED);
            }
            for (var key : java.util.List.of(Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS)) {
                biome(level, key);
                tick(player);
                helper.assertFalse(FelineWetness.exposed(player), "Dry biomes and snowfall do not count as rain: " + key);
                dryTicks(player, FelineWetness.DRYING_TICKS);
                helper.assertFalse(FelineWetness.active(player), "Dry biomes and snowfall allow the timer to finish");
            }
            biome(level, Biomes.PLAINS);
            tick(player);
            helper.assertTrue(FelineWetness.active(player), "Entering a rainy biome reapplies the effect during the same weather");
            level.setRainLevel(0);
            tick(player);
            helper.assertTrue(wetEffect(player).getDuration() == FelineWetness.DRYING_TICKS, "Stopping rain starts a full ten seconds");
            dryTicks(player, FelineWetness.DRYING_TICKS - 1);
            helper.assertTrue(FelineWetness.active(player), "The effect remains for the entire ten seconds");
            dryTicks(player, 1);
            helper.assertFalse(FelineWetness.active(player), "The condition ends after ten dry seconds");
            level.setRainLevel(1);
            tick(player);
            player.getData(Content.NEKO).setPhase(NekoState.Phase.HUMAN);
            tick(player);
            helper.assertFalse(FelineWetness.active(player), "Becoming human immediately clears both rain effects");
            equal(helper, player.getAttributeValue(Attributes.MOVEMENT_SPEED), vanillaSprint, "Humans never receive feline speed changes from rain");
            biome(level, Biomes.WINDSWEPT_HILLS);
            player.teleportTo(level, 8.5, 240, 8.5, 0, 0);
            helper.assertTrue(level.getBiome(player.blockPosition()).value().getPrecipitationAt(player.blockPosition()) == Biome.Precipitation.SNOW,
                    "The high altitude fixture actually has snow");
            helper.assertFalse(FelineWetness.exposed(player), "Altitude dependent snowfall is excluded");
            for (var dimension : java.util.List.of(Level.NETHER, Level.END)) {
                var other = player.server.getLevel(dimension);
                var cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
                var actor = new ServerPlayer(player.server, other, cookie.gameProfile(), cookie.clientInformation());
                float old = other.getRainLevel(1);
                try {
                    other.setRainLevel(1);
                    helper.assertFalse(FelineWetness.exposed(actor), "A dimension without an open sky cannot inherit overworld rain");
                } finally { other.setRainLevel(old); }
            }
        } finally {
            level.setRainLevel(oldRain);
            biome(level, Biomes.PLAINS);
            GameplayTests.remove(player);
        }
        helper.succeed();
    }

    private static void biome(ServerLevel level, ResourceKey<Biome> key) {
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
        level.getChunk(0, 0).fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
    }

    @GameTest(template = "empty")
    public static void waterRainAndFormChangesShareOneDryingCountdown(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var level = helper.getLevel();
        float oldRain = level.getRainLevel(1);
        var pool = new BlockPos(8, 70, 8);
        var roof = pool.above(4);
        try {
            level.setRainLevel(0);
            biome(level, Biomes.PLAINS);
            for (var phase : java.util.List.of(NekoState.Phase.NEKO, NekoState.Phase.CAT)) {
                player.getData(Content.NEKO).setPhase(phase);
                TransformationHandler.refreshEffects(player);
                player.removeEffect(Content.HEAVY_EARS);
                player.removeEffect(Content.HEAVY_FUR);
                player.teleportTo(level, 8.5, 70.1, 8.5, 0, 0);
                level.setBlockAndUpdate(pool, Blocks.WATER.defaultBlockState());
                player.baseTick();
                helper.assertTrue(player.isInWater(), "The fixture actually contacts water");
                FelineWetness.update(player);
                var contact = wetEffect(player);
                helper.assertTrue(contact != null && contact.isInfiniteDuration(), "Water applies the same persistent contact condition as rain");
                for (int i = 0; i < 250; i++) FelineWetness.update(player);
                helper.assertTrue(wetEffect(player) == contact, "Staying wet does not refresh or resend the effect every tick");
                level.setBlockAndUpdate(pool, Blocks.AIR.defaultBlockState());
                player.baseTick();
                helper.assertFalse(player.isInWater(), "The player has fully exited water");
                FelineWetness.update(player);
                helper.assertTrue(wetEffect(player).getDuration() == 200, "Leaving water starts exactly ten seconds");
                dryTicks(player, 100);
                helper.assertTrue(wetEffect(player).getDuration() == 100, "Dry ticks decrease instead of resetting the timer");
                level.setRainLevel(1);
                FelineWetness.update(player);
                helper.assertTrue(wetEffect(player).isInfiniteDuration(), "Rain interrupts the water drying countdown");
                level.setBlockAndUpdate(pool, Blocks.WATER.defaultBlockState());
                player.baseTick();
                level.setBlockAndUpdate(roof, Blocks.GLASS.defaultBlockState());
                FelineWetness.update(player);
                helper.assertTrue(wetEffect(player).isInfiniteDuration(), "Water keeps contact active even after shelter stops rain");
                level.setBlockAndUpdate(pool, Blocks.AIR.defaultBlockState());
                player.baseTick();
                FelineWetness.update(player);
                helper.assertTrue(wetEffect(player).getDuration() == 200, "The last remaining contact determines the start of drying");
                dryTicks(player, 63);
                player.getData(Content.NEKO).setPhase(phase == NekoState.Phase.CAT ? NekoState.Phase.NEKO : NekoState.Phase.CAT);
                TransformationHandler.refreshEffects(player);
                FelineWetness.update(player);
                helper.assertTrue(wetEffect(player).getDuration() == 137, "Changing between ears and fur preserves remaining dry time");
                dryTicks(player, 136);
                helper.assertTrue(wetEffect(player).getDuration() == 1, "The last dry tick still has an effect");
                dryTicks(player, 1);
                helper.assertFalse(FelineWetness.active(player), "The condition ends exactly after 200 ticks without contact");
                level.removeBlock(roof, false);
                level.setRainLevel(0);
            }
        } finally {
            level.setBlockAndUpdate(pool, Blocks.AIR.defaultBlockState());
            level.removeBlock(roof, false);
            level.setRainLevel(oldRain);
            GameplayTests.remove(player);
        }
        helper.succeed();
    }

    private static MobEffectInstance wetEffect(ServerPlayer player) {
        var ears = player.getEffect(Content.HEAVY_EARS);
        return ears == null ? player.getEffect(Content.HEAVY_FUR) : ears;
    }

    private static void dryTicks(ServerPlayer player, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            for (var effect : java.util.List.of(Content.HEAVY_EARS, Content.HEAVY_FUR)) {
                var instance = player.getEffect(effect);
                if (instance != null && !instance.tick(player, () -> {})) player.removeEffect(effect);
            }
            FelineWetness.update(player);
        }
    }

    private static void tick(ServerPlayer player) { TransformationHandler.tick(new PlayerTickEvent.Post(player)); }

    private static void equal(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < .00001, message + ": " + actual + " vs " + expected);
    }
}
