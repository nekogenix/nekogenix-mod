package io.github.nekogenix.pneumatic;

import io.github.nekogenix.Content;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PneumaticStandMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public PneumaticStandMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(5), new SimpleContainerData(2)); }
    public PneumaticStandMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(Content.PNEUMATIC_MENU.get(), id);
        checkContainerSize(container, 5);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        addMachineSlot(0, 56, 51);
        addMachineSlot(1, 79, 58);
        addMachineSlot(2, 102, 51);
        addMachineSlot(3, 79, 17);
        addMachineSlot(4, 17, 17);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }
    private void addMachineSlot(int index, int x, int y) {
        addSlot(new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return index < 3 ? stack.is(Content.SYRINGE) : index == 3 ? stack.is(Content.CAT_NUCLEOCARD) : stack.is(Items.WIND_CHARGE);
            }
            @Override public int getMaxStackSize() { return index == 4 ? 64 : 1; }
        });
    }
    public boolean running() { return data.get(0) > 0; }
    public float fraction() { return data.get(0) / (float) PneumaticStandBlockEntity.PROCESS_TICKS; }
    public int target() { return running() ? data.get(1) : -1; }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < 5) moved = moveItemStackTo(stack, 5, 41, true);
        else if (stack.is(Content.SYRINGE)) moved = moveItemStackTo(stack, 0, 3, false);
        else if (stack.is(Content.CAT_NUCLEOCARD)) moved = moveItemStackTo(stack, 3, 4, false);
        else if (stack.is(Items.WIND_CHARGE)) moved = moveItemStackTo(stack, 4, 5, false);
        else moved = index < 32 ? moveItemStackTo(stack, 32, 41, false) : moveItemStackTo(stack, 5, 32, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
