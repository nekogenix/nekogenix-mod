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
    @GameTest(template = "empty")
    public static void pneumaticRecipeUsesIronRedstoneBreezeAndSmoothStone(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe)level.getRecipeManager().byKey(Content.id("pneumatic_stand")).orElseThrow().value();
        var items = new java.util.ArrayList<>(java.util.List.of(Items.IRON_INGOT.getDefaultInstance(), Items.IRON_INGOT.getDefaultInstance(), Items.IRON_INGOT.getDefaultInstance(),
                Items.REDSTONE.getDefaultInstance(), Items.BREEZE_ROD.getDefaultInstance(), Items.REDSTONE.getDefaultInstance(),
                Items.SMOOTH_STONE.getDefaultInstance(), Items.SMOOTH_STONE.getDefaultInstance(), Items.SMOOTH_STONE.getDefaultInstance()));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, items);
        helper.assertTrue(recipe.matches(input, level) && recipe.assemble(input, level.registryAccess()).is(Content.PNEUMATIC_STAND.get().asItem()), "The requested three by three recipe crafts the pneumatic stand");
        items.set(7, Items.COBBLESTONE.getDefaultInstance());
        helper.assertFalse(recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, items), level), "Cobblestone cannot replace smooth stone");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void addingSecondSyringeRestartsTheWholeBatch(GameTestHelper helper) {
        restartBatch(helper, false);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void addingSecondSyringeAlsoRestartsCrystallizingBatch(GameTestHelper helper) {
        restartBatch(helper, true);
        helper.succeed();
    }

    private static void restartBatch(GameTestHelper helper, boolean crystallizing) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.PNEUMATIC_STAND.get().defaultBlockState());
        var stand = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
        stand.setItem(0, Content.SYRINGE.toStack());
        stand.setItem(3, crystallizing ? Content.NEKO_STABILIZER.toStack() : Content.CAT_NUCLEOCARD.toStack());
        stand.setItem(4, new ItemStack(Items.WIND_CHARGE, 2));
        for (int tick = 0; tick < 390; tick++) PneumaticStandBlockEntity.tick(level, pos, stand.getBlockState(), stand);
        stand.setItem(2, Content.SYRINGE.toStack());
        PneumaticStandBlockEntity.tick(level, pos, stand.getBlockState(), stand);
        helper.assertTrue(stand.progress() == 1 && stand.getItem(4).getCount() == 1,
                "A late second syringe restarts the batch with the pressure already supplied");
        for (int tick = 0; tick < 20; tick++) PneumaticStandBlockEntity.tick(level, pos, stand.getBlockState(), stand);
        helper.assertTrue(stand.getItem(0).is(Content.SYRINGE) && stand.getItem(2).is(Content.SYRINGE),
                "Neither syringe completes at the old deadline");
        var saved = stand.saveWithFullMetadata(level.registryAccess());
        var restored = (PneumaticStandBlockEntity) BlockEntity.loadStatic(pos, stand.getBlockState(), saved, level.registryAccess());
        level.removeBlockEntity(pos);
        level.setBlockEntity(restored);
        for (int tick = 0; tick < 379; tick++) PneumaticStandBlockEntity.tick(level, pos, restored.getBlockState(), restored);
        var output = crystallizing ? Content.STABILIZING_INJECTION : Content.INJECTION;
        helper.assertTrue(restored.getItem(0).is(output) && restored.getItem(2).is(output)
                        && (crystallizing ? restored.getItem(3).is(Items.GLASS_BOTTLE) : restored.getItem(3).is(Content.BLANK_NUCLEOCARD))
                        && restored.getItem(4).getCount() == 1,
                "The restarted batch fills both syringes together across a reload without extra fuel");
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void oneChargeFillsBothSyringesAcrossReload(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.PNEUMATIC_STAND.get().defaultBlockState());
        var stand = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
        var player = GameplayTests.player(helper);
        for (int i : new int[] {0, 2}) stand.setItem(i, Content.SYRINGE.toStack());
        stand.setItem(3, Content.CAT_NUCLEOCARD.toStack());
        stand.setItem(4, Items.BLAZE_POWDER.getDefaultInstance());
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(stand.running(), "Blaze powder cannot power the pneumatic stand");
            stand.removeItem(4, 1);
            player.openMenu(stand);
            var menu = (PneumaticStandMenu) player.containerMenu;
            helper.assertTrue(menu.slots.size() == 40, "The menu has four machine slots and thirty six inventory slots");
            helper.assertFalse(menu.getSlot(3).mayPlace(Items.BLAZE_POWDER.getDefaultInstance()), "The fuel slot rejects blaze powder");
            helper.assertFalse(menu.getSlot(2).mayPlace(Content.SALIVA_SWAB.toStack()), "The card slot rejects saliva swabs");
            player.getInventory().setItem(0, new ItemStack(Items.WIND_CHARGE, 2));
            menu.quickMoveStack(player, 31);
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
            helper.assertTrue(restored.getItem(0).is(Content.INJECTION) && restored.getItem(1).isEmpty()
                    && restored.getItem(2).is(Content.INJECTION) && restored.getItem(3).is(Content.BLANK_NUCLEOCARD)
                    && restored.getItem(4).getCount() == 1, "One card and charge fill both syringes and return a blank card after reload");
        });
        helper.runAfterDelay(450, () -> {
            var restored = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
            helper.assertFalse(restored.running(), "A blank card cannot start another cycle");
            restored.removeItem(0, 1);
            restored.setItem(2, Content.SYRINGE.toStack());
            restored.setItem(3, Content.CAT_NUCLEOCARD.toStack());
        });
        helper.runAfterDelay(870, () -> {
            var restored = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(restored.getItem(0).isEmpty() && restored.getItem(1).isEmpty()
                    && restored.getItem(2).is(Content.INJECTION) && restored.getItem(3).is(Content.BLANK_NUCLEOCARD)
                    && restored.getItem(4).isEmpty(), "The same procedure also works with only the right syringe occupied");
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
        stand.setItem(2, Content.SYRINGE.toStack());
        stand.setItem(3, Content.CAT_NUCLEOCARD.toStack());
        stand.setItem(4, Items.WIND_CHARGE.getDefaultInstance());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(stand.running(), "Processing has started");
            helper.assertTrue(stand.removeItem(3, 1).is(Content.CAT_NUCLEOCARD), "An interrupted card remains filled");
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!stand.running() && stand.getItem(0).is(Content.SYRINGE)
                            && stand.getItem(2).is(Content.SYRINGE) && stand.getItem(3).isEmpty(),
                    "Removing the card cancels progress without an injection or duplicate card");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void previousCenterSlotKeepsItsItemsAndProgress(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.PNEUMATIC_STAND.get().defaultBlockState());
        var stand = (PneumaticStandBlockEntity) level.getBlockEntity(pos);
        for (int slot = 0; slot < 3; slot++) stand.setItem(slot, Content.SYRINGE.toStack());
        stand.setItem(3, Content.CAT_NUCLEOCARD.toStack());
        stand.setItem(4, Items.WIND_CHARGE.getDefaultInstance());
        var saved = stand.saveWithFullMetadata(level.registryAccess());
        saved.remove("Targets");
        saved.putInt("Target", 1);
        saved.putInt("Progress", 200);
        var restored = (PneumaticStandBlockEntity) BlockEntity.loadStatic(pos, stand.getBlockState(), saved, level.registryAccess());
        level.removeBlockEntity(pos);
        level.setBlockEntity(restored);
        for (int tick = 0; tick < 200; tick++) PneumaticStandBlockEntity.tick(level, pos, restored.getBlockState(), restored);
        helper.assertTrue(restored.getItem(0).is(Content.SYRINGE) && restored.getItem(2).is(Content.SYRINGE)
                        && restored.getItem(1).isEmpty() && restored.getItem(3).is(Content.BLANK_NUCLEOCARD)
                        && restored.getItem(4).getCount() == 1,
                "A saved center slot cycle finishes once without spending more fuel or changing other syringes");
        helper.assertItemEntityCountIs(Content.INJECTION.get(), new BlockPos(2, 2, 2), 2, 1);
        restored.removeItem(0, 1);
        restored.setItem(1, Content.SYRINGE.toStack());
        PneumaticStandBlockEntity.tick(level, pos, restored.getBlockState(), restored);
        helper.assertTrue(restored.getItem(0).is(Content.SYRINGE) && restored.getItem(1).isEmpty(),
                "An idle center slot item moves into an available visible slot");
        helper.assertItemEntityCountIs(Content.INJECTION.get(), new BlockPos(2, 2, 2), 2, 1);
        helper.succeed();
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
