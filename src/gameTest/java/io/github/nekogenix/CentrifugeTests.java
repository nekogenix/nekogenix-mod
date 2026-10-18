package io.github.nekogenix;

import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import io.github.nekogenix.centrifuge.CentrifugeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class CentrifugeTests {
    @GameTest(template = "empty", timeoutTicks = 720)
    public static void redstoneLocksAndReload(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.CENTRIFUGE.get().defaultBlockState());
        var machine = (CentrifugeBlockEntity) level.getBlockEntity(pos);
        var player = GameplayTests.player(helper);
        for (int i = 0; i < 5; i++) machine.setItem(i, Content.SALIVA_BOTTLE.toStack());
        machine.setItem(6, Content.BLANK_NUCLEOCARD.toStack());
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(machine.running(), "Five samples do not start processing");
            machine.setItem(5, Content.SALIVA_BOTTLE.toStack());
        });
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(machine.running(), "Six samples and a card still require redstone dust");
            machine.setItem(7, new ItemStack(Items.REDSTONE, 63));
        });
        helper.runAfterDelay(15, () -> {
            helper.assertFalse(machine.running(), "Fewer than sixty four dust cannot start processing");
            helper.assertTrue(machine.getItem(7).getCount() == 63, "Insufficient fuel is not consumed");
            machine.setItem(7, new ItemStack(Items.REDSTONE, 64));
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(machine.running() && machine.getItem(7).getCount() == 64, "Fuel remains available at the beginning of processing");
            player.teleportTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            player.openMenu(machine);
            var menu = (CentrifugeMenu) player.containerMenu;
            for (var type : new ClickType[] {ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.SWAP, ClickType.THROW}) {
                menu.clicked(6, 0, type, player);
                helper.assertTrue(machine.getItem(6).is(Content.BLANK_NUCLEOCARD) && menu.getCarried().isEmpty(),
                        "All menu click paths leave processing slots unchanged");
            }
            helper.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "Shift transfer is locked");
            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
            helper.assertTrue(handler != null && handler.extractItem(0, 1, false).isEmpty(), "Automation cannot extract during processing");
            helper.assertTrue(handler.insertItem(7, Items.REDSTONE.getDefaultInstance(), false).getCount() == 1,
                    "Automation cannot insert during processing");
            helper.assertTrue(machine.removeItemNoUpdate(6).isEmpty(), "Direct container extraction also respects the lock");
            player.closeContainer();
            GameplayTests.remove(player);
        });
        helper.runAfterDelay(150, () -> {
            int progress = machine.progress();
            var saved = machine.saveWithFullMetadata(level.registryAccess());
            var restored = (CentrifugeBlockEntity) BlockEntity.loadStatic(pos, machine.getBlockState(), saved, level.registryAccess());
            helper.assertTrue(restored != null && restored.progress() == progress
                            && restored.getItem(7).getCount() == machine.getItem(7).getCount()
                            && restored.getItem(7).getCount() > 0 && restored.getItem(7).getCount() < 64,
                    "Saved block retains progress and the partially consumed stack");
            level.removeBlockEntity(pos);
            level.setBlockEntity(restored);
        });
        helper.runAfterDelay(650, () -> {
            var restored = (CentrifugeBlockEntity) level.getBlockEntity(pos);
            helper.assertFalse(restored.running(), "Reloaded batch finishes");
            for (int i = 0; i < 6; i++) helper.assertTrue(restored.getItem(i).is(Items.GLASS_BOTTLE), "Each sample becomes an empty bottle");
            helper.assertTrue(restored.getItem(6).is(Content.CAT_NUCLEOCARD) && restored.getItem(7).isEmpty(),
                    "One filled card is produced without consuming fuel twice");
            helper.assertFalse(restored.getUpdateTag(level.registryAccess()).getBoolean("Open"), "An unattended finished machine stays closed");
            helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN).extractItem(0, 1, false).is(Items.GLASS_BOTTLE),
                    "Output extraction unlocks after processing");
            level.destroyBlock(pos, true);
            helper.assertItemEntityCountIs(Content.CAT_NUCLEOCARD.get(), new BlockPos(2, 2, 2), 2, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void lastDustFinishesTheCard(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.CENTRIFUGE.get().defaultBlockState());
        var machine = (CentrifugeBlockEntity) level.getBlockEntity(pos);
        for (int slot = 0; slot < 6; slot++) machine.setItem(slot, Content.SALIVA_BOTTLE.toStack());
        machine.setItem(6, Content.BLANK_NUCLEOCARD.toStack());
        machine.setItem(7, new ItemStack(Items.REDSTONE, 64));
        for (int tick = 1; tick <= 599; tick++) {
            CentrifugeBlockEntity.tick(level, pos, machine.getBlockState(), machine);
            if (tick == 300) helper.assertTrue(machine.getItem(7).getCount() == 32, "Half a stack remains halfway through extraction");
        }
        helper.assertTrue(machine.getItem(7).getCount() == 1 && machine.getItem(6).is(Content.BLANK_NUCLEOCARD),
                "The last dust remains until the card is complete");
        CentrifugeBlockEntity.tick(level, pos, machine.getBlockState(), machine);
        helper.assertTrue(machine.getItem(7).isEmpty() && machine.getItem(6).is(Content.CAT_NUCLEOCARD),
                "The last dust and the finished card change together");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void lidFollowsViewers(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Content.CENTRIFUGE.get().defaultBlockState());
        var machine = (CentrifugeBlockEntity) level.getBlockEntity(pos);
        var first = GameplayTests.player(helper);
        var second = GameplayTests.player(helper);
        first.openMenu(machine);
        second.openMenu(machine);
        helper.assertTrue(machine.getUpdateTag(level.registryAccess()).getBoolean("Open"), "Viewing an idle machine opens its lid");
        first.closeContainer();
        helper.assertTrue(machine.getUpdateTag(level.registryAccess()).getBoolean("Open"), "Another viewer keeps the lid open");
        second.closeContainer();
        helper.assertFalse(machine.getUpdateTag(level.registryAccess()).getBoolean("Open"), "The last viewer closes the lid");
        first.openMenu(machine);
        helper.assertTrue(machine.getUpdateTag(level.registryAccess()).getBoolean("Open"), "Reopening the menu reopens the lid");
        first.containerMenu = first.inventoryMenu;
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(machine.getUpdateTag(level.registryAccess()).getBoolean("Open"), "Scheduled recheck recovers a missing close notification");
            GameplayTests.remove(first);
            GameplayTests.remove(second);
            helper.succeed();
        });
    }

}
