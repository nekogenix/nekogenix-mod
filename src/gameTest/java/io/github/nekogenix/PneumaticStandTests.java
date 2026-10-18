package io.github.nekogenix;

import io.github.nekogenix.pneumatic.PneumaticStandBlockEntity;
import io.github.nekogenix.pneumatic.PneumaticStandMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class PneumaticStandTests {
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void oneChargeFillsOneSyringeAcrossReload(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.PNEUMATIC_STAND.get().defaultBlockState());
        var stand = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
        var player = GameplayTests.player(helper);
        for (int i = 0; i < 3; i++) stand.setItem(i, Content.SYRINGE.toStack());
        stand.setItem(3, Content.CAT_NUCLEOCARD.toStack());
        stand.setItem(4, Items.BLAZE_POWDER.getDefaultInstance());
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(stand.running(), "Blaze powder cannot power the pneumatic stand");
            stand.removeItem(4, 1);
            player.openMenu(stand);
            var menu = (PneumaticStandMenu) player.containerMenu;
            helper.assertFalse(menu.getSlot(4).mayPlace(Items.BLAZE_POWDER.getDefaultInstance()), "The fuel slot rejects blaze powder");
            helper.assertFalse(menu.getSlot(3).mayPlace(Content.SALIVA_SWAB.toStack()), "The card slot rejects saliva swabs");
            player.getInventory().setItem(0, new ItemStack(Items.WIND_CHARGE, 2));
            menu.quickMoveStack(player, 32);
            helper.assertTrue(stand.getItem(4).getCount() == 2, "Shift transfer inserts wind charges into the fuel slot");
            player.closeContainer();
            GameplayTests.remove(player);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(stand.running() && stand.getItem(4).getCount() == 1, "A cycle uses exactly one wind charge");
            helper.assertTrue(stand.getItem(3).is(Content.CAT_NUCLEOCARD), "The card remains filled during the procedure");
        });
        helper.runAfterDelay(150, () -> {
            var saved = stand.saveWithFullMetadata(level.registryAccess());
            var restored = (PneumaticStandBlockEntity) BlockEntity.loadStatic(pos, stand.getBlockState(), saved, level.registryAccess());
            helper.assertTrue(restored != null && restored.progress() == stand.progress() && restored.getItem(4).getCount() == 1,
                    "Progress and already used pressure survive a reload");
            level.removeBlockEntity(pos);
            level.setBlockEntity(restored);
        });
        helper.runAfterDelay(430, () -> {
            var restored = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(restored.getItem(0).is(Content.INJECTION) && restored.getItem(1).is(Content.SYRINGE)
                    && restored.getItem(2).is(Content.SYRINGE) && restored.getItem(3).is(Content.BLANK_NUCLEOCARD)
                    && restored.getItem(4).getCount() == 1, "One card fills one syringe and returns blank without spending extra fuel");
        });
        helper.runAfterDelay(450, () -> {
            var restored = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
            helper.assertFalse(restored.running(), "A blank card cannot start another cycle");
            restored.setItem(3, Content.CAT_NUCLEOCARD.toStack());
        });
        helper.runAfterDelay(870, () -> {
            var restored = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(restored.getItem(0).is(Content.INJECTION) && restored.getItem(1).is(Content.INJECTION)
                    && restored.getItem(2).is(Content.SYRINGE) && restored.getItem(3).is(Content.BLANK_NUCLEOCARD)
                    && restored.getItem(4).isEmpty(), "The next charge and card fill only the next syringe");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void removingCardStopsWithoutProducingInjection(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.PNEUMATIC_STAND.get().defaultBlockState());
        var stand = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
        stand.setItem(0, Content.SYRINGE.toStack());
        stand.setItem(3, Content.CAT_NUCLEOCARD.toStack());
        stand.setItem(4, Items.WIND_CHARGE.getDefaultInstance());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(stand.running(), "Processing has started");
            helper.assertTrue(stand.removeItem(3, 1).is(Content.CAT_NUCLEOCARD), "An interrupted card remains filled");
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!stand.running() && stand.getItem(0).is(Content.SYRINGE) && stand.getItem(3).isEmpty(),
                    "Removing the card cancels progress without an injection or duplicate card");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 450)
    public static void salivaStaysInTheVanillaBrewingStand(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
        var stand = (BrewingStandBlockEntity) level.getBlockEntity(pos);
        for (int i = 0; i < 3; i++) stand.setItem(i, Items.GLASS_BOTTLE.getDefaultInstance());
        stand.setItem(3, Content.SALIVA_SWAB.toStack());
        stand.setItem(4, Items.BLAZE_POWDER.getDefaultInstance());
        helper.assertFalse(level.potionBrewing().isIngredient(Content.CAT_NUCLEOCARD.toStack()), "DNA cards no longer brew in vanilla stands");
        helper.runAfterDelay(430, () -> {
            for (int i = 0; i < 3; i++) helper.assertTrue(stand.getItem(i).is(Content.SALIVA_BOTTLE), "Swabs still brew glass bottles into saliva bottles");
            helper.succeed();
        });
    }
}
