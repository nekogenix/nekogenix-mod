package io.github.nekogenix;

import io.github.nekogenix.gameplay.BottledEffects;
import io.github.nekogenix.item.SalivaProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class BottleVariantsTests {
    @GameTest(template = "empty")
    public static void bothBrewingChainsUseVanillaIngredientsAndReturnDragonBreathGlass(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
        var stand = (BrewingStandBlockEntity)level.getBlockEntity(pos);
        var pairs = new ItemStack[][] {
                {Content.SALIVA_BOTTLE.toStack(), Content.SPLASH_SALIVA.toStack(), Content.LINGERING_SALIVA.toStack()},
                {Content.NEKO_STABILIZER.toStack(), Content.SPLASH_SWEET_AMETHYST.toStack(), Content.LINGERING_SWEET_AMETHYST.toStack()}};
        for (var pair : pairs) {
            stand.setItem(0, ItemStack.EMPTY);
            helper.assertTrue(stand.canPlaceItem(0, pair[0]) && stand.canPlaceItem(0, pair[1]), "Both recipe inputs fit native brewing stand bottle slots");
            helper.assertTrue(level.potionBrewing().mix(Items.GUNPOWDER.getDefaultInstance(), pair[0]).is(pair[1].getItem()), "Gunpowder converts drinking into splash");
            helper.assertTrue(level.potionBrewing().mix(Items.DRAGON_BREATH.getDefaultInstance(), pair[1]).is(pair[2].getItem()), "Dragon breath converts splash into lingering");
            stand.setItem(0, pair[1].copy());
            stand.setItem(3, Items.DRAGON_BREATH.getDefaultInstance());
            stand.setItem(4, Items.BLAZE_POWDER.getDefaultInstance());
            for (int i = 0; i < 405; i++) BrewingStandBlockEntity.serverTick(level, pos, stand.getBlockState(), stand);
            helper.assertTrue(stand.getItem(0).is(pair[2].getItem()) && stand.getItem(3).is(Items.GLASS_BOTTLE), "Actual brewing produces lingering fluid and returns the ingredient bottle");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void splashSweetnessUsesDistanceWhileSalivaKeepsItsDelayedEffects(GameTestHelper helper) {
        var level = helper.getLevel();
        var direct = helper.spawn(EntityType.COW, new BlockPos(1, 40, 1));
        var edge = helper.spawn(EntityType.COW, new BlockPos(4, 40, 1));
        var outside = helper.spawn(EntityType.COW, new BlockPos(7, 40, 1));
        var projectile = shot(helper, Content.SPLASH_SWEET_AMETHYST.toStack(), direct.position());
        BottledEffects.impact(projectile, new EntityHitResult(direct));
        helper.assertTrue(direct.getEffect(Content.SWEETNESS).getDuration() == 24000, "A direct hit grants the full twenty minute effect");
        helper.assertTrue(edge.getEffect(Content.SWEETNESS).getDuration() == 6000 && !outside.hasEffect(Content.SWEETNESS), "Splash duration fades with distance and stops outside four blocks");
        helper.assertFalse(direct.hasEffect(Content.CRYSTALLIZATION), "Thrown sweet amethyst does not trigger the injection penalty");
        long now = level.getServer().overworld().getGameTime();
        projectile = shot(helper, Content.SPLASH_SALIVA.toStack(), direct.position());
        BottledEffects.impact(projectile, new EntityHitResult(direct));
        helper.assertTrue(direct.getData(Content.EXPOSURE) == now + 24000 && !outside.hasData(Content.EXPOSURE), "Splash saliva preserves the twenty minute delay and radius");
        direct.setData(Content.EXPOSURE, now);
        io.github.nekogenix.gameplay.SalivaExposure.entityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(direct));
        helper.assertTrue(direct.getEffect(net.minecraft.world.effect.MobEffects.POISON).getAmplifier() == 2
                && direct.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION).getDuration() == 1200, "Scheduled saliva still causes Poison III and Nausea");
        direct.discard(); edge.discard(); outside.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 90)
    public static void lingeringCloudsWaitShrinkPersistAndApplyTheirOwnEffects(GameTestHelper helper) {
        var level = helper.getLevel();
        var sweetTarget = helper.spawn(EntityType.COW, new BlockPos(1, 60, 1));
        var salivaTarget = helper.spawn(EntityType.COW, new BlockPos(1, 80, 1));
        sweetTarget.setNoGravity(true); sweetTarget.setNoAi(true);
        salivaTarget.setNoGravity(true); salivaTarget.setNoAi(true);
        BottledEffects.impact(shot(helper, Content.LINGERING_SWEET_AMETHYST.toStack(), sweetTarget.position()), new EntityHitResult(sweetTarget));
        BottledEffects.impact(shot(helper, Content.LINGERING_SALIVA.toStack(), salivaTarget.position()), new EntityHitResult(salivaTarget));
        var sweetCloud = level.getEntitiesOfClass(AreaEffectCloud.class, sweetTarget.getBoundingBox().inflate(1)).getFirst();
        var original = level.getEntitiesOfClass(AreaEffectCloud.class, salivaTarget.getBoundingBox().inflate(1)).getFirst();
        var saved = original.saveWithoutId(new CompoundTag());
        var salivaCloud = new AreaEffectCloud(level, 0, 0, 0);
        salivaCloud.load(saved);
        original.discard(); level.addFreshEntity(salivaCloud);
        helper.assertTrue(sweetCloud.getRadius() == 3 && salivaCloud.getWaitTime() == 10 && salivaCloud.getDuration() == 600,
                "Cloud shape lifetime and wait time match vanilla and survive loading");
        helper.runAfterDelay(5, () -> helper.assertTrue(!sweetTarget.hasEffect(Content.SWEETNESS) && !salivaTarget.hasData(Content.EXPOSURE), "Lingering fluids wait before affecting nearby entities"));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(sweetTarget.hasEffect(Content.SWEETNESS) && sweetTarget.getEffect(Content.SWEETNESS).getDuration() <= 6000
                    && sweetTarget.getEffect(Content.SWEETNESS).getDuration() > 5980, "Lingering sweetness lasts five minutes as a native quarter duration effect");
            helper.assertTrue(salivaTarget.hasData(Content.EXPOSURE) && !salivaTarget.hasEffect(Content.SALIVA_CLOUD)
                    && !salivaTarget.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Saliva clouds schedule the delayed illness without a visible helper effect");
            helper.assertTrue(sweetCloud.getRadius() < 2.5F && salivaCloud.getRadius() < 2.5F, "Clouds shrink when used and over time");
            var due = salivaTarget.getData(Content.EXPOSURE);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(salivaTarget.getData(Content.EXPOSURE) == due, "Remaining in a cloud never pushes back the illness timer");
                sweetCloud.discard(); salivaCloud.discard(); sweetTarget.discard(); salivaTarget.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void dispenserLaunchesAllFourThrowableBottles(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 100, 1));
        level.setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, net.minecraft.core.Direction.EAST));
        var dispenser = (net.minecraft.world.level.block.entity.DispenserBlockEntity)level.getBlockEntity(pos);
        var variants = new ItemStack[] {Content.SPLASH_SALIVA.toStack(), Content.LINGERING_SALIVA.toStack(), Content.SPLASH_SWEET_AMETHYST.toStack(), Content.LINGERING_SWEET_AMETHYST.toStack()};
        for (int i = 0; i < variants.length; i++) {
            int index = i;
            helper.runAfterDelay(i * 15 + 1, () -> {
                dispenser.setItem(0, variants[index].copy());
                level.setBlockAndUpdate(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            });
            helper.runAfterDelay(i * 15 + 8, () -> {
                helper.assertTrue(dispenser.getItem(0).isEmpty(), "The dispenser consumes one bottle");
                var shots = level.getEntitiesOfClass(SalivaProjectile.class, new net.minecraft.world.phys.AABB(pos).inflate(15), projectile -> projectile.getItem().is(variants[index].getItem()));
                helper.assertTrue(!shots.isEmpty(), "The real dispenser launches variant " + index + " as a projectile");
                shots.forEach(SalivaProjectile::discard);
                level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
            });
        }
        helper.runAfterDelay(65, helper::succeed);
    }

    private static SalivaProjectile shot(GameTestHelper helper, ItemStack stack, Vec3 position) {
        var projectile = new SalivaProjectile(Content.SALIVA_PROJECTILE.get(), helper.getLevel());
        projectile.setItem(stack); projectile.setPos(position);
        helper.getLevel().addFreshEntity(projectile);
        return projectile;
    }
}
